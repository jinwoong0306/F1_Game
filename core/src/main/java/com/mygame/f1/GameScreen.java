// This should match the package name in your project's core/src folder
package com.mygame.f1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen implements Screen {

    // --- 물리 시뮬레이션 안정화를 위한 변수 ---
    private float accumulator = 0;
    private static final float TIME_STEP = 1 / 60f; // 1초에 60번 계산

    private OrthographicCamera camera;
    private Viewport viewport;
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private SpriteBatch batch;
    private Texture carTexture;
    private Texture backgroundTexture;

    // --- Physics Objects ---
    private World world;
    private Box2DDebugRenderer box2DDebugRenderer;
    private Body playerCar;

    // --- 초보자도 쉽게 조작 가능한 파라미터 ---
    private float maxForwardSpeed = 5f;        // 전진 최고 속도 (더 느리게: 7 -> 5)
    private float maxReverseSpeed = 2f;        // 후진 최고 속도 (더 느리게: 2.8 -> 2)
    private float forwardAcceleration = 5f;    // 전진 가속력 (더 빠르게: 4.2 -> 5)
    private float reverseAcceleration = 2.5f;  // 후진 가속력 (더 빠르게: 2.1 -> 2.5)
    private float brakingPower = 10f;          // 제동력 (더 강하게: 8.4 -> 10)
    private float turningPower = 10f;          // 회전력 증가 (12 -> 18) - 회전 조건이 엄격해져서 보상
    private float grip = 18.0f;                 // 타이어 그립 더 증가 (6.0 -> 8.0)
    private float minSpeedForTurn = 0.8f;      // 회전 최소 속도 감소 (1.0 -> 0.8)

    // --- 조향 각도 제한 ---
    private float maxSteeringAngle = 60f;      // 최대 조향 각도 (좌우 60도)
    private float initialAngle = 0f;           // 초기 각도 (중앙 기준점)

    // --- 부드럽고 예측 가능한 조작감 ---
    private float currentAcceleration = 0f;
    private float accelerationSmoothness = 7f;  // 가속 반응 더 빠르게 (5 -> 7)
    private float currentTorque = 0f;
    private float torqueSmoothness = 15f;       // 회전 더 부드럽게 (12 -> 15)

    // --- 안정적인 감쇠 시스템 ---
    private float defaultLinearDamping = 2.0f;  // 기본 감쇠 더 증가 (1.5 -> 2.0)
    private float brakingLinearDamping = 5.0f;  // 제동 감쇠 더 증가 (4.0 -> 5.0)
    private float collisionDamping = 4.0f;      // 충돌 감쇠 감소 (10.0 -> 4.0) - 끼임 방지

    // --- 충돌 감지 ---
    private boolean isColliding = false;
    private boolean wasColliding = false;       // 이전 프레임 충돌 상태
    private float collisionTimer = 0f;
    private float collisionDuration = 0.2f;     // 충돌 효과 지속 시간 감소 (0.3 -> 0.2)

    public static final float PPM = 100;
    private static final boolean USE_TILED_MAP = true;

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(1600 / PPM, 900 / PPM, camera);
        camera.zoom = 0.4f;

        batch = new SpriteBatch();
        carTexture = new Texture("pitstop_car_3.png");
        backgroundTexture = new Texture("test_racingTrack.png");

        world = new World(new Vector2(0, 0), true);
        box2DDebugRenderer = new Box2DDebugRenderer();

        // 충돌 리스너 설정
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                // 플레이어 카가 벽에 충돌했는지 확인
                if (contact.getFixtureA().getBody() == playerCar ||
                    contact.getFixtureB().getBody() == playerCar) {
                    isColliding = true;
                    collisionTimer = collisionDuration;

                    // 충돌 시 속도 감소 (덜 강하게)
                    Vector2 velocity = playerCar.getLinearVelocity();
                    playerCar.setLinearVelocity(velocity.scl(0.4f)); // 속도를 40%로 감소 (0.15 -> 0.4)
                    playerCar.setAngularVelocity(playerCar.getAngularVelocity() * 0.3f); // 회전 속도 감소 (0.1 -> 0.3)
                }
            }

            @Override
            public void endContact(Contact contact) {
                // 충돌 종료는 타이머로 처리
            }

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

        // 초기 각도 저장 (중앙 기준점)
        initialAngle = playerCar.getAngle();
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
        fixtureDef.friction = 0.3f;             // 벽 마찰력 대폭 감소 (0.8 -> 0.3)
        fixtureDef.restitution = 0.02f;         // 약간의 반발력 추가 (0.0 -> 0.02)

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
        bodyDef.angularDamping = 20.0f;  // 회전 감쇠 더 증가 (5.0 -> 6.0)
        playerCar = world.createBody(bodyDef);

        FixtureDef fixtureDef = new FixtureDef();
        PolygonShape carShape = new PolygonShape();
        // 자동차 크기 1.5배 감소
        carShape.setAsBox(10.67f / PPM, 21.33f / PPM);
        fixtureDef.shape = carShape;
        fixtureDef.density = 2.5f;      // 밀도 더 증가로 안정성 극대화 (2.0 -> 2.5)
        fixtureDef.friction = 0.3f;     // 마찰력 감소 (0.8 -> 0.3) - 끼임 방지
        fixtureDef.restitution = 0.02f; // 약간의 반발력 (0.0 -> 0.02) - 끼임 방지

        playerCar.createFixture(fixtureDef);
        carShape.dispose();
    }

    public void update(float delta) {
        // 물리 시뮬레이션
        world.step(Math.min(delta, 1/30f), 8, 3);

        // 충돌 타이머 업데이트
        if (collisionTimer > 0) {
            collisionTimer -= delta;
            if (collisionTimer <= 0) {
                wasColliding = isColliding;
                isColliding = false;
            }
        }

        // 입력 처리
        handleInput(delta);
        updateSteering(delta);

        // 타이어 마찰 및 속도 제한
        updateFriction();
        limitSpeed();
//        limitSteeringAngle();  // 조향 각도 제한

        // 충돌 중일 때 추가 감쇠 적용 (덜 강하게)
        if (isColliding) {
            playerCar.setLinearDamping(collisionDamping);

            // 충돌 중 벽에서 살짝 밀어내기 (끼임 방지)
            if (playerCar.getLinearVelocity().len() < 0.5f) {
                // 매우 느린 속도일 때 벽에서 살짝 떨어지도록
                Vector2 forwardDirection = playerCar.getWorldVector(new Vector2(0, -0.3f));
                playerCar.applyLinearImpulse(forwardDirection, playerCar.getWorldCenter(), true);
            }
        }

        // 카메라 부드럽게 따라가기
        float lerpFactor = 5f * delta;
        camera.position.x = MathUtils.lerp(camera.position.x, playerCar.getPosition().x, lerpFactor);
        camera.position.y = MathUtils.lerp(camera.position.y, playerCar.getPosition().y, lerpFactor);
        camera.update();

        if (USE_TILED_MAP && mapRenderer != null) {
            mapRenderer.setView(camera);
        }
    }

    private void handleInput(float delta) {
        // 제동 처리 (스페이스바)
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            playerCar.setLinearDamping(brakingLinearDamping);
        } else if (!isColliding) {
            playerCar.setLinearDamping(defaultLinearDamping);
        }

        // 현재 속도 및 방향 계산
        Vector2 forwardNormal = playerCar.getWorldVector(new Vector2(0, 1));
        float forwardSpeed = playerCar.getLinearVelocity().dot(forwardNormal);
        float currentSpeed = playerCar.getLinearVelocity().len();

        // 목표 가속도 계산 (전진/후진 구분)
        float targetAcceleration = 0;
        boolean movingForward = false;
        boolean movingReverse = false;

        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            targetAcceleration = forwardAcceleration;
            movingForward = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            targetAcceleration = -reverseAcceleration;  // 후진은 느리게
            movingReverse = true;
        }

        // 현재 가속도를 목표값으로 부드럽게 보간
        currentAcceleration = MathUtils.lerp(currentAcceleration, targetAcceleration, accelerationSmoothness * delta);

        // 자동차 방향으로 힘 적용
        if (Math.abs(currentAcceleration) > 0.1f) {
            Vector2 forceVector = playerCar.getWorldVector(new Vector2(0, currentAcceleration));
            playerCar.applyForceToCenter(forceVector, true);
        }

        // 회전 처리 (제자리 회전 방지 - 반드시 가속 중일 때만 회전)
        // 최소 속도 이상이고, 실제로 전진/후진 키를 누르고 있을 때만 회전 가능
        float speedFactor = 0f;

        // 전진 또는 후진 키를 누르고 있고, 최소 속도 이상일 때만 회전
        if (currentSpeed >= minSpeedForTurn && (movingForward || movingReverse)) {
            // 전진 중이고 앞으로 이동하는 경우
            if (movingForward && forwardSpeed > 0.5f) {
                speedFactor = Math.min(currentSpeed / 4f, 1.0f);
            }
            // 후진 중이고 뒤로 이동하는 경우 (후진 시 조향 반대)
            else if (movingReverse && forwardSpeed < -0.5f) {
                speedFactor = Math.min(currentSpeed / 3f, 0.7f);  // 후진 시 조향력 감소
            }
        }

        // 목표 토크 계산 (speedFactor가 0이면 회전 안 함)
//        float targetTorque = 0;
//        if (speedFactor > 0.1f) {  // 충분한 속도가 있을 때만
//            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
//                // 후진 중이면 조향 반대로
//                targetTorque = (movingReverse && forwardSpeed < -0.5f) ? -turningPower * speedFactor : turningPower * speedFactor;
//            }
//            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
//                targetTorque = (movingReverse && forwardSpeed < -0.5f) ? turningPower * speedFactor : -turningPower * speedFactor;
//            }
//        }
//
//        // 현재 토크를 목표값으로 부드럽게 보간
//        //currentTorque = MathUtils.lerp(currentTorque, targetTorque, torqueSmoothness * delta);
//
//        if (Math.abs(targetTorque) > 0.1f) {
//            playerCar.applyTorque(targetTorque, true);
//        }
    }
    private void updateSteering(float delta) {
        // 1. 목표 회전 속도 설정 (단위: radians/sec)
        // 이 값이 차가 얼마나 빨리 돌지를 결정합니다. 180도/초 정도로 설정.
        float targetAngularVelocity = 0;
        float maxAngularVelocity = MathUtils.degreesToRadians * 180; // 초당 180도

        // 2. 현재 주행 상태 확인 (handleInput의 로직 재활용)
        Vector2 forwardNormal = playerCar.getWorldVector(new Vector2(0, 1));
        float forwardSpeed = playerCar.getLinearVelocity().dot(forwardNormal);
        boolean movingForward = Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W);
        boolean movingReverse = Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S);

        // 3. 키 입력에 따라 목표 회전 속도 결정
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            // 후진 시 조향 반대 적용
            targetAngularVelocity = (movingReverse && forwardSpeed < -0.5f) ? -maxAngularVelocity : maxAngularVelocity;
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            targetAngularVelocity = (movingReverse && forwardSpeed < -0.5f) ? maxAngularVelocity : -maxAngularVelocity;
        }

        // 4. 현재 회전 속도에서 목표 회전 속도로 부드럽게 변경하기 위한 "충격량" 계산
        // 이것이 바로 부드러움을 만드는 핵심 로직입니다.
        float currentAngularVelocity = playerCar.getAngularVelocity();
        float velocityChange = targetAngularVelocity - currentAngularVelocity;

        // 차의 회전 관성(Inertia)을 고려하여 필요한 충격량을 정확히 계산
        float impulse = playerCar.getInertia() * velocityChange;

        // 계산된 충격량을 적용하여 회전 속도를 직접 제어
        playerCar.applyAngularImpulse(impulse, true);
    }


    private void updateFriction() {
        // 조향 중일 때와 아닐 때 그립 조정
        boolean isSteering = Gdx.input.isKeyPressed(Input.Keys.LEFT) ||
            Gdx.input.isKeyPressed(Input.Keys.RIGHT) ||
            Gdx.input.isKeyPressed(Input.Keys.A) ||
            Gdx.input.isKeyPressed(Input.Keys.D);
        float gripFactor = isSteering ? 0.9f : 1.0f;  // 조향 중 그립 더 증가 (0.85 -> 0.9)

        // 향상된 횡방향 마찰 (드리프트 방지)
        Vector2 lateralVelocity = getLateralVelocity();
//        Vector2 impulse = lateralVelocity.scl(-playerCar.getMass() * grip * gripFactor);
        Vector2 frictionForce = lateralVelocity.scl(-playerCar.getMass() * grip);
        playerCar.applyForceToCenter(frictionForce, true);
//        playerCar.applyLinearImpulse(impulse, playerCar.getWorldCenter(), true);

        // 전진 방향 저항 (공기 저항)
        Vector2 forwardVelocity = getForwardVelocity();
        float forwardSpeed = forwardVelocity.len();
        if (forwardSpeed > 0.1f) {
            float dragCoefficient = 0.05f * forwardSpeed;  // 공기 저항 더 증가 (0.04 -> 0.05)
            Vector2 drag = forwardVelocity.scl(-dragCoefficient * playerCar.getMass());
            playerCar.applyForceToCenter(drag, true);
        }
    }

    private void limitSpeed() {
        // 전진/후진 방향 확인
        Vector2 forwardNormal = playerCar.getWorldVector(new Vector2(0, 1));
        float forwardSpeed = playerCar.getLinearVelocity().dot(forwardNormal);

        float speed = playerCar.getLinearVelocity().len();

        // 전진 중일 때
        if (forwardSpeed > 0 && speed > maxForwardSpeed) {
            playerCar.setLinearVelocity(playerCar.getLinearVelocity().scl(maxForwardSpeed / speed));
        }
        // 후진 중일 때 (더 낮은 최고 속도)
        else if (forwardSpeed < 0 && speed > maxReverseSpeed) {
            playerCar.setLinearVelocity(playerCar.getLinearVelocity().scl(maxReverseSpeed / speed));
        }
    }

    private Vector2 getLateralVelocity() {
        Vector2 rightNormal = playerCar.getWorldVector(new Vector2(1, 0));
        float rightSpeed = playerCar.getLinearVelocity().dot(rightNormal);
        return rightNormal.scl(rightSpeed);
    }

    private Vector2 getForwardVelocity() {
        Vector2 forwardNormal = playerCar.getWorldVector(new Vector2(0, 1));
        float forwardSpeed = playerCar.getLinearVelocity().dot(forwardNormal);
        return forwardNormal.scl(forwardSpeed);
    }

//    private void limitSteeringAngle() {
//        // 현재 각도를 -180 ~ 180 범위로 정규화
//        float currentAngle = playerCar.getAngle() * MathUtils.radiansToDegrees;
//        float initialAngleDeg = initialAngle * MathUtils.radiansToDegrees;
//
//        // 초기 각도 기준으로 회전한 각도 계산
//        float angleDifference = currentAngle - initialAngleDeg;
//
//        // -180 ~ 180 범위로 정규화
//        while (angleDifference > 180) angleDifference -= 360;
//        while (angleDifference < -180) angleDifference += 360;
//
//        // 최대 조향 각도를 벗어나면 각도를 제한
//        if (angleDifference > maxSteeringAngle) {
//            float newAngle = initialAngleDeg + maxSteeringAngle;
//            playerCar.setTransform(playerCar.getPosition(), newAngle * MathUtils.degreesToRadians);
//            playerCar.setAngularVelocity(0);  // 각속도를 0으로 설정하여 더 이상 회전하지 않도록
//        } else if (angleDifference < -maxSteeringAngle) {
//            float newAngle = initialAngleDeg - maxSteeringAngle;
//            playerCar.setTransform(playerCar.getPosition(), newAngle * MathUtils.degreesToRadians);
//            playerCar.setAngularVelocity(0);
//        }
//    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 배경 이미지 그리기
        float bgWidth = viewport.getWorldWidth();
        float bgHeight = viewport.getWorldHeight();
        batch.draw(backgroundTexture,
            0, 0,
            bgWidth, bgHeight);

        // 자동차 그리기 (크기 1.5배 감소: 32/PPM -> 21.33/PPM, 64/PPM -> 42.67/PPM)
        Vector2 carPos = playerCar.getPosition();
        float carWidth = 21.33f / PPM;
        float carHeight = 42.67f / PPM;
        batch.draw(carTexture,
            carPos.x - carWidth / 2, carPos.y - carHeight / 2,
            carWidth / 2, carHeight / 2,
            carWidth, carHeight,
            1, 1,
            playerCar.getAngle() * MathUtils.radiansToDegrees,
            0, 0, carTexture.getWidth(), carTexture.getHeight(),
            false, false);

        batch.end();

        // 디버그 렌더링
        box2DDebugRenderer.render(world, camera.combined);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        world.dispose();
        box2DDebugRenderer.dispose();
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        batch.dispose();
        carTexture.dispose();
        backgroundTexture.dispose();
    }

    @Override
    public void hide() { }

    @Override
    public void pause() { }

    @Override
    public void resume() { }
}
