package com.mygame.f1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygame.f1.screens.MainMenuScreen;
import com.mygame.f1.ui.SkinFactory;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import com.mygame.f1.network.LobbyClient;
import com.mygame.f1.shared.Packets;

public class GameScreen implements Screen {
    private enum GameState { PRE_START, NORMAL, PIT_ENTERING, PIT_MINIGAME, PIT_EXITING }

    private final Main gameRef;
    private final LobbyClient lobbyClient;
    private final String roomId;
    private final int selfId;
    private final com.badlogic.gdx.utils.IntIntMap playerVehicles = new com.badlogic.gdx.utils.IntIntMap();

    // --- 재사용 변수 (성능 최적화) ---
    private final Vector2 v2_tmp1 = new Vector2();
    private final Vector2 v2_tmp2 = new Vector2();

    // --- 물리 시뮬레이션 ---
    private World world;
    private Box2DDebugRenderer box2DDebugRenderer;
    private Body playerCar;
    private float accumulator = 0;
    private static final float TIME_STEP = 1 / 60f;

    // --- 카메라 ---
    private OrthographicCamera camera;
    private Viewport viewport;
    private float cameraAngle = 0f;
    private float positionSmoothness = 5.0f;
    private float cameraRotationSmoothness = 3.0f;
    private float cameraOffsetFromCar = 0.8f;
    private Vector2 cameraTargetPosition = new Vector2();

    // --- 렌더링 ---
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private SpriteBatch batch;
    private Texture carTexture;
    private boolean ownsCarTexture = false;
    private float mapWorldWidth = 0f;
    private float mapWorldHeight = 0f;
    private Timer.Task stateSendTask;
    private final IntMap<RemoteCar> remoteCars = new IntMap<>();

    // --- HUD / 미니게임 리소스 ---
    private SpriteBatch hudBatch;
    private OrthographicCamera hudCamera;
    private BitmapFont hudFont;
    private BitmapFont hudSmallFont;
    private BitmapFont hudSpeedFont;
    private GlyphLayout layout = new GlyphLayout();
    private Texture lapBestBgTexture, lapLastBgTexture;
    private Texture minimapFrameTexture, minimapRegion, minimapCarTexture;
    private Texture durabilityLabelTexture, durabilityBgTexture, durabilityFgTexture;
    private Texture raceStatusTexture, speedHudTexture, tireLabelTexture, tireBgTexture, tireFgTexture, tireCompoundTexture;
    private Texture pitPanelTexture, pitBarTexture, pitPointerTexture, pitTyreSelectTexture;
    private Texture tireCompoundSoftTexture, tireCompoundMediumTexture, tireCompoundHardTexture;
    private Texture startLightOnTexture, startLightOffTexture;
    private boolean startLightsDone = false;
    private float startCountdown = 3.0f;
    private float goTimer = 0f;
    private float offTimer = 0f;
    private GameState gameState = GameState.PRE_START;
    // minimap 설정
    private float minimapFrameSize = 200f;
    private float minimapPadding = 16f;
    private float minimapInset = 12f;

    // pit / 미니게임 상태
    private Rectangle pitEntryRect, pitServiceRect, pitExitRect;
    private Vector2 pitServicePos = new Vector2();
    private Vector2 pitExitPos = new Vector2();
    private float pitExitAngleDeg = 0f;
    private float pitServiceAngleDeg = 0f;
    private String pitSelectedCompound = "medium";
    private float pitPointerT = 0.5f;
    private float pitPointerDir = 1f;
    private float pitPointerSpeed = 1.5f;
    private float pitServiceTimeTotal = 0f;
    private float pitServiceTimeRemaining = 0f;
    private boolean pitMiniGameLocked = false;
    private String lastPitResult = "";
    // 내구도 연동
    private float tireDurability = 100f;
    private float tireWearRate = 0.15f; // 기본 마모율 (초당 15%/100초 소진 기준)
    private int currentLap = 0;
    private int totalLaps = 3;
    private float lapTimeSeconds = 0f;
    private float bestLapTime = -1f;
    private float lastLapTime = -1f;
    private List<Checkpoint> checkpoints = new ArrayList<>();
    private Rectangle startLineBounds;
    private int totalCheckpoints = 0;
    private int lastCheckpointIndex = 0;
    private Set<Integer> checkpointsInside = new HashSet<>();
    private boolean insideStartLine = false;

    // --- Pause UI ---
    private boolean paused = false;
    private Stage pauseStage;
    private Skin pauseSkin;

    // --- 물리 파라미터 ---
    private float maxForwardSpeed = 3.5f;
    private float maxReverseSpeed = 1.5f;
    private float forwardAcceleration = 2.5f;
    private float reverseAcceleration = 1.5f;
    private float turningPower = 10f;
    private float grip = 18.0f;
    private float minSpeedForTurn = 0.8f;
    private float maxSteeringAngle = 60f;
    private float initialAngle = 0f;
    private float currentAcceleration = 0f;
    private float accelerationSmoothness = 7f;
    private float currentTorque = 0f;
    private float torqueSmoothness = 15f;
    private float defaultLinearDamping = 2.0f;
    private float brakingLinearDamping = 5.0f;
    private float collisionDamping = 4.0f;

    // --- 충돌 감지 ---
    private boolean isColliding = false;
    private boolean wasColliding = false;
    private float collisionTimer = 0f;
    private float collisionDuration = 0.2f;

    // --- 상수 ---
    public static final float PPM = 100;
    private static final boolean USE_TILED_MAP = true;
    private static final String MAP_PATH = "f1_racing_map.tmx";
    // Use full path because AssetManager loads under cars/
    private static final String CAR_PATH = "cars/Silverline S11.png";
    // 고정 렌더 크기(로컬/원격 동일 적용)
    private static final float CAR_DRAW_WIDTH = (12.80f * 1.7f) / PPM;
    private static final float CAR_DRAW_HEIGHT = (25.60f * 1.7f) / PPM;
    private static final float VIEW_WIDTH = 1600f / PPM;
    private static final float VIEW_HEIGHT = 900f / PPM;
    // 멀티플레이 시 기본 스폰 슬롯(첫 번째 맵 기준 요청 값)
    private static final Vector2[] GRID_SPAWNS = new Vector2[]{
            new Vector2((87 * 32) / PPM, ((120 - 92) * 32) / PPM), // p1
            new Vector2((90 * 32) / PPM, ((120 - 92) * 32) / PPM), // p2
            new Vector2((87 * 32) / PPM, ((120 - 94) * 32) / PPM), // p3
            new Vector2((90 * 32) / PPM, ((120 - 94) * 32) / PPM)  // p4
    };
    private static final String[] CAR_PATHS = {
            "cars/Astra A4.png",
            "cars/Boltworks RX-1.png",
            "cars/Emerald E7.png",
            "cars/Gold Rush GT.png",
            "cars/Midnight P4.png",
            "cars/Silverline S11.png"
    };

    private final String mapPathOverride;
    private final String carPathOverride;

    public GameScreen() { this(null, null, null, null, null, -1, null); }

    public GameScreen(Main game) { this(game, null, null, null, null, -1, null); }

    public GameScreen(Main game, String mapPath, String carPath) { this(game, mapPath, carPath, null, null, -1, null); }

    public GameScreen(Main game, String mapPath, String carPath, LobbyClient lobbyClient, String roomId, int selfId, com.badlogic.gdx.utils.IntIntMap vehicles) {
        this.gameRef = game;
        this.mapPathOverride = mapPath;
        this.carPathOverride = carPath;
        this.lobbyClient = lobbyClient;
        this.roomId = roomId;
        this.selfId = selfId;
        if (vehicles != null) {
            this.playerVehicles.putAll(vehicles);
        }
    }

    @Override
    public void show() {
        long t0 = System.nanoTime();
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_WIDTH * 2f, VIEW_HEIGHT * 2f, camera);
        camera.zoom = 0.35f; // closer view

        batch = new SpriteBatch();
        hudBatch = new SpriteBatch();
        // AssetManager를 통해 에셋 로드 (선택한 차량 경로 우선)
        String carPath = carPathOverride != null ? carPathOverride : CAR_PATH;
        long assetStart = System.nanoTime();
        if (Main.assetManager.isLoaded(carPath, Texture.class)) {
            carTexture = Main.assetManager.get(carPath, Texture.class);
        } else {
            try { carTexture = new Texture(Gdx.files.internal(carPath)); ownsCarTexture = true; }
            catch (Exception ignored) { carTexture = Main.assetManager.get(CAR_PATH, Texture.class); }
        }
        Gdx.app.log("PERF", String.format("GameScreen car texture load: %.2f ms", (System.nanoTime() - assetStart) / 1_000_000f));

        world = new World(new Vector2(0, 0), true);
        box2DDebugRenderer = new Box2DDebugRenderer();

        // 충돌 리스너 설정
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                if (contact.getFixtureA().getBody() == playerCar || contact.getFixtureB().getBody() == playerCar) {
                    isColliding = true;
                    collisionTimer = collisionDuration;
                    Vector2 velocity = playerCar.getLinearVelocity();
                    playerCar.setLinearVelocity(velocity.scl(0.4f));
                    playerCar.setAngularVelocity(playerCar.getAngularVelocity() * 0.3f);
                }
            }

            @Override
            public void endContact(Contact contact) {}

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {}

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {}
        });

        if (USE_TILED_MAP) {
            long mapStart = System.nanoTime();
            String mapPath = mapPathOverride != null ? mapPathOverride : MAP_PATH;
            // 일부 환경에서 classpath 로드 시 타일셋 내부 리소스가 누락되어 발생하는 경우가 있어
            // 내부/로컬 경로 모두 확인 후 존재하는 쪽으로 로드한다.
            TmxMapLoader loader;
            com.badlogic.gdx.files.FileHandle mapHandle = Gdx.files.internal(mapPath);
            if (!mapHandle.exists()) {
                mapHandle = Gdx.files.local(mapPath);
            }
            if (mapHandle.type() == com.badlogic.gdx.Files.FileType.Internal) {
                loader = new TmxMapLoader();
            } else {
                loader = new TmxMapLoader(new LocalFileHandleResolver());
            }
            map = loader.load(mapHandle.path());
            System.out.println(mapHandle.path());
            mapRenderer = new OrthogonalTiledMapRenderer(map, 1 / PPM);
            // derive map world size from TMX
            int tilesX = map.getProperties().get("width", Integer.class);
            int tilesY = map.getProperties().get("height", Integer.class);
            int tileW = map.getProperties().get("tilewidth", Integer.class);
            int tileH = map.getProperties().get("tileheight", Integer.class);
            mapWorldWidth = tilesX * tileW / PPM;
            mapWorldHeight = tilesY * tileH / PPM;
            System.out.println("H : " + mapWorldHeight + ", W : " + mapWorldWidth);
            if (mapWorldWidth > 0 && mapWorldHeight > 0) {
                viewport.setWorldSize(mapWorldWidth, mapWorldHeight);
                camera.position.set(mapWorldWidth / 2f, mapWorldHeight / 2f, 0);
                camera.update();
            }

            if (map.getLayers().get("Collision") != null) {
                //TODO: TO BE ERASED
                System.out.println("Collision detected.");
                for (RectangleMapObject object : map.getLayers().get("Collision").getObjects().getByType(RectangleMapObject.class)) {
                    Rectangle rect = object.getRectangle();
                    createWall(rect.x / PPM, rect.y / PPM, rect.width / PPM, rect.height / PPM);
                }
            }
            parsePitLayer();
            loadCheckpointsFromMap();
            loadStartLineFromMap();
            Gdx.app.log("PERF", String.format("Map load+parse: %.2f ms", (System.nanoTime() - mapStart) / 1_000_000f));
        }

        long worldStart = System.nanoTime();
        createPlayerCar();
        createScreenBoundaryWalls();
        Gdx.app.log("PERF", String.format("GameScreen world/walls: %.2f ms", (System.nanoTime() - worldStart) / 1_000_000f));

        initialAngle = playerCar.getAngle();

        // HUD 카메라/폰트/텍스처 초기화
        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera.update();
        long hudStart = System.nanoTime();
        initHudResources();
        Gdx.app.log("PERF", String.format("GameScreen HUD init: %.2f ms", (System.nanoTime() - hudStart) / 1_000_000f));

        // pause UI init
        pauseSkin = SkinFactory.createDefaultSkin();
        pauseStage = new Stage(new ScreenViewport());

        // 네트워크 전송/수신 설정
        if (lobbyClient != null && roomId != null) {
            lobbyClient.onGameState(this::handleGameState);
            stateSendTask = Timer.schedule(new Timer.Task() {
                @Override public void run() { sendState(); }
            }, 0.05f, 0.05f);
        }

        // 스타트 카운트 초기화
        gameState = GameState.PRE_START;
        startCountdown = 3.0f;
        goTimer = 0f;
        offTimer = 0f;
        startLightsDone = false;
            lapTimeSeconds = 0f;
            lastLapTime = -1f;
            bestLapTime = -1f;

        Gdx.app.log("PERF", String.format("GameScreen.show total: %.2f ms", (System.nanoTime() - t0) / 1_000_000f));
    }

    private void createWall(float x, float y, float width, float height) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(x + width / 2, y + height / 2);
        Body body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2, height / 2);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.friction = 0.3f;
        fixtureDef.restitution = 0.02f;

        body.createFixture(fixtureDef);
        shape.dispose();
    }

    private void createScreenBoundaryWalls() {
        float worldWidth = mapWorldWidth > 0 ? mapWorldWidth : viewport.getWorldWidth();
        float worldHeight = mapWorldHeight > 0 ? mapWorldHeight : viewport.getWorldHeight();
        float wallThickness = 0.05f;
        // Place walls exactly on map bounds
        // bottom / top
        createWall(0f, 0f, worldWidth, wallThickness);
        createWall(0f, worldHeight - wallThickness, worldWidth, wallThickness);
        // left / right
        createWall(0f, 0f, wallThickness, worldHeight);
        createWall(worldWidth - wallThickness, 0f, wallThickness, worldHeight);
    }

    private void createPlayerCar() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        Vector2 spawn = computeSpawnPosition();
        bodyDef.position.set(spawn);
        bodyDef.linearDamping = defaultLinearDamping;
        bodyDef.angularDamping = 20.0f;
        playerCar = world.createBody(bodyDef);

        FixtureDef fixtureDef = new FixtureDef();
        PolygonShape carShape = new PolygonShape();
        carShape.setAsBox(6.40f / PPM, 12.80f / PPM);
        fixtureDef.shape = carShape;
        fixtureDef.density = 3.5f;
        fixtureDef.friction = 0.3f;
        fixtureDef.restitution = 0.02f;

        playerCar.createFixture(fixtureDef);
        carShape.dispose();
    }

    private Vector2 computeSpawnPosition() {
        // 기본값: 맵 중앙
        float fallbackX = mapWorldWidth > 0 ? mapWorldWidth / 2f : 960 / PPM;
        float fallbackY = mapWorldHeight > 0 ? mapWorldHeight / 2f : 640 / PPM;

        // 멀티플레이일 때 플레이어 ID 순으로 정렬해 슬롯을 배정
        if (roomId != null && selfId >= 0) {
            IntArray ids = playerVehicles.size > 0 ? playerVehicles.keys().toArray() : new IntArray(new int[]{selfId});
            ids.sort();
            int idx = ids.indexOf(selfId);
            if (idx >= 0) {
                Vector2 slot = GRID_SPAWNS[idx % GRID_SPAWNS.length];
                return new Vector2(slot);
            }
        }
        return new Vector2(fallbackX, fallbackY);
    }

    public void update(float delta) {
        float frameTime = Math.min(delta, 0.25f);
        accumulator += frameTime;
        while (accumulator >= TIME_STEP) {
            world.step(TIME_STEP, 8, 3);
            accumulator -= TIME_STEP;
        }
        updateRemoteCars(delta);

        if (collisionTimer > 0) {
            collisionTimer -= delta;
            if (collisionTimer <= 0) {
                wasColliding = isColliding;
                isColliding = false;
            }
        }

        handleInput(delta);
        updateSteering(delta);
        updateFriction();
        limitSpeed();

        handlePitState(delta);
        updateLapAndCheckpoints(delta);

        if (isColliding) {
            playerCar.setLinearDamping(collisionDamping);
            if (playerCar.getLinearVelocity().len() < 0.5f) {
                Vector2 forwardDirection = playerCar.getWorldVector(new Vector2(0, -0.3f));
                playerCar.applyLinearImpulse(forwardDirection, playerCar.getWorldCenter(), true);
            }
        }

        Vector2 forwardVector = new Vector2(0, cameraOffsetFromCar);
        Vector2 worldSpaceOffset = playerCar.getWorldVector(forwardVector);
        Vector2 targetPosition = new Vector2(playerCar.getPosition()).add(worldSpaceOffset);

        camera.position.x = MathUtils.lerp(camera.position.x, targetPosition.x, positionSmoothness * delta);
        camera.position.y = MathUtils.lerp(camera.position.y, targetPosition.y, positionSmoothness * delta);

        float targetAngle = -playerCar.getAngle() * MathUtils.radiansToDegrees;
        cameraAngle = MathUtils.lerpAngleDeg(cameraAngle, targetAngle, cameraRotationSmoothness * delta);

        camera.up.set(0, 1, 0);
        camera.direction.set(0, 0, -1);
        camera.rotate(cameraAngle);
        camera.update();

        if (USE_TILED_MAP && mapRenderer != null) {
            mapRenderer.setView(camera);
        }
    }

    private void handleInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            playerCar.setLinearDamping(brakingLinearDamping);
        } else if (!isColliding) {
            playerCar.setLinearDamping(defaultLinearDamping);
        }

        Vector2 forwardNormal = playerCar.getWorldVector(new Vector2(0, 1));
        float forwardSpeed = playerCar.getLinearVelocity().dot(forwardNormal);
        float currentSpeed = playerCar.getLinearVelocity().len();

        float targetAcceleration = 0;
        boolean movingForward = false;
        boolean movingReverse = false;

        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            targetAcceleration = forwardAcceleration;
            movingForward = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            targetAcceleration = -reverseAcceleration;
            movingReverse = true;
        }

        currentAcceleration = MathUtils.lerp(currentAcceleration, targetAcceleration, accelerationSmoothness * delta);

        if (Math.abs(currentAcceleration) > 0.1f) {
            Vector2 forceVector = playerCar.getWorldVector(new Vector2(0, currentAcceleration));
            playerCar.applyForceToCenter(forceVector, true);
        }

        if (currentSpeed >= minSpeedForTurn && (movingForward || movingReverse)) {
            if (movingForward && forwardSpeed > 0.5f) {
                // speedFactor logic was here, seems incomplete in original, handled in updateSteering
            } else if (movingReverse && forwardSpeed < -0.5f) {
                // speedFactor logic was here, seems incomplete in original, handled in updateSteering
            }
        }
    }

    private void updateSteering(float delta) {
        float targetAngularVelocity = 0;
        float maxAngularVelocity = MathUtils.degreesToRadians * 190;

        Vector2 forwardNormal = playerCar.getWorldVector(new Vector2(0, 1));
        float forwardSpeed = playerCar.getLinearVelocity().dot(forwardNormal);
        boolean movingForward = Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W);
        boolean movingReverse = Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S);

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            targetAngularVelocity = (movingReverse && forwardSpeed < -0.5f) ? -maxAngularVelocity : maxAngularVelocity;
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            targetAngularVelocity = (movingReverse && forwardSpeed < -0.5f) ? maxAngularVelocity : -maxAngularVelocity;
        }

        float currentAngularVelocity = playerCar.getAngularVelocity();
        float velocityChange = targetAngularVelocity - currentAngularVelocity;
        float impulse = playerCar.getInertia() * velocityChange;
        playerCar.applyAngularImpulse(impulse, true);
    }

    private void updateFriction() {
        boolean isSteering = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.D);
        float gripFactor = isSteering ? 0.9f : 1.0f;

        Vector2 lateralVelocity = getLateralVelocity();
        v2_tmp1.set(lateralVelocity).scl(-playerCar.getMass() * grip * gripFactor);
        playerCar.applyForceToCenter(v2_tmp1, true);

        Vector2 forwardVelocity = getForwardVelocity();
        float forwardSpeed = forwardVelocity.len();
        if (forwardSpeed > 0.1f) {
            float dragCoefficient = 0.05f * forwardSpeed;
            v2_tmp1.set(forwardVelocity).scl(-dragCoefficient * playerCar.getMass());
            playerCar.applyForceToCenter(v2_tmp1, true);
        }
    }

    private void limitSpeed() {
        Vector2 forwardNormal = playerCar.getWorldVector(new Vector2(0, 1));
        float forwardSpeed = playerCar.getLinearVelocity().dot(forwardNormal);
        float speed = playerCar.getLinearVelocity().len();

        if (forwardSpeed > 0 && speed > maxForwardSpeed) {
            playerCar.setLinearVelocity(playerCar.getLinearVelocity().scl(maxForwardSpeed / speed));
        } else if (forwardSpeed < 0 && speed > maxReverseSpeed) {
            playerCar.setLinearVelocity(playerCar.getLinearVelocity().scl(maxReverseSpeed / speed));
        }
    }

    private Vector2 getLateralVelocity() {
        v2_tmp1.set(playerCar.getWorldVector(new Vector2(1, 0)));
        float rightSpeed = playerCar.getLinearVelocity().dot(v2_tmp1);
        return v2_tmp1.scl(rightSpeed);
    }

    private Vector2 getForwardVelocity() {
        v2_tmp2.set(playerCar.getWorldVector(new Vector2(0, 1)));
        float forwardSpeed = playerCar.getLinearVelocity().dot(v2_tmp2);
        return v2_tmp2.scl(forwardSpeed);
    }

    @Override
    public void render(float delta) {
        if (paused && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }
        if (!paused) {
            update(delta);
            updateStartLights(delta);
            updateDurability(delta);
        }

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (USE_TILED_MAP && mapRenderer != null) {
            mapRenderer.setView(camera);
            mapRenderer.render();
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        Vector2 carPos = playerCar.getPosition();
        float carWidth = CAR_DRAW_WIDTH;
        float carHeight = CAR_DRAW_HEIGHT;
        batch.draw(carTexture,
            carPos.x - carWidth / 2, carPos.y - carHeight / 2,
            carWidth / 2, carHeight / 2,
            carWidth, carHeight,
            1, 1,
            playerCar.getAngle() * MathUtils.radiansToDegrees,
            0, 0, carTexture.getWidth(), carTexture.getHeight(),
            false, false);

        batch.end();

        // 원격 차량 렌더링
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (IntMap.Entry<RemoteCar> e : remoteCars) {
            RemoteCar rc = e.value;
            Texture tex = rc.texture != null ? rc.texture : carTexture;
            float w = CAR_DRAW_WIDTH;
            float h = CAR_DRAW_HEIGHT;
            batch.draw(tex,
                    rc.position.x - w / 2f, rc.position.y - h / 2f,
                    w / 2f, h / 2f,
                    w, h,
                    1f, 1f,
                    rc.rotation * MathUtils.radiansToDegrees,
                    0, 0, tex.getWidth(), tex.getHeight(), false, false);
        }
        batch.end();

        box2DDebugRenderer.render(world, camera.combined);

        if (paused) {
            drawPauseOverlay();
        }

        // HUD (간단 표시: 속도/레이스 상태/내구도)
        drawHud(delta);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        if (pauseStage != null) pauseStage.getViewport().update(width, height, true);
        if (hudCamera != null) {
            hudCamera.setToOrtho(false, width, height);
            hudCamera.update();
        }
    }

    @Override
    public void dispose() {
        if (stateSendTask != null) stateSendTask.cancel();
        if (world != null) world.dispose();
        if (box2DDebugRenderer != null) box2DDebugRenderer.dispose();
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (batch != null) batch.dispose();
        if (hudBatch != null) hudBatch.dispose();
        if (pauseStage != null) pauseStage.dispose();
        if (pauseSkin != null) pauseSkin.dispose();
        if (ownsCarTexture && carTexture != null) carTexture.dispose();
        disposeFont(hudFont, hudSmallFont, hudSpeedFont);
        disposeTex(lapBestBgTexture, lapLastBgTexture, minimapFrameTexture, minimapRegion, minimapCarTexture,
                durabilityLabelTexture, durabilityBgTexture, durabilityFgTexture, raceStatusTexture, speedHudTexture,
                tireLabelTexture, tireBgTexture, tireFgTexture, tireCompoundTexture,
                pitPanelTexture, pitBarTexture, pitPointerTexture, pitTyreSelectTexture,
                tireCompoundSoftTexture, tireCompoundMediumTexture, tireCompoundHardTexture,
                startLightOnTexture, startLightOffTexture);
        if (lobbyClient != null) lobbyClient.onGameState(null);
        for (IntMap.Entry<RemoteCar> e : remoteCars) {
            if (e.value.textureOwned && e.value.texture != null) e.value.texture.dispose();
        }
    }

    private void drawHud(float delta) {
        if (hudBatch == null || hudCamera == null) return;
        hudCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera.update();
        hudBatch.setProjectionMatrix(hudCamera.combined);

        hudBatch.begin();
        drawRaceStatusHud();
        drawSpeedHud();
        drawDurabilityHud();
        drawTireHud();
        drawMinimapHud();
        drawStartLightsHud();
        drawPitMinigameHud();
        drawLapTimeHud();
        hudBatch.end();
    }

    private void drawRaceStatusHud() {
        if (raceStatusTexture == null || hudFont == null || hudCamera == null) return;
        float padding = 16f;
        float texH = raceStatusTexture.getHeight();
        float x = hudCamera.viewportWidth - raceStatusTexture.getWidth() - padding;
        float y = hudCamera.viewportHeight - texH - padding - 10f;
        hudBatch.draw(raceStatusTexture, x, y);
        String status = gameRef != null ? gameRef.playerName : "PLAYER";
        layout.setText(hudFont, status);
        hudFont.draw(hudBatch, status, x + 12, y + texH - 12);
        // 랩 정보를 플레이어 정보 오른쪽 60px에 배치
        String lapTxt = String.format("LAP %d/%d", currentLap, totalLaps);
        layout.setText(hudFont, lapTxt);
        hudFont.draw(hudBatch, lapTxt, x + raceStatusTexture.getWidth() + 60f, y + texH - 12);
    }

    private void drawSpeedHud() {
        if (speedHudTexture == null || hudCamera == null || hudSpeedFont == null || playerCar == null) return;
        float padding = 16f;
        float texW = speedHudTexture.getWidth();
        float texH = speedHudTexture.getHeight();
        float x = (hudCamera.viewportWidth - texW) * 0.5f;
        float y = padding;
        hudBatch.draw(speedHudTexture, x, y);
        float speed = playerCar.getLinearVelocity().len() * PPM * 1.1f; // km/h
        String txt = String.format("%.0f", speed);
        layout.setText(hudSpeedFont, txt);
        hudSpeedFont.draw(hudBatch, txt,
                x + texW * 0.5f - layout.width * 0.5f + 20f,
                y + texH * 0.55f + layout.height * 0.5f + 20f);
    }

    private void drawDurabilityHud() {
        if (durabilityBgTexture == null || durabilityFgTexture == null || hudCamera == null) return;
        float padding = 16f;
        float bgW = durabilityBgTexture.getWidth();
        float bgH = durabilityBgTexture.getHeight();
        float y = padding;
        float x = hudCamera.viewportWidth - bgW - padding;
        hudBatch.draw(durabilityBgTexture, x, y);
        // 내구도 비율에 따라 FG 폭 스케일
        float ratio = MathUtils.clamp(tireDurability / 100f, 0f, 1f);
        if (durabilityFgTexture != null) {
            float fgW = durabilityFgTexture.getWidth() * ratio;
            float fgH = durabilityFgTexture.getHeight();
            hudBatch.draw(durabilityFgTexture, x, y, fgW, fgH);
        }
        if (durabilityLabelTexture != null) {
            hudBatch.draw(durabilityLabelTexture, x, y + bgH + 8f);
        }
        if (hudSmallFont != null) {
            String val = String.format("%.0f%%", MathUtils.clamp(tireDurability, 0f, 100f));
            layout.setText(hudSmallFont, val);
            hudSmallFont.draw(hudBatch, val, x + 12, y + bgH - 8);
        }
    }

    private void drawTireHud() {
        if (tireBgTexture == null || tireFgTexture == null || hudCamera == null) return;
        float padding = 16f;
        float bgW = tireBgTexture.getWidth();
        float bgH = tireBgTexture.getHeight();
        float bgX = padding;
        float bgY = padding;

        hudBatch.draw(tireBgTexture, bgX, bgY);
        hudBatch.draw(tireFgTexture, bgX, bgY);
        if (tireCompoundTexture != null) {
            hudBatch.draw(tireCompoundTexture, bgX, bgY);
        }
    }

    private void drawPitMinigameHud() {
        if (gameState != GameState.PIT_MINIGAME) return;
        if (pitPanelTexture == null || pitBarTexture == null || hudCamera == null) return;
        float panelW = pitPanelTexture.getWidth();
        float panelH = pitPanelTexture.getHeight();
        float panelX = (hudCamera.viewportWidth - panelW) * 0.5f;
        float panelY = (hudCamera.viewportHeight - panelH) * 0.5f;

        float barW = pitBarTexture.getWidth();
        float barH = pitBarTexture.getHeight();
        float barX = (hudCamera.viewportWidth - barW) * 0.5f;
        float barY = panelY - 42f;

        BitmapFont font = hudFont != null ? hudFont : new BitmapFont();

        hudBatch.draw(pitPanelTexture, panelX, panelY);
        hudBatch.draw(pitBarTexture, barX, barY);
        float pointerX = barX + pitPointerT * barW;
        if (pitPointerTexture != null) {
            hudBatch.draw(pitPointerTexture, pointerX - pitPointerTexture.getWidth() / 2f, barY - 10f);
        }

        font.draw(hudBatch, "Pit: " + pitSelectedCompound.toUpperCase(), panelX + 24f, panelY + panelH - 20f);
        font.draw(hudBatch, "Result: " + lastPitResult, panelX + 24f, panelY + 60f);
        if (pitTyreSelectTexture != null) {
            float selW = pitTyreSelectTexture.getWidth();
            float selH = pitTyreSelectTexture.getHeight();
            float selX = panelX + (panelW - selW) * 0.5f;
            float selY = panelY + 12f;
            hudBatch.draw(pitTyreSelectTexture, selX, selY);
            font.draw(hudBatch, "Q/W/E = SOFT/MEDIUM/HARD", selX + 12f, selY + selH - 12f);
        }
        if (pitServiceTimeRemaining > 0f) {
            font.draw(hudBatch, String.format("Service: %.1fs", Math.max(0f, pitServiceTimeRemaining)), barX, barY - 10f);
        } else {
            font.draw(hudBatch, "Press ENTER/SPACE", barX, barY - 10f);
        }
    }

    private void drawLapTimeHud() {
        if (lapBestBgTexture == null || lapLastBgTexture == null || hudCamera == null || hudFont == null) return;
        float padding = 12f;
        float bestW = lapBestBgTexture.getWidth();
        float bestH = lapBestBgTexture.getHeight();
        float lastW = lapLastBgTexture.getWidth();
        float lastH = lapLastBgTexture.getHeight();

        float bestX = hudCamera.viewportWidth - bestW - padding - 10f;
        float bestY = hudCamera.viewportHeight - raceStatusTexture.getHeight() - bestH - padding * 2f - 20f;
        float lastX = bestX;
        float lastY = bestY - lastH - 8f;

        hudBatch.draw(lapBestBgTexture, bestX, bestY);
        hudBatch.draw(lapLastBgTexture, lastX, lastY);

        String bestTxt = bestLapTime > 0 ? formatLap(bestLapTime) : "--:--";
        String lastTxt = lastLapTime > 0 ? formatLap(lastLapTime) : "--:--";
        hudFont.draw(hudBatch, "BEST", bestX + 12, bestY + bestH - 12);
        hudFont.draw(hudBatch, bestTxt, bestX + 12, bestY + bestH / 2f);
        hudFont.draw(hudBatch, "LAST", lastX + 12, lastY + lastH - 12);
        hudFont.draw(hudBatch, lastTxt, lastX + 12, lastY + lastH / 2f);
    }

    private String formatLap(float seconds) {
        int total = MathUtils.floor(seconds);
        int m = total / 60;
        int s = total % 60;
        int ms = MathUtils.floor((seconds - total) * 1000f);
        return String.format("%02d:%02d.%03d", m, s, ms);
    }

    private void drawMinimapHud() {
        if (minimapFrameTexture == null || hudCamera == null) return;
        float frameX = hudCamera.viewportWidth - minimapFrameSize - minimapPadding;
        float frameY = hudCamera.viewportHeight - minimapFrameSize - minimapPadding;
        float hudW = minimapFrameSize - (minimapInset * 2f);
        float hudH = hudW;
        float mapW = mapWorldWidth > 0 ? mapWorldWidth : hudW;
        float mapH = mapWorldHeight > 0 ? mapWorldHeight : hudH;
        float scaleX = hudW / mapW;
        float scaleY = hudH / mapH;

        hudBatch.draw(minimapFrameTexture, frameX, frameY, minimapFrameSize, minimapFrameSize);

        // 맵 영역 텍스처(있을 경우) 표시
        if (minimapRegion != null) {
            hudBatch.draw(minimapRegion, frameX + minimapInset, frameY + minimapInset, hudW, hudH);
        }
        // 플레이어(자신) 점 표시
        if (minimapCarTexture != null && playerCar != null) {
            Vector2 pos = playerCar.getPosition();
            float px = frameX + minimapInset + pos.x * scaleX;
            float py = frameY + minimapInset + pos.y * scaleY;
            hudBatch.draw(minimapCarTexture, px - minimapCarTexture.getWidth() / 2f, py - minimapCarTexture.getHeight() / 2f);
        }
    }

    private void drawStartLightsHud() {
        if (startLightsDone || hudCamera == null) return;
        if (startLightOnTexture == null || startLightOffTexture == null) return;
        if (startCountdown > 3.1f) return; // 아직 준비 안 됨

        float paddingTop = 40f;
        float gap = 12f;
        float drawW = startLightOnTexture.getWidth();
        float drawH = startLightOnTexture.getHeight();
        float totalW = drawW * 4f + gap * 3f;
        float startX = (hudCamera.viewportWidth - totalW) * 0.5f;
        float y = hudCamera.viewportHeight - drawH - paddingTop;

        // 표시할 상태 계산: 3->2->1->GO 후 offTimer
        Texture[] lamps = new Texture[]{startLightOffTexture, startLightOffTexture, startLightOffTexture, startLightOffTexture};
        if (startCountdown > 2f) {
            lamps[0] = startLightOnTexture;
        } else if (startCountdown > 1f) {
            lamps[0] = startLightOnTexture; lamps[1] = startLightOnTexture;
        } else if (startCountdown > 0f) {
            lamps[0] = startLightOnTexture; lamps[1] = startLightOnTexture; lamps[2] = startLightOnTexture;
        } else if (goTimer > 0f) {
            lamps[0] = startLightOnTexture; lamps[1] = startLightOnTexture; lamps[2] = startLightOnTexture; lamps[3] = startLightOnTexture;
        } else if (offTimer > 0f) {
            lamps[0] = startLightOffTexture; lamps[1] = startLightOffTexture; lamps[2] = startLightOffTexture; lamps[3] = startLightOffTexture;
        }

        for (int i = 0; i < 4; i++) {
            float x = startX + i * (drawW + gap);
            hudBatch.draw(lamps[i], x, y);
        }
    }

    private void updateStartLights(float delta) {
        if (startLightsDone) return;
        if (startCountdown > 0f) {
            startCountdown -= delta;
            if (startCountdown <= 0f) {
                startCountdown = 0f;
                goTimer = 0.8f; // GO 표시 시간
            }
        } else if (goTimer > 0f) {
            goTimer -= delta;
            if (goTimer <= 0f) {
                goTimer = 0f;
                offTimer = 0.5f; // ALL OFF 시간
            }
        } else if (offTimer > 0f) {
            offTimer -= delta;
            if (offTimer <= 0f) {
                offTimer = 0f;
                startLightsDone = true;
                gameState = GameState.NORMAL;
            }
        }
    }

    private void updateDurability(float delta) {
        if (gameState != GameState.NORMAL) return;
        if (playerCar == null) return;
        float speed = playerCar.getLinearVelocity().len() * PPM; // units/sec
        if (speed < 0.5f) return;
        tireDurability = MathUtils.clamp(tireDurability - getCompoundWearRate() * delta, 0f, 100f);
    }

    private float getCompoundWearRate() {
        if ("soft".equalsIgnoreCase(pitSelectedCompound)) return 100f / 50f;  // 50초에 100% 소모
        if ("hard".equalsIgnoreCase(pitSelectedCompound)) return 100f / 90f;  // 90초에 100% 소모
        return 100f / 70f; // medium default
    }

    private void handlePitState(float delta) {
        if (gameState == GameState.NORMAL) {
            if (pitEntryRect != null && pitEntryRect.contains(playerCar.getPosition())) {
                gameState = GameState.PIT_ENTERING;
                playerCar.setLinearVelocity(0, 0);
            }
        } else if (gameState == GameState.PIT_ENTERING) {
            if (pitServicePos != null) {
                Vector2 pos = pitServicePos.cpy();
                float angRad = pitServiceAngleDeg * MathUtils.degreesToRadians;
                playerCar.setTransform(pos, angRad);
                pitSelectedCompound = "medium";
                pitPointerT = 0.5f; pitPointerDir = 1f; pitPointerSpeed = 1.5f;
                pitMiniGameLocked = false;
                pitServiceTimeTotal = 0f;
                pitServiceTimeRemaining = 0f;
                lastPitResult = "";
                gameState = GameState.PIT_MINIGAME;
            } else {
                gameState = GameState.NORMAL;
            }
        } else if (gameState == GameState.PIT_MINIGAME) {
            if (!pitMiniGameLocked) {
                pitPointerT += pitPointerSpeed * pitPointerDir * delta;
                if (pitPointerT > 1f) { pitPointerT = 1f; pitPointerDir = -1f; }
                if (pitPointerT < 0f) { pitPointerT = 0f; pitPointerDir = 1f; }
                // 타이어 선택: 1/2/3 또는 Q/W/E
                if (keyHit(Input.Keys.NUM_1, Input.Keys.NUMPAD_1, Input.Keys.Q)) pitSelectedCompound = "soft";
                if (keyHit(Input.Keys.NUM_2, Input.Keys.NUMPAD_2, Input.Keys.W)) pitSelectedCompound = "medium";
                if (keyHit(Input.Keys.NUM_3, Input.Keys.NUMPAD_3, Input.Keys.E)) pitSelectedCompound = "hard";

                if (keyHit(Input.Keys.ENTER, Input.Keys.SPACE)) {
                    float baseServiceTime = 3.0f;
                    float grade = pitPointerT;
                    if (grade >= 0.40f && grade <= 0.60f) { lastPitResult = "Perfect"; pitServiceTimeTotal = baseServiceTime * 0.7f; }
                    else if ((grade >= 0.25f && grade < 0.40f) || (grade > 0.60f && grade <= 0.75f)) { lastPitResult = "Good"; pitServiceTimeTotal = baseServiceTime; }
                    else { lastPitResult = "Bad"; pitServiceTimeTotal = baseServiceTime * 1.5f; }
                    pitServiceTimeRemaining = pitServiceTimeTotal;
                    pitPointerSpeed = 0f;
                    pitMiniGameLocked = true;
                    setTireCompound(pitSelectedCompound);
                }
            }
            if (pitServiceTimeRemaining > 0f) {
                pitServiceTimeRemaining -= delta;
                if (pitServiceTimeRemaining <= 0f) {
                    pitServiceTimeRemaining = 0f;
                    lapTimeSeconds = 0f; // PIT 후 랩 타임 리셋
                    gameState = GameState.PIT_EXITING;
                }
            }
        } else if (gameState == GameState.PIT_EXITING) {
            if (pitExitPos != null) {
                Vector2 pos = playerCar.getPosition();
                Vector2 dir = new Vector2(pitExitPos).sub(pos);
                if (dir.len() > 0.01f) {
                    dir.nor().scl(0.5f);
                    playerCar.setLinearVelocity(dir);
                } else {
                    playerCar.setTransform(pitExitPos, pitExitAngleDeg * MathUtils.degreesToRadians);
                    gameState = GameState.NORMAL;
                }
            } else {
                gameState = GameState.NORMAL;
            }
        }
    }

    private boolean keyHit(int... keys) {
        for (int k : keys) if (Gdx.input.isKeyJustPressed(k)) return true;
        return false;
    }

    private void setTireCompound(String comp) {
        if ("soft".equalsIgnoreCase(comp) && tireCompoundSoftTexture != null) {
            tireCompoundTexture = tireCompoundSoftTexture;
            tireWearRate = 0.22f;
        } else if ("hard".equalsIgnoreCase(comp) && tireCompoundHardTexture != null) {
            tireCompoundTexture = tireCompoundHardTexture;
            tireWearRate = 0.12f;
        } else if (tireCompoundMediumTexture != null) {
            tireCompoundTexture = tireCompoundMediumTexture;
            tireWearRate = 0.15f;
        }
        pitSelectedCompound = comp;
    }

    private void initHudResources() {
        hudFont = loadFont("fonts/capitolcity.ttf", 18);
        hudSmallFont = loadFont("fonts/capitolcity.ttf", 14);
        hudSpeedFont = loadFont("fonts/capitolcity.ttf", 26);

        lapBestBgTexture = loadTextureSafe("ui/laptime/lap_time_bg_best.png");
        lapLastBgTexture = loadTextureSafe("ui/laptime/lap_time_bg_last.png");
        minimapFrameTexture = loadTextureSafe("hud/minimap_frame_bg.png");
        minimapRegion = loadTextureSafe("hud/minimap.png");
        durabilityLabelTexture = loadTextureSafe("ui/durability/vehicle_durability_label.png");
        durabilityBgTexture = loadTextureSafe("ui/durability/vehicle_durability_bg.png");
        durabilityFgTexture = loadTextureSafe("ui/durability/vehicle_durability_fg.png");
        raceStatusTexture = loadTextureSafe("hud/race_status_bg.png");
        speedHudTexture = loadTextureSafe("ui/speed/speed_hud_bg.png");
        tireLabelTexture = loadTextureSafe("ui/tire/tire_durability_label.png");
        tireBgTexture = loadTextureSafe("ui/tire/tire_durability_bg.png");
        tireFgTexture = loadTextureSafe("ui/tire/tire_durability_fg.png");
        tireCompoundSoftTexture = loadTextureSafe("ui/tire/tire_durability_compound_soft.png");
        tireCompoundMediumTexture = loadTextureSafe("ui/tire/tire_durability_compound_medium.png");
        tireCompoundHardTexture = loadTextureSafe("ui/tire/tire_durability_compound_hard.png");
        tireCompoundTexture = tireCompoundMediumTexture != null ? tireCompoundMediumTexture : tireCompoundSoftTexture;
        pitPanelTexture = loadTextureSafe("ui/pit/pit_minigame_panel.png");
        pitBarTexture = loadTextureSafe("ui/pit/pit_timing_bar_bg.png");
        pitPointerTexture = loadTextureSafe("ui/pit/pit_pointer.png");
        pitTyreSelectTexture = loadTextureSafe("ui/pit/tyre_select_panel.png");
        startLightOnTexture = loadTextureSafe("ui/startLight/light_on.png");
        startLightOffTexture = loadTextureSafe("ui/startLight/light_off.png");
        // 미니맵 차량 점 Texture
        if (minimapCarTexture == null) {
            Pixmap pm = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
            pm.setColor(1f, 0.3f, 0.3f, 1f);
            pm.fillCircle(4, 4, 4);
            minimapCarTexture = new Texture(pm);
            pm.dispose();
        }
    }

    private Texture loadTextureSafe(String path) {
        try {
            if (!Gdx.files.internal(path).exists()) return null;
            Texture t = new Texture(Gdx.files.internal(path));
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return t;
        } catch (Exception e) {
            return null;
        }
    }

    private BitmapFont loadFont(String path, int size) {
        try {
            if (!Gdx.files.internal(path).exists()) return new BitmapFont();
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(path));
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = size;
            p.color = Color.WHITE;
            BitmapFont f = gen.generateFont(p);
            gen.dispose();
            return f;
        } catch (Exception e) {
            return new BitmapFont();
        }
    }

    private void disposeTex(Texture... texes) {
        if (texes == null) return;
        for (Texture t : texes) {
            if (t != null) t.dispose();
        }
    }

    private void disposeFont(BitmapFont... fonts) {
        if (fonts == null) return;
        for (BitmapFont f : fonts) {
            if (f != null) f.dispose();
        }
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            Gdx.input.setInputProcessor(pauseStage);
            buildPauseUI();
        } else {
            Gdx.input.setInputProcessor(null);
            if (pauseStage != null) pauseStage.clear();
        }
    }

    private void buildPauseUI() {
        pauseStage.clear();
        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(pauseSkin.getDrawable("bg"));
        pauseStage.addActor(root);

        Table panel = new Table();
        panel.setBackground(pauseSkin.getDrawable("panel"));
        panel.defaults().pad(8).width(300).height(44);

        Label title = new Label("Pause", pauseSkin, "title");
        TextButton resume = new TextButton("Resume", pauseSkin);
        TextButton mainMenu = new TextButton("Main Menu", pauseSkin);
        TextButton exit = new TextButton("Exit", pauseSkin);

        resume.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y){ togglePause(); }
        });
        mainMenu.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y){
                if (gameRef != null) gameRef.setScreen(new MainMenuScreen(gameRef)); else Gdx.app.exit();
            }
        });
        exit.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y){ Gdx.app.exit(); }
        });

        panel.add(title).row();
        panel.add(resume).row();
        panel.add(mainMenu).row();
        panel.add(exit).row();
        root.add(panel).center();
    }

    private void drawPauseOverlay() {
        if (pauseStage != null) {
            pauseStage.act(Gdx.graphics.getDeltaTime());
            pauseStage.draw();
        }
    }

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    private void sendState() {
        if (lobbyClient == null || roomId == null || playerCar == null) return;
        Vector2 pos = playerCar.getPosition();
        Packets.PlayerState ps = new Packets.PlayerState();
        ps.playerId = selfId;
        ps.x = pos.x; ps.y = pos.y;
        ps.rotation = playerCar.getAngle();
        Vector2 lv = playerCar.getLinearVelocity();
        ps.velocityX = lv.x;
        ps.velocityY = lv.y;
        ps.angularVelocity = playerCar.getAngularVelocity();
        ps.vehicleIndex = playerVehicles.get(selfId, 0);
        lobbyClient.sendPlayerState(roomId, ps);
    }

    private void handleGameState(Packets.GameStatePacket gs) {
        if (gs == null || gs.playerStates == null) return;
        for (Packets.PlayerState ps : gs.playerStates) {
            if (ps.playerId == selfId) continue;
            RemoteCar rc = remoteCars.get(ps.playerId);
            if (rc == null) {
                rc = new RemoteCar();
                rc.vehicleIndex = ps.vehicleIndex;
                rc.texture = loadCarTexture(ps.vehicleIndex);
                rc.textureOwned = rc.texture != null && rc.texture != carTexture;
                remoteCars.put(ps.playerId, rc);
            }
            rc.targetPosition.set(ps.x, ps.y);
            rc.targetRotation = ps.rotation;
            if (!rc.initialized) {
                rc.position.set(rc.targetPosition);
                rc.rotation = rc.targetRotation;
                rc.initialized = true;
            }
        }
    }

    private void parsePitLayer() {
        pitEntryRect = pitServiceRect = pitExitRect = null;
        if (map == null) return;
        MapLayer pitLayer = map.getLayers().get("pit");
        if (pitLayer == null) return;
        for (MapObject obj : pitLayer.getObjects()) {
            if (!(obj instanceof RectangleMapObject rectObj)) continue;
            MapProperties props = rectObj.getProperties();
            String type = props.get("type", String.class);
            Rectangle r = rectObj.getRectangle();
            Rectangle rectWorld = new Rectangle(r.x / PPM, r.y / PPM, r.width / PPM, r.height / PPM);
            if ("entry".equalsIgnoreCase(type)) {
                pitEntryRect = rectWorld;
            } else if ("service".equalsIgnoreCase(type)) {
                pitServiceRect = rectWorld;
                pitServicePos.set(rectWorld.x + rectWorld.width / 2f, rectWorld.y + rectWorld.height / 2f);
                Object ang = props.get("angle");
                if (ang instanceof Number) pitServiceAngleDeg = ((Number) ang).floatValue();
            } else if ("exit".equalsIgnoreCase(type)) {
                pitExitRect = rectWorld;
                pitExitPos.set(rectWorld.x + rectWorld.width / 2f, rectWorld.y + rectWorld.height / 2f);
                Object ang = props.get("angle");
                if (ang instanceof Number) pitExitAngleDeg = ((Number) ang).floatValue();
            }
        }
    }

    private void loadCheckpointsFromMap() {
        checkpoints.clear();
        checkpointsInside.clear();
        totalCheckpoints = 0;
        lastCheckpointIndex = 0;
        if (map == null) return;
        MapLayer cpLayer = map.getLayers().get("checkpoints");
        if (cpLayer == null) return;
        for (MapObject obj : cpLayer.getObjects()) {
            if (!(obj instanceof RectangleMapObject rectObj)) continue;
            MapProperties props = rectObj.getProperties();
            Object idxObj = props.get("index");
            int idx;
            try {
                if (idxObj instanceof Number) idx = ((Number) idxObj).intValue();
                else idx = Integer.parseInt(String.valueOf(idxObj));
            } catch (Exception e) {
                continue;
            }
            Rectangle r = rectObj.getRectangle();
            Rectangle rectWorld = new Rectangle(r.x / PPM, r.y / PPM, r.width / PPM, r.height / PPM);
            checkpoints.add(new Checkpoint(idx, rectWorld));
            totalCheckpoints = Math.max(totalCheckpoints, idx);
        }
        checkpoints.sort(Comparator.comparingInt(a -> a.index));
    }

    private void loadStartLineFromMap() {
        startLineBounds = null;
        if (map == null) return;
        MapLayer startLayer = map.getLayers().get("startgrid");
        if (startLayer == null) return;
        for (MapObject obj : startLayer.getObjects()) {
            if (obj instanceof RectangleMapObject rectObj) {
                Rectangle r = rectObj.getRectangle();
                startLineBounds = new Rectangle(r.x / PPM, r.y / PPM, r.width / PPM, r.height / PPM);
                break;
            }
        }
    }

    private void updateLapAndCheckpoints(float delta) {
        if (playerCar == null) return;
        lapTimeSeconds += delta;
        if (checkpoints.isEmpty() || startLineBounds == null) return;

        Vector2 carPos = playerCar.getPosition();
        for (Checkpoint cp : checkpoints) {
            boolean contains = cp.bounds.contains(carPos.x, carPos.y);
            boolean alreadyInside = checkpointsInside.contains(cp.index);
            if (contains && !alreadyInside) {
                checkpointsInside.add(cp.index);
                if (cp.index == lastCheckpointIndex + 1) {
                    lastCheckpointIndex = cp.index;
                }
            } else if (!contains && alreadyInside) {
                checkpointsInside.remove(cp.index);
            }
        }

        boolean inStart = startLineBounds.contains(carPos.x, carPos.y);
        if (inStart && !insideStartLine) {
            insideStartLine = true;
            if (totalCheckpoints > 0 && lastCheckpointIndex == totalCheckpoints) {
                lastLapTime = lapTimeSeconds;
                if (bestLapTime < 0 || lastLapTime < bestLapTime) bestLapTime = lastLapTime;
                currentLap++;
                lapTimeSeconds = 0f;
                lastCheckpointIndex = 0;
                checkpointsInside.clear();
            }
        }
        if (!inStart && insideStartLine) insideStartLine = false;
    }

    private void updateRemoteCars(float delta) {
        // 보간 속도를 높여 좀 더 매끄럽게 이동 (필요시 15~20 정도로 조정 가능)
        float lerp = MathUtils.clamp(delta * 15f, 0f, 1f);
        for (IntMap.Entry<RemoteCar> e : remoteCars) {
            RemoteCar rc = e.value;
            if (!rc.initialized) continue;
            rc.position.lerp(rc.targetPosition, lerp);
            rc.rotation = MathUtils.lerpAngle(rc.rotation, rc.targetRotation, lerp);
        }
    }

    private Texture loadCarTexture(int vehicleIndex) {
        int idx = MathUtils.clamp(vehicleIndex, 0, CAR_PATHS.length - 1);
        String path = CAR_PATHS[idx];
        if (Main.assetManager.isLoaded(path, Texture.class)) return Main.assetManager.get(path, Texture.class);
        try {
            Texture t = new Texture(Gdx.files.internal(path));
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return t;
        } catch (Exception e) {
            return carTexture;
        }
    }

    private static class RemoteCar {
        int vehicleIndex;
        final Vector2 position = new Vector2();
        final Vector2 targetPosition = new Vector2();
        float rotation;
        float targetRotation;
        Texture texture;
        boolean textureOwned;
        boolean initialized = false;
    }

    private static class Checkpoint {
        final int index;
        final Rectangle bounds;
        Checkpoint(int index, Rectangle bounds) {
            this.index = index;
            this.bounds = bounds;
        }
    }
}
