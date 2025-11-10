# F1 2D Racing Game - Project Overview

## 🎯 프로젝트 비전

**"실제 F1의 속도감과 전략, 그리고 타이어 관리를 2D 환경에서 직관적으로 구현한 레이싱 게임"**

본 프로젝트는 Java 기반 libGDX 프레임워크를 사용하여 정교한 물리 엔진(Box2D)과 역동적인 카메라 워크가 특징인 2D 탑다운 F1 레이싱 게임입니다.

---

## 📊 프로젝트 정보

| 항목 | 내용 |
|------|------|
| **프로젝트명** | F1 2D Racing Game |
| **개발 기간** | 10-11주 (Phase 0~5) |
| **팀 규모** | 4-6명 권장 |
| **타겟 플랫폼** | Desktop (Windows, macOS, Linux) |
| **개발 언어** | Java 17+ |
| **메인 프레임워크** | libGDX 1.13.1 |
| **저장소** | https://github.com/yourname/f1-racing-game |

---

## 🎮 핵심 기능

### 1. 게임 모드
- **싱글플레이어 타임 어택**: 개인 최고 기록 도전
- **멀티플레이어 온라인 레이스**: 2-4인 실시간 대전
- **연습 모드**: 자유 주행 및 트랙 학습

### 2. 전략 시스템
- **3종 타이어 시스템**: Soft/Medium/Hard (성능 vs 내구도 트레이드오프)
- **차량 내구도 관리**: 충돌 및 오프로드 주행 시 손상
- **피트 스톱 미니게임**: 타이밍 기반 정비 시간 최소화

### 3. 물리 엔진
- **Box2D 기반 시뮬레이션**: 현실적인 가속, 감속, 드리프트
- **충돌 반응**: 벽/차량 충돌 시 속도 감소 및 손상
- **트래킹 카메라**: 차량을 중심으로 회전하는 몰입형 시점

### 4. 네트워킹
- **KryoNet 기반**: 빠르고 효율적인 Java 네트워크 라이브러리
- **로비 시스템**: 방 생성/참가/목록 조회
- **실시간 동기화**: 클라이언트 예측 + 서버 권위 모델

### 5. 데이터 관리
- **SQLite 데이터베이스**: 사용자 계정, 랩 타임, 전적 관리
- **랭킹 시스템**: 트랙별 상위 10위 리더보드
- **도전과제**: 숨겨진 업적 및 보상

---

## 🏗️ 기술 스택

### Core Technologies
| 레이어 | 기술 | 버전 | 역할 |
|--------|------|------|------|
| **프레임워크** | libGDX | 1.13.1 | 게임 루프, 렌더링, 입력 처리 |
| **물리 엔진** | Box2D | bundled | 차량 물리, 충돌 감지 |
| **UI 시스템** | Scene2D.ui | bundled | HUD 및 메뉴 화면 |
| **네트워크** | KryoNet | 2.24.0 | 멀티플레이어 동기화 |
| **데이터베이스** | SQLite | 3.45+ | 로컬 데이터 저장 |
| **빌드 도구** | Gradle | 8.5+ | 의존성 관리 및 빌드 |
| **맵 에디터** | Tiled | 1.10+ | 트랙 디자인 |

### Development Tools
- **IDE**: IntelliJ IDEA / VS Code
- **VCS**: Git + GitHub
- **CI/CD**: GitHub Actions
- **테스팅**: JUnit5 + AssertJ
- **프로파일링**: VisualVM

---

## 📁 프로젝트 구조

```
f1-racing-game/
├── docs/                           # 📚 문서
│   ├── PROJECT-OVERVIEW.md         # 이 파일
│   ├── PHASES.md                   # 개발 단계별 계획
│   ├── AGENTS.md                   # AI 개발 가이드
│   ├── architecture/               # 아키텍처 문서
│   └── specs/                      # 상세 스펙 문서
│       ├── core/                   # 핵심 시스템
│       ├── gameplay/               # 게임플레이 로직
│       ├── ui/                     # UI/HUD
│       ├── network/                # 네트워크
│       ├── data/                   # 데이터베이스
│       └── assets/                 # 에셋 명세
│
├── core/                           # 🎮 게임 로직
│   └── src/
│       ├── main/java/              # 메인 소스 코드
│       │   └── com/mygame/f1/
│       │       ├── screens/        # 게임 화면들
│       │       ├── gameplay/       # 게임플레이 시스템
│       │       ├── physics/        # 물리 엔진
│       │       ├── ui/             # UI 컴포넌트
│       │       ├── network/        # 네트워크 로직
│       │       ├── data/           # 데이터 접근
│       │       └── utils/          # 유틸리티
│       └── test/java/              # 테스트 코드
│
├── lwjgl3/                         # 🖥️ 데스크톱 런처
│
├── assets/                         # 🎨 게임 에셋
│   ├── vehicles/                   # 차량 스프라이트
│   ├── tracks/                     # 트랙 맵 (Tiled)
│   ├── ui/                         # UI 텍스처
│   ├── effects/                    # 파티클 효과
│   ├── sounds/                     # 사운드 이펙트
│   └── fonts/                      # 폰트 파일
│
├── data/                           # 💾 런타임 데이터
│   └── game.db                     # SQLite 데이터베이스
│
└── migrations/                     # 🔄 DB 마이그레이션
```

---

## 🎯 개발 로드맵 (10주)

### Phase 0: 프로젝트 설정 (3일)
- 개발 환경 구축
- 스펙 문서 작성
- CI/CD 파이프라인 설정
- 에셋 요구사항 정리

### Phase 1: 핵심 기반 (2주)
**목표**: 주행 가능한 프로토타입
- Box2D 물리 월드 설정
- 차량 제어 (가속, 조향, 브레이크)
- 트래킹 카메라 구현
- Tiled 맵 로딩 및 충돌 처리

**완료 기준**: 트랙을 돌 수 있는 차량, 60 FPS 안정

### Phase 2: 싱글플레이어 (2주)
**목표**: 완전한 타임 어택 모드
- HUD 시스템 (속도계, 랩 타이머, 미니맵)
- F1 스타일 레이스 시작 (신호등)
- SQLite 데이터베이스 연동
- 랭킹 보드

**완료 기준**: 시작부터 끝까지 플레이 가능, 기록 저장

### Phase 3: 전략 시스템 (2주)
**목표**: 타이어 및 피트 스톱
- 3종 타이어 시스템 (Soft/Medium/Hard)
- 타이어 마모 및 성능 저하
- 차량 내구도 시스템
- 피트 스톱 미니게임

**완료 기준**: 전략적 피트 스톱이 게임 승패에 영향

### Phase 4: 멀티플레이어 (3주)
**목표**: 온라인 대전
- KryoNet 서버/클라이언트
- 로비 시스템 (방 생성/참가)
- 실시간 차량 동기화
- 전적 관리

**완료 기준**: 4인 온라인 레이스 안정적 동작

### Phase 5: 폴리싱 (1주)
**목표**: 출시 준비
- 버그 수정
- 비주얼/오디오 이펙트
- 성능 최적화
- 배포 패키징

**완료 기준**: 크리티컬 버그 0개, 배포 가능한 JAR

---

## 🎨 시각적 디자인 가이드

### 화면 레이아웃 (1920x1080 기준)

#### 게임 화면
```
┌────────────────────────────────────────────────┐
│ LAP 2/5    Track Name           1ST   Minimap │ Top Bar
│ 1:23.456                        +0.5s  [Map]  │
├────────────────────────────────────────────────┤
│                                                 │
│              [Gameplay Area]                    │ Main View
│           (Rotating Camera)                     │
│                                                 │
├────────────────────────────────────────────────┤
│ Tire: MEDIUM  [████████░░]     Speed: 285 km/h│ Bottom Bar
│ Durability:   [███████░░░]     RPM: [████████]│
└────────────────────────────────────────────────┘
```

#### 메인 메뉴
```
┌────────────────────────────────────────────────┐
│  [Logo]                    [Profile] [Settings]│
│                                                 │
│              [F1 RACING LOGO]                   │
│                                                 │
│         ┌─────────────────────────┐            │
│         │    Single Player        │            │
│         │    Multiplayer          │            │
│         │    Leaderboard          │            │
│         │    Garage               │            │
│         │    Settings             │            │
│         │    Exit                 │            │
│         └─────────────────────────┘            │
│                                                 │
│  [Recent Achievement: Monaco Master]           │
└────────────────────────────────────────────────┘
```

---

## 🎮 게임플레이 흐름

### 싱글플레이어
```
Login → Main Menu → Track Selection → Vehicle Selection
  ↓
Race Start (Lights) → Racing (Lap 1-5) → Pit Stop (Optional)
  ↓
Race End → Results Screen → Leaderboard → Main Menu
```

### 멀티플레이어
```
Login → Main Menu → Multiplayer Lobby
  ↓
Create Room / Join Room → Waiting Room (Ready Check)
  ↓
Race Start → Racing (4 Players) → Race End
  ↓
Results Screen → Save Stats → Lobby
```

---

## 📊 성능 목표

| 메트릭 | 목표 | 측정 방법 |
|--------|------|-----------|
| **프레임레이트** | 60 FPS 안정 | 1분 주행 중 평균 |
| **메모리 사용량** | < 512 MB | 힙 메모리 최대값 |
| **시작 시간** | < 3초 | 런처 실행 → 메인 메뉴 |
| **네트워크 지연** | 200ms 이하 플레이 가능 | 4인 레이스 테스트 |
| **데이터베이스 쿼리** | < 50ms | 리더보드 조회 |

---

## 🧪 품질 보증

### 테스트 전략
- **단위 테스트**: 핵심 로직 70% 커버리지
- **통합 테스트**: 물리 엔진, 네트워크 동기화
- **성능 테스트**: 60 FPS 유지 확인
- **수동 테스트**: 플레이테스터 피드백

### 테스트 자동화
```bash
# 모든 테스트 실행
./gradlew test

# 커버리지 리포트
./gradlew jacocoTestReport

# CI/CD 자동 실행 (GitHub Actions)
```

---

## 🚀 배포 전략

### 빌드 아티팩트
```bash
# 범용 JAR
./gradlew lwjgl3:dist
# 출력: lwjgl3/build/libs/f1-racing-1.0.0.jar

# 플랫폼별 최적화 JAR
./gradlew lwjgl3:jarWin    # Windows
./gradlew lwjgl3:jarMac    # macOS
./gradlew lwjgl3:jarLinux  # Linux
```

### 배포 채널
- **GitHub Releases**: 소스 코드 + JAR 파일
- **Itch.io**: 인디 게임 플랫폼
- **Steam**: (옵션, Phase 6+)

---

## 👥 팀 역할

| 역할 | 책임 영역 | 필수 스킬 |
|------|-----------|-----------|
| **Physics Lead** | Box2D, 차량 물리 | Java, 물리 엔진 |
| **UI Lead** | Scene2D, HUD, 메뉴 | Java, UI 디자인 |
| **Network Lead** | KryoNet, 동기화 | Java, 네트워크 |
| **Database Lead** | SQLite, DAO | Java, SQL |
| **Assets Lead** | 그래픽, 사운드, 맵 | Tiled, 이미지 편집 |
| **QA Lead** | 테스팅, 버그 추적 | 게임 테스팅 |

---

## 📈 성공 지표

### Phase별 완료 기준
- **Phase 1**: 차량이 트랙을 주행할 수 있음
- **Phase 2**: 타임 어택 모드 완전 플레이 가능
- **Phase 3**: 피트 스톱 전략이 게임에 영향
- **Phase 4**: 4인 온라인 레이스 안정적
- **Phase 5**: 크리티컬 버그 0개, 배포 준비 완료

### 최종 목표
- [ ] 플레이 가능한 데모 (2개 트랙, 3대 차량)
- [ ] 온라인 멀티플레이어 작동
- [ ] 리더보드 및 전적 시스템
- [ ] 60 FPS 안정적 동작
- [ ] 3개 플랫폼 배포 (Win/Mac/Linux)

---

## 📚 문서 체계

### 필수 문서 (현재)
1. ✅ **PROJECT-OVERVIEW.md** (이 문서) - 전체 개요
2. ✅ **PHASES.md** - 개발 단계별 계획
3. ✅ **AGENTS.md** - AI 개발 가이드
4. ✅ **VEHICLE-PHYSICS.md** - 차량 물리 스펙
5. ✅ **DATABASE-SCHEMA.md** - DB 스키마
6. ✅ **HUD-SPECIFICATION.md** - HUD 명세

### 추가 필요 문서
- [ ] **CAMERA-SYSTEM.md** - 카메라 추적 로직
- [ ] **TIRE-SYSTEM.md** - 타이어 시스템
- [ ] **PITSTOP-MINIGAME.md** - 피트 스톱 메카닉
- [ ] **MULTIPLAYER-SYNC.md** - 네트워크 동기화
- [ ] **LOBBY-SYSTEM.md** - 로비 시스템
- [ ] **TRACK-DESIGN.md** - Tiled 맵 가이드
- [ ] **MENU-SYSTEM.md** - 메뉴 화면 흐름

---

## 🔗 유용한 링크

### 공식 문서
- [libGDX Wiki](https://libgdx.com/wiki/)
- [Box2D Manual](https://box2d.org/documentation/)
- [KryoNet GitHub](https://github.com/EsotericSoftware/kryonet)
- [Tiled Documentation](https://doc.mapeditor.org/)

### 커뮤니티
- [libGDX Discord](https://discord.gg/6pgDK9F)
- [r/libgdx](https://reddit.com/r/libgdx)
- [Box2D Forums](https://box2d.org/posts/)

### 학습 자료
- [libGDX Tutorial Series](https://libgdx.com/wiki/start/tutorials)
- [Box2D Physics for Games](https://www.iforce2d.net/b2dtut/)
- [Game Programming Patterns](https://gameprogrammingpatterns.com/)

---

## 🎯 다음 단계

### 즉시 시작 가능
1. **저장소 클론 및 초기화**
   ```bash
   git clone https://github.com/yourname/f1-racing-game
   cd f1-racing-game
   ./gradlew init
   ```

2. **문서 배치**
    - 이 문서들을 `docs/` 폴더에 복사
    - 팀원들과 리뷰

3. **Phase 0 시작**
    - `docs/PHASES.md` 참조
    - 개발 환경 설정
    - 첫 번째 스프린트 계획

4. **첫 커밋**
   ```bash
   git add .
   git commit -m "Initial project setup"
   git push origin main
   ```

---

## 📝 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|-----------|
| 2025-01-15 | 1.0.0 | 초기 프로젝트 개요 작성 |

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2025-01-15  
**관리자**: Development Team  
**상태**: Living Document (프로젝트 진행에 따라 업데이트)
