# F1 프로젝트 진행 현황 노트 (Status & Plan)

본 문서는 신뢰할 수 있는 실무 자료를 참고하여(하단 참고 자료) 현재 저장소의 구조, 구현 상태, 위험/이슈, 다음 단계, 검증 방법을 한 눈에 볼 수 있게 정리한 진행 노트입니다.

## 1) 요약 (현재 상태)
- 데스크톱 런처(LWJGL3)로 실행 가능. 자산 폴더(`assets/`)를 워킹 디렉터리로 지정해 즉시 플레이 테스트 가능.
- 핵심 게임 루프/물리(Box2D)/카메라 추적/입력(가속·후진·좌우 조향·브레이크) 로직 구현.
- Tiled 맵(`assets/track.tmx`)을 로드하고 `Collision` 레이어의 사각형 오브젝트를 바탕으로 월(Static Body) 생성.
- 배경은 텍스처(`Track_t2.png`)로 렌더, 차량 스프라이트(`pitstop_car_3.png`) 렌더, Box2D 디버그 렌더 활성화.
- 빌드 스크립트(멀티모듈/패키징/런타임 자산 연결) 구성 완성도 높음.

주요 발견 이슈(우선순위 순):
1) Tiled 맵 렌더 호출 누락: `mapRenderer.setView(camera)`만 호출되고 `mapRenderer.render()` 호출이 없음 → 맵 시각 표시가 의도였다면 추가 필요.
2) AssetManager 사용 혼용: `Main.assetManager`로 로드한 텍스처를 `GameScreen.dispose()`에서 직접 `dispose()` 함 → 중복 해제 위험. AssetManager 참조만 사용하거나 해제는 `assetManager.dispose()` 단일 경로로 통일 권장.
3) 자동화 테스트 부재: AGENTS.md 가이드와 일치하나, 핵심 물리/충돌/카메라 회전 등은 리그레션 위험 높음 → JUnit5+AssertJ 테스트(HeadlessApplication) 권장.

## 2) 저장소 구조(최상위)
- `settings.gradle` – 멀티모듈 포함(`lwjgl3`, `core`)
- `build.gradle` – 공통 빌드 설정, `generateAssetList` 태스크 정의(빌드 시 `assets/assets.txt` 생성)
- `gradle.properties` – `gdxVersion=1.13.1`, `projectVersion=1.0.0`, `enableGraalNative=false` 등
- `gradlew`, `gradlew.bat`, `gradle/` – Gradle 래퍼
- `AGENTS.md` – 빌드/코딩/테스트 가이드(한글)
- `README.md` – 한글 설명(현재 일부 인코딩 깨짐)
- `assets/` – 런타임 자산(예: `track.tmx`, `Track_t2.png`, `pitstop_car_3.png` 등, 다양한 트랙 이미지/타일셋 포함)
- `core/`
  - `build.gradle` – `gdx`, `gdx-box2d`, `gdx-freetype` 의존성
  - `src/main/java/com/mygame/f1/`
    - `Main.java` – `AssetManager`로 텍스처 로드 후 `GameScreen` 진입
    - `GameScreen.java` – 월드/카메라/입력/마찰·드래그/속도 제한/충돌 감쇠/맵 충돌월 생성/렌더
    - `FirstScreen.java` – 템플릿 스크린(미사용)
- `lwjgl3/`
  - `build.gradle`, `nativeimage.gradle`
  - `src/main/java/com/mygame/f1/lwjgl3/`
    - `Lwjgl3Launcher.java` – 창 크기, VSync, 아이콘, 메인 클래스로 `Main` 실행
    - `StartupHelper.java` – macOS `-XstartOnFirstThread` 및 Windows 특수 계정명 처리
  - `src/main/resources/` – 아이콘(`libgdx*.png`)
  - `icons/` – OS별 아이콘 파일

## 3) 빌드·실행(AGENTS.md 권장 절차)
- 실행/플레이 테스트: `./gradlew lwjgl3:run` (Windows: `gradlew.bat lwjgl3:run`)
- JAR 패키징: `./gradlew lwjgl3:jar` 또는 OS 전용 `lwjgl3:jarWin|jarMac|jarLinux`
- 자산 리스트 재생성: `./gradlew generateAssetList` → `assets/assets.txt`
- 전체 검증: `./gradlew clean build` (Java 17 컴파일 및 런처 패키징 확인)

## 4) 구현 상세(핵심 포인트)
- 월드/업데이트 고정 스텝: 누적기 `accumulator` 기반, `TIME_STEP = 1/60f`, `world.step(TIME_STEP, 8, 3)`
- 입력
  - 가속/후진: `UP/W`, `DOWN/S` → 전/후 방향 힘 가감속(스무딩 적용)
  - 좌/우 조향: `LEFT/A`, `RIGHT/D` → 각속도 임펄스 적용(후진 시 반대 조향)
  - 브레이크: `SPACE` → `linearDamping` 일시 증가
- 마찰/드래그: 횡방향 속도에 질량·그립(steering 시 감소) 기반 감쇠, 전방속도 비례 드래그
- 속도 제한: 전진/후진에 각기 다른 최고 속도 적용
- 충돌 감쇠: 충돌 시 충격 흡수(선속·각속 감소), 저속 정지시 미세 임펄스 재가속
- 카메라: 차량 전방 오프셋 추적 + 위치/회전 스무딩, `FitViewport(1600/PPM, 900/PPM)`, `zoom=0.5`
- 맵/충돌: `track.tmx` 로드, `Collision` 레이어의 `RectangleMapObject`로 월(StaticBody) 생성
- 렌더: 배경 텍스처 → 차량 스프라이트 → Box2D 디버그 렌더(맵 렌더 호출은 현재 없음)

## 5) 진행 현황 보드(요약)
- 완료(Implemented)
  - 데스크톱 런처 구성 및 실행 설정
  - Box2D 월드/차량 바디/입력/마찰·드래그/속도 제한/카메라 추적
  - Tiled 맵 충돌 레이어 → Box2D 월 생성
  - 자산 로딩/기본 렌더 파이프라인
- 진행 중(In Progress)
  - 트랙 시각화 방식 확정(배경 텍스처 vs. Tiled 맵 렌더)
- 예정/백로그(Backlog)
  - Tiled 맵 렌더 활성화(`mapRenderer.render()`) 또는 배경 텍스처 기반 트랙 개선 중 선택
  - AssetManager 해제 정책 정리(참조만 사용하고 `assetManager.dispose()`로 일괄 해제)
  - JUnit5+AssertJ 테스트 추가(HeadlessApplication): 충돌 처리, 감쇠 튜닝, 카메라 추적 시나리오
  - `README.md` 인코딩 정리(UTF‑8) 및 스크린샷/단축키/조작 설명 보강
  - 자산 변경 시 `generateAssetList` 재실행 CI화(선택)

## 6) 리스크/이슈
- 맵 시각화 부재(또는 불일치): 충돌은 맵 기반인데 화면은 배경 텍스처만 그림 → 플레이어가 보이는 트랙과 실제 충돌 경계가 어긋날 수 있음.
- 자원 해제 중복 위험: `AssetManager`와 직접 `dispose()` 혼용 → 크래시 가능성.
- 자동화 테스트 미비: 물리/카메라 파라미터 변경 시 리그레션 위험.

## 7) 다음 단계(권장 실행 순서)
1) 렌더 경로 결정 및 반영(택1)
   - A안: Tiled 맵을 실제로 렌더: `render()`에서 `mapRenderer.setView(camera); mapRenderer.render();` 호출 후 배경 텍스처 제거 또는 보조 레이어로 유지.
   - B안: 배경 텍스처 기반 유지: 충돌 레이어와 시각 요소를 일치시키도록 텍스처/좌표계 재검증.
2) AssetManager 일원화: `GameScreen.dispose()`에서 텍스처 직접 `dispose()` 제거, 자산 로드는 `Main.assetManager` 참조만 사용.
3) 최소 테스트 3종 추가(core 테스트)
   - 충돌 처리 감쇠 유지 검증(정면/측면 충돌 후 속도/각속 감쇠)
   - 카메라 추적 일관성(차량 회전·가속 시 위치/회전 스무딩 범위)
   - 속도 상한 로직(전진/후진 최고 속도 유지)
4) 수동 주행 체크(가이드): `./gradlew lwjgl3:run` → 입력·물리·자산 로딩 확인
5) 배포 패키징 검증: `./gradlew clean build` 및 `lwjgl3:jar*` 아티팩트 생성 확인

## 8) 검증 체크리스트
- 빌드 전: `./gradlew generateAssetList`로 `assets/assets.txt` 최신화
- 실행 테스트: `./gradlew lwjgl3:run` 후 1랩 주행(충돌 처리/카메라/조작감 확인)
- 패키징: `./gradlew clean build`, 필요 시 `lwjgl3:jarWin|jarMac|jarLinux`
- 코드 검토: `GameScreen.render()`에 맵 렌더 추가 여부 및 AssetManager 해제 정책 반영 확인
- 테스트: `core/src/test/java/com/mygame/f1`에 JUnit5+AssertJ 케이스 실행

## 12) 진행 체크리스트(현 시점)
- [x] LWJGL3 런처/빌드 스크립트 구성 검토 완료
- [x] Box2D 월드/차량/입력/카메라/충돌 감쇠 구현 확인
- [x] 맵 렌더 경로 추가(`mapRenderer.render()`), 임시 비활성화 플래그 적용
- [x] AssetManager 일괄 해제 정책으로 정리(GameScreen 직접 dispose 제거)
- [x] 시작 흐름 구성: Splash → Login → MainMenu → GameScreen
- [x] 메인 메뉴 항목: Single/Multiplayer(TBD)/Settings/Exit (숫자키 1~4)
- [ ] 실행 검증(플레이테스트): `gradlew.bat lwjgl3:run`
- [ ] 자산 리스트 재생성: `./gradlew generateAssetList`
- [ ] HUD/싱글 타임어택/DB 연동(Phase 2) 착수
- [ ] Tiled 맵 저작 및 경로 재활성화(USE_TILED_MAP=true)

## 9) 참고 자료(신뢰 가능한 출처)
- Scrum Guide | Scrum Guides – https://scrumguides.org/scrum-guide.html
- GitHub Docs: About Projects – https://docs.github.com/en/issues/planning-and-tracking-with-projects/learning-about-projects/about-projects
- Atlassian: Status reports – https://www.atlassian.com/agile/project-management/status-reports
- Agile Alliance: Information Radiator – https://www.agilealliance.org/glossary/information-radiator/
- DORA: DevOps Research & Assessment – https://dora.dev/
- PMI: Status Reporting for Executives – https://www.pmi.org/learning/library/status-reporting-executives-6338

각 문서의 권장사항을 반영하여 본 노트는 다음 원칙을 따릅니다: 정보 라디에이터처럼 한 페이지에 핵심 지표/현황/다음 단계 표시, RAG/리스크/차기 행동을 명료하게 기록, 실행 가능한 검증 목록 제공, 코드·빌드·런 명령을 한 곳에 모아 공유 비용 최소화.

## 10) 목표 폴더 구조(제안)
아래 구조는 문서·에셋·소스·테스트·CI를 체계화하기 위한 제안입니다. 현 구조에서 점진적으로 이행하는 것을 권장합니다.

```
f1-racing-game/
├── .github/
│   └── workflows/
│       ├── build.yml                    # CI/CD 파이프라인
│       ├── test.yml                     # 자동 테스트
│       └── release.yml                  # 릴리스 자동화
│
├── docs/
│   ├── PHASES.md                        # 개발 단계별 계획
│   ├── AGENTS.md                        # AI 개발 가이드
│   ├── CHANGELOG.md                     # 버전별 변경사항
│   ├── USER_GUIDE.md                    # 사용자 가이드
│   ├── CONTRIBUTING.md                  # 기여 가이드
│   │
│   ├── architecture/
│   │   ├── SYSTEM-OVERVIEW.md           # 전체 시스템 아키텍처
│   │   ├── CLASS-DIAGRAM.puml           # UML 클래스 다이어그램
│   │   ├── SEQUENCE-DIAGRAMS.md         # 시퀀스 다이어그램들
│   │   └── COMPONENT-DEPENDENCIES.md    # 컴포넌트 의존성 그래프
│   │
│   └── specs/
│       ├── core/
│       │   ├── GAME-LOOP.md
│       │   ├── PHYSICS-ENGINE.md
│       │   ├── CAMERA-SYSTEM.md
│       │   └── INPUT-HANDLING.md
│       │
│       ├── gameplay/
│       │   ├── VEHICLE-PHYSICS.md
│       │   ├── TIRE-SYSTEM.md
│       │   ├── DAMAGE-MODEL.md
│       │   ├── PITSTOP-MINIGAME.md
│       │   └── RACE-START.md
│       │
│       ├── ui/
│       │   ├── HUD-SPECIFICATION.md
│       │   ├── MENU-SYSTEM.md
│       │   ├── LOGIN-SCREEN.md
│       │   ├── MAIN-MENU.md
│       │   ├── GARAGE-SCREEN.md
│       │   ├── TRACK-SELECTION.md
│       │   ├── LEADERBOARD.md
│       │   └── SETTINGS-SCREEN.md
│       │
│       ├── network/
│       │   ├── MULTIPLAYER-SYNC.md
│       │   ├── LOBBY-SYSTEM.md
│       │   ├── PACKET-PROTOCOL.md
│       │   └── NETWORK-SECURITY.md
│       │
│       ├── data/
│       │   ├── DATABASE-SCHEMA.md
│       │   ├── USER-SESSION.md
│       │   └── SAVE-SYSTEM.md
│       │
│       └── assets/
│           ├── TRACK-DESIGN.md
│           ├── VISUAL-EFFECTS.md
│           ├── AUDIO-SPECIFICATION.md
│           └── ASSET-NAMING.md
│
├── assets/
│   ├── assets.txt                       # 자동 생성된 에셋 리스트
│   │
│   ├── vehicles/
│   │   ├── car_01/
│   │   │   ├── body.png
│   │   │   ├── shadow.png
│   │   │   └── config.json              # 차량 스탯 정의
│   │   ├── car_02/
│   │   └── car_03/
│   │
│   ├── tracks/
│   │   ├── track_01_monaco/
│   │   │   ├── track.tmx                # Tiled 맵
│   │   │   ├── tileset.png
│   │   │   ├── background.png
│   │   │   ├── minimap.png
│   │   │   └── config.json              # 트랙 메타데이터
│   │   ├── track_02_monza/
│   │   └── track_03_silverstone/
│   │
│   ├── ui/
│   │   ├── skins/
│   │   │   └── default/
│   │   │       ├── uiskin.json          # Scene2D UI 스킨
│   │   │       └── uiskin.png
│   │   ├── hud/
│   │   │   ├── speedometer_bg.png
│   │   │   ├── speedometer_needle.png
│   │   │   ├── tire_gauge.png
│   │   │   ├── durability_bar.png
│   │   │   └── minimap_frame.png
│   │   ├── menu/
│   │   │   ├── button_normal.9.png
│   │   │   ├── button_hover.9.png
│   │   │   ├── button_pressed.9.png
│   │   │   └── panel_bg.9.png
│   │   ├── icons/
│   │   │   ├── tire_soft.png
│   │   │   ├── tire_medium.png
│   │   │   ├── tire_hard.png
│   │   │   ├── settings.png
│   │   │   └── exit.png
│   │   └── login/
│   │       ├── background.png
│   │       └── logo.png
│   │
│   ├── effects/
│   │   ├── particles/
│   │   │   ├── tire_smoke.p             # Particle effect
│   │   │   ├── tire_smoke.png
│   │   │   ├── collision_spark.p
│   │   │   └── collision_spark.png
│   │   └── textures/
│   │       ├── smoke_particle.png
│   │       └── spark_particle.png
│   │
│   ├── sounds/
│   │   ├── engine/
│   │   │   ├── idle.ogg
│   │   │   ├── accel.ogg
│   │   │   └── high_rpm.ogg
│   │   ├── effects/
│   │   │   ├── collision.ogg
│   │   │   ├── tire_screech.ogg
│   │   │   ├── pit_wrench.ogg
│   │   │   └── countdown_beep.ogg
│   │   ├── ui/
│   │   │   ├── button_click.ogg
│   │   │   └── menu_hover.ogg
│   │   └── music/
│   │       ├── menu_theme.ogg
│   │       └── race_theme.ogg
│   │
│   └── fonts/
│       ├── f1_regular.ttf
│       ├── f1_bold.ttf
│       └── digital.ttf
│
├── core/
│   ├── build.gradle
│   │
│   └── src/
│       ├── main/
│       │   └── java/
│       │       └── com/
│       │           └── mygame/
│       │               └── f1/
│       │                   ├── Main.java
│       │                   │
│       │                   ├── screens/
│       │                   │   ├── BaseScreen.java
│       │                   │   ├── SplashScreen.java
│       │                   │   ├── LoginScreen.java
│       │                   │   ├── MainMenuScreen.java
│       │                   │   ├── GarageScreen.java
│       │                   │   ├── TrackSelectionScreen.java
│       │                   │   ├── GameScreen.java
│       │                   │   ├── MultiplayerLobbyScreen.java
│       │                   │   ├── MultiplayerGameScreen.java
│       │                   │   ├── ResultsScreen.java
│       │                   │   ├── LeaderboardScreen.java
│       │                   │   └── SettingsScreen.java
│       │                   │
│       │                   ├── gameplay/
│       │                   │   ├── vehicle/
│       │                   │   │   ├── VehicleController.java
│       │                   │   │   ├── VehicleConfig.java
│       │                   │   │   └── VehicleFactory.java
│       │                   │   ├── track/
│       │                   │   │   ├── TrackLoader.java
│       │                   │   │   ├── TrackConfig.java
│       │                   │   │   └── LapCounter.java
│       │                   │   ├── tire/
│       │                   │   │   ├── TireManager.java
│       │                   │   │   ├── TireType.java
│       │                   │   │   └── TireDegradation.java
│       │                   │   ├── damage/
│       │                   │   │   ├── DamageManager.java
│       │                   │   │   └── DamageCalculator.java
│       │                   │   ├── pitstop/
│       │                   │   │   ├── PitStopManager.java
│       │                   │   │   ├── PitStopMinigame.java
│       │                   │   │   └── TimingBar.java
│       │                   │   └── race/
│       │                   │       ├── RaceManager.java
│       │                   │       ├── RaceState.java
│       │                   │       ├── StartSequence.java
│       │                   │       └── PositionCalculator.java
│       │                   │
│       │                   ├── physics/
│       │                   │   ├── PhysicsWorld.java
│       │                   │   ├── ContactHandler.java
│       │                   │   ├── PhysicsUtils.java
│       │                   │   └── BodyBuilder.java
│       │                   │
│       │                   ├── camera/
│       │                   │   ├── CameraController.java
│       │                   │   └── CameraConfig.java
│       │                   │
│       │                   ├── ui/
│       │                   │   ├── hud/
│       │                   │   │   ├── HUDManager.java
│       │                   │   │   ├── Speedometer.java
│       │                   │   │   ├── LapDisplay.java
│       │                   │   │   ├── TimerDisplay.java
│       │                   │   │   ├── Minimap.java
│       │                   │   │   ├── TireGauge.java
│       │                   │   │   ├── DurabilityBar.java
│       │                   │   │   └── PositionBadge.java
│       │                   │   ├── widgets/
│       │                   │   │   ├── CustomButton.java
│       │                   │   │   ├── CustomTable.java
│       │                   │   │   └── ProgressBar.java
│       │                   │   ├── UIFactory.java
│       │                   │   └── DialogManager.java
│       │                   │
│       │                   ├── network/
│       │                   │   ├── NetworkManager.java
│       │                   │   ├── GameServer.java
│       │                   │   ├── GameClient.java
│       │                   │   ├── packets/
│       │                   │   │   ├── PacketRegistry.java
│       │                   │   │   ├── PlayerJoinPacket.java
│       │                   │   │   ├── VehicleStatePacket.java
│       │                   │   │   ├── RoomListPacket.java
│       │                   │   │   ├── RaceStartPacket.java
│       │                   │   │   └── RaceEndPacket.java
│       │                   │   └── sync/
│       │                   │       ├── StateInterpolator.java
│       │                   │       └── ClientPredictor.java
│       │                   │
│       │                   ├── data/
│       │                   │   ├── DatabaseManager.java
│       │                   │   ├── UserDAO.java
│       │                   │   ├── LapTimeDAO.java
│       │                   │   ├── VehicleDAO.java
│       │                   │   └── models/
│       │                   │       ├── User.java
│       │                   │       ├── LapTime.java
│       │                   │       ├── Vehicle.java
│       │                   │       └── UserPreferences.java
│       │                   │
│       │                   ├── session/
│       │                   │   ├── UserSession.java
│       │                   │   └── GameSession.java
│       │                   │
│       │                   ├── managers/
│       │                   │   ├── AssetLoader.java
│       │                   │   ├── AudioManager.java
│       │                   │   ├── InputManager.java
│       │                   │   └── ScreenManager.java
│       │                   │
│       │                   └── utils/
│       │                       ├── Constants.java
│       │                       ├── GameConfig.java
│       │                       ├── MathUtils.java
│       │                       ├── PasswordHasher.java
│       │                       ├── ValidationUtils.java
│       │                       └── ObjectPool.java
│       │
│       └── test/
│           └── java/
│               └── com/
│                   └── mygame/
│                       └── f1/
│                           ├── physics/
│                           │   ├── VehiclePhysicsTest.java
│                           │   ├── CollisionTest.java
│                           │   └── FrictionTest.java
│                           ├── gameplay/
│                           │   ├── TireSystemTest.java
│                           │   ├── DamageSystemTest.java
│                           │   ├── PitStopTest.java
│                           │   └── LapDetectionTest.java
│                           ├── network/
│                           │   ├── PacketSerializationTest.java
│                           │   ├── SyncTest.java
│                           │   └── LobbyTest.java
│                           ├── data/
│                           │   ├── DatabaseTest.java
│                           │   └── DAOTest.java
│                           └── utils/
│                               └── TestUtils.java
│
├── lwjgl3/
│   ├── build.gradle
│   ├── nativeimage.gradle
│   │
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── mygame/
│   │       │           └── f1/
│   │       │               └── lwjgl3/
│   │       │                   ├── Lwjgl3Launcher.java
│   │       │                   └── StartupHelper.java
│   │       └── resources/
│   │           ├── libgdx128.png
│   │           ├── libgdx64.png
│   │           └── libgdx32.png
│   │
│   └── icons/
│       ├── icon_128.png
│       ├── icon_64.png
│       └── icon_32.png
│
├── data/                                # 런타임 데이터 (gitignored)
│   ├── game.db                          # SQLite 데이터베이스
│   └── logs/
│       └── game.log
│
├── migrations/                          # 데이터베이스 마이그레이션
│   ├── 001_initial_schema.sql
│   ├── 002_add_multiplayer_stats.sql
│   └── 003_add_vehicle_customization.sql
│
├── scripts/                             # 유틸리티 스크립트
│   ├── generate_assets_list.sh
│   ├── optimize_textures.sh
│   └── backup_database.sh
│
├── .gitignore
├── .editorconfig
├── README.md
├── LICENSE
├── settings.gradle
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.jar
        └── gradle-wrapper.properties
```

## 11) 마이그레이션 가이드(현 구조 → 목표 구조)
- 문서 폴더화: 루트 `AGENTS.md`를 유지하되, `docs/` 하위에 사본/확장본을 두고 링크(README에서 경로 안내).
- CI 도입: `.github/workflows/{build.yml,test.yml,release.yml}` 추가. Gradle 캐시/자산 생성(`generateAssetList`) 포함.
- 에셋 리팩터: `assets/` 하위에 `vehicles/`, `tracks/`, `ui/`, `effects/`, `sounds/`, `fonts/` 생성 후 점진 이동. 코드 경로/로드 키를 이동 순서에 맞춰 갱신하고, 매 이동 후 `./gradlew generateAssetList` 실행.
- 패키지 분리: `com.mygame.f1.screens`, `...gameplay`, `...physics` 등 패키지 생성 → 클래스 점진 이동(빈도 낮은 클래스부터). 각 이동은 작은 PR 단위로 수행.
- 테스트 추가: `core`에 JUnit5+AssertJ 의존성 추가 후, 물리/카메라/입력부터 스펙 테스트 작성.
- 호환성 체크: 각 단계마다 `./gradlew lwjgl3:run` 수동 주행, `./gradlew clean build`로 패키징 검증.
