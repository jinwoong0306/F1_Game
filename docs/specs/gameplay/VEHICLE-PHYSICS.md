# VEHICLE-PHYSICS.md

## Overview
차량의 가속, 조향, 브레이크, 마찰력 등 모든 물리적 움직임을 정의하는 핵심 스펙입니다.

**Owner**: Physics Lead  
**Status**: Draft  
**Last Updated**: 2025-01-15

---

## Dependencies
- **Parent Specs**: `docs/specs/core/PHYSICS-ENGINE.md`
- **Related Systems**: `TIRE-SYSTEM.md`, `DAMAGE-MODEL.md`
- **External Libraries**: Box2D (bundled with libGDX)

---

## Requirements

### Functional Requirements

**FR-001: Forward Acceleration**
- 차량은 UP/W 키 입력 시 전방으로 가속해야 함
- 가속도는 타이어 종류와 차량 내구도에 영향을 받음
- 최고 속도는 300 km/h (83.33 m/s)

**FR-002: Reverse Acceleration**
- 차량은 DOWN/S 키 입력 시 후방으로 가속해야 함
- 후진 최고 속도는 50 km/h (13.89 m/s)
- 후진 중 조향 방향이 반전되어야 함

**FR-003: Steering**
- LEFT/A 키 입력 시 반시계방향 회전
- RIGHT/D 키 입력 시 시계방향 회전
- 회전 속도는 현재 속도에 비례 (저속일 때 조향 더 민감)

**FR-004: Braking**
- SPACE 키 입력 시 즉시 감속
- 브레이크 감속률은 타이어 그립에 영향을 받음
- 완전 정지 시 미끄러짐 없음

**FR-005: Lateral Friction**
- 차량은 진행 방향과 수직인 힘에 마찰 저항을 가짐
- 마찰력은 타이어 그립 계수에 비례
- 드리프트는 고속 코너링 시 자연스럽게 발생

**FR-006: Air Resistance**
- 속도에 비례하는 공기 저항 적용
- 저항은 속도의 제곱에 비례 (realistic drag model)

### Non-Functional Requirements

**NFR-001: Performance**
- 물리 업데이트는 고정 타임스텝 (1/60초)
- 차량당 CPU 사용률 < 5% (60 FPS 유지)

**NFR-002: Stability**
- 차량이 벽에 끼거나 튕겨나가는 현상 없음
- 고속 충돌 시에도 물리 시뮬레이션 안정적

**NFR-003: Responsiveness**
- 입력에서 물리 반응까지 지연 < 16ms (1 frame)

---

## Design

### 1. Box2D Body Configuration

```java
// 차량 Body 생성
BodyDef bodyDef = new BodyDef();
bodyDef.type = BodyDef.BodyType.DynamicBody;
bodyDef.position.set(startX, startY);
bodyDef.angle = startAngle;
bodyDef.linearDamping = 0.1f;      // 기본 선형 감쇠
bodyDef.angularDamping = 0.3f;     // 기본 각 감쇠

Body vehicleBody = world.createBody(bodyDef);

// 차량 Fixture 생성 (사각형)
PolygonShape shape = new PolygonShape();
shape.setAsBox(0.5f, 1.0f);  // 1m x 2m (half-width, half-height)

FixtureDef fixtureDef = new FixtureDef();
fixtureDef.shape = shape;
fixtureDef.density = 500f;    // kg/m² (총 질량: 1000kg)
fixtureDef.friction = 0.3f;
fixtureDef.restitution = 0.1f; // 충돌 시 반발력 최소화

vehicleBody.createFixture(fixtureDef);
shape.dispose();
```

### 2. Acceleration System

#### 가속 곡선 (Acceleration Curve)
```
속도 구간         가속도 계수
0 - 50%           1.0x (선형)
50% - 80%         0.6x (로그)
80% - 100%        0.3x (포화)
```

#### 구현 알고리즘
```java
public class VehicleController {
    private static final float MAX_SPEED_MS = 83.33f; // 300 km/h
    private static final float MAX_REVERSE_SPEED_MS = 13.89f; // 50 km/h
    private static final float BASE_ACCELERATION = 15000f; // Newtons
    
    private Body body;
    private float accelerationInput = 0f; // -1 to 1
    private float tireGripModifier = 1.0f; // from TireManager
    private float durabilityModifier = 1.0f; // from DamageManager
    
    public void update(float delta) {
        applyAcceleration(delta);
        applyLateralFriction();
        applyAirResistance();
        enforceSpeedLimits();
    }
    
    private void applyAcceleration(float delta) {
        if (accelerationInput == 0) return;
        
        Vector2 forward = body.getWorldVector(new Vector2(0, 1));
        float currentSpeed = body.getLinearVelocity().len();
        
        // 가속도 곡선 적용
        float speedRatio = currentSpeed / MAX_SPEED_MS;
        float accelMultiplier = 1.0f;
        
        if (speedRatio < 0.5f) {
            accelMultiplier = 1.0f; // 선형 구간
        } else if (speedRatio < 0.8f) {
            accelMultiplier = 0.6f; // 로그 구간
        } else {
            accelMultiplier = 0.3f; // 포화 구간
        }
        
        // 최종 가속력 계산
        float finalAccel = BASE_ACCELERATION 
            * accelerationInput 
            * accelMultiplier 
            * tireGripModifier 
            * durabilityModifier;
        
        Vector2 force = forward.scl(finalAccel);
        body.applyForceToCenter(force, true);
    }
}
```

### 3. Steering System

#### 회전 속도 공식
```
Angular Impulse = Base Torque × Speed Factor × Steering Input
Speed Factor = 1.0 - (current_speed / max_speed) × 0.5
```

#### 구현
```java
private static final float BASE_TORQUE = 500f;

private void applySteering(float steeringInput) {
    float currentSpeed = body.getLinearVelocity().len();
    float speedFactor = 1.0f - (currentSpeed / MAX_SPEED_MS) * 0.5f;
    
    // 후진 중이면 조향 반전
    Vector2 forward = body.getWorldVector(new Vector2(0, 1));
    float forwardDot = forward.dot(body.getLinearVelocity());
    if (forwardDot < 0) {
        steeringInput *= -1; // 후진 시 조향 반전
    }
    
    float torque = BASE_TORQUE * steeringInput * speedFactor;
    body.applyAngularImpulse(torque, true);
}
```

### 4. Lateral Friction (Drift Simulation)

#### Kill Orthogonal Velocity 알고리즘
```java
private void applyLateralFriction() {
    // 차량의 우측 방향 벡터 (월드 좌표계)
    Vector2 rightNormal = body.getWorldVector(new Vector2(1, 0));
    
    // 현재 속도의 횡방향 성분 계산
    float lateralVelocity = rightNormal.dot(body.getLinearVelocity());
    
    // 마찰 임펄스 계산
    float grip = tireGripModifier;
    
    // 조향 중이면 그립 감소 (드리프트 유도)
    if (Math.abs(steeringInput) > 0.1f) {
        grip *= 0.7f; // 30% 그립 감소
    }
    
    Vector2 impulse = rightNormal.scl(-body.getMass() * lateralVelocity * grip);
    body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
}
```

### 5. Air Resistance

```java
private void applyAirResistance() {
    Vector2 velocity = body.getLinearVelocity();
    float speed = velocity.len();
    
    if (speed < 0.1f) return; // 정지 시 무시
    
    // Drag Force = 0.5 × Cd × A × ρ × v²
    float dragCoefficient = 0.35f; // F1 차량 Cd
    float dragForce = 0.5f * dragCoefficient * speed * speed;
    
    Vector2 drag = velocity.nor().scl(-dragForce);
    body.applyForceToCenter(drag, true);
}
```

### 6. Speed Limits

```java
private void enforceSpeedLimits() {
    Vector2 velocity = body.getLinearVelocity();
    Vector2 forward = body.getWorldVector(new Vector2(0, 1));
    
    float forwardSpeed = forward.dot(velocity);
    
    // 전진 속도 제한
    if (forwardSpeed > MAX_SPEED_MS) {
        velocity = forward.scl(MAX_SPEED_MS).add(
            velocity.sub(forward.scl(forwardSpeed))
        );
        body.setLinearVelocity(velocity);
    }
    
    // 후진 속도 제한
    if (forwardSpeed < -MAX_REVERSE_SPEED_MS) {
        velocity = forward.scl(-MAX_REVERSE_SPEED_MS).add(
            velocity.sub(forward.scl(forwardSpeed))
        );
        body.setLinearVelocity(velocity);
    }
}
```

---

## Collision Response

### Velocity Damping on Impact
```java
@Override
public void beginContact(Contact contact) {
    Fixture fixtureA = contact.getFixtureA();
    Fixture fixtureB = contact.getFixtureB();
    
    // 차량-벽 충돌 감지
    if (isVehicleWallCollision(fixtureA, fixtureB)) {
        Body vehicleBody = getVehicleBody(fixtureA, fixtureB);
        
        // 속도 30% 감소
        Vector2 velocity = vehicleBody.getLinearVelocity();
        vehicleBody.setLinearVelocity(velocity.scl(0.7f));
        
        // 각속도 50% 감소 (스핀 방지)
        float angularVel = vehicleBody.getAngularVelocity();
        vehicleBody.setAngularVelocity(angularVel * 0.5f);
        
        // DamageManager에 충돌 알림
        damageManager.onCollision(impactForce);
    }
}
```

---

## Input Handling

### Smoothed Input (Anti-Jitter)
```java
private float targetAcceleration = 0f;
private float currentAcceleration = 0f;
private static final float INPUT_SMOOTHING = 0.1f; // 100ms ramp time

public void handleInput() {
    // 목표 가속도 설정
    if (Gdx.input.isKeyPressed(Input.Keys.UP) || 
        Gdx.input.isKeyPressed(Input.Keys.W)) {
        targetAcceleration = 1.0f;
    } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || 
               Gdx.input.isKeyPressed(Input.Keys.S)) {
        targetAcceleration = -0.5f; // 후진은 절반 힘
    } else {
        targetAcceleration = 0f;
    }
    
    // 스무딩 적용
    currentAcceleration = MathUtils.lerp(
        currentAcceleration, 
        targetAcceleration, 
        INPUT_SMOOTHING
    );
    
    accelerationInput = currentAcceleration;
}
```

---

## Tuning Parameters

### 조정 가능한 파라미터 (GameConfig.properties)
```properties
vehicle.maxSpeed=300              # km/h
vehicle.maxReverseSpeed=50        # km/h
vehicle.baseAcceleration=15000    # Newtons
vehicle.baseTorque=500            # Angular impulse
vehicle.lateralGrip=0.8           # Base grip coefficient
vehicle.driftGripReduction=0.3    # Grip loss during steering
vehicle.dragCoefficient=0.35      # Air resistance
vehicle.linearDamping=0.1         # Base damping
vehicle.angularDamping=0.3        # Rotation damping
vehicle.collisionSpeedLoss=0.3    # 30% speed reduction on impact
vehicle.collisionAngularLoss=0.5  # 50% angular velocity reduction
```

---

## Testing Strategy

### Unit Tests

#### Test 1: Linear Acceleration
```java
@Test
@DisplayName("Vehicle should reach 50% max speed in 2 seconds")
public void testLinearAcceleration() {
    // Given
    VehicleController vehicle = new VehicleController(world);
    vehicle.setSpeed(0);
    
    // When: Apply full throttle for 2 seconds
    for (int i = 0; i < 120; i++) { // 120 frames at 60 FPS
        vehicle.setAccelerationInput(1.0f);
        vehicle.update(1/60f);
        world.step(1/60f, 8, 3);
    }
    
    // Then
    float expectedSpeed = VehicleController.MAX_SPEED_MS * 0.5f;
    assertThat(vehicle.getSpeed())
        .isCloseTo(expectedSpeed, within(5f));
}
```

#### Test 2: Speed Limit Enforcement
```java
@Test
@DisplayName("Vehicle should not exceed max speed")
public void testSpeedLimit() {
    // Given
    VehicleController vehicle = new VehicleController(world);
    vehicle.setSpeed(VehicleController.MAX_SPEED_MS + 10f);
    
    // When
    vehicle.update(1/60f);
    
    // Then
    assertThat(vehicle.getSpeed())
        .isLessThanOrEqualTo(VehicleController.MAX_SPEED_MS);
}
```

#### Test 3: Collision Damping
```java
@Test
@DisplayName("Collision should reduce speed by 30%")
public void testCollisionDamping() {
    // Given
    VehicleController vehicle = new VehicleController(world);
    vehicle.setSpeed(50f);
    
    // When
    vehicle.handleCollision(CollisionType.WALL, 1000f);
    
    // Then
    assertThat(vehicle.getSpeed())
        .isCloseTo(35f, within(2f));
}
```

#### Test 4: Lateral Friction
```java
@Test
@DisplayName("Lateral velocity should be dampened")
public void testLateralFriction() {
    // Given
    VehicleController vehicle = new VehicleController(world);
    vehicle.getBody().setLinearVelocity(10f, 5f); // Forward + Sideways
    
    // When
    vehicle.update(1/60f);
    
    // Then: Lateral velocity reduced, forward velocity maintained
    Vector2 velocity = vehicle.getBody().getLinearVelocity();
    assertThat(Math.abs(velocity.x)).isLessThan(3f); // Reduced
    assertThat(velocity.y).isCloseTo(10f, within(1f)); // Maintained
}
```

### Integration Tests

#### Test 5: Full Lap Physics Consistency
```java
@Test
@DisplayName("Vehicle physics should remain stable over full lap")
public void testLapStability() {
    // Setup track and vehicle
    TrackLoader track = new TrackLoader("test_track.tmx", world);
    VehicleController vehicle = new VehicleController(world);
    
    // Simulate 1 lap (approx 60 seconds)
    for (int i = 0; i < 3600; i++) {
        vehicle.setAccelerationInput(0.8f);
        vehicle.update(1/60f);
        world.step(1/60f, 8, 3);
        
        // Verify no NaN values
        assertThat(vehicle.getPosition().x).isFinite();
        assertThat(vehicle.getPosition().y).isFinite();
    }
    
    // Verify vehicle completed lap
    assertThat(vehicle.getCurrentLap()).isGreaterThan(0);
}
```

### Performance Benchmarks

```java
@Test
@DisplayName("Physics update should complete in <1ms")
public void testPhysicsPerformance() {
    VehicleController vehicle = new VehicleController(world);
    
    long startTime = System.nanoTime();
    
    for (int i = 0; i < 1000; i++) {
        vehicle.update(1/60f);
    }
    
    long duration = (System.nanoTime() - startTime) / 1_000_000; // ms
    
    assertThat(duration).isLessThan(1000); // <1ms per update
}
```

---

## Validation Criteria

### Physics Accuracy
- [ ] Vehicle accelerates smoothly without sudden jumps
- [ ] Top speed reached within 5% of specified value
- [ ] Steering feels responsive at all speeds
- [ ] Drift occurs naturally during high-speed turns
- [ ] No "moonwalking" (vehicle moving sideways unnaturally)

### Performance
- [ ] 60 FPS maintained with 1 vehicle
- [ ] 60 FPS maintained with 4 vehicles (multiplayer)
- [ ] No frame drops during collisions
- [ ] Memory stable (no leaks after 1000+ physics steps)

### Stability
- [ ] No vehicles stuck in walls
- [ ] No vehicles flying into air on collision
- [ ] Angular velocity doesn't spiral out of control
- [ ] Physics deterministic (same inputs = same outputs)

### Player Feel
- [ ] Controls feel intuitive (playtester feedback)
- [ ] Collision impact feels impactful but not frustrating
- [ ] Speed sensation conveyed through camera/particles
- [ ] Drift controllable by skilled players

---

## Known Issues & Limitations

### Current Limitations
1. **Single Body Model**: Vehicle is single rigid body (no suspension)
2. **Simplified Aerodynamics**: Drag is velocity-squared only (no downforce)
3. **No Weight Transfer**: Braking/acceleration doesn't affect grip distribution
4. **Fixed Mass**: Vehicle mass is constant (no fuel consumption)

### Future Enhancements
- [ ] Multi-body suspension model (wheels as separate bodies)
- [ ] Downforce calculation for high-speed stability
- [ ] Weight transfer affecting corner grip
- [ ] Fuel consumption reducing mass over race

---

## References

### Box2D Documentation
- Box2D Manual: https://box2d.org/documentation/
- Friction & Restitution: https://box2d.org/documentation/md__d_1__git_hub_box2d_docs_dynamics.html

### Physics Research
- "Vehicle Dynamics for Racing Games" - Brian Beckman (Microsoft Research)
- "Real-Time Racing Car Simulation" - Marco Monster

### LibGDX Resources
- LibGDX Box2D Wiki: https://libgdx.com/wiki/extensions/physics/box2d
- LibGDX Physics Best Practices: https://libgdx.com/wiki/articles/box2d

---

## Change Log

### [2025-01-15] Initial Draft
- Defined core acceleration/steering mechanics
- Implemented lateral friction algorithm
- Added collision response system
- Created initial test suite

---

**Version**: 1.0.0  
**Status**: Ready for Implementation  
**Next Review**: After Phase 1 completion
