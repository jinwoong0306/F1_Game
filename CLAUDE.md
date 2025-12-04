# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

F1 2D 레이싱 게임은 libGDX 기반의 탑다운 포뮬러 1 레이싱 게임으로, 전략적 타이어 관리와 온라인 멀티플레이어 기능을 제공합니다. Box2D 물리 엔진, KryoNet 네트워킹, Scene2D UI 프레임워크를 사용하여 개발되었습니다.

**기술 스택**: Java 17, libGDX 1.13.1, Box2D, KryoNet 2.22.0-RC1, Gradle 8.5+

## 빌드 명령어

> **Windows**: `gradlew` 사용, **macOS/Linux**: `./gradlew` 사용

```bash
# 전체 프로젝트 빌드
gradlew build

# 데스크톱 클라이언트 실행
gradlew lwjgl3:run

# 전용 서버 실행 (TCP 54555, UDP 54777)
gradlew server:run

# 커스텀 포트로 서버 실행
gradlew server:run --args="54555 54777"

# 특정 모듈만 컴파일 (빠른 검증)
gradlew :core:compileJava
gradlew :server:compileJava

# UI 텍스처를 아틀라스로 패킹 (raw-ui/ -> atlas/game_ui.atlas)
gradlew lwjgl3:packAtlas

# 배포용 JAR 빌드
gradlew lwjgl3:dist
# 출력: lwjgl3/build/libs/My-f1-Game-1.0.0.jar

# 플랫폼별 JAR (파일 크기 감소)
gradlew lwjgl3:jarWin    # Windows
gradlew lwjgl3:jarMac    # macOS
gradlew lwjgl3:jarLinux  # Linux

# 테스트 실행
gradlew test

# 단일 테스트 클래스 실행
gradlew test --tests "com.mygame.f1.SomeTest"

# IDEA 프로젝트 파일 생성
gradlew idea
```

## 프로젝트 구조 (멀티 모듈)

```
F1/
├── core/           플랫폼 독립적 게임 로직
│   ├── screens/    Scene2D를 사용한 UI 화면 (로그인, 메뉴, 게임플레이, 로비)
│   ├── network/    클라이언트측 KryoNet 래퍼 (LobbyClient)
│   ├── ui/         SkinFactory - 중앙화된 테마/스타일링
│   └── data/       UserStore - JSON 기반 사용자 데이터 저장
│
├── lwjgl3/         데스크톱 런처 및 도구
│   └── tools/      TexturePackerTool - 아틀라스 생성 유틸리티
│
├── shared/         네트워크 패킷 정의 (클라이언트-서버 공유)
│   └── Packets.java - 멀티플레이어용 20+ 패킷 클래스
│
├── server/         전용 게임 서버
│   ├── GameServer.java      - 방 관리, 상태 동기화, 충돌 처리
│   ├── PacketRegistry.java  - Kryo 직렬화 설정
│   └── ServerLauncher.java  - 서버 진입점
│
└── assets/         게임 리소스 (텍스처, 폰트, 맵, 데이터)
```

**모듈 의존성**:
- `core`: `gdx`, `gdx-box2d`, `gdx-freetype`, `shared`, `kryonet` 의존
- `lwjgl3`: `gdx-backend-lwjgl3`, `core` 의존
- `server`: `kryonet`, `shared` 의존
- `shared`: 외부 의존성 없음 (순수 Java)

## 아키텍처 개요

### 3계층 아키텍처

1. **프레젠테이션 계층** (Scene2D UI + 렌더링)
   - [SkinFactory.java](core/src/main/java/com/mygame/f1/ui/SkinFactory.java)를 통한 중앙화된 스킨 적용 Stage 기반 위젯 시스템
   - 화면 흐름: SplashScreen → LoginScreen → MainMenuScreen → SinglePlayScreen/MultiplayerPlaceholderScreen → GameScreen
   - 일관된 테마를 위해 모든 폰트/색상이 `SkinFactory.Palette`에 정의됨

2. **게임 로직 계층** (물리 + 게임플레이)
   - 고정 1/60초 타임스텝을 사용하는 Box2D 물리 엔진
   - 차량 물리: 조정 가능한 파라미터 (maxForwardSpeed, acceleration, lateralFriction, airResistance)
   - Tiled 맵 레이어에서 로드되는 체크포인트/랩타임 시스템
   - 타이밍 바 메커니즘을 사용한 피트스톱 미니게임
   - 위치/회전 스무딩(lerp)을 적용한 추적 카메라

3. **네트워크 계층** (클라이언트-서버)
   - KryoNet: TCP (신뢰성) + UDP (실시간 상태 동기화 @ 20Hz)
   - 서버 권위 모델: 서버가 충돌, 방 관리, 게임 단계 처리
   - 부드러운 렌더링을 위한 클라이언트 예측과 보간/외삽
   - 방 시스템: 방당 최대 4명, 단계 (WAITING → COUNTDOWN → RUNNING → FINISHED)

### 진입점

**데스크톱 클라이언트**: `Lwjgl3Launcher.main()` → `Main` (Game 인스턴스) 생성 → 에셋 로드 → `SplashScreen` 표시

**서버**: `ServerLauncher.main()` → `GameServer` 생성 → TCP/UDP 포트에서 수신 대기

### 화면 흐름

```
SplashScreen (3초 애니메이션 로딩)
  └─ LoginScreen (사용자명/비밀번호, UserStore 연동)
      └─ MainMenuScreen (싱글/멀티/설정/종료)
          ├─ SinglePlayScreen (차량 선택, 트랙 선택)
          │   └─ GameScreen (물리, HUD, 랩타임을 포함한 게임플레이)
          │
          └─ MultiplayerPlaceholderScreen (로비 UI)
              ├─ 방 생성/참가/퇴장
              ├─ 준비 상태를 포함한 플레이어 명단
              ├─ 차량 캐러셀 + 트랙 선택 (호스트)
              └─ 채팅 패널
              └─ 레이스 시작 시 → GameScreen (멀티플레이어 모드)
```

## 핵심 시스템 및 파일

### 물리 시스템
**파일**: [GameScreen.java](core/src/main/java/com/mygame/f1/GameScreen.java) (1-700줄)

- 누산기(accumulator) 패턴을 사용하는 고정 타임스텝(1/60초) Box2D 월드
- 플레이어 차량: 동적 원형 바디 (반경 ~0.19m)
- Tiled 맵의 "collision" 레이어에서 로드되는 트랙 충돌 바디
- 접촉 리스너가 벽 충돌을 감지하고 속도 페널티 적용
- 차량 튜닝 파라미터: `maxForwardSpeed`, `acceleration`, `lateralFriction`, `airResistance`, `angularDamping`, `brakeLinearDamping`
- 측면 마찰이 미끄러짐을 억제하여 현실적인 코너링 구현

### 네트워크 아키텍처
**클라이언트**: [LobbyClient.java](core/src/main/java/com/mygame/f1/network/LobbyClient.java)
- 메서드가 비동기 처리를 위해 `CompletableFuture<Response>` 반환
- UDP: 빈번한 플레이어 상태 업데이트에 사용 (위치, 속도, 회전)
- TCP: 신뢰성 있는 메시지에 사용 (방 생성, 채팅, 준비 상태)

**서버**: [GameServer.java](server/src/main/java/com/mygame/f1/server/GameServer.java)
- 포트: TCP 54555, UDP 54777 (기본값)
- 방 관리: Map<roomId, Room>, 각 Room은 최대 4명의 플레이어 추적
- UDP를 통한 20Hz (50ms 간격) 상태 브로드캐스트
- 물리 동기화를 위한 단순 원형 충돌 감지
- 게임 단계: WAITING (로비) → COUNTDOWN (5-10초) → RUNNING (레이스) → FINISHED

**패킷**: [Packets.java](shared/src/main/java/com/mygame/f1/shared/Packets.java)
- 직렬화를 위해 클라이언트/서버 간 공유
- 주요 클래스: `CreateRoomRequest/Response`, `JoinRoomRequest/Response`, `RaceStartPacket`, `PlayerStateUpdate`, `GameStatePacket`, `ChatMessage`
- 데이터 모델: `PlayerInfo`, `PlayerState`, `RoomState`, `RoomPhase`

### UI 시스템 (Scene2D)
**테마 팩토리**: [SkinFactory.java](core/src/main/java/com/mygame/f1/ui/SkinFactory.java)

- 중앙화된 색상 팔레트: BG (#000000), PANEL (#0f1118), BTN_NORMAL (#222838), ACCENT (#3D7BFF), NEON_RED (#FF0000)
- 폰트 정의: `capitolcity.ttf` (영어), `NotoSansKR-Regular.ttf` (한글 지원)
- 위젯 스타일: Label, TextButton, ImageTextButton, TextField, ScrollPane, CheckBox, Slider
- 아틀라스 로딩: 아이콘 별칭을 포함한 `assets/atlas/game_ui.atlas` (예: "icon-car" → "select-car")
- 모든 UI 화면이 일관된 스타일링을 위해 이 팩토리 사용

**화면 패턴**:
```java
public class SomeScreen implements Screen {
  private Stage stage;
  private Skin skin;

  @Override
  public void show() {
    skin = SkinFactory.createDefaultSkin();
    stage = new Stage(new ScreenViewport());
    Gdx.input.setInputProcessor(stage);
    // Table과 Widget으로 UI 구성
  }

  @Override
  public void render(float delta) {
    stage.act(delta);
    stage.draw();
  }
}
```

### 에셋 관리
**파일**: [Main.java](core/src/main/java/com/mygame/f1/Main.java)

- 싱글톤 `AssetManager`가 `create()`에서 중요 에셋을 사전 로드하여 게임 중 끊김 방지
- 차량 텍스처: `assets/cars/`의 6대 차량 (Astra A4, Boltworks RX-1, Emerald E7, Gold Rush GT, Midnight P4, Silverline S11)
- UI 에셋: `assets/ui/login/logo.png`, `assets/atlas/game_ui.atlas`
- 특정 크기/문자로 `FreeTypeFontGenerator`를 통해 폰트 로드
- Gradle 태스크로 자동 생성되는 에셋 목록: `assets/assets.txt`

### 사용자 데이터 저장
**파일**: [UserStore.java](core/src/main/java/com/mygame/f1/data/UserStore.java)

- JSON 기반 저장소: `assets/data/users.json`
- 각 레코드: `username`, `salt` (Base64 인코딩 16바이트 랜덤), `passwordHash` (SHA-256(password + salt))
- 메서드: `register(username, password)`, `verify(username, password)`, `load()`, `save()`
- 보안: 사용자별 salt로 레인보우 테이블 방지, 타이밍 공격 저항을 위한 상수 시간 비교

### 카메라 시스템
**파일**: [GameScreen.java](core/src/main/java/com/mygame/f1/GameScreen.java) (카메라 추적)

- 오버 더 숄더(over-the-shoulder) 추적 카메라가 플레이어 차량을 따라감
- 위치 스무딩: `lerp(current, target, 5.0 * delta)`
- 회전 스무딩: `lerpAngle(current, target, 3.0 * delta)`
- 오프셋: 더 나은 시야를 위해 차량보다 0.8m 앞
- 뷰포트: 해상도에 관계없이 일관된 월드 크기를 위한 `FitViewport`

### 랩타임 시스템
**파일**: [GameScreen.java](core/src/main/java/com/mygame/f1/GameScreen.java) (updateLapAndCheckpoints)

- Tiled 맵 레이어 "checkpoints"에서 체크포인트 로드 (index 속성을 가진 사각형)
- 순차적 검증: 체크포인트를 순서대로 통과해야 함
- 모든 체크포인트 통과 후 출발선 통과 = 랩 완료
- 추적 항목: `bestLapTime`, `lastLapTime`, `currentLap`, `lapTimeSeconds`
- 피트스톱 영역: 맵의 진입, 서비스, 퇴장 사각형
- 게임 상태: PRE_START → NORMAL → PIT_ENTERING → PIT_MINIGAME → PIT_EXITING → NORMAL

## 개발 가이드라인

### 성능 최적화
- **객체 재사용**: 렌더 루프에서 Vector2 객체를 생성하는 대신 임시 변수 사용 (예: `v2_tmp1`, `v2_tmp2`)
- **에셋 사전 로드**: GC 끊김을 방지하기 위해 `Main.create()`에서 모든 중요 텍스처 로드
- **보간**: 60 FPS에서 부드러운 움직임을 위해 카메라와 원격 차량이 `lerp()` 사용
- **배칭**: 단일 `Stage.draw()` 호출을 통해 UI 렌더링
- **고정 물리 타임스텝**: 비결정성을 방지하는 누산기를 사용한 1/60초

### Tiled 맵 작업
- 맵 저장 위치: `assets/*.tmx` (f1_racing_map.tmx, america.tmx, japan.tmx)
- 필수 레이어:
  - "collision": 트랙 경계를 위한 정적 바디
  - "checkpoints": "index" 속성을 가진 사각형 (순차적 검증)
  - "start_line": 랩 시작/종료 감지를 위한 사각형
  - "pit_entry", "pit_service", "pit_exit": 피트스톱 영역
- 로딩: `TmxMapLoader`, 렌더링: `OrthogonalTiledMapRenderer`
- GameScreen 생성자에서 맵 객체로부터 충돌 바디 생성

### UI 텍스처 아틀라스 워크플로우
1. `assets/raw-ui/`에 원본 이미지 배치
2. 실행: `./gradlew lwjgl3:packAtlas`
3. 출력: `assets/atlas/game_ui.atlas` + `game_ui.png`
4. 아틀라스를 로드하는 `SkinFactory.createDefaultSkin()`을 통해 코드에서 참조
5. SkinFactory에서 아이콘 별칭 설정 (예: "icon-single" → "single-play")

### 새 화면 추가
1. `Screen` 인터페이스를 구현하는 클래스 생성
2. `SkinFactory.createDefaultSkin()`을 통해 `Skin` 초기화
3. 적절한 뷰포트로 `Stage` 생성
4. 입력 프로세서 설정: `Gdx.input.setInputProcessor(stage)`
5. 스킨의 `Table`과 위젯을 사용하여 UI 구성
6. 렌더 루프 구현: `stage.act(delta)` + `stage.draw()`
7. 리소스 해제: `stage.dispose()`, `skin.dispose()`
8. 화면 전환: `Main.getInstance().setScreen(new YourScreen())`

### 네트워크 패킷 개발
1. [shared/src/main/java/com/mygame/f1/shared/Packets.java](shared/src/main/java/com/mygame/f1/shared/Packets.java)에 패킷 클래스 정의
2. [server/src/main/java/com/mygame/f1/server/PacketRegistry.java](server/src/main/java/com/mygame/f1/server/PacketRegistry.java)에 패킷 등록
3. `server.addListener(new Listener() {...})`를 통해 `GameServer`에 핸들러 추가
4. `client.addListener(new Listener() {...})`를 통해 `LobbyClient`에 클라이언트 핸들러 추가
5. 신뢰성 있는 메시지는 TCP, 빈번한 상태 업데이트는 UDP 사용

### 새 차량 추가
1. `assets/cars/`에 PNG 텍스처 추가
2. `Main.create()`에서 사전 로드: `assetManager.load("cars/YourCar.png", Texture.class)`
3. `SinglePlayScreen.java`와 `MultiplayerPlaceholderScreen.java`의 차량 선택 UI에 추가
4. 차량 데이터 구조에 튜닝 파라미터 (속도, 가속도, 핸들링) 정의
5. UI 화면의 차량 수 상수 업데이트

## 테스트

### 로컬 멀티플레이어 테스트
1. 서버 시작: `gradlew server:run`
2. 클라이언트 1 시작: `gradlew lwjgl3:run`
3. 클라이언트 2 시작: `gradlew lwjgl3:run` (별도 터미널에서)
4. 두 클라이언트: 로그인 → MULTI PLAY → 방 생성/참가
5. 차량 선택, 준비 표시, 호스트가 레이스 시작

### 디버그 설정
- Box2D 메모리 디버깅: `gradlew lwjgl3:run -Dbox2d.debugMemory=true`
- 상세 로깅: `gradlew lwjgl3:run --info`
- `server:run` 중 콘솔에 서버 로그 표시

## 주요 설정

### 물리 튜닝 (GameScreen.java)
- `maxForwardSpeed`: 3.5 m/s (기본값, 타이어 컴파운드에 따라 수정됨)
- `acceleration`: 차량별 설정 (높을수록 빠른 가속)
- `lateralFriction`: 미끄러짐 억제, 조향 시 감소
- `airResistance`: 속도에 비례 (자연스러운 감속)
- `angularDamping`: 회전 스무딩
- `brakeLinearDamping`: 급정지 없는 부드러운 브레이크

### 네트워크 설정
- 서버 포트: TCP 54555, UDP 54777 (인자로 설정 가능)
- 클라이언트 타임아웃: 30000ms (30초)
- 상태 브로드캐스트 빈도: 20Hz (50ms 간격)
- 스레드 모델: `ThreadedListener` (백그라운드 스레드에서 패킷 처리)

### 렌더링 설정 (Lwjgl3Launcher.java)
- 창 크기: 1600x900
- VSync: 활성화
- FPS: 60 (제한)
- macOS: `-XstartOnFirstThread` JVM 인자 필요

## 문서 참조

`docs/` 디렉토리의 상세 문서:
- [PROJECT-OVERVIEW.md](docs/specs/PROJECT-OVERVIEW.md) - 프로젝트 전체 비전
- [PHASES.md](docs/PHASES.md) - 개발 로드맵
- [VEHICLE-PHYSICS.md](docs/specs/gameplay/VEHICLE-PHYSICS.md) - 물리 시스템 상세
- [TIRE-SYSTEM.md](docs/specs/gameplay/TIRE-SYSTEM.md) - 타이어 마모 메커니즘
- [PITSTOP-MINIGAME.md](docs/specs/gameplay/PITSTOP-MINIGAME.md) - 피트스톱 구현
- [HUD-SPECIFICATION.md](docs/specs/ui/HUD-SPECIFICATION.md) - HUD 레이아웃 및 요소
- [MULTIPLAYER-SYNC.md](docs/specs/network/MULTIPLAYER-SYNC.md) - 네트워크 동기화 전략
- [DATABASE-SCHEMA.md](docs/specs/data/DATABASE-SCHEMA.md) - 데이터 저장 스키마

## 빠른 네비게이션

| 이해하려는 내용 | 읽어야 할 파일 |
|------------------|------------------|
| 게임 시작 | [Lwjgl3Launcher.java](lwjgl3/src/main/java/com/mygame/f1/lwjgl3/Lwjgl3Launcher.java) → [Main.java](core/src/main/java/com/mygame/f1/Main.java) |
| 화면 전환 | [Main.java](core/src/main/java/com/mygame/f1/Main.java) (setScreen 호출), 각 Screen 클래스 |
| 게임플레이 물리 | [GameScreen.java](core/src/main/java/com/mygame/f1/GameScreen.java) (물리 루프) |
| 카메라 추적 | [GameScreen.java](core/src/main/java/com/mygame/f1/GameScreen.java) (updateCamera 메서드) |
| 랩타임 | [GameScreen.java](core/src/main/java/com/mygame/f1/GameScreen.java) (updateLapAndCheckpoints) |
| 네트워킹 | [LobbyClient.java](core/src/main/java/com/mygame/f1/network/LobbyClient.java), [GameServer.java](server/src/main/java/com/mygame/f1/server/GameServer.java), [Packets.java](shared/src/main/java/com/mygame/f1/shared/Packets.java) |
| 멀티플레이어 로비 | [MultiplayerPlaceholderScreen.java](core/src/main/java/com/mygame/f1/screens/MultiplayerPlaceholderScreen.java) |
| UI 스타일링 | [SkinFactory.java](core/src/main/java/com/mygame/f1/ui/SkinFactory.java) |
| 사용자 인증 | [UserStore.java](core/src/main/java/com/mygame/f1/data/UserStore.java) |
| 에셋 관리 | [Main.java](core/src/main/java/com/mygame/f1/Main.java) (create 메서드) |
| 서버 설정 | [ServerLauncher.java](server/src/main/java/com/mygame/f1/server/ServerLauncher.java), [GameServer.java](server/src/main/java/com/mygame/f1/server/GameServer.java) |

## Git 브랜치 전략

- **main**: 안정 버전 (배포용)
- **feature/singleplay-polish**: 싱글플레이 기능 개선
- **feature/multiplayer-core**: 멀티플레이어 핵심 기능 개발

커밋 메시지 규칙:
- `feat:` 새로운 기능 추가
- `fix:` 버그 수정
- `refactor:` 리팩토링
- `docs:` 문서 수정

## 언어 관련 참고사항

이 프로젝트는 한국에서 개발된 프로젝트입니다. README.md와 많은 주석이 한글로 작성되어 있습니다. UI는 영어(capitolcity.ttf)와 한글(NotoSansKR-Regular.ttf) 폰트를 모두 지원하며, 문자 렌더링 문제를 방지하기 위한 적절한 글리프 지원이 포함되어 있습니다.
