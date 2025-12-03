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
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.utils.ShortArray;
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
    private enum GameState { PRE_START, NORMAL, PIT_ENTERING, PIT_MINIGAME, PIT_EXITING, FINISHED }

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
    private BitmapFont hudLapFont; // LAP HUD 전용 큰 폰트
    private GlyphLayout layout = new GlyphLayout();
    private TextureAtlas gameAtlas; // TextureAtlas 참조
    private TextureRegion lapBestBgRegion, lapLastBgRegion;
    private Texture minimapFrameTexture, minimapRegion, minimapCarTexture;
    private TextureRegion durabilityLabelRegion, durabilityBgRegion, durabilityFgRegion;
    private Texture durabilityLabelTexture, tireLabelTexture; // Label 텍스처 (개별 로드)
    private Texture raceStatusTexture;
    private TextureRegion speedHudRegion, tireLabelRegion, tireBgRegion, tireFgRegion, tireCompoundRegion;
    private TextureRegion pitPanelRegion, pitBarRegion, pitPointerRegion, pitTyreSelectRegion;
    private TextureRegion tireCompoundSoftRegion, tireCompoundMediumRegion, tireCompoundHardRegion;
    private TextureRegion startLightOnRegion, startLightOffRegion;
    private TextureRegion tyreSelectSlotRegion; // LAP HUD 및 버튼 배경으로 사용
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
    private float vehicleDurability = 100f; // 차량 내구도
    private float tireDurability = 100f;
    private float tireWearRate = 0.15f; // 기본 마모율 (초당 15%/100초 소진 기준) - 실제 계산은 getCompoundWearRate() 사용
    private float tireSpeedMultiplier = 1.0f; // 컴파운드별 최고속도 보정 (soft:+5%, hard:-5%, medium:1.0)
    private int currentLap = 0; // 완료된 랩 수 (0부터 시작, 첫 랩 완료 시 1이 됨)
    private int totalLaps = 3;
    private float lapTimeSeconds = 0f;
    private float bestLapTime = -1f;
    private float lastLapTime = -1f;
    private float totalRaceTime = 0f;  // 전체 레이스 시간 (모든 랩의 합)
    private List<Checkpoint> checkpoints = new ArrayList<>();
    private Rectangle startLineBounds;
    private int totalCheckpoints = 0;
    private int lastCheckpointIndex = 0;
    private Set<Integer> checkpointsInside = new HashSet<>();
    private boolean insideStartLine = false;

    // --- Grass (오프트랙) 영역 ---
    // Grass 영역은 Box2D 센서로 관리되며 ContactListener에서 isOnGrass를 업데이트
    private boolean isOnGrass = false;
    private float grassSpeedPenalty = 0.6f; // 60% 속도 감소

    // --- Pause UI ---
    private boolean paused = false;
    private Stage pauseStage;
    private Skin pauseSkin;

    // --- 물리 파라미터 ---
    private float maxForwardSpeed = 3.5f;  // 2.7 * 1.3 = 3.51 (~3.5) (30% 증가)
    private float maxReverseSpeed = 1.3f;  // 1.0 * 1.3 = 1.3 (30% 증가)
    private float forwardAcceleration = 1.7f;  // 가속력은 유지
    private float reverseAcceleration = 1.0f;  // 가속력은 유지

    // --- 점진적 가속 시스템 ---
    private float speedMultiplier = 0.5f; // 초기 속도는 50%부터 시작
    private float speedMultiplierTarget = 1.0f; // 목표 100%
    private float speedRampUpRate = 0.10f; // 초당 10% 증가 (약 5초에 100% 도달, 더 느림)
    private float turningPower = 10f;
    private float grip = 18.0f;
    private float minSpeedForTurn = 0.8f;
    private float maxSteeringAngle = 60f;
    private float highSpeedTurnReduction = 0.5f;  // 고속에서 회전 감소 비율 (50%까지 감소)
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
    private static final float MAP_RENDER_EXPANSION = 2f;
    // 고정 렌더 크기(로컬/원격 동일 적용)
    private static final float CAR_DRAW_WIDTH = (12.80f * 1.7f) / PPM;
    private static final float CAR_DRAW_HEIGHT = (25.60f * 1.7f) / PPM;
    private static final float VIEW_WIDTH = 1600f / PPM;
    private static final float VIEW_HEIGHT = 900f / PPM;
    // 멀티플레이 시 기본 스폰 슬롯(첫 번째 맵 기준 요청 값)
    private static final Vector2[] GRID_SPAWNS = new Vector2[]{
        new Vector2(((87 * 32) + 19) / PPM, (((120 - 92) * 32) - 11) / PPM), // p1: 오른쪽 19px, 뒤로 11px
        new Vector2(((90 * 32) + 19) / PPM, (((120 - 92) * 32) - 11) / PPM), // p2: 오른쪽 19px, 뒤로 11px
        new Vector2(((87 * 32) + 19) / PPM, (((120 - 94) * 32) - 11) / PPM), // p3: 오른쪽 19px, 뒤로 11px
        new Vector2(((90 * 32) + 19) / PPM, (((120 - 94) * 32) - 11) / PPM)  // p4: 오른쪽 19px, 뒤로 11px
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
        camera.zoom = 0.22f; // 더 가까운 뷰 (0.35 -> 0.30, 약 14% 더 가까이)

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
                Body bodyA = contact.getFixtureA().getBody();
                Body bodyB = contact.getFixtureB().getBody();

                // Grass 센서 진입 감지
                if (bodyA == playerCar || bodyB == playerCar) {
                    Body other = (bodyA == playerCar) ? bodyB : bodyA;
                    if ("GRASS".equals(other.getUserData())) {
                        isOnGrass = true;
                        Gdx.app.log("GameScreen", "Vehicle entered grass zone (Box2D sensor)");
                        return; // Grass는 센서이므로 물리 충돌 처리 안 함
                    }
                }

                // 벽 충돌 처리 (기존 로직)
                if (bodyA == playerCar || bodyB == playerCar) {
                    isColliding = true;
                    collisionTimer = collisionDuration;

                    // 충돌 강도 계산 (속도 기반)
                    Vector2 velocity = playerCar.getLinearVelocity();
                    float collisionSpeed = velocity.len() * PPM;

                    // 충돌 시 내구도 10 감소
                    if (collisionSpeed > 20f) {
                        float damage = 10f;
                        vehicleDurability = MathUtils.clamp(vehicleDurability - damage, 0f, 100f);
                        Gdx.app.log("GameScreen", String.format("Collision damage: %.1f (speed: %.1f km/h, durability: %.1f%%)",
                            damage, collisionSpeed, vehicleDurability));
                    }

                    playerCar.setLinearVelocity(velocity.scl(0.4f));
                    playerCar.setAngularVelocity(playerCar.getAngularVelocity() * 0.3f);
                }
            }

            @Override
            public void endContact(Contact contact) {
                Body bodyA = contact.getFixtureA().getBody();
                Body bodyB = contact.getFixtureB().getBody();

                // Grass 센서 퇴장 감지
                if (bodyA == playerCar || bodyB == playerCar) {
                    Body other = (bodyA == playerCar) ? bodyB : bodyA;
                    if ("GRASS".equals(other.getUserData())) {
                        isOnGrass = false;
                        Gdx.app.log("GameScreen", "Vehicle left grass zone (Box2D sensor)");
                    }
                }
            }

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

            // Collision 레이어 로드 (object 그룹 내부에 중첩되어 있을 수 있음)
            MapLayer collisionLayer = findLayer("Collision");
            if (collisionLayer != null) {
                int wallCount = 0;
                for (RectangleMapObject object : collisionLayer.getObjects().getByType(RectangleMapObject.class)) {
                    Rectangle rect = object.getRectangle();
                    createWall(rect.x / PPM, rect.y / PPM, rect.width / PPM, rect.height / PPM);
                    wallCount++;
                }
                Gdx.app.log("GameScreen", String.format("Created %d collision walls from Collision layer", wallCount));
            } else {
                Gdx.app.log("GameScreen", "WARNING: Collision layer not found!");
            }
            parsePitLayer();
            loadCheckpointsFromMap();
            loadStartLineFromMap();
            loadGrassZonesFromMap();
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
        pauseSkin = Main.getSharedSkin();
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
        // 초기 각도를 왼쪽으로 90도 회전 (PI/2 라디안)
        bodyDef.angle = (float) Math.PI / 2f;
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
        // 멀티플레이일 때 플레이어 ID 순으로 정렬해 슬롯을 배정
        if (roomId != null && selfId >= 0) {
            IntArray ids = playerVehicles.size > 0 ? playerVehicles.keys().toArray() : new IntArray(new int[]{selfId});
            ids.sort();
            int idx = ids.indexOf(selfId);
            if (idx >= 0 && idx < GRID_SPAWNS.length) {
                Vector2 slot = GRID_SPAWNS[idx];
                Gdx.app.log("GameScreen", String.format("Multiplayer spawn at p%d: (%.2f, %.2f)", idx + 1, slot.x, slot.y));
                return new Vector2(slot);
            }
        }

        // 싱글플레이: GRID_SPAWNS[0] (p1) 위치 사용
        if (GRID_SPAWNS.length > 0) {
            Vector2 p1 = GRID_SPAWNS[0];
            Gdx.app.log("GameScreen", String.format("Singleplayer spawn at p1: (%.2f, %.2f)", p1.x, p1.y));
            return new Vector2(p1);
        }

        // fallback: 맵 중앙
        float fallbackX = mapWorldWidth > 0 ? mapWorldWidth / 2f : 960 / PPM;
        float fallbackY = mapWorldHeight > 0 ? mapWorldHeight / 2f : 640 / PPM;
        Gdx.app.log("GameScreen", String.format("Fallback spawn at map center: (%.2f, %.2f)", fallbackX, fallbackY));
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

        // 점진적 가속 시스템: 레이스 시작 후 천천히 속도 증가
        if (gameState == GameState.NORMAL && speedMultiplier < speedMultiplierTarget) {
            speedMultiplier = Math.min(speedMultiplier + speedRampUpRate * delta, speedMultiplierTarget);
        }

        handleInput(delta);
        updateSteering(delta);
        updateFriction();
        // Grass 영역 감지는 Box2D ContactListener에서 자동 처리됨
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

        updateMapRendererView();
    }

    private void handleInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }

        // 레이스 종료 시 입력 차단 (결과 화면에서 버튼만 사용)
        if (gameState == GameState.FINISHED) {
            if (playerCar != null) {
                playerCar.setLinearVelocity(0, 0);
                playerCar.setAngularVelocity(0);
            }
            return;
        }

        // 신호등이 꺼지기 전까지 입력 차단 (ESC는 허용)
        if (!startLightsDone) {
            // 차량을 강제로 정지 상태로 유지
            playerCar.setLinearVelocity(0, 0);
            playerCar.setAngularVelocity(0);
            return;
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
            targetAcceleration = forwardAcceleration * speedMultiplier;
            movingForward = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            targetAcceleration = -reverseAcceleration * speedMultiplier;
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
        // 신호등이 꺼지기 전까지 방향 전환 차단
        if (!startLightsDone) {
            return;
        }

        float targetAngularVelocity = 0;
        float baseMaxAngularVelocity = MathUtils.degreesToRadians * 190;

        Vector2 forwardNormal = playerCar.getWorldVector(new Vector2(0, 1));
        float forwardSpeed = playerCar.getLinearVelocity().dot(forwardNormal);
        boolean movingForward = Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W);
        boolean movingReverse = Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S);

        // 속도에 따른 회전력 감소 계산
        float currentSpeed = playerCar.getLinearVelocity().len();
        float speedRatio = Math.min(currentSpeed / maxForwardSpeed, 1.0f);  // 0.0 ~ 1.0
        // 고속일수록 회전력 감소: 최고 속도에서 50%까지 감소
        float turnMultiplier = 1.0f - (speedRatio * highSpeedTurnReduction);
        float maxAngularVelocity = baseMaxAngularVelocity * turnMultiplier;

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

        // 점진적 가속 적용
        float effectiveMaxForward = maxForwardSpeed * speedMultiplier;
        float effectiveMaxReverse = maxReverseSpeed * speedMultiplier;

        // 내구도 0 이하일 때 최고 속도를 30%로 제한
        float durabilityLimiter = (vehicleDurability <= 0f || tireDurability <= 0f) ? 0.3f : 1f;
        effectiveMaxForward *= durabilityLimiter;
        effectiveMaxReverse *= durabilityLimiter;

        // 타이어 컴파운드 속도 보정
        effectiveMaxForward *= tireSpeedMultiplier;
        effectiveMaxReverse *= tireSpeedMultiplier;

        // Grass 페널티 추가 적용
        if (isOnGrass) {
            effectiveMaxForward *= grassSpeedPenalty;
            effectiveMaxReverse *= grassSpeedPenalty;
        }

        if (forwardSpeed > 0 && speed > effectiveMaxForward) {
            playerCar.setLinearVelocity(playerCar.getLinearVelocity().scl(effectiveMaxForward / speed));
        } else if (forwardSpeed < 0 && speed > effectiveMaxReverse) {
            playerCar.setLinearVelocity(playerCar.getLinearVelocity().scl(effectiveMaxReverse / speed));
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
            updateMapRendererView();
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

        // Box2D 디버그 렌더링 (개발 중에만 활성화)
        // box2DDebugRenderer.render(world, camera.combined);

        if (paused) {
            drawPauseOverlay();
        }

        // HUD (간단 표시: 속도/레이스 상태/내구도)
        drawHud(delta);
    }

    private void updateMapRendererView() {
        if (!USE_TILED_MAP || mapRenderer == null || camera == null) return;

        float renderWidth = camera.viewportWidth * camera.zoom * MAP_RENDER_EXPANSION;
        float renderHeight = camera.viewportHeight * camera.zoom * MAP_RENDER_EXPANSION;
        float centerX = camera.position.x;
        float centerY = camera.position.y;
        mapRenderer.setView(camera.combined,
            centerX - renderWidth / 2f,
            centerY - renderHeight / 2f,
            renderWidth,
            renderHeight);
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
        // pauseSkin은 Main이 관리하므로 dispose 하지 않음
        if (ownsCarTexture && carTexture != null) carTexture.dispose();
        disposeFont(hudFont, hudSmallFont, hudSpeedFont, hudLapFont);
        // TextureRegion은 dispose 불필요 (TextureAtlas가 관리)
        // Atlas에 없는 개별 텍스처만 dispose
        disposeTex(minimapFrameTexture, minimapRegion, minimapCarTexture, raceStatusTexture,
            durabilityLabelTexture, tireLabelTexture);
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
        drawRaceResultHud();  // 레이스 종료 화면 (마지막에 그려서 최상위 레이어)
        hudBatch.end();
    }

    private void drawRaceStatusHud() {
        if (tyreSelectSlotRegion == null || hudLapFont == null || hudCamera == null) return;
        float padding = 16f;

        // 왼쪽 상단 끝에 배치: "P1 / LAP / 1 / 3" 형식
        String playerNum = "P1"; // 기본값 (멀티플레이어에서는 selfId로 결정)
        if (roomId != null && selfId >= 0) {
            // 멀티플레이어: 플레이어 순서 확인
            com.badlogic.gdx.utils.IntArray ids = playerVehicles.keys().toArray();
            ids.sort();
            int idx = ids.indexOf(selfId);
            playerNum = "P" + (idx + 1);
        }

        // "P1 / LAP / 1 / 3" 형식
        String infoText = String.format("%s / LAP / %d / %d", playerNum, currentLap, totalLaps);

        // 큰 폰트 사용 (1.2배 스케일 적용)
        hudLapFont.getData().setScale(1.2f);
        layout.setText(hudLapFont, infoText);

        // 배경 패널 크기 계산 (더욱 넉넉하게)
        float panelW = (layout.width + 50f); // 40에서 50으로 증가
        float panelH = (layout.height + 40f); // 32에서 40으로 증가

        // 왼쪽 상단 위치
        float panelX = padding;
        float panelY = hudCamera.viewportHeight - padding - panelH;

        // 배경 패널 (atlas 이미지 사용, 흰색 배경)
        hudBatch.setColor(1f, 1f, 1f, 0.9f); // 흰색 배경 (불투명도 90%)
        hudBatch.draw(tyreSelectSlotRegion, panelX, panelY, panelW, panelH);
        hudBatch.setColor(Color.WHITE);

        // 텍스트를 패널 내부 세로 중앙에 배치 (검은색)
        float textX = panelX + 25f; // 20에서 25로 증가
        float textY = panelY + panelH / 2f + layout.height / 2f;
        hudLapFont.setColor(0f, 0f, 0f, 1f); // 검은색 텍스트
        hudLapFont.draw(hudBatch, infoText, textX, textY);
        hudLapFont.setColor(1f, 1f, 1f, 1f); // 흰색으로 복원
        hudLapFont.getData().setScale(1.0f); // 스케일 복원
    }

    private void drawSpeedHud() {
        if (speedHudRegion == null || hudCamera == null || hudSpeedFont == null || playerCar == null) return;
        float padding = 16f;

        // 속도계 크기를 절반으로 축소
        float originalW = speedHudRegion.getRegionWidth();
        float originalH = speedHudRegion.getRegionHeight();
        float texW = originalW * 0.5f;
        float texH = originalH * 0.5f;

        float x = (hudCamera.viewportWidth - texW) * 0.5f;
        float y = padding;
        hudBatch.draw(speedHudRegion, x, y, texW, texH);

        // 속도 계산: 최대 450km/h로 제한 (5.5 m/s = ~312 km/h * 1.1 = ~343 km/h)
        float rawSpeed = playerCar.getLinearVelocity().len() * PPM * 1.1f; // m/s → km/h
        float speed = Math.min(rawSpeed, 450f); // 최대 450km/h

        String txt = String.format("%.0f", speed);

        // 폰트 크기 10% 증가
        hudSpeedFont.getData().setScale(1.1f);
        layout.setText(hudSpeedFont, txt);

        // Grass zone에 있을 때 색상 변경 (노란색으로 경고)
        if (isOnGrass) {
            hudSpeedFont.setColor(1f, 1f, 0f, 1f); // 노란색
        }

        // 축소된 크기에 맞춰 텍스트 위치 조정 (왼쪽 20px, 위로 7px 추가 이동)
        hudSpeedFont.draw(hudBatch, txt,
            x + texW * 0.5f - layout.width * 0.5f - 15f - 10f,  // 왼쪽으로 20px 총 이동
            y + texH * 0.55f + layout.height * 0.5f + 10f - 30f + 7f); // 아래로 30px, 위로 7px 총 이동

        // 색상 및 스케일 복원
        hudSpeedFont.setColor(1f, 1f, 1f, 1f); // 흰색
        hudSpeedFont.getData().setScale(1.0f); // 스케일 복원

        // Grass zone 경고 텍스트 추가
        if (isOnGrass && hudFont != null) {
            String grassWarning = "OFF-TRACK (60%)";
            layout.setText(hudFont, grassWarning);
            hudFont.setColor(1f, 0.5f, 0f, 1f); // 주황색
            hudFont.draw(hudBatch, grassWarning,
                x + texW * 0.5f - layout.width * 0.5f,
                y - 10f);
            hudFont.setColor(1f, 1f, 1f, 1f); // 흰색 복원
        }
    }

    private void drawDurabilityHud() {
        if (durabilityBgRegion == null || durabilityFgRegion == null || hudCamera == null) return;
        float padding = 16f;
        float sizeScale = 1.35f; // 1.5배에서 10% 축소 = 1.35배
        float barScale = 0.972f; // 막대 그래프 추가 10% 축소 (1.08 * 0.9 = 0.972)

        // 수직 바: 왼쪽 하단에 배치, 위에서 아래로 감소
        float barWidth = 50f * sizeScale * barScale;  // 1.35배 * 0.972배
        float barHeight = 240f * sizeScale * barScale; // 1.35배 * 0.972배
        float x = padding + 20f; // 오른쪽으로 20px 이동
        float y = padding; // 아래쪽에 붙도록 padding만 적용

        // 배경 (수직 바)
        hudBatch.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        hudBatch.draw(durabilityBgRegion, x, y, barWidth, barHeight);
        hudBatch.setColor(Color.WHITE);

        // 내구도 비율에 따라 높이 조절 (아래에서 위로 채워짐, 위에서부터 줄어듦)
        float ratio = MathUtils.clamp(vehicleDurability / 100f, 0f, 1f);
        if (durabilityFgRegion != null) {
            float fgHeight = barHeight * ratio;
            float fgY = y; // 바닥부터 시작

            // 내구도에 따라 색상 변경 (초록→노랑→빨강)
            if (ratio > 0.5f) {
                hudBatch.setColor(0.3f, 0.8f, 0.4f, 0.9f); // 초록
            } else if (ratio > 0.2f) {
                hudBatch.setColor(0.95f, 0.75f, 0.15f, 0.9f); // 노랑
            } else {
                hudBatch.setColor(0.9f, 0.2f, 0.2f, 0.9f); // 빨강
            }
            hudBatch.draw(durabilityFgRegion, x, fgY, barWidth, fgHeight);
            hudBatch.setColor(Color.WHITE);
        }

        // 라벨을 바 위에 배치 (왼쪽으로 10px 이동)
        if (durabilityLabelRegion != null) {
            float labelW = Math.min(durabilityLabelRegion.getRegionWidth() * sizeScale, barWidth * 2f);
            float labelH = 40f * sizeScale;
            float labelX = x - (labelW - barWidth) / 2f - 10f; // 왼쪽으로 10px 이동
            float labelY = y + barHeight + 8f;
            hudBatch.draw(durabilityLabelRegion, labelX, labelY, labelW, labelH);
        }

        // 퍼센트 텍스트
        if (hudSmallFont != null) {
            hudSmallFont.getData().setScale(sizeScale);
            String val = String.format("%.0f%%", MathUtils.clamp(vehicleDurability, 0f, 100f));
            layout.setText(hudSmallFont, val);
            hudSmallFont.draw(hudBatch, val, x + (barWidth - layout.width) / 2f, y + barHeight / 2f + layout.height / 2f);
            hudSmallFont.getData().setScale(1.0f);
        }
    }

    private void drawTireHud() {
        if (tireBgRegion == null || tireFgRegion == null || hudCamera == null) return;
        float padding = 16f;
        float sizeScale = 1.35f; // 1.5배에서 10% 축소 = 1.35배 (durability와 동일)
        float barScale = 0.972f; // 막대 그래프 추가 10% 축소 (durability와 동일)

        // 수직 바: 오른쪽 하단에 배치
        float barWidth = 50f * sizeScale * barScale;  // 1.35배 * 0.972배
        float barHeight = 240f * sizeScale * barScale; // 1.35배 * 0.972배
        float x = hudCamera.viewportWidth - barWidth - padding;
        float y = padding; // 아래쪽에 붙도록 padding만 적용

        // 배경 (수직 바)
        hudBatch.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        hudBatch.draw(tireBgRegion, x, y, barWidth, barHeight);
        hudBatch.setColor(Color.WHITE);

        // 타이어 마모도 (아래에서 위로 채워짐, 위에서부터 줄어듦)
        float ratio = MathUtils.clamp(tireDurability / 100f, 0f, 1f);
        if (tireFgRegion != null) {
            float fgHeight = barHeight * ratio;
            float fgY = y; // 바닥부터 시작

            // 타이어 바는 항상 노란색으로 표시 (차량 내구도와 구분)
            hudBatch.setColor(0.95f, 0.75f, 0.15f, 0.9f);
            hudBatch.draw(tireFgRegion, x, fgY, barWidth, fgHeight);
            hudBatch.setColor(Color.WHITE);
        }

        // 타이어 타입 이미지를 바 위에 배치 (왼쪽으로 10px 이동)
        if (tireCompoundRegion != null) {
            float originalW = tireCompoundRegion.getRegionWidth();
            float originalH = tireCompoundRegion.getRegionHeight();
            float aspectRatio = originalH / originalW;

            float tireIconScale = 1.5f; // 원래 크기 유지
            float tireW = 80f * tireIconScale;
            float tireH = tireW * aspectRatio; // 비율 유지
            float tireX = x - (tireW - barWidth) / 2f - 10f; // 왼쪽으로 10px 이동
            float tireY = y + barHeight + 8f;
            hudBatch.draw(tireCompoundRegion, tireX, tireY, tireW, tireH);
        }

        // 타이어 상태 텍스트
        if (hudSmallFont != null) {
            hudSmallFont.getData().setScale(sizeScale);
            String val = String.format("%.0f%%", MathUtils.clamp(tireDurability, 0f, 100f));
            layout.setText(hudSmallFont, val);
            hudSmallFont.draw(hudBatch, val, x + (barWidth - layout.width) / 2f, y + barHeight / 2f + layout.height / 2f);
            hudSmallFont.getData().setScale(1.0f);
        }
    }

    private void drawPitMinigameHud() {
        if (gameState != GameState.PIT_MINIGAME) return;
        if (pitPanelRegion == null || pitBarRegion == null || hudCamera == null) return;
        float panelW = pitPanelRegion.getRegionWidth();
        float panelH = pitPanelRegion.getRegionHeight();
        float panelX = (hudCamera.viewportWidth - panelW) * 0.5f;
        float panelY = (hudCamera.viewportHeight - panelH) * 0.5f;
        float selectionAbovePanel = 12f; // 타이어 선택 슬롯을 패널 위로 살짝 띄움

        float barW = pitBarRegion.getRegionWidth();
        float barH = pitBarRegion.getRegionHeight();
        float barX = (hudCamera.viewportWidth - barW) * 0.5f;
        float barY = panelY - 80f; // 더 아래로 이동 (패널과 바 사이 충분한 간격)

        BitmapFont font = hudFont != null ? hudFont : new BitmapFont();

        // 1. 패널 배경 그리기
        hudBatch.draw(pitPanelRegion, panelX, panelY);

        // 2. 타이밍 바 그리기 (패널 아래)
        hudBatch.draw(pitBarRegion, barX, barY);
        float pointerX = barX + pitPointerT * barW;
        if (pitPointerRegion != null) {
            hudBatch.draw(pitPointerRegion, pointerX - pitPointerRegion.getRegionWidth() / 2f, barY - 10f);
        }

        // 3. 패널 상단: 타이어 선택 정보
        font.draw(hudBatch, "Tire: " + pitSelectedCompound.toUpperCase(), panelX + 24f, panelY + panelH - 24f);

        // 4. 패널 중앙: 결과 표시 (공간 확보)
        if (!lastPitResult.isEmpty()) {
            font.draw(hudBatch, "Result: " + lastPitResult, panelX + 24f, panelY + panelH / 2f + 30f);
        }

        // 5. 타이어 선택 패널 (하단)
        if (pitTyreSelectRegion != null) {
            float selW = pitTyreSelectRegion.getRegionWidth();
            float selH = pitTyreSelectRegion.getRegionHeight();
            float selX = panelX + (panelW - selW) * 0.5f;
            float selY = panelY + panelH + selectionAbovePanel; // 패널 바로 위쪽에 배치
            hudBatch.draw(pitTyreSelectRegion, selX, selY);

            // 버튼 텍스트 색상은 아틀라스 이미지에 포함돼 있어 코드로만 변경 불가
            // 별도 컬러 텍스트가 필요하면 색상이 적용된 새 슬롯/버튼 아틀라스를 추가해야 함
        }

        // 6. 타이밍 바 상태 텍스트 (바 아래, 기존 위치)
        String statusText;
        if (pitServiceTimeRemaining > 0f) {
            statusText = String.format("Service: %.1fs", Math.max(0f, pitServiceTimeRemaining));
        } else {
            statusText = "Press ENTER/SPACE";
        }
        layout.setText(font, statusText);
        float statusX = barX + (barW - layout.width) / 2f; // 기존처럼 바 기준 중앙
        float statusY = barY - 32f; // 기존처럼 바 아래 충분한 간격
        font.draw(hudBatch, statusText, statusX, statusY);

        // 7. 타이어 선택 안내 텍스트: Press 문구 바로 아래 (패널 내부)
        String instructionText = "Q/W/E = SOFT/MEDIUM/HARD";
        layout.setText(font, instructionText);
        float instructionX = statusX; // 동일 x 정렬
        float instructionY = statusY - 28f; // Press 바로 아래
        font.draw(hudBatch, instructionText, instructionX, instructionY);
    }

    private void drawLapTimeHud() {
        if (lapBestBgRegion == null || lapLastBgRegion == null || hudCamera == null || hudFont == null) return;
        float boxW = 200f;
        float boxH = 40f;

        // 미니맵 바로 아래에 배치
        float minimapBottom = hudCamera.viewportHeight - minimapFrameSize - minimapPadding;
        float bestX = hudCamera.viewportWidth - boxW - minimapPadding;
        float bestY = minimapBottom - boxH - 8f;
        float lastX = bestX;
        float lastY = bestY - boxH - 8f;

        // BEST LAP 박스 (2번째 랩부터 표시 - 가장 빠른 랩)
        hudBatch.setColor(0.1f, 0.1f, 0.1f, 0.85f);
        hudBatch.draw(lapBestBgRegion, bestX, bestY, boxW, boxH);
        hudBatch.setColor(Color.WHITE);

        // 2번째 랩 완료부터 BEST 표시 (currentLap은 완료된 랩 + 1)
        String bestTxt;
        if (currentLap >= 2 && bestLapTime > 0) {
            bestTxt = formatLap(bestLapTime);
        } else {
            bestTxt = "--:--.---";
        }
        String bestLine = "BEST: " + bestTxt;
        layout.setText(hudFont, bestLine);
        hudFont.draw(hudBatch, bestLine, bestX + 12, bestY + boxH / 2f + layout.height / 2f);

        // LAST LAP 박스 (가장 최근 완주한 LAP 기록 - 1번째 랩부터 표시)
        hudBatch.setColor(0.1f, 0.1f, 0.1f, 0.85f);
        hudBatch.draw(lapLastBgRegion, lastX, lastY, boxW, boxH);
        hudBatch.setColor(Color.WHITE);

        String lastTxt = lastLapTime > 0 ? formatLap(lastLapTime) : "--:--.---";
        String lastLine = "LAST: " + lastTxt;
        layout.setText(hudFont, lastLine);
        hudFont.draw(hudBatch, lastLine, lastX + 12, lastY + boxH / 2f + layout.height / 2f);
    }

    private String formatLap(float seconds) {
        int total = MathUtils.floor(seconds);
        int m = total / 60;
        int s = total % 60;
        int ms = MathUtils.floor((seconds - total) * 1000f);
        return String.format("%02d:%02d.%03d", m, s, ms);
    }

    /**
     * 레이스 종료 시 결과 화면 표시
     * 반투명 검은 배경 위에 기록과 버튼 표시
     */
    private void drawRaceResultHud() {
        if (gameState != GameState.FINISHED || hudCamera == null || hudFont == null) return;

        float screenW = hudCamera.viewportWidth;
        float screenH = hudCamera.viewportHeight;

        // 1. 반투명 검은 배경 오버레이
        hudBatch.setColor(0f, 0f, 0f, 0.8f);
        if (raceStatusTexture != null) {
            hudBatch.draw(raceStatusTexture, 0, 0, screenW, screenH);
        }
        hudBatch.setColor(Color.WHITE);

        // 2. "Record" 타이틀 (화면 상단 중앙)
        String titleText = "RECORD";
        layout.setText(hudFont, titleText);
        float titleX = (screenW - layout.width) / 2f;
        float titleY = screenH * 0.75f;
        hudFont.draw(hudBatch, titleText, titleX, titleY);

        // 3. 총 레이스 시간 표시 (타이틀 아래)
        String timeText = "Total Time: " + formatLap(totalRaceTime);
        layout.setText(hudFont, timeText);
        float timeX = (screenW - layout.width) / 2f;
        float timeY = titleY - 80f;
        hudFont.draw(hudBatch, timeText, timeX, timeY);

        // 4. Replay 버튼 (중앙 왼쪽)
        float buttonW = 200f;
        float buttonH = 60f;
        float buttonSpacing = 40f;
        float replayX = (screenW / 2f) - buttonW - (buttonSpacing / 2f);
        float replayY = screenH * 0.35f;

        // Replay 버튼 배경 (atlas 이미지 사용)
        hudBatch.setColor(0.2f, 0.6f, 0.2f, 0.9f); // 초록색 틴트
        if (tyreSelectSlotRegion != null) {
            hudBatch.draw(tyreSelectSlotRegion, replayX, replayY, buttonW, buttonH);
        }
        hudBatch.setColor(Color.WHITE);

        // Replay 버튼 텍스트
        String replayText = "REPLAY";
        layout.setText(hudFont, replayText);
        float replayTextX = replayX + (buttonW - layout.width) / 2f;
        float replayTextY = replayY + (buttonH + layout.height) / 2f;
        hudFont.draw(hudBatch, replayText, replayTextX, replayTextY);

        // 5. Exit 버튼 (중앙 오른쪽)
        float exitX = (screenW / 2f) + (buttonSpacing / 2f);
        float exitY = replayY;

        // Exit 버튼 배경 (atlas 이미지 사용)
        hudBatch.setColor(0.7f, 0.2f, 0.2f, 0.9f); // 빨간색 틴트
        if (tyreSelectSlotRegion != null) {
            hudBatch.draw(tyreSelectSlotRegion, exitX, exitY, buttonW, buttonH);
        }
        hudBatch.setColor(Color.WHITE);

        // Exit 버튼 텍스트
        String exitText = "EXIT";
        layout.setText(hudFont, exitText);
        float exitTextX = exitX + (buttonW - layout.width) / 2f;
        float exitTextY = exitY + (buttonH + layout.height) / 2f;
        hudFont.draw(hudBatch, exitText, exitTextX, exitTextY);

        // 6. 버튼 클릭 처리 (마우스/터치)
        if (Gdx.input.justTouched()) {
            int touchX = Gdx.input.getX();
            int touchY = Gdx.graphics.getHeight() - Gdx.input.getY(); // Y 좌표 반전

            // Replay 버튼 클릭 확인
            if (touchX >= replayX && touchX <= replayX + buttonW &&
                touchY >= replayY && touchY <= replayY + buttonH) {
                Gdx.app.log("GameScreen", "Replay button clicked - restarting race");
                restartRace();
            }

            // Exit 버튼 클릭 확인
            if (touchX >= exitX && touchX <= exitX + buttonW &&
                touchY >= exitY && touchY <= exitY + buttonH) {
                Gdx.app.log("GameScreen", "Exit button clicked - returning to menu");
                exitToMenu();
            }
        }
    }

    private void drawMinimapHud() {
        if (minimapFrameTexture == null || hudCamera == null) return;

        // 미니맵 프레임 위치 (오른쪽 상단 끝에 붙임)
        float frameX = hudCamera.viewportWidth - minimapFrameSize - minimapPadding;
        float frameY = hudCamera.viewportHeight - minimapFrameSize - minimapPadding;

        // 프레임 그리기
        hudBatch.draw(minimapFrameTexture, frameX, frameY, minimapFrameSize, minimapFrameSize);

        // 미니맵 내부 영역 (프레임 안쪽)
        float mapAreaX = frameX + minimapInset;
        float mapAreaY = frameY + minimapInset;
        float mapAreaW = minimapFrameSize - (minimapInset * 2f);
        float mapAreaH = mapAreaW;

        // 맵 크기 계산
        float mapW = mapWorldWidth > 0 ? mapWorldWidth : mapAreaW;
        float mapH = mapWorldHeight > 0 ? mapWorldHeight : mapAreaH;

        // 맵을 프레임 내부에 비율 맞춰 그리기
        float scaleX = mapAreaW / mapW;
        float scaleY = mapAreaH / mapH;
        float scale = Math.min(scaleX, scaleY); // 비율 유지

        float renderW = mapW * scale;
        float renderH = mapH * scale;

        // 중앙 정렬
        float offsetX = (mapAreaW - renderW) / 2f;
        float offsetY = (mapAreaH - renderH) / 2f;

        // HUD 배치 종료하고 Tiled 맵 렌더링
        hudBatch.end();

        // Tiled 맵을 미니맵 영역에 렌더링
        if (mapRenderer != null && map != null && mapW > 0 && mapH > 0) {
            // 미니맵용 임시 카메라 설정 (맵 전체를 보도록)
            OrthographicCamera minimapCamera = new OrthographicCamera();
            minimapCamera.setToOrtho(false, mapW, mapH);
            minimapCamera.position.set(mapW / 2f, mapH / 2f, 0);
            minimapCamera.zoom = 1.0f; // 전체 맵이 보이도록
            minimapCamera.update();

            // 뷰포트 설정 (미니맵 영역만 렌더링하도록 제한)
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            int scissorX = (int)(mapAreaX + offsetX);
            int scissorY = (int)(mapAreaY + offsetY);
            int scissorW = (int)renderW;
            int scissorH = (int)renderH;
            Gdx.gl.glScissor(scissorX, scissorY, scissorW, scissorH);

            // 뷰포트를 미니맵 영역으로 설정
            Gdx.gl.glViewport(scissorX, scissorY, scissorW, scissorH);

            // Tiled 맵 렌더링 (전체 맵)
            mapRenderer.setView(minimapCamera);
            mapRenderer.render();

            // 뷰포트를 원래대로 복원
            Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        }

        // HUD 배치 재개
        hudBatch.begin();

        // 플레이어 위치 표시
        if (minimapCarTexture != null && playerCar != null) {
            Vector2 pos = playerCar.getPosition();
            float px = mapAreaX + offsetX + (pos.x / mapW) * renderW;
            float py = mapAreaY + offsetY + (pos.y / mapH) * renderH;

            // 플레이어 점이 맵 영역 내에 있는지 확인
            if (px >= mapAreaX && px <= mapAreaX + mapAreaW &&
                py >= mapAreaY && py <= mapAreaY + mapAreaH) {
                hudBatch.setColor(1f, 0.2f, 0.2f, 1f); // 빨간색
                hudBatch.draw(minimapCarTexture,
                    px - minimapCarTexture.getWidth() / 2f,
                    py - minimapCarTexture.getHeight() / 2f);
                hudBatch.setColor(Color.WHITE);
            }
        }

        // 다른 플레이어들 표시 (멀티플레이어)
        for (RemoteCar remote : remoteCars.values()) {
            if (minimapCarTexture == null || !remote.initialized) continue;

            float rx = mapAreaX + offsetX + (remote.position.x / mapW) * renderW;
            float ry = mapAreaY + offsetY + (remote.position.y / mapH) * renderH;

            if (rx >= mapAreaX && rx <= mapAreaX + mapAreaW &&
                ry >= mapAreaY && ry <= mapAreaY + mapAreaH) {
                hudBatch.setColor(0.2f, 0.5f, 1f, 1f); // 파란색
                hudBatch.draw(minimapCarTexture,
                    rx - minimapCarTexture.getWidth() / 2f,
                    ry - minimapCarTexture.getHeight() / 2f);
                hudBatch.setColor(Color.WHITE);
            }
        }
    }

    private void drawStartLightsHud() {
        if (startLightsDone || hudCamera == null) return;
        if (startLightOnRegion == null || startLightOffRegion == null) return;
        if (startCountdown > 3.1f) return; // 아직 준비 안 됨

        float paddingTop = 40f;
        float scale = 0.5f; // 크기 50% 축소
        float gap = 6f; // 간격도 절반으로
        float drawW = startLightOnRegion.getRegionWidth() * scale;
        float drawH = startLightOnRegion.getRegionHeight() * scale;
        float totalW = drawW * 4f + gap * 3f;
        float startX = (hudCamera.viewportWidth - totalW) * 0.5f;
        float y = hudCamera.viewportHeight - drawH - paddingTop;

        // 표시할 상태 계산: 3->2->1->GO 후 offTimer
        TextureRegion[] lamps = new TextureRegion[]{startLightOffRegion, startLightOffRegion, startLightOffRegion, startLightOffRegion};
        if (startCountdown > 2f) {
            lamps[0] = startLightOnRegion;
        } else if (startCountdown > 1f) {
            lamps[0] = startLightOnRegion; lamps[1] = startLightOnRegion;
        } else if (startCountdown > 0f) {
            lamps[0] = startLightOnRegion; lamps[1] = startLightOnRegion; lamps[2] = startLightOnRegion;
        } else if (goTimer > 0f) {
            lamps[0] = startLightOnRegion; lamps[1] = startLightOnRegion; lamps[2] = startLightOnRegion; lamps[3] = startLightOnRegion;
        } else if (offTimer > 0f) {
            lamps[0] = startLightOffRegion; lamps[1] = startLightOffRegion; lamps[2] = startLightOffRegion; lamps[3] = startLightOffRegion;
        }

        for (int i = 0; i < 4; i++) {
            float x = startX + i * (drawW + gap);
            hudBatch.draw(lamps[i], x, y, drawW, drawH);
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
                // 점진적 가속 시스템 초기화: 50%에서 시작
                speedMultiplier = 0.5f;
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
        if ("soft".equalsIgnoreCase(pitSelectedCompound)) return 100f / 60f;  // 약 60초에 100% 소모
        if ("hard".equalsIgnoreCase(pitSelectedCompound)) return 100f / 95f;  // 약 95초에 100% 소모
        return 100f / 75f; // medium 기본 약 75초
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
                    float grade = pitPointerT;
                    // Perfect (중앙): 2.5초, Good (중간): 4초, Bad (외각): 6초
                    if (grade >= 0.40f && grade <= 0.60f) {
                        lastPitResult = "Perfect";
                        pitServiceTimeTotal = 2.5f;
                    } else if ((grade >= 0.25f && grade < 0.40f) || (grade > 0.60f && grade <= 0.75f)) {
                        lastPitResult = "Good";
                        pitServiceTimeTotal = 4.0f;
                    } else {
                        lastPitResult = "Bad";
                        pitServiceTimeTotal = 6.0f;
                    }
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

                    // 피트 서비스 완료: 차량 내구도 20% 회복 + 타이어 100% 수리
                    vehicleDurability = Math.min(100f, vehicleDurability + 20f);
                    tireDurability = 100f;
                    Gdx.app.log("GameScreen", String.format("Pit service: Vehicle +20%% (now %.1f%%), Tire 100%%", vehicleDurability));

                    // 피트 서비스 완료 후 즉시 Exit 포인트로 차량 이동
                    if (pitExitPos != null && playerCar != null) {
                        playerCar.setTransform(pitExitPos, pitExitAngleDeg * MathUtils.degreesToRadians);
                        playerCar.setLinearVelocity(0, 0);
                        playerCar.setAngularVelocity(0);
                        Gdx.app.log("GameScreen", String.format("Pit exit: teleported to (%.2f, %.2f) angle: %.1f",
                            pitExitPos.x, pitExitPos.y, pitExitAngleDeg));
                    }
                    gameState = GameState.NORMAL;
                    Gdx.app.log("GameScreen", "Pit service completed, returned to track");
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
        if ("soft".equalsIgnoreCase(comp) && tireCompoundSoftRegion != null) {
            tireCompoundRegion = tireCompoundSoftRegion;
            tireWearRate = 100f / 60f;
            tireSpeedMultiplier = 1.05f;
        } else if ("hard".equalsIgnoreCase(comp) && tireCompoundHardRegion != null) {
            tireCompoundRegion = tireCompoundHardRegion;
            tireWearRate = 100f / 95f;
            tireSpeedMultiplier = 0.95f;
        } else if (tireCompoundMediumRegion != null) {
            tireCompoundRegion = tireCompoundMediumRegion;
            tireWearRate = 100f / 75f;
            tireSpeedMultiplier = 1.0f;
        }
        pitSelectedCompound = comp;
    }

    private void initHudResources() {
        hudFont = loadFont("fonts/capitolcity.ttf", 18);
        hudSmallFont = loadFont("fonts/capitolcity.ttf", 14);
        hudSpeedFont = loadFont("fonts/capitolcity.ttf", 52);
        hudLapFont = loadFontWithFilter("fonts/capitolcity.ttf", 36); // LAP HUD용 큰 폰트

        // TextureAtlas 로드
        try {
            gameAtlas = Main.assetManager.get("atlas/game_ui.atlas", TextureAtlas.class);
        } catch (Exception e) {
            Gdx.app.log("GameScreen", "Failed to load game_ui.atlas: " + e.getMessage());
        }

        // Atlas에서 TextureRegion 추출 (캐싱)
        if (gameAtlas != null) {
            lapBestBgRegion = gameAtlas.findRegion("lap_time_bg_best");
            lapLastBgRegion = gameAtlas.findRegion("lap_time_bg_last");
            durabilityBgRegion = gameAtlas.findRegion("vehicle_durability_bg");
            durabilityFgRegion = gameAtlas.findRegion("vehicle_durability_fg");
            speedHudRegion = gameAtlas.findRegion("speed_hud_bg");
            tireBgRegion = gameAtlas.findRegion("tire_durability_bg");
            tireFgRegion = gameAtlas.findRegion("tire_durability_fg");
            tireCompoundSoftRegion = gameAtlas.findRegion("tire_durability_compound_soft");
            tireCompoundMediumRegion = gameAtlas.findRegion("tire_durability_compound_medium");
            tireCompoundHardRegion = gameAtlas.findRegion("tire_durability_compound_hard");
            tireCompoundRegion = tireCompoundMediumRegion != null ? tireCompoundMediumRegion : tireCompoundSoftRegion;
            pitPanelRegion = gameAtlas.findRegion("pit_minigame_panel");
            pitBarRegion = gameAtlas.findRegion("pit_timing_bar_bg");
            pitPointerRegion = gameAtlas.findRegion("pit_pointer");
            pitTyreSelectRegion = gameAtlas.findRegion("tyre_select_panel");
            startLightOnRegion = gameAtlas.findRegion("light-on");
            startLightOffRegion = gameAtlas.findRegion("light-off");
            tyreSelectSlotRegion = gameAtlas.findRegion("tyre_select_slot"); // LAP HUD 및 버튼 배경
        }

        // Atlas에 없는 텍스처들 (개별 로드)
        durabilityLabelTexture = loadTextureSafe("ui/durability/vehicle_durability_label.png");
        if (durabilityLabelTexture != null) durabilityLabelRegion = new TextureRegion(durabilityLabelTexture);

        tireLabelTexture = loadTextureSafe("ui/tire/tire_durability_label.png");
        if (tireLabelTexture != null) tireLabelRegion = new TextureRegion(tireLabelTexture);

        minimapFrameTexture = loadTextureSafe("hud/minimap_frame_bg.png");
        minimapRegion = loadTextureSafe("hud/minimap.png");
        raceStatusTexture = loadTextureSafe("hud/race_status_bg.png");

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

    private BitmapFont loadFontWithFilter(String path, int size) {
        try {
            if (!Gdx.files.internal(path).exists()) return new BitmapFont();
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(path));
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = size;
            p.color = Color.WHITE;
            // 더 나은 필터링 설정
            p.minFilter = Texture.TextureFilter.Linear;
            p.magFilter = Texture.TextureFilter.Linear;
            // ASCII + 숫자 + 슬래시 문자 포함
            p.characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789/!? ";
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

    /**
     * 맵에서 레이어를 찾습니다. 최상위 레이어와 그룹 내부 레이어를 모두 검색합니다.
     * Tiled 맵의 레이어가 그룹 안에 중첩되어 있을 수 있기 때문입니다.
     */
    private MapLayer findLayer(String layerName) {
        if (map == null) return null;

        // 1. 최상위 레이어에서 직접 찾기
        MapLayer layer = map.getLayers().get(layerName);
        if (layer != null) return layer;

        // 2. 모든 그룹 내부를 재귀적으로 검색
        for (MapLayer topLayer : map.getLayers()) {
            if (topLayer instanceof com.badlogic.gdx.maps.MapGroupLayer) {
                com.badlogic.gdx.maps.MapGroupLayer group = (com.badlogic.gdx.maps.MapGroupLayer) topLayer;
                MapLayer found = findLayerInGroup(group, layerName);
                if (found != null) return found;
            }
        }

        return null;
    }

    private MapLayer findLayerInGroup(com.badlogic.gdx.maps.MapGroupLayer group, String layerName) {
        // 그룹 내부의 레이어 검색
        MapLayer layer = group.getLayers().get(layerName);
        if (layer != null) return layer;

        // 중첩된 그룹도 재귀적으로 검색
        for (MapLayer child : group.getLayers()) {
            if (child instanceof com.badlogic.gdx.maps.MapGroupLayer) {
                MapLayer found = findLayerInGroup((com.badlogic.gdx.maps.MapGroupLayer) child, layerName);
                if (found != null) return found;
            }
        }

        return null;
    }

    private void parsePitLayer() {
        pitEntryRect = pitServiceRect = pitExitRect = null;
        if (map == null) {
            Gdx.app.log("GameScreen", "WARNING: map is null in parsePitLayer");
            return;
        }
        MapLayer pitLayer = findLayer("pit");
        if (pitLayer == null) {
            Gdx.app.log("GameScreen", "WARNING: Pit layer not found in map");
            return;
        }
        Gdx.app.log("GameScreen", "Pit layer found with " + pitLayer.getObjects().getCount() + " objects");
        for (MapObject obj : pitLayer.getObjects()) {
            if (!(obj instanceof RectangleMapObject rectObj)) continue;
            MapProperties props = rectObj.getProperties();
            String type = props.get("type", String.class);
            if (type == null) {
                Gdx.app.log("GameScreen", "Pit object missing type: " + obj.getName());
                continue;
            }
            Rectangle r = rectObj.getRectangle();
            Rectangle rectWorld = new Rectangle(r.x / PPM, r.y / PPM, r.width / PPM, r.height / PPM);

            // 실제 맵에서는 "PIT_ENTRY", "PIT_SERVICE", "PIT_EXIT" 사용
            if ("PIT_ENTRY".equalsIgnoreCase(type) || "entry".equalsIgnoreCase(type)) {
                pitEntryRect = rectWorld;
                Gdx.app.log("GameScreen", String.format("Pit entry loaded: (%.2f, %.2f)", rectWorld.x, rectWorld.y));
            } else if ("PIT_SERVICE".equalsIgnoreCase(type) || "service".equalsIgnoreCase(type)) {
                pitServiceRect = rectWorld;
                pitServicePos.set(rectWorld.x + rectWorld.width / 2f, rectWorld.y + rectWorld.height / 2f);
                Object ang = props.get("serviceAngle");
                if (ang == null) ang = props.get("angle");
                if (ang instanceof Number) pitServiceAngleDeg = ((Number) ang).floatValue();
                Gdx.app.log("GameScreen", String.format("Pit service loaded: (%.2f, %.2f) angle: %.1f", rectWorld.x, rectWorld.y, pitServiceAngleDeg));
            } else if ("PIT_EXIT".equalsIgnoreCase(type) || "exit".equalsIgnoreCase(type)) {
                pitExitRect = rectWorld;
                pitExitPos.set(rectWorld.x + rectWorld.width / 2f, rectWorld.y + rectWorld.height / 2f);
                Object ang = props.get("exitAngle");
                if (ang == null) ang = props.get("angle");
                if (ang instanceof Number) pitExitAngleDeg = ((Number) ang).floatValue();
                Gdx.app.log("GameScreen", String.format("Pit exit loaded: (%.2f, %.2f) angle: %.1f", rectWorld.x, rectWorld.y, pitExitAngleDeg));
            }
        }
    }

    private void loadCheckpointsFromMap() {
        checkpoints.clear();
        checkpointsInside.clear();
        totalCheckpoints = 0;
        lastCheckpointIndex = 0;
        if (map == null) {
            Gdx.app.log("GameScreen", "WARNING: map is null in loadCheckpointsFromMap");
            return;
        }
        MapLayer cpLayer = findLayer("checkpoints");
        if (cpLayer == null) {
            Gdx.app.log("GameScreen", "WARNING: checkpoints layer not found");
            return;
        }
        Gdx.app.log("GameScreen", "Checkpoints layer found with " + cpLayer.getObjects().getCount() + " objects");
        for (MapObject obj : cpLayer.getObjects()) {
            if (!(obj instanceof RectangleMapObject rectObj)) continue;
            MapProperties props = rectObj.getProperties();
            // 실제 맵에서는 "cpindex" 속성 사용 (index도 fallback으로 지원)
            // TMX에서는 cpIndex(CamelCase)로 저장되어 있어 소문자 키만 검사하면 못 읽는다.
            Object idxObj = props.get("cpindex");
            if (idxObj == null) idxObj = props.get("cpIndex");
            if (idxObj == null) idxObj = props.get("index");
            int idx;
            try {
                if (idxObj instanceof Number) idx = ((Number) idxObj).intValue();
                else idx = Integer.parseInt(String.valueOf(idxObj));
            } catch (Exception e) {
                Gdx.app.log("GameScreen", "Checkpoint missing cpindex/index: " + obj.getName());
                continue;
            }
            Rectangle r = rectObj.getRectangle();

            // 회전 처리: Tiled에서 회전된 객체는 width/height가 바뀔 수 있음
            // rotation 속성 확인
            Object rotationObj = props.get("rotation");
            float rotation = 0f;
            if (rotationObj instanceof Number) {
                rotation = ((Number) rotationObj).floatValue();
            }

            // 회전이 있는 경우 width와 height를 교환
            float finalWidth = r.width;
            float finalHeight = r.height;
            float finalX = r.x;
            float finalY = r.y;

            if (Math.abs(rotation) > 45f && Math.abs(rotation) < 135f) {
                // 90도 회전: width와 height 교환
                finalWidth = r.height;
                finalHeight = r.width;
                // Tiled의 회전은 객체의 왼쪽 하단 모서리를 기준으로 하므로 위치 조정
                if (rotation < 0) { // -90도
                    finalX = r.x - finalWidth;
                }
            }

            Rectangle rectWorld = new Rectangle(finalX / PPM, finalY / PPM, finalWidth / PPM, finalHeight / PPM);
            checkpoints.add(new Checkpoint(idx, rectWorld));
            Gdx.app.log("GameScreen", String.format("Checkpoint %d: pos=(%.2f, %.2f) size=(%.2f x %.2f) rotation=%.1f",
                idx, rectWorld.x, rectWorld.y, rectWorld.width, rectWorld.height, rotation));
            totalCheckpoints = Math.max(totalCheckpoints, idx);
        }
        checkpoints.sort(Comparator.comparingInt(a -> a.index));
        Gdx.app.log("GameScreen", String.format("Loaded %d checkpoints (max index: %d)", checkpoints.size(), totalCheckpoints));
    }

    private void loadStartLineFromMap() {
        startLineBounds = null;
        if (map == null) {
            Gdx.app.log("GameScreen", "WARNING: map is null in loadStartLineFromMap");
            return;
        }
        MapLayer startLayer = findLayer("startgrid");
        if (startLayer == null) {
            Gdx.app.log("GameScreen", "WARNING: startgrid layer not found");
            return;
        }
        Gdx.app.log("GameScreen", "Start line layer found with " + startLayer.getObjects().getCount() + " objects");
        for (MapObject obj : startLayer.getObjects()) {
            if (obj instanceof RectangleMapObject rectObj) {
                Rectangle r = rectObj.getRectangle();
                startLineBounds = new Rectangle(r.x / PPM, r.y / PPM, r.width / PPM, r.height / PPM);
                Gdx.app.log("GameScreen", String.format("Start line loaded at (%.2f, %.2f) size (%.2f x %.2f)",
                    startLineBounds.x, startLineBounds.y, startLineBounds.width, startLineBounds.height));
                break;
            }
        }
        if (startLineBounds == null) {
            Gdx.app.log("GameScreen", "WARNING: No valid start line rectangle found");
        }
    }

    /**
     * Grass 영역을 Box2D 정적 바디(센서)로 로드.
     * 모든 Grass 폴리곤을 하나의 정적 Body로 통합하여 효율성 향상.
     */
    private void loadGrassZonesFromMap() {
        if (map == null) {
            Gdx.app.log("GameScreen", "WARNING: map is null in loadGrassZonesFromMap");
            return;
        }

        MapLayer grassLayer = findLayer("Grass");
        if (grassLayer == null) {
            Gdx.app.log("GameScreen", "WARNING: Grass layer not found");
            return;
        }

        // 하나의 정적 Body에 모든 Grass 센서를 추가 (최적화)
        com.badlogic.gdx.physics.box2d.BodyDef bodyDef = new com.badlogic.gdx.physics.box2d.BodyDef();
        bodyDef.type = com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody;
        com.badlogic.gdx.physics.box2d.Body grassBody = world.createBody(bodyDef);
        grassBody.setUserData("GRASS");

        // EarClippingTriangulator 재사용
        EarClippingTriangulator triangulator = new EarClippingTriangulator();
        int fixtureCount = 0;

        for (MapObject obj : grassLayer.getObjects()) {
            if (obj instanceof com.badlogic.gdx.maps.objects.PolygonMapObject polyObj) {
                fixtureCount += addPolygonFixtures(grassBody, polyObj.getPolygon(), triangulator);
            } else if (obj instanceof RectangleMapObject rectObj) {
                fixtureCount += addRectangleFixture(grassBody, rectObj.getRectangle());
            }
        }

        Gdx.app.log("GameScreen", String.format("Loaded %d grass fixtures on single body", fixtureCount));
    }

    /**
     * Polygon을 주어진 Body에 Fixture로 추가 (최적화).
     * @return 추가된 Fixture 개수
     */
    private int addPolygonFixtures(Body grassBody, com.badlogic.gdx.math.Polygon polygon, EarClippingTriangulator triangulator) {
        float[] vertices = polygon.getTransformedVertices();
        int vertexCount = vertices.length / 2;

        if (vertexCount <= 8) {
            // 8개 이하: 단일 폴리곤으로 처리
            Vector2[] box2dVertices = new Vector2[vertexCount];
            for (int i = 0; i < vertexCount; i++) {
                box2dVertices[i] = new Vector2(vertices[i * 2] / PPM, vertices[i * 2 + 1] / PPM);
            }
            PolygonShape shape = new PolygonShape();
            shape.set(box2dVertices);
            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.isSensor = true;
            grassBody.createFixture(fixtureDef);
            shape.dispose();
            return 1;
        }

        // 8개 초과: 삼각분할
        ShortArray triangleIndices = triangulator.computeTriangles(vertices);
        int triangleCount = triangleIndices.size / 3;
        final float MIN_AREA = 0.0001f;
        int validCount = 0;

        // 삼각형 버텍스 재사용 (GC 압력 감소)
        Vector2[] triangle = new Vector2[3];
        triangle[0] = new Vector2();
        triangle[1] = new Vector2();
        triangle[2] = new Vector2();

        for (int i = 0; i < triangleCount; i++) {
            int idx0 = triangleIndices.get(i * 3);
            int idx1 = triangleIndices.get(i * 3 + 1);
            int idx2 = triangleIndices.get(i * 3 + 2);

            triangle[0].set(vertices[idx0 * 2] / PPM, vertices[idx0 * 2 + 1] / PPM);
            triangle[1].set(vertices[idx1 * 2] / PPM, vertices[idx1 * 2 + 1] / PPM);
            triangle[2].set(vertices[idx2 * 2] / PPM, vertices[idx2 * 2 + 1] / PPM);

            // 면적 검증
            float area = Math.abs(
                (triangle[1].x - triangle[0].x) * (triangle[2].y - triangle[0].y) -
                    (triangle[2].x - triangle[0].x) * (triangle[1].y - triangle[0].y)
            ) * 0.5f;

            if (area < MIN_AREA) continue;

            PolygonShape shape = new PolygonShape();
            shape.set(triangle);
            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.isSensor = true;
            grassBody.createFixture(fixtureDef);
            shape.dispose();
            validCount++;
        }

        return validCount;
    }

    /**
     * Rectangle을 주어진 Body에 Fixture로 추가 (최적화).
     * @return 추가된 Fixture 개수 (항상 1)
     */
    private int addRectangleFixture(Body grassBody, com.badlogic.gdx.math.Rectangle rect) {
        PolygonShape shape = new PolygonShape();
        // 중심점 기준으로 박스 생성 (Body는 원점에 있으므로 offset 적용)
        shape.setAsBox(
            rect.width / 2f / PPM,
            rect.height / 2f / PPM,
            new Vector2((rect.x + rect.width / 2f) / PPM, (rect.y + rect.height / 2f) / PPM),
            0f
        );

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.isSensor = true;
        grassBody.createFixture(fixtureDef);
        shape.dispose();
        return 1;
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
                    Gdx.app.log("GameScreen", String.format("✓ Checkpoint %d/%d passed", lastCheckpointIndex, totalCheckpoints));
                }
            } else if (!contains && alreadyInside) {
                checkpointsInside.remove(cp.index);
            }
        }

        boolean inStart = startLineBounds.contains(carPos.x, carPos.y);
        if (inStart && !insideStartLine) {
            insideStartLine = true;
            Gdx.app.log("GameScreen", String.format("Start line crossed! lastCheckpointIndex=%d, totalCheckpoints=%d", lastCheckpointIndex, totalCheckpoints));
            if (totalCheckpoints > 0 && lastCheckpointIndex == totalCheckpoints) {
                lastLapTime = lapTimeSeconds;
                if (bestLapTime < 0 || lastLapTime < bestLapTime) bestLapTime = lastLapTime;

                // 총 레이스 시간에 현재 랩 타임 누적
                totalRaceTime += lastLapTime;

                currentLap++;

                // 레이스 완료 확인
                if (currentLap > totalLaps) {
                    onRaceFinished();
                } else {
                    Gdx.app.log("GameScreen", String.format("Lap %d/%d completed! Time: %.3f", currentLap - 1, totalLaps, lastLapTime));
                    lapTimeSeconds = 0f;
                    lastCheckpointIndex = 0;
                    checkpointsInside.clear();
                }
            }
        }
        if (!inStart && insideStartLine) insideStartLine = false;
    }

    // updateGrassZoneCheck() 메서드 제거됨 - Box2D ContactListener에서 자동 처리

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

    /**
     * 레이스 완료 시 호출되는 메서드
     * 게임을 일시정지하고 결과 화면 표시
     */
    private void onRaceFinished() {
        Gdx.app.log("GameScreen", String.format("=== RACE FINISHED ==="));
        Gdx.app.log("GameScreen", String.format("Total Laps: %d", totalLaps));
        Gdx.app.log("GameScreen", String.format("Best Lap Time: %.3f seconds", bestLapTime));
        Gdx.app.log("GameScreen", String.format("Last Lap Time: %.3f seconds", lastLapTime));
        Gdx.app.log("GameScreen", String.format("Total Race Time: %.3f seconds", totalRaceTime));

        // 차량 정지
        if (playerCar != null) {
            playerCar.setLinearVelocity(0, 0);
            playerCar.setAngularVelocity(0);
        }

        // 게임 상태를 종료로 변경
        gameState = GameState.FINISHED;
    }

    /**
     * 레이스 재시작
     * 모든 상태를 초기화하고 처음부터 다시 시작
     */
    private void restartRace() {
        // 랩 관련 상태 초기화
        currentLap = 0; // 완료된 랩 수 초기화
        lapTimeSeconds = 0f;
        lastLapTime = -1f;
        bestLapTime = -1f;
        totalRaceTime = 0f;
        lastCheckpointIndex = 0;
        checkpointsInside.clear();
        insideStartLine = false;

        // 차량 위치 초기화 (시작 지점으로)
        if (playerCar != null) {
            Vector2 spawn = computeSpawnPosition();
            playerCar.setTransform(spawn, (float) Math.PI / 2f); // 초기 각도: 왼쪽으로 90도
            playerCar.setLinearVelocity(0, 0);
            playerCar.setAngularVelocity(0);
        }

        // 차량 내구도/타이어 초기화
        vehicleDurability = 100f;
        tireDurability = 100f;
        pitSelectedCompound = "medium";
        tireCompoundRegion = tireCompoundMediumRegion;

        // 게임 상태 초기화
        gameState = GameState.PRE_START;
        startCountdown = 3.0f;
        startLightsDone = false;
        goTimer = 0f;
        offTimer = 0f;

        Gdx.app.log("GameScreen", "Race restarted");
    }

    /**
     * 메인 메뉴로 돌아가기
     */
    private void exitToMenu() {
        Gdx.app.log("GameScreen", "Exiting to main menu");
        gameRef.setScreen(new com.mygame.f1.screens.MainMenuScreen(gameRef));
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
