# PHASES.md - F1 2D Racing Game Development Roadmap

> **Detailed Phase-by-Phase Development Plan with Checklists**

## 📊 Phase Overview

| Phase | Duration | Focus Area | Completion Criteria |
|-------|----------|-----------|---------------------|
| **Phase 0** | 3 days | Project Setup | Build system working, specs written |
| **Phase 1** | 2 weeks | Core Foundation | Vehicle moves, camera tracks, physics stable |
| **Phase 2** | 2 weeks | Single Player | HUD complete, time attack playable |
| **Phase 3** | 2 weeks | Strategic Systems | Tire/damage systems working, pit stops functional |
| **Phase 4** | 3 weeks | Multiplayer | 4-player online races stable |
| **Phase 5** | 1 week | Polish & Release | All bugs fixed, packaged for distribution |

**Total Estimated Time**: 10-11 weeks

---

## Phase 0: Project Setup & Specification (Days 1-3)

### Goals
- Establish development environment
- Write all specification documents
- Prepare asset requirements list
- Set up CI/CD pipeline

### Tasks

#### Day 1: Environment & Repository
- [ ] **Setup Development Tools**
    - [ ] Install IntelliJ IDEA / VS Code
    - [ ] Install JDK 17+
    - [ ] Install Gradle 8.5+
    - [ ] Install Git
    - [ ] Install Tiled Map Editor 1.10+
    - [ ] Install SQLite DB Browser

- [ ] **Create Project Structure**
  ```bash
  ./gradlew init  # Initialize LibGDX project
  ```
    - [ ] Configure `settings.gradle` with modules: `core`, `lwjgl3`
    - [ ] Set up `gradle.properties` (gdxVersion, projectVersion)
    - [ ] Create `assets/` directory structure
    - [ ] Create `docs/specs/` directory structure
    - [ ] Create `docs/architecture/` directory

- [ ] **Version Control Setup**
    - [ ] Initialize Git repository
    - [ ] Create `.gitignore` (exclude `build/`, `.gradle/`, `*.class`)
    - [ ] Create initial commit
    - [ ] Push to remote repository (GitHub/GitLab)
    - [ ] Set up branch protection (main requires PR review)

#### Day 2: Specification Writing
- [ ] **Write Core Specifications** (in `docs/specs/core/`)
    - [ ] `GAME-LOOP.md` - Main game loop, fixed timestep
    - [ ] `PHYSICS-ENGINE.md` - Box2D world setup, PPM constant
    - [ ] `CAMERA-SYSTEM.md` - Tracking camera with rotation
    - [ ] `INPUT-HANDLING.md` - Keyboard/gamepad input mapping

- [ ] **Write Gameplay Specifications** (in `docs/specs/gameplay/`)
    - [ ] `VEHICLE-PHYSICS.md` - Acceleration, steering, braking
    - [ ] `TIRE-SYSTEM.md` - 3 tire types, degradation formula
    - [ ] `DAMAGE-MODEL.md` - Durability calculation
    - [ ] `PITSTOP-MINIGAME.md` - Timing bar mechanics

- [ ] **Write UI Specifications** (in `docs/specs/ui/`)
    - [ ] `HUD-SPECIFICATION.md` - All HUD elements layout
    - [ ] `MENU-SYSTEM.md` - Screen navigation flow
    - [ ] `LOGIN-SCREEN.md` - Authentication UI

- [ ] **Write Network Specifications** (in `docs/specs/network/`)
    - [ ] `MULTIPLAYER-SYNC.md` - Client-server architecture
    - [ ] `LOBBY-SYSTEM.md` - Room creation/joining

- [ ] **Write Data Specifications** (in `docs/specs/data/`)
    - [ ] `DATABASE-SCHEMA.md` - SQLite table definitions
    - [ ] `USER-SESSION.md` - Session lifecycle

#### Day 3: CI/CD & Asset Planning
- [ ] **Setup CI/CD** (GitHub Actions)
    - [ ] Create `.github/workflows/build.yml`
    - [ ] Configure automated testing on push/PR
    - [ ] Set up artifact uploads (JAR files)
    - [ ] Configure code coverage reporting (JaCoCo)

- [ ] **Create Asset Requirements Document**
    - [ ] List all required images (vehicles, UI, tracks)
    - [ ] List all required sounds (engine, effects, music)
    - [ ] List all required fonts
    - [ ] Assign priorities (P0: MVP, P1: Polish)

- [ ] **Create Development Tasks in Project Board**
    - [ ] Create GitHub Project board
    - [ ] Populate with tasks from Phases 1-5
    - [ ] Label tasks (backend, ui, physics, network)
    - [ ] Assign initial task owners

### Completion Criteria
- [ ] All specs reviewed and approved by team
- [ ] CI/CD pipeline runs successfully
- [ ] Asset requirements documented
- [ ] Development environment verified on all team machines

---

## Phase 1: Core Foundation (Weeks 1-2)

### Goals
- Implement basic game loop with Box2D physics
- Create controllable vehicle
- Implement tracking camera
- Load and render simple track

### Week 1: Physics & Vehicle

#### Day 1-2: Game Loop & Physics World
- [ ] **Implement Main Class** (`Main.java`)
    - [ ] Create `AssetManager` instance
    - [ ] Load essential textures (car sprite, track background)
    - [ ] Implement `create()`, `render()`, `dispose()`

- [ ] **Implement GameScreen** (`GameScreen.java`)
    - [ ] Create Box2D `World` with zero gravity
    - [ ] Implement fixed timestep game loop (1/60s)
    - [ ] Add `Box2DDebugRenderer` for physics visualization
    - [ ] Test: Empty world renders at 60 FPS

**Spec Reference**: `docs/specs/core/GAME-LOOP.md`, `PHYSICS-ENGINE.md`

**Test Checklist**:
- [ ] `./gradlew lwjgl3:run` launches without errors
- [ ] Box2D debug render shows coordinate system
- [ ] Frame rate stable at 60 FPS

#### Day 3-4: Vehicle Body & Basic Controls
- [ ] **Create VehicleController Class**
    - [ ] Create rectangular `Body` (1.0m x 2.0m)
    - [ ] Set density (500 kg/m²), friction (0.3), restitution (0.1)
    - [ ] Implement `applyAcceleration(float force)`
    - [ ] Implement `applySteering(float torque)`

- [ ] **Implement InputProcessor**
    - [ ] Detect UP/W key → apply forward force
    - [ ] Detect DOWN/S key → apply backward force
    - [ ] Detect LEFT/A key → apply left torque
    - [ ] Detect RIGHT/D key → apply right torque
    - [ ] Detect SPACE key → apply braking

- [ ] **Add Vehicle Sprite Rendering**
    - [ ] Load `pitstop_car_3.png` (or placeholder)
    - [ ] Sync sprite position/rotation with `Body`
    - [ ] Convert Box2D coords (meters) to screen coords (pixels)

**Spec Reference**: `docs/specs/gameplay/VEHICLE-PHYSICS.md#basic-movement`

**Test Checklist**:
- [ ] Vehicle accelerates forward with UP key
- [ ] Vehicle rotates smoothly with LEFT/RIGHT keys
- [ ] Vehicle stops when SPACE held
- [ ] Sprite visually matches physics body
- [ ] No jittering or teleportation

#### Day 5: Lateral Friction & Drift
- [ ] **Implement Lateral Friction Method**
  ```java
  private void killOrthogonalVelocity(Body body) {
      Vector2 currentRightNormal = body.getWorldVector(new Vector2(1, 0));
      float lateralVelocity = currentRightNormal.dot(body.getLinearVelocity());
      Vector2 impulse = currentRightNormal.scl(-body.getMass() * lateralVelocity);
      body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
  }
  ```
    - [ ] Call every physics step
    - [ ] Multiply impulse by grip coefficient (0.8 default)
    - [ ] Reduce grip during steering input (simulate drift)

- [ ] **Add Forward Drag**
    - [ ] Calculate speed-proportional drag force
    - [ ] Apply opposite to velocity direction

**Spec Reference**: `docs/specs/gameplay/VEHICLE-PHYSICS.md#friction-model`

**Test Checklist**:
- [ ] Vehicle doesn't slide sideways excessively
- [ ] Drift occurs naturally at high speed turns
- [ ] Vehicle slows down realistically without braking

### Week 2: Camera & Track

#### Day 6-7: Tracking Camera
- [ ] **Implement CameraController Class**
    - [ ] Create `OrthographicCamera` (FitViewport 1600x900 @ PPM=32)
    - [ ] Implement `smoothFollow(Body target, float delta)`
        - [ ] Lerp camera position to target (factor: 0.1)
        - [ ] Lerp camera rotation to target angle (factor: 0.15)
        - [ ] Add forward offset (2 meters ahead of vehicle)

- [ ] **Integrate Camera with GameScreen**
    - [ ] Update camera every frame before rendering
    - [ ] Apply camera matrix to SpriteBatch
    - [ ] Apply camera matrix to Box2DDebugRenderer

**Spec Reference**: `docs/specs/core/CAMERA-SYSTEM.md`

**Test Checklist**:
- [ ] Camera follows vehicle smoothly (no lag)
- [ ] Camera rotates with vehicle
- [ ] No stuttering at high speeds
- [ ] Viewport scales correctly on window resize

#### Day 8-9: Track Loading (Tiled Integration)
- [ ] **Create Basic Track in Tiled**
    - [ ] Create 2048x2048 pixel map
    - [ ] Add "Background" layer (visual tiles)
    - [ ] Add "Collision" object layer (rectangles for walls)
    - [ ] Save as `assets/track_01.tmx`

- [ ] **Implement Track Loading**
    - [ ] Load TMX file with `TmxMapLoader`
    - [ ] Parse "Collision" layer objects
    - [ ] Create static Box2D bodies for each rectangle
    - [ ] Render background layer with `OrthogonalTiledMapRenderer`

**Spec Reference**: `docs/specs/assets/TRACK-DESIGN.md`

**Test Checklist**:
- [ ] Track loads without errors
- [ ] Collision walls are invisible but functional
- [ ] Vehicle cannot pass through walls
- [ ] Track renders correctly under vehicle

#### Day 10: Collision Handling & Speed Limits
- [ ] **Implement ContactListener**
    - [ ] Detect vehicle-wall collisions
    - [ ] Apply velocity reduction (30% speed loss)
    - [ ] Play collision sound (placeholder)

- [ ] **Add Speed Limits**
    - [ ] Max forward speed: 300 km/h (83.33 m/s)
    - [ ] Max reverse speed: 50 km/h (13.89 m/s)
    - [ ] Clamp velocity every frame

**Spec Reference**: `docs/specs/gameplay/VEHICLE-PHYSICS.md#collision-response`

**Test Checklist**:
- [ ] Collision reduces speed visibly
- [ ] Vehicle cannot exceed max speed
- [ ] Reverse speed limited correctly
- [ ] No crashes on high-speed impacts

### Phase 1 Deliverables
- [ ] **Playable Prototype**
    - Vehicle drives around track
    - Camera follows vehicle
    - Collisions work
    - 60 FPS stable

- [ ] **Unit Tests**
    - [ ] `VehiclePhysicsTest.testAcceleration()`
    - [ ] `VehiclePhysicsTest.testSteering()`
    - [ ] `VehiclePhysicsTest.testCollisionDamping()`
    - [ ] `CameraTest.testSmoothTracking()`

- [ ] **Documentation**
    - [ ] Update README with controls
    - [ ] Document known issues in `ISSUES.md`

---

## Phase 2: Single Player & HUD (Weeks 3-4)

### Goals
- Implement complete HUD system
- Add time attack game mode
- Create start sequence (lights)
- Integrate database for lap times

### Week 3: HUD System

#### Day 1-2: HUD Framework
- [ ] **Create HUDManager Class**
    - [ ] Set up Scene2D `Stage` for UI
    - [ ] Create root `Table` for layout
    - [ ] Implement `update(float delta)` for data refresh
    - [ ] Implement `render()` for drawing

- [ ] **Implement Speedometer**
    - [ ] Display current speed in km/h
    - [ ] Create circular gauge (optional) or digital display
    - [ ] Position: bottom-right corner

**Spec Reference**: `docs/specs/ui/HUD-SPECIFICATION.md#speedometer`

#### Day 3: Lap Counter & Timer
- [ ] **Implement Lap Detection**
    - [ ] Create invisible sensor at start/finish line
    - [ ] Increment lap count on crossing
    - [ ] Reset direction flag to prevent double-counting

- [ ] **Implement Timer System**
    - [ ] Track current lap time
    - [ ] Track best lap time (session)
    - [ ] Display both in top-left corner

**Spec Reference**: `docs/specs/ui/HUD-SPECIFICATION.md#timer`

**Test Checklist**:
- [ ] Lap increments only when crossing finish line forward
- [ ] Timer resets correctly on new lap
- [ ] Best lap updates only when beaten

#### Day 4-5: Minimap & Position Display
- [ ] **Implement Minimap**
    - [ ] Create secondary `OrthographicCamera` (zoomed out)
    - [ ] Render track to small viewport (200x200 px)
    - [ ] Show player position as colored dot
    - [ ] Position: top-right corner

- [ ] **Implement Position Display** (for multiplayer prep)
    - [ ] Calculate race position (1st, 2nd, etc.)
    - [ ] Display large badge with ordinal number
    - [ ] Position: top-center

**Spec Reference**: `docs/specs/ui/HUD-SPECIFICATION.md#minimap`

### Week 4: Game Mode & Database

#### Day 6-7: Time Attack Mode
- [ ] **Create TimeAttackMode Class**
    - [ ] Implement race start countdown
    - [ ] Track total race time
    - [ ] Implement race completion detection (e.g., 3 laps)
    - [ ] Show results screen on finish

- [ ] **Implement Start Sequence**
    - [ ] Display 5 red lights sequentially (1s intervals)
    - [ ] Wait random delay (1-2s) after all lights on
    - [ ] Turn off all lights simultaneously (race start!)
    - [ ] Play start sound effect

**Spec Reference**: `docs/specs/gameplay/RACE-START.md`

**Test Checklist**:
- [ ] Lights sequence correctly
- [ ] Vehicle cannot move until lights out
- [ ] Random delay feels fair (not too predictable)

#### Day 8-9: Database Integration
- [ ] **Create DatabaseManager Class**
    - [ ] Set up SQLite connection (JDBC)
    - [ ] Create tables: `users`, `lap_times`, `user_preferences`
    - [ ] Implement CRUD operations

- [ ] **Implement Lap Time Saving**
    - [ ] Save lap time after race finish
    - [ ] Associate with current user
    - [ ] Query best lap time from database

**Spec Reference**: `docs/specs/data/DATABASE-SCHEMA.md`

**SQL Schema**:
```sql
CREATE TABLE lap_times (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    track_id VARCHAR(50) NOT NULL,
    lap_time_ms INTEGER NOT NULL,
    vehicle_id INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

#### Day 10: Leaderboard Screen
- [ ] **Create LeaderboardScreen Class**
    - [ ] Query top 10 lap times from database
    - [ ] Display in scrollable table
    - [ ] Show: Rank, Username, Time, Date
    - [ ] Highlight current user's entry

**Test Checklist**:
- [ ] Leaderboard shows correct rankings
- [ ] Times formatted as MM:SS.mmm
- [ ] Scroll works for >10 entries

### Phase 2 Deliverables
- [ ] **Complete Single Player Mode**
    - Full HUD functional
    - Time attack playable start to finish
    - Lap times saved to database

- [ ] **Unit Tests**
    - [ ] `LapDetectionTest.testFinishLineCrossing()`
    - [ ] `TimerTest.testLapTimeCalculation()`
    - [ ] `DatabaseTest.testLapTimeSave()`

- [ ] **Screens Implemented**
    - [ ] `GameScreen` (with HUD)
    - [ ] `ResultsScreen`
    - [ ] `LeaderboardScreen`

---

## Phase 3: Strategic Systems (Weeks 5-6)

### Goals
- Implement tire degradation system
- Implement vehicle damage model
- Create pit stop mini-game
- Balance gameplay mechanics

### Week 5: Tire & Damage Systems

#### Day 1-3: Tire System
- [ ] **Create TireManager Class**
    - [ ] Define 3 tire types (Soft, Medium, Hard)
    - [ ] Implement degradation formula:
      ```java
      degradation = baseRate * (1 + speedFactor + steeringFactor)
      ```
    - [ ] Apply performance modifiers based on condition

- [ ] **Tire Type Properties**
  ```java
  Soft:   speed +10%, accel +10%, grip +15%, durability 30s
  Medium: speed +5%,  accel +5%,  grip +5%,  durability 60s
  Hard:   speed 0%,   accel 0%,   grip 0%,   durability 100s
  ```

- [ ] **Implement Tire UI**
    - [ ] Display current tire type icon
    - [ ] Show degradation gauge (color-coded)
    - [ ] Add warning flash at <20% condition

**Spec Reference**: `docs/specs/gameplay/TIRE-SYSTEM.md`

**Test Checklist**:
- [ ] Soft tires degrade faster than hard
- [ ] Performance decreases as tires wear
- [ ] Tire UI updates in real-time

#### Day 4-6: Damage Model
- [ ] **Create DamageManager Class**
    - [ ] Track vehicle durability (0-100%)
    - [ ] Reduce durability on:
        - Collision with walls (-5% per hit)
        - Off-track driving (-0.1% per second)
        - Collision with other vehicles (-3% per hit)

- [ ] **Apply Damage Effects**
    - [ ] At <50% durability: -10% top speed
    - [ ] At <25% durability: -20% top speed, -15% acceleration
    - [ ] At 0% durability: Vehicle retired (DNF)

- [ ] **Implement Damage UI**
    - [ ] Display durability bar below tire gauge
    - [ ] Show vehicle damage indicator (visual cracks on sprite)
    - [ ] Add critical damage warning (flashing red)

**Spec Reference**: `docs/specs/gameplay/DAMAGE-MODEL.md`

**Test Checklist**:
- [ ] Durability decreases correctly on impacts
- [ ] Performance penalties apply at thresholds
- [ ] Retirement triggered at 0%

### Week 6: Pit Stop System

#### Day 7-8: Pit Lane Detection
- [ ] **Create Pit Lane in Tiled**
    - [ ] Add "PitEntry" and "PitExit" objects
    - [ ] Define pit stop zone (repair area)

- [ ] **Implement Pit Stop Trigger**
    - [ ] Detect vehicle entering pit zone
    - [ ] Pause vehicle physics
    - [ ] Launch mini-game

**Spec Reference**: `docs/specs/gameplay/PITSTOP-MINIGAME.md#pit-lane`

#### Day 9-10: Pit Stop Mini-Game
- [ ] **Create PitStopMinigame Class**
    - [ ] Display timing bar (moving cursor)
    - [ ] Define 3 zones: Perfect (center 80px), Good (160px), Bad (rest)
    - [ ] Detect player input (SPACE bar)
    - [ ] Calculate stop time:
        - Perfect: 3 seconds
        - Good: 5 seconds
        - Bad: 8 seconds

- [ ] **Pit Stop Effects**
    - [ ] Restore vehicle durability to 100%
    - [ ] Allow tire type selection
    - [ ] Reset tire degradation to 0%
    - [ ] Add stop time to race timer

- [ ] **Pit Stop UI**
    - [ ] Show timing bar animation
    - [ ] Display result feedback ("PERFECT!", "GOOD", "BAD")
    - [ ] Show tire selection menu

**Spec Reference**: `docs/specs/gameplay/PITSTOP-MINIGAME.md#minigame-mechanics`

**Test Checklist**:
- [ ] Mini-game launches reliably in pit zone
- [ ] Timing zones correctly sized
- [ ] Perfect/Good/Bad detection accurate
- [ ] Stop times added to race timer correctly
- [ ] Durability and tires restored

### Phase 3 Deliverables
- [ ] **Strategic Gameplay Working**
    - Tire degradation affects performance
    - Damage reduces vehicle capability
    - Pit stops are functional and strategic

- [ ] **Unit Tests**
    - [ ] `TireSystemTest.testDegradationRate()`
    - [ ] `DamageTest.testPerformancePenalties()`
    - [ ] `PitStopTest.testMinigameZoneDetection()`

- [ ] **Balance Testing**
    - [ ] Document optimal pit stop strategies
    - [ ] Tune degradation rates for ~2-3 stops per 10-lap race

---

## Phase 4: Multiplayer (Weeks 7-9)

### Goals
- Implement client-server networking with KryoNet
- Create lobby system
- Synchronize vehicle positions and race state
- Handle player disconnections

### Week 7: Network Foundation

#### Day 1-2: KryoNet Setup
- [ ] **Add KryoNet Dependency**
  ```gradle
  implementation "com.esotericsoftware:kryonet:2.24.0"
  ```

- [ ] **Create Network Packets**
    - [ ] `PlayerJoinPacket` (username, vehicle_id)
    - [ ] `VehicleStatePacket` (x, y, rotation, speed, lap)
    - [ ] `RaceStartPacket` (countdown trigger)
    - [ ] `RaceEndPacket` (final positions)

- [ ] **Implement NetworkManager**
    - [ ] Create server class (`GameServer.java`)
    - [ ] Create client class (`GameClient.java`)
    - [ ] Register packet classes with Kryo
    - [ ] Implement connection/disconnection handlers

**Spec Reference**: `docs/specs/network/MULTIPLAYER-SYNC.md#kryonet-setup`

#### Day 3-5: Server Logic
- [ ] **Implement GameServer**
    - [ ] Accept client connections (port 54555)
    - [ ] Maintain list of connected players
    - [ ] Broadcast vehicle states (20 Hz tick rate)
    - [ ] Handle race start/end commands
    - [ ] Validate client inputs (anti-cheat)

- [ ] **Implement Authority Model**
    - [ ] Server is authoritative for positions
    - [ ] Clients send inputs, server simulates
    - [ ] Broadcast corrected positions to all clients

**Spec Reference**: `docs/specs/network/MULTIPLAYER-SYNC.md#server-authority`

**Test Checklist**:
- [ ] Server accepts multiple connections
- [ ] Broadcasts reach all clients
- [ ] Server survives client disconnections
- [ ] No memory leaks after 1000+ packets

### Week 8: Lobby System

#### Day 6-7: Lobby UI
- [ ] **Create MultiplayerLobbyScreen**
    - [ ] Display "Create Room" button
    - [ ] Display "Join Room" button
    - [ ] Show list of available rooms (scrollable table)
    - [ ] Each room shows: name, track, players (X/4), host

- [ ] **Implement Room Creation Dialog**
    - [ ] Input field for room name
    - [ ] Dropdown for track selection
    - [ ] Dropdown for max players (2-4)
    - [ ] "Create" button

**Spec Reference**: `docs/specs/network/LOBBY-SYSTEM.md#ui-design`

#### Day 8-9: Lobby Backend
- [ ] **Implement Room Management**
    - [ ] `RoomManager` class on server
    - [ ] Create room (assign unique ID)
    - [ ] Join room (validate capacity)
    - [ ] Leave room (handle host migration)
    - [ ] Close room (all players leave)

- [ ] **Implement Ready System**
    - [ ] Players toggle "Ready" status
    - [ ] Race starts when all players ready
    - [ ] Host can force-start after timer

**Spec Reference**: `docs/specs/network/LOBBY-SYSTEM.md#room-lifecycle`

**Test Checklist**:
- [ ] Multiple rooms can exist simultaneously
- [ ] Room list updates in real-time
- [ ] Host migration works (if host leaves)
- [ ] Race starts only when all ready

### Week 9: Race Synchronization

#### Day 10-12: State Synchronization
- [ ] **Implement Client-Side Prediction**
    - [ ] Client simulates physics locally
    - [ ] Apply player inputs immediately
    - [ ] Smooth-correct on server updates (lerp)

- [ ] **Implement Lag Compensation**
    - [ ] Interpolate remote player positions
    - [ ] Buffer last 100ms of states
    - [ ] Extrapolate if no update received

**Spec Reference**: `docs/specs/network/MULTIPLAYER-SYNC.md#prediction`

#### Day 13-14: Race Completion
- [ ] **Implement Finish Detection**
    - [ ] Server detects when each player finishes
    - [ ] Broadcast final positions to all clients
    - [ ] Save results to database

- [ ] **Create Multiplayer Results Screen**
    - [ ] Show final standings (1st, 2nd, 3rd, 4th)
    - [ ] Display each player's best lap
    - [ ] Show total race time
    - [ ] "Return to Lobby" button

**Test Checklist**:
- [ ] All players see consistent finish order
- [ ] Results saved for all participants
- [ ] No crashes on race completion

### Phase 4 Deliverables
- [ ] **Functional Multiplayer**
    - 2-4 players can race online
    - Lobby system working
    - Stable synchronization (playable at 200ms ping)

- [ ] **Stress Tests**
    - [ ] 4 players, 5-lap race, no crashes
    - [ ] 10 concurrent rooms on server
    - [ ] Simulate 200ms latency, still playable

- [ ] **Network Tests**
    - [ ] `NetworkTest.testPacketSerialization()`
    - [ ] `SyncTest.testStateInterpolation()`
    - [ ] `LobbyTest.testRoomLifecycle()`

---

## Phase 5: Polish & Release (Week 10)

### Goals
- Fix all critical bugs
- Add visual/audio polish
- Optimize performance
- Package for distribution

### Day 1-2: Bug Fixing
- [ ] **Triage Issues**
    - [ ] Review GitHub Issues
    - [ ] Prioritize: Critical > High > Medium
    - [ ] Fix all Critical and High priority bugs

- [ ] **Common Bug Areas**
    - [ ] Physics glitches (vehicles stuck in walls)
    - [ ] Network desync
    - [ ] UI scaling on different resolutions
    - [ ] Memory leaks

### Day 3-4: Visual Effects
- [ ] **Add Particle Effects**
    - [ ] Tire smoke during drift (ParticleEffect)
    - [ ] Collision sparks
    - [ ] Pit stop dust clouds

- [ ] **Polish Vehicle Graphics**
    - [ ] Add shadow sprite below vehicle
    - [ ] Tire trail marks on track
    - [ ] Damage visual indicators (cracks, dents)

**Spec Reference**: `docs/specs/assets/VISUAL-EFFECTS.md`

### Day 5: Audio Integration
- [ ] **Implement AudioManager**
    - [ ] Load all sound effects
    - [ ] Implement volume controls
    - [ ] Handle simultaneous sounds (pooling)

- [ ] **Add Sound Effects**
    - [ ] Engine sound (looping, pitch varies with speed)
    - [ ] Tire screech on drift
    - [ ] Collision impacts
    - [ ] UI button clicks
    - [ ] Pit stop sounds (wrench, air gun)

- [ ] **Add Music**
    - [ ] Menu background music
    - [ ] Race background music (high-energy)

### Day 6: Performance Optimization
- [ ] **Profile Performance**
    - [ ] Run with VisualVM profiler
    - [ ] Identify hotspots (CPU, memory)

- [ ] **Optimization Tasks**
    - [ ] Object pooling for particles
    - [ ] Reduce draw calls (batch sprites)
    - [ ] Optimize Box2D (reduce body count if possible)
    - [ ] Compress textures (ETC1 for mobile future)

- [ ] **Target Metrics**
    - [ ] 60 FPS stable on mid-range hardware
    - [ ] Heap usage < 512 MB
    - [ ] Startup time < 3 seconds

### Day 7: Packaging & Documentation
- [ ] **Build Distribution Packages**
    - [ ] `./gradlew lwjgl3:dist` (universal JAR)
    - [ ] `./gradlew lwjgl3:jarWin` (Windows-optimized)
    - [ ] `./gradlew lwjgl3:jarMac` (macOS)
    - [ ] `./gradlew lwjgl3:jarLinux` (Linux)

- [ ] **Create Release Documentation**
    - [ ] Update README.md with:
        - Installation instructions
        - System requirements
        - Controls/keybindings
        - Troubleshooting
    - [ ] Write CHANGELOG.md (features, fixes)
    - [ ] Create USER_GUIDE.md (gameplay tutorial)

- [ ] **Quality Assurance**
    - [ ] Full playthrough (single + multiplayer)
    - [ ] Test on 3+ different machines
    - [ ] Verify all assets load correctly

### Phase 5 Deliverables
- [ ] **Polished Game**
    - All planned features working
    - No critical bugs
    - Smooth performance

- [ ] **Distribution Packages**
    - Windows JAR
    - macOS JAR
    - Linux JAR
    - Source code (GitHub release)

- [ ] **Documentation**
    - Complete README
    - User guide
    - Developer documentation

---

## Post-Release: Future Enhancements

### Potential Phase 6 Features (Optional)
- [ ] **Additional Content**
    - [ ] 3+ more tracks
    - [ ] 5+ more vehicles with unique stats
    - [ ] Unlock/progression system

- [ ] **Advanced Features**
    - [ ] Weather system (rain, wet tires)
    - [ ] AI opponents (single-player races)
    - [ ] Replay system (save/playback races)
    - [ ] Customization (vehicle skins, decals)

- [ ] **Platform Expansion**
    - [ ] Android port (touch controls)
    - [ ] Steam release
    - [ ] Leaderboard API integration

---

## Progress Tracking

### Phase Completion Matrix

| Phase | Status | Start Date | End Date | Blockers |
|-------|--------|------------|----------|----------|
| Phase 0 | ⬜ Not Started | - | - | - |
| Phase 1 | ⬜ Not Started | - | - | - |
| Phase 2 | ⬜ Not Started | - | - | Phase 1 |
| Phase 3 | ⬜ Not Started | - | - | Phase 2 |
| Phase 4 | ⬜ Not Started | - | - | Phase 3 |
| Phase 5 | ⬜ Not Started | - | - | Phase 4 |

**Status Legend**:
- ⬜ Not Started
- 🟡 In Progress
- ✅ Completed
- 🔴 Blocked

### Risk Management

| Risk | Probability | Impact | Mitigation Strategy |
|------|------------|--------|---------------------|
| Box2D physics instability | Medium | High | Thorough testing, fixed timestep, safety limits |
| Network desync issues | High | High | Authoritative server, client prediction, extensive testing |
| Asset creation bottleneck | Medium | Medium | Use placeholder assets initially, prioritize core gameplay |
| Scope creep | High | Medium | Strictly follow phase plan, defer features to post-release |
| Performance issues | Low | High | Profile early, optimize as needed, target 60 FPS from start |

---

## Definition of Done (DoD)

A phase is considered complete when ALL of the following are met:

### Code Quality
- [ ] All planned features implemented per specs
- [ ] Code reviewed by at least one other developer
- [ ] No critical or high-priority bugs remaining
- [ ] Code follows standards in `AGENTS.md#code-standards`

### Testing
- [ ] Unit tests written for new components (min 70% coverage)
- [ ] Integration tests pass
- [ ] Manual playtesting completed
- [ ] Performance benchmarks met (60 FPS)

### Documentation
- [ ] Specs updated to reflect implementation
- [ ] Code comments added for complex logic
- [ ] Known issues documented
- [ ] Phase retrospective completed

### Build
- [ ] `./gradlew clean build` succeeds
- [ ] `./gradlew test` passes (100% tests)
- [ ] CI/CD pipeline green
- [ ] No compiler warnings

---

## Team Roles & Responsibilities

| Role | Team Member | Primary Responsibilities |
|------|------------|-------------------------|
| **Physics Lead** | TBD | Box2D integration, vehicle physics, collision |
| **UI Lead** | TBD | Scene2D screens, HUD, menu systems |
| **Network Lead** | TBD | KryoNet, multiplayer sync, lobby system |
| **Database Lead** | TBD | SQLite schema, user management, data persistence |
| **Assets Lead** | TBD | Graphics, sounds, Tiled maps |
| **QA Lead** | TBD | Testing, bug tracking, performance monitoring |

---

## Meeting Schedule

### Daily Standups (15 min)
- **When**: Every weekday, 9:00 AM
- **Format**:
    - What I completed yesterday
    - What I'm working on today
    - Any blockers

### Sprint Planning (2 hours)
- **When**: Start of each phase
- **Agenda**:
    - Review previous phase deliverables
    - Break down phase tasks into subtasks
    - Assign tasks to team members
    - Set milestones and deadlines

### Sprint Retrospective (1 hour)
- **When**: End of each phase
- **Agenda**:
    - What went well
    - What could be improved
    - Action items for next phase

### Demo Day (30 min)
- **When**: End of Phases 2, 3, 4, 5
- **Agenda**:
    - Showcase completed features
    - Gather feedback from stakeholders
    - Celebrate wins

---

## Estimation Methodology

### Story Points
We use Fibonacci sequence for estimation:
- **1 point**: Trivial (<1 hour)
- **2 points**: Simple (<2 hours)
- **3 points**: Moderate (<4 hours)
- **5 points**: Complex (<1 day)
- **8 points**: Very complex (1-2 days)
- **13 points**: Epic (split into smaller tasks)

### Velocity Tracking
- Track completed story points per week
- Use velocity to adjust future estimates
- Target: 20-30 points per developer per week

---

## Contingency Plans

### If Phase Runs Over Schedule
1. **Triage Features**: Move non-critical features to next phase
2. **Increase Resources**: Add team members or extend hours
3. **Simplify Scope**: Reduce complexity of problematic features
4. **Skip Ahead**: Parallelize work on independent phases

### If Critical Bug Found
1. **Halt Development**: Stop new features immediately
2. **Root Cause Analysis**: Identify source of bug
3. **Hot Fix**: Implement minimal fix
4. **Regression Test**: Verify fix doesn't break other features
5. **Post-Mortem**: Document lessons learned

### If Team Member Unavailable
1. **Cross-Training**: Ensure knowledge sharing throughout phases
2. **Documentation**: Keep specs and code well-documented
3. **Backup Assignee**: Each task has primary and secondary owner

---

## Success Metrics

### Phase 1 (Foundation)
- [ ] Vehicle controllable with 5+ inputs
- [ ] Camera follows smoothly (no lag >50ms)
- [ ] Track collision 100% accurate
- [ ] 60 FPS on target hardware

### Phase 2 (Single Player)
- [ ] HUD displays 8+ data points in real-time
- [ ] Lap times recorded with <10ms accuracy
- [ ] Leaderboard shows top 10 times
- [ ] Complete race playable start-to-finish

### Phase 3 (Strategic Systems)
- [ ] 3 tire types with distinct characteristics
- [ ] Pit stop mini-game 95%+ success rate
- [ ] Damage system affects performance measurably
- [ ] Optimal strategy requires 2-3 pit stops per 10-lap race

### Phase 4 (Multiplayer)
- [ ] 4 players race stably (0 crashes in 10 races)
- [ ] Network latency <200ms feels responsive
- [ ] Lobby supports 5+ concurrent rooms
- [ ] Race results consistent across all clients

### Phase 5 (Polish)
- [ ] 0 critical bugs, <5 minor bugs
- [ ] Visual effects present in 10+ scenarios
- [ ] Audio feedback for 15+ actions
- [ ] Package builds on Windows, macOS, Linux
- [ ] Complete documentation (README, USER_GUIDE)

---

## Appendix: Quick Reference Commands

### Development
```bash
# Run game (development)
./gradlew lwjgl3:run

# Run with debug logging
./gradlew lwjgl3:run --info

# Run with Box2D memory debugging
./gradlew lwjgl3:run -Dbox2d.debugMemory=true
```

### Testing
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "VehiclePhysicsTest"

# Run tests with coverage report
./gradlew test jacocoTestReport
```

### Building
```bash
# Clean build
./gradlew clean build

# Generate asset list
./gradlew generateAssetList

# Create distribution JAR
./gradlew lwjgl3:dist

# Platform-specific builds
./gradlew lwjgl3:jarWin
./gradlew lwjgl3:jarMac
./gradlew lwjgl3:jarLinux
```


### Database
```bash
# Create new migration
sqlite3 data/game.db < migrations/001_initial_schema.sql

# Backup database
cp data/game.db data/game_backup_$(date +%Y%m%d).db

# Reset database (development only!)
rm data/game.db && ./gradlew run
```

---

**Document Version**: 1.0.0  
**Last Updated**: 2025-01-15  
**Maintained By**: Development Team  
**Status**: Living Document (Update as phases progress)
