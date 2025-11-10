package com.mygame.f1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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

public class GameScreen implements Screen {
    private final Main gameRef;

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
    private Texture backgroundTexture;

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
    private static final boolean USE_TILED_MAP = false;

    public GameScreen() { this(null); }

    public GameScreen(Main game) { this.gameRef = game; }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(1600 / PPM, 900 / PPM, camera);
        camera.zoom = 0.5f;

        batch = new SpriteBatch();
        // AssetManager를 통해 에셋 로드
        carTexture = Main.assetManager.get("pitstop_car_3.png", Texture.class);
        backgroundTexture = Main.assetManager.get("Track_t2.png", Texture.class);

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
            map = new TmxMapLoader().load("track.tmx");
            mapRenderer = new OrthogonalTiledMapRenderer(map, 1 / PPM);

            if (map.getLayers().get("Collision") != null) {
                for (RectangleMapObject object : map.getLayers().get("Collision").getObjects().getByType(RectangleMapObject.class)) {
                    Rectangle rect = object.getRectangle();
                    createWall(rect.x / PPM, rect.y / PPM, rect.width / PPM, rect.height / PPM);
                }
            }
        }

        createPlayerCar();
        createScreenBoundaryWalls();

        initialAngle = playerCar.getAngle();

        // pause UI init
        pauseSkin = SkinFactory.createDefaultSkin();
        pauseStage = new Stage(new ScreenViewport());
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
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        float wallThickness = 0.1f;

        createWall(0, worldHeight - wallThickness, worldWidth, wallThickness);
        createWall(0, 0, worldWidth, wallThickness);
        createWall(0, 0, wallThickness, worldHeight);
        createWall(worldWidth - wallThickness, 0, wallThickness, worldHeight);
    }

    private void createPlayerCar() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(960 / PPM, 640 / PPM);
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

    public void update(float delta) {
        float frameTime = Math.min(delta, 0.25f);
        accumulator += frameTime;
        while (accumulator >= TIME_STEP) {
            world.step(TIME_STEP, 8, 3);
            accumulator -= TIME_STEP;
        }

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
        }

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (USE_TILED_MAP && mapRenderer != null) {
            mapRenderer.setView(camera);
            mapRenderer.render();
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float bgWidth = viewport.getWorldWidth();
        float bgHeight = viewport.getWorldHeight();
        batch.draw(backgroundTexture, 0, 0, bgWidth, bgHeight);

        Vector2 carPos = playerCar.getPosition();
        float carWidth = 12.80f / PPM;
        float carHeight = 25.60f / PPM;
        batch.draw(carTexture,
            carPos.x - carWidth / 2, carPos.y - carHeight / 2,
            carWidth / 2, carHeight / 2,
            carWidth, carHeight,
            1, 1,
            playerCar.getAngle() * MathUtils.radiansToDegrees,
            0, 0, carTexture.getWidth(), carTexture.getHeight(),
            false, false);

        batch.end();

        box2DDebugRenderer.render(world, camera.combined);

        if (paused) {
            drawPauseOverlay();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        if (pauseStage != null) pauseStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        world.dispose();
        box2DDebugRenderer.dispose();
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        batch.dispose();
        if (pauseStage != null) pauseStage.dispose();
        if (pauseSkin != null) pauseSkin.dispose();
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
}
