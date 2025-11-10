# Repository Guidelines

## 프로젝트 구조와 모듈 구성
이 저장소는 Gradle 다중 모듈로 구성되며, 게임 로직은 `core/src/main/java/com/mygame/f1`에, 데스크톱 런처는 `lwjgl3/src/main/java/com/mygame/f1/lwjgl3`에 위치합니다. 모든 런타임 에셋과 `assets.txt`는 `assets/`에 집중 배치하고, `core`와 `lwjgl3` 각각의 `build/` 폴더는 생성물만 담도록 유지하세요. 새 테스트 코드는 `core/src/test/java/com/mygame/f1`에 두어 프로덕션 코드와 분리합니다.

## 빌드·테스트·개발 명령
- `./gradlew lwjgl3:run` 또는 `gradlew.bat lwjgl3:run` : 자산 폴더를 작업 디렉터리로 지정해 즉시 플레이테스트합니다.
- `./gradlew lwjgl3:jar` 및 `lwjgl3:jarWin|jarMac|jarLinux` : 교차 플랫폼 JAR 또는 OS 전용 JAR을 만듭니다.
- `./gradlew generateAssetList` : `assets/assets.txt`를 재생성합니다(대량 자산 갱신 직후 수동 실행 권장).
- `./gradlew clean build` : Java 17 컴파일과 런처 패키징을 모두 검증해 스크립트·경로 일관성을 보장합니다.

## 코딩 스타일·네이밍 규칙
최상위 `.editorconfig`는 UTF-8, LF, 공백 들여쓰기, Java/Kotlin 4칸·Gradle 2칸을 강제합니다. 클래스·스크린은 UpperCamelCase, 필드는 lowerCamelCase, 상수는 SCREAMING_SNAKE_CASE를 사용하세요. LibGDX 화면 클래스는 `com.mygame.f1`, 실행 전용 클래스는 `com.mygame.f1.lwjgl3` 네임스페이스를 따릅니다.

## 테스트 가이드
현재 자동화 테스트는 없으므로 신규 테스트는 `core/src/test/java/com/mygame/f1`에 JUnit 5 + AssertJ 조합으로 추가하세요. LibGDX 의존 코드는 `HeadlessApplication` 등으로 목킹하고, 모든 변경 후 `./gradlew lwjgl3:run`으로 최소 한 바퀴 수동 주행을 수행해 입력·물리·자산 로딩을 확인합니다. 충돌 처리, 감쇠 튜닝, 카메라 추적 등 재현 가능한 시나리오를 중심으로 케이스를 설계합니다.

## 커밋·풀 리퀘스트 지침
기존 히스토리처럼 `Feat:`, `Docs:` 등 대문자 타입 프리픽스를 제목 앞에 붙이고 60자 내외로 요약합니다. 본문에는 사용자 영향, 관련 이슈, 검증 명령(`gradlew lwjgl3:run`, `gradlew clean build`)을 적고, 트랙·카메라 변경 시 스크린샷이나 짧은 영상 링크를 포함하세요. PR 설명은 영어 또는 한국어 모두 가능하나 동일 이슈 스레드와 링크되어야 합니다.

## 에셋·설정 팁
큰 바이너리 에셋은 반드시 `assets/`에 보관하고 필요 시 경량 버전이나 포인터를 커밋하세요. 파일 추가·삭제 후 `generateAssetList`를 재실행해 `assets.txt`를 최신 상태로 유지합니다. `gradle.properties`의 `enableGraalNative` 등 빌드 스위치를 변경할 때는 PR 설명에 명시해 다른 기여자가 동일한 패키징 절차를 재현할 수 있게 합니다.

# AGENTS.md - F1 2D Racing Game Development Guide

> **AI-Assisted Development Guide for Spec-Driven Development using GitHub Codex (2025)**

## 📋 Table of Contents
1. [Project Overview](#project-overview)
2. [Development Philosophy](#development-philosophy)
3. [Codex Integration Guidelines](#codex-integration-guidelines)
4. [Specification Structure](#specification-structure)
5. [Development Workflow](#development-workflow)
6. [Code Standards](#code-standards)
7. [Testing Requirements](#testing-requirements)
8. [Build & Deployment](#build--deployment)

---

## 1. Project Overview

### Project Goal
Develop a 2D top-down F1 racing game using libGDX framework with Box2D physics, featuring:
- Single-player time attack mode
- Multiplayer online racing with KryoNet
- Strategic pit stop management system
- Realistic vehicle physics and damage system

### Technology Stack
| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | libGDX | 1.13.1 |
| Physics | Box2D | (bundled) |
| UI | Scene2D.ui | (bundled) |
| Network | KryoNet | 2.24.0 |
| Database | SQLite (JDBC) | 3.45+ |
| Map Editor | Tiled | 1.10+ |
| Build Tool | Gradle | 8.5+ |
| Java Version | OpenJDK | 17+ |

---

## 2. Development Philosophy

### Spec-Driven Development (SDD)
We follow a **specification-first approach**:

1. **Define Before Code**: All features start as detailed specifications in `docs/specs/`
2. **AI-Readable Specs**: Specifications are structured for optimal AI comprehension
3. **Iterative Refinement**: Specs evolve through implementation feedback
4. **Single Source of Truth**: Code must match spec; discrepancies require spec updates

### Key Principles
- ✅ **Explicit over Implicit**: Document assumptions, constraints, edge cases
- ✅ **Testable Specifications**: Every spec must include validation criteria
- ✅ **Component Isolation**: Each module has clear interfaces and dependencies
- ✅ **Physics-First Design**: Game mechanics driven by Box2D simulation
- ✅ **Network Determinism**: Client-server architecture with authoritative server

---

## 3. Codex Integration Guidelines

### 3.1 How to Use GitHub Codex (2025 Best Practices)

#### Setting Up Codex
```bash
# Install GitHub Copilot CLI (includes Codex access)
gh extension install github/gh-copilot

# Authenticate
gh auth login --with-token < your_token

# Enable Codex suggestions in IDE (VS Code / IntelliJ IDEA)
# Install GitHub Copilot extension and configure
```

#### Codex Prompt Engineering

**🎯 Effective Prompts for Game Development**

```java
// BAD PROMPT (Vague):
// "Make car move"

// GOOD PROMPT (Specific):
/**
 * CODEX: Implement vehicle acceleration using Box2D.
 * REQUIREMENTS:
 * - Apply force in vehicle's forward direction (body angle)
 * - Max speed: 300 km/h (83.33 m/s in Box2D units)
 * - Acceleration curve: linear up to 50%, then logarithmic
 * - Input smoothing: 0.1s ramp time
 * - Apply force at center of mass
 * SPEC: docs/specs/VEHICLE-PHYSICS.md#acceleration
 */
```

**Context Injection for Codex**
```java
// Always include relevant context in comments:
/**
 * CONTEXT:
 * - PPM (Pixels Per Meter): 32.0f
 * - World coordinates: Box2D units (meters)
 * - Screen coordinates: Pixels
 * - Conversion: screen = world * PPM
 * 
 * DEPENDENCIES:
 * - GameConstants.PPM
 * - VehicleConfig.maxSpeed
 * 
 * REFERENCED BY:
 * - InputProcessor.keyDown()
 * - GameScreen.update()
 */
```

#### 3.2 Codex Code Review Checklist

Before committing Codex-generated code, verify:

- [ ] **Spec Alignment**: Code matches specification in `docs/specs/`
- [ ] **Physics Consistency**: Units match Box2D conventions (meters, kg, seconds)
- [ ] **Resource Management**: All textures/bodies properly disposed
- [ ] **Null Safety**: Checks for null references in network/DB operations
- [ ] **Performance**: No allocations in render loop (use object pooling)
- [ ] **Testing**: Unit test exists and passes
- [ ] **Comments**: AI-generated code has explanatory comments

---

## 4. Specification Structure

### 4.1 Spec Document Template

All specs in `docs/specs/` follow this structure:

```markdown
# [COMPONENT-NAME].md

## Overview
Brief description and purpose.

## Dependencies
- Parent specs: SPEC-001.md
- Related systems: Physics, Networking
- External libraries: Box2D, KryoNet

## Requirements
### Functional Requirements
FR-001: [Description]
FR-002: [Description]

### Non-Functional Requirements
NFR-001: Performance target (e.g., 60 FPS stable)
NFR-002: Memory limit (e.g., < 512 MB heap)

## Design

### Data Structures
```java
public class VehicleState {
    public Vector2 position;
    public float rotation;
    public float speed;
}
```

### Algorithms
Pseudocode or flowcharts.

## Implementation Notes
- Box2D quirks
- LibGDX best practices
- Platform-specific considerations

## Validation Criteria
- [ ] Unit tests pass
- [ ] Integration with [System X]
- [ ] Performance benchmarks met

## References
- libGDX docs: https://libgdx.com/wiki/
- Box2D manual: https://box2d.org/documentation/
```

### 4.2 Spec Organization

```
docs/
├── specs/
│   ├── core/
│   │   ├── GAME-LOOP.md            # Main game loop architecture
│   │   ├── PHYSICS-ENGINE.md       # Box2D integration
│   │   └── CAMERA-SYSTEM.md        # Tracking camera
│   ├── gameplay/
│   │   ├── VEHICLE-PHYSICS.md      # Car movement/controls
│   │   ├── TIRE-SYSTEM.md          # Tire degradation
│   │   ├── DAMAGE-MODEL.md         # Vehicle durability
│   │   └── PITSTOP-MINIGAME.md     # Pit stop mechanics
│   ├── ui/
│   │   ├── HUD-SPECIFICATION.md    # In-game HUD
│   │   ├── MENU-SYSTEM.md          # All menu screens
│   │   └── LOGIN-SCREEN.md         # Authentication UI
│   ├── network/
│   │   ├── MULTIPLAYER-SYNC.md     # KryoNet protocols
│   │   └── LOBBY-SYSTEM.md         # Room management
│   ├── data/
│   │   ├── DATABASE-SCHEMA.md      # SQLite tables
│   │   └── USER-SESSION.md         # Session management
│   └── assets/
│       ├── TRACK-DESIGN.md         # Tiled map standards
│       └── VISUAL-EFFECTS.md       # Particles, animations
└── architecture/
├── SYSTEM-OVERVIEW.md          # High-level architecture
└── CLASS-DIAGRAM.md            # UML diagrams
```

---

## 5. Development Workflow

### 5.1 Phase-Based Development

Follow `docs/PHASES.md` for ordered development:

```
Phase 1: Foundation (Weeks 1-2)
├─ Core game loop
├─ Physics world setup
└─ Basic vehicle controls

Phase 2: Single Player (Weeks 3-4)
├─ HUD implementation
├─ Track system
└─ Time attack mode

Phase 3: Strategic Systems (Weeks 5-6)
├─ Tire degradation
├─ Damage model
└─ Pit stop mini-game

Phase 4: Multiplayer (Weeks 7-9)
├─ Network architecture
├─ Lobby system
└─ Race synchronization

Phase 5: Polish (Week 10)
├─ Visual effects
├─ Audio integration
└─ Bug fixes
```

### 5.2 Git Workflow

```bash
# Create feature branch from spec
git checkout -b feature/VEHICLE-PHYSICS-001

# Commit with spec reference
git commit -m "Implement acceleration system (VEHICLE-PHYSICS.md#FR-003)"

# Pull request template must include:
# - Spec reference
# - Test results
# - Performance impact
```

### 5.3 Daily Development Cycle

1. **Morning**: Read spec, write tests (TDD)
2. **Midday**: Implement with Codex assistance
3. **Afternoon**: Integration testing, spec updates
4. **Evening**: Code review, documentation

---

## 6. Code Standards

### 6.1 Naming Conventions

```java
// Classes: PascalCase
public class VehicleController { }

// Methods: camelCase with verb prefix
public void updatePhysics(float delta) { }

// Constants: UPPER_SNAKE_CASE
public static final float PPM = 32.0f;

// Box2D bodies: descriptive suffixes
Body vehicleBody;
Body wallBody;

// Assets: lowercase-with-dashes
Texture track_monaco_bg = assetManager.get("tracks/track-monaco-bg.png");
```

### 6.2 Box2D Best Practices

```java
// ✅ CORRECT: Use consistent units
float speedKmh = 100f;
float speedMs = speedKmh / 3.6f; // Convert to m/s for Box2D
vehicleBody.setLinearVelocity(speedMs, 0);

// ❌ WRONG: Mixing units
vehicleBody.setLinearVelocity(100, 0); // Is this km/h or m/s?

// ✅ CORRECT: Update world with fixed timestep
private static final float TIME_STEP = 1/60f;
private float accumulator = 0;

public void update(float delta) {
    accumulator += Math.min(delta, 0.25f);
    while (accumulator >= TIME_STEP) {
        world.step(TIME_STEP, 8, 3);
        accumulator -= TIME_STEP;
    }
}

// ✅ CORRECT: Dispose resources
@Override
public void dispose() {
    world.dispose();
    debugRenderer.dispose();
    // Never dispose AssetManager assets directly here!
}
```

6.3 LibGDX Asset Management
java// ✅ CORRECT: Single AssetManager pattern
public class Main extends Game {
public static AssetManager assetManager;

    @Override
    public void create() {
        assetManager = new AssetManager();
        assetManager.load("vehicles/car.png", Texture.class);
        assetManager.finishLoading(); // Blocking load
        setScreen(new GameScreen(this));
    }
    
    @Override
    public void dispose() {
        assetManager.dispose(); // Disposes ALL loaded assets
    }
}

// ✅ CORRECT: Reference assets, don't dispose in screens
public class GameScreen extends ScreenAdapter {
private Texture carTexture;

    @Override
    public void show() {
        carTexture = Main.assetManager.get("vehicles/car.png");
    }
    
    @Override
    public void dispose() {
        // DON'T dispose textures here!
        // AssetManager handles it
    }
}

// ❌ WRONG: Direct disposal causes crashes
public class GameScreen extends ScreenAdapter {
private Texture carTexture;

    @Override
    public void dispose() {
        carTexture.dispose(); // CRASH if AssetManager also disposes!
    }
}
6.4 Performance Guidelines
java// ✅ CORRECT: Object pooling in render loop
private Array<Particle> particlePool = new Array<>();

public void render(float delta) {
// Reuse pooled objects
Particle particle = particlePool.size > 0
? particlePool.pop()
: new Particle();
}

// ❌ WRONG: Allocations in render loop (causes GC pauses)
public void render(float delta) {
Vector2 tempVec = new Vector2(); // BAD! Creates garbage
}

// ✅ CORRECT: Use static/field-level temporaries
private final Vector2 tempVec = new Vector2();

public void render(float delta) {
tempVec.set(x, y); // Reuse same object
}

7. Testing Requirements
   7.1 Test Structure
   core/src/test/java/com/mygame/f1/
   ├── physics/
   │   ├── VehiclePhysicsTest.java
   │   ├── CollisionTest.java
   │   └── DriftTest.java
   ├── gameplay/
   │   ├── TireDegradationTest.java
   │   ├── DamageSystemTest.java
   │   └── PitStopTest.java
   ├── network/
   │   ├── SynchronizationTest.java
   │   └── LobbyTest.java
   └── utils/
   └── TestUtils.java
   7.2 Test Template
   javaimport com.badlogic.gdx.ApplicationAdapter;
   import com.badlogic.gdx.Gdx;
   import com.badlogic.gdx.backends.headless.HeadlessApplication;
   import com.badlogic.gdx.graphics.GL20;
   import com.badlogic.gdx.physics.box2d.World;
   import org.junit.jupiter.api.*;
   import static org.assertj.core.api.Assertions.*;
   import static org.mockito.Mockito.*;

/**
* Test: Vehicle acceleration behavior
* Spec: docs/specs/gameplay/VEHICLE-PHYSICS.md#FR-001
  */
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  public class VehiclePhysicsTest {

  private HeadlessApplication app;
  private World world;
  private VehicleController vehicle;

  @BeforeAll
  public void setup() {
  // Mock OpenGL for headless testing
  Gdx.gl = mock(GL20.class);
  Gdx.gl20 = mock(GL20.class);

       // Initialize headless LibGDX application
       app = new HeadlessApplication(new ApplicationAdapter() {});
       
       // Setup Box2D world
       world = new World(new Vector2(0, 0), true);
       vehicle = new VehicleController(world);
  }

  @Test
  @DisplayName("Vehicle should accelerate linearly up to 50% max speed")
  public void testLinearAcceleration() {
  // Given: Vehicle at rest
  vehicle.setSpeed(0);
  float maxSpeed = 83.33f; // 300 km/h in m/s
  float halfSpeed = maxSpeed * 0.5f;

       // When: Accelerate for 2 seconds
       for (int i = 0; i < 120; i++) { // 120 frames at 60 FPS
           vehicle.accelerate(1/60f);
           world.step(1/60f, 8, 3);
       }
       
       // Then: Speed should reach ~50% of max (linear phase)
       assertThat(vehicle.getSpeed())
           .isGreaterThan(halfSpeed * 0.8f)
           .isLessThan(halfSpeed * 1.2f);
  }

  @Test
  @DisplayName("Collision should reduce speed by 30%")
  public void testCollisionDamping() {
  // Given: Vehicle moving at 50 m/s
  vehicle.setSpeed(50f);

       // When: Collision occurs
       vehicle.handleCollision(CollisionType.WALL, 100f /* impulse */);
       
       // Then: Speed reduced to ~35 m/s
       assertThat(vehicle.getSpeed())
           .isCloseTo(35f, within(2f));
  }

  @AfterAll
  public void teardown() {
  world.dispose();
  app.exit();
  }
  }
  7.3 Test Coverage Requirements
  ComponentMin CoveragePriorityPhysics Engine85%CriticalVehicle Controller80%CriticalNetwork Sync75%HighTire System70%HighUI Components50%MediumVisual Effects30%Low
  7.4 Running Tests
  bash# Run all tests
  ./gradlew test

# Run specific test suite
./gradlew test --tests "VehiclePhysicsTest"

# Generate coverage report (JaCoCo)
./gradlew jacocoTestReport
# View: build/reports/jacoco/test/html/index.html

# Continuous testing (auto-run on file changes)
./gradlew test --continuous

8. Build & Deployment
   8.1 Build Commands
   bash# Development build (fast iteration)
   ./gradlew lwjgl3:run

# Production JAR (desktop distribution)
./gradlew lwjgl3:dist
# Output: lwjgl3/build/libs/f1-racing-1.0.0.jar

# Platform-specific JARs
./gradlew lwjgl3:jarWin    # Windows-optimized
./gradlew lwjgl3:jarMac    # macOS-optimized
./gradlew lwjgl3:jarLinux  # Linux-optimized

# Native image (experimental, requires GraalVM)
./gradlew lwjgl3:nativeImage
8.2 Asset Pipeline
bash# Generate asset list (required after adding new assets)
./gradlew generateAssetList
# Creates: assets/assets.txt

# Validate assets (checks for missing files)
./gradlew validateAssets

# Optimize textures (compress PNGs)
./gradlew optimizeTextures
8.3 Pre-Deployment Checklist
Before releasing a build:

All tests pass: ./gradlew test
No Box2D memory leaks: Run with -Dbox2d.debugMemory=true
Asset list updated: ./gradlew generateAssetList
Performance target met: 60 FPS stable on min-spec hardware
Network stress test: 4+ concurrent players, 200ms latency simulation
Database migration scripts tested
Version number updated in gradle.properties
Changelog updated in CHANGELOG.md

8.4 Continuous Integration (GitHub Actions)
.github/workflows/build.yml:
yamlname: Build and Test

on: [push, pull_request]

jobs:
test:
runs-on: ubuntu-latest
steps:
- uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: ~/.gradle/caches
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
      
      - name: Run tests
        run: ./gradlew test
      
      - name: Generate coverage report
        run: ./gradlew jacocoTestReport
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        
      - name: Build JAR
        run: ./gradlew lwjgl3:dist
      
      - name: Upload artifact
        uses: actions/upload-artifact@v3
        with:
          name: f1-racing-jar
          path: lwjgl3/build/libs/*.jar

9. AI-Specific Development Tips
   9.1 Effective Codex Prompts for LibGDX/Box2D
   java/**
* CODEX TASK: Implement camera smoothing for vehicle tracking
*
* CONTEXT:
* - Camera should follow player vehicle (Body)
* - Smooth position with lerp factor 0.1
* - Smooth rotation to match vehicle angle
* - Offset camera ahead of vehicle by 2 meters
*
* INPUTS:
* - playerBody: Box2D Body
* - camera: OrthographicCamera
* - delta: float (frame time)
*
* CONSTRAINTS:
* - Must convert Box2D coords (meters) to screen coords (pixels)
* - Use Constants.PPM = 32.0f for conversion
* - Rotation in degrees, Box2D uses radians
*
* SPEC: docs/specs/core/CAMERA-SYSTEM.md#smooth-tracking
  */
  public void updateCamera(Body playerBody, OrthographicCamera camera, float delta) {
  // Codex will generate implementation here
  }
  9.2 Common Pitfalls (Teach Codex to Avoid)
  java// ❌ PITFALL: Forgetting coordinate system conversion
  camera.position.set(playerBody.getPosition(), 0); // WRONG! Box2D units

// ✅ CORRECT:
Vector2 pos = playerBody.getPosition();
camera.position.set(pos.x * PPM, pos.y * PPM, 0);

// ❌ PITFALL: Angle conversion
sprite.setRotation(playerBody.getAngle()); // WRONG! Radians vs degrees

// ✅ CORRECT:
sprite.setRotation(playerBody.getAngle() * MathUtils.radiansToDegrees);

// ❌ PITFALL: Applying force in wrong coordinate system
playerBody.applyForce(inputX, inputY, 0, 0); // WRONG! World coords

// ✅ CORRECT: Apply force in body's local forward direction
Vector2 forward = playerBody.getWorldVector(new Vector2(0, 1));
playerBody.applyForce(forward.scl(acceleration), playerBody.getWorldCenter(), true);
9.3 Debugging with Codex
When asking Codex to debug:
java/**
* CODEX DEBUG: Vehicle drifts unexpectedly when steering
*
* OBSERVED BEHAVIOR:
* - Vehicle slides sideways even when not steering
* - Lateral friction seems too low
*
* SUSPECTED CAUSES:
* - Lateral friction calculation incorrect
* - Grip coefficient might be too low
* - Linear damping not applied to lateral velocity
*
* CURRENT VALUES:
* - Grip: 0.8f
* - Lateral damping: 0.95f
* - Mass: 500kg
*
* PLEASE REVIEW:
* 1. KillOrthogonalVelocity() implementation
* 2. Friction force magnitude calculation
* 3. Grip modifier during steering
     */
     private void applyLateralFriction() {
     // Buggy code here
     }

10. Maintenance & Evolution
    10.1 Spec Updates
    When code diverges from spec:

If spec is wrong: Update spec first, then code
If code is wrong: Fix code to match spec
If both are wrong: Discuss in team, update spec, then code

Spec Update Template:
markdown## Change Log

### [2025-01-15] Updated acceleration curve
- **Reason**: Players reported sluggish feel below 50 km/h
- **Changes**:
    - Increased low-speed torque multiplier from 1.5x to 2.0x
    - Adjusted RPM curve breakpoints
- **Impact**:
    - Better initial acceleration
    - No change to top speed
- **Tests Updated**: VehiclePhysicsTest.testLowSpeedAcceleration()
  10.2 Dependency Updates
  Check monthly for updates:
  bash# Check for LibGDX updates
  ./gradlew dependencyUpdates

# Update in gradle.properties
gdxVersion=1.13.2  # New version
Test rigorously after major version bumps (e.g., 1.13.x → 1.14.x).

11. Resources
    Official Documentation

LibGDX Wiki: https://libgdx.com/wiki/
Box2D Manual: https://box2d.org/documentation/
KryoNet: https://github.com/EsotericSoftware/kryonet
Tiled Docs: https://doc.mapeditor.org/

Community Resources

LibGDX Discord: https://discord.gg/6pgDK9F
Box2D Forums: https://box2d.org/posts/
r/libgdx: https://reddit.com/r/libgdx

AI Development

GitHub Copilot Docs: https://docs.github.com/copilot
Codex Best Practices: https://platform.openai.com/docs/guides/code


12. Contact & Support

Project Lead: [Your Name]
Repository: https://github.com/yourname/f1-racing
Issue Tracker: Use GitHub Issues with labels:

bug: Code defects
spec-mismatch: Code doesn't match spec
enhancement: New features
question: Need clarification




Last Updated: 2025-01-15
Document Version: 1.0.0
Maintained By: Development Team
