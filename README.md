# F1 게임 프로젝트 (기초 게임엔진 개발)

LibGDX와 Box2D를 사용하여 개발 중인 2D 탑다운 레이싱 게임입니다.

## 주요 기능 (Features)

### 1. 어드밴스드 차량 물리 모델
- **파라미터 기반 핸들링:** `최고 속도`, `가속력`, `그립`, `회전력` 등의 변수를 통해 차량의 주행 특성을 쉽게 튜닝할 수 있습니다.
- **횡방향 마찰력 (타이어 그립):** 차량이 옆으로 미끄러지는 것을 억제하고, 조향 시에는 마찰력을 조절하여 부드러운 코너링이 가능합니다.
- **공기 저항:** 속도에 비례하여 공기 저항이 적용되어 자연스러운 감속 및 최고 속도 유지가 구현됩니다.
- **부드러운 제동:** `linearDamping`을 이용해 급정지나 후진 없이 부드럽게 감속하여 멈추는 브레이크 시스템을 갖추고 있습니다.

### 2. 최적화된 렌더링 및 에셋 관리
- **AssetManager 도입:** 게임 시작 시 모든 이미지 에셋을 미리 불러와, 게임 중 발생할 수 있는 버벅임(Stuttering)을 원천 차단합니다.
- **가비지 최소화(GC-Friendly):** 게임 루프 내에서 불필요한 객체 생성을 방지하고 재사용 변수를 활용하여 '가비지 컬렉션'으로 인한 성능 저하를 최소화합니다.

### 3. 동적 카메라 시스템
- **추적 카메라:** 자동차를 따라다니는 '오버 더 숄더' 스타일의 카메라를 구현했습니다.
- **부드러운 움직임:** 카메라의 위치와 회전 모두에 `lerp`(선형 보간)를 적용하여, 물리 엔진의 미세한 떨림에도 불구하고 안정적이고 부드러운 화면을 제공합니다.
- **시야 확보:** 카메라가 자동차보다 항상 약간 앞을 비추도록 하여, 플레이어가 다가오는 트랙을 더 쉽게 인지할 수 있습니다.

### 4. 물리 및 충돌 시스템
- **Box2D 기반:** LibGDX의 Box2D 물리 엔진을 사용합니다.
- **충돌 감지 및 효과:** `ContactListener`를 통해 벽과의 충돌을 감지하고, 충돌 시 속도를 줄이는 등 페널티를 적용합니다. 벽에 끼이는 현상을 방지하는 로직도 포함됩니다.
- **충돌 경계:** Tiled 맵 에디터로 제작된 보이지 않는 트랙 경계선과 화면 가장자리 경계선을 모두 사용합니다.

## 조작법
- **가속:** `UP` (↑)
- **조향:** `LEFT` (←) / `RIGHT` (→)
- **브레이크:** `SPACE`

# 🏎️ F1 2D Racing Game

> **libGDX 기반 2D 탑다운 F1 레이싱 게임 with 전략적 타이어 관리 & 온라인 멀티플레이어**

[![Build Status](https://img.shields.io/github/workflow/status/yourname/f1-racing-game/Build)](https://github.com/yourname/f1-racing-game/actions)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![libGDX](https://img.shields.io/badge/libGDX-1.13.1-red.svg)](https://libgdx.com/)

---

## 📖 목차

1. [프로젝트 소개](#프로젝트-소개)
2. [핵심 기능](#핵심-기능)
3. [빠른 시작](#빠른-시작)
4. [조작법](#조작법)
5. [문서 구조](#문서-구조)
6. [개발 가이드](#개발-가이드)
7. [기여하기](#기여하기)
8. [라이선스](#라이선스)

---

## 프로젝트 소개

**F1 2D Racing Game**은 실제 Formula 1의 전략적 요소(타이어 관리, 피트 스톱)와 박진감 넘치는 레이싱을 2D 탑다운 뷰로 구현한 게임입니다.

### 🎯 핵심 컨셉
- **현실적인 물리 엔진**: Box2D 기반으로 실제 차량의 가속, 드리프트, 충돌을 시뮬레이션
- **전략적 깊이**: 3종 타이어 선택, 마모 관리, 타이밍 기반 피트 스톱
- **온라인 멀티플레이어**: 최대 4인 실시간 레이스 with KryoNet
- **몰입형 카메라**: 차량을 따라 회전하는 트래킹 카메라

### 🖼️ 스크린샷
_(개발 완료 후 추가 예정)_

---

## 핵심 기능

### 🏁 게임 모드
- **싱글플레이어 타임 어택**: 개인 최고 기록 도전 및 리더보드 등록
- **멀티플레이어**: 2-4인 온라인 대전 with 로비 시스템
- **연습 모드**: 자유 주행 및 트랙 숙지

### 🔧 전략 시스템
| 타이어 | 성능 보너스 | 내구도 | 최적 상황 |
|--------|------------|--------|-----------|
| **Soft (빨강)** | 속도 +10%, 그립 +15% | 30초 | 짧은 스틴트, 오버테이킹 |
| **Medium (노랑)** | 속도 +5%, 그립 +5% | 60초 | 균형잡힌 전략 |
| **Hard (흰색)** | 기본 성능 | 100초 | 긴 스틴트, 원스톱 |

- **타이어 마모**: 실시간 성능 저하, 속도/조향 입력에 비례
- **차량 내구도**: 충돌/오프로드 시 손상, 성능 페널티
- **피트 스톱 미니게임**: 타이밍 바 정확도에 따라 정비 시간 결정 (3~8초)

### 🎮 물리 & 제어
- **Box2D 물리 엔진**: 현실적인 마찰, 드리프트, 충돌 반응
- **트래킹 카메라**: 차량 중심 회전 뷰, 부드러운 추적
- **입력 스무딩**: 키보드/게임패드 지원, 자연스러운 조작감

### 🌐 멀티플레이어
- **클라이언트-서버 아키텍처**: 서버 권위 모델로 치트 방지
- **클라이언트 예측**: 입력 즉시 반응, 지연 없는 플레이
- **보간/외삽**: 200ms 지연 환경에서도 부드러운 동기화

---

## 빠른 시작

### 시스템 요구사항
- **OS**: Windows 10+, macOS 10.15+, Linux (Ubuntu 20.04+)
- **Java**: JDK 17 이상
- **메모리**: 최소 4GB RAM
- **그래픽**: OpenGL 2.0 지원

### 설치 방법

#### 1. 사전 빌드 다운로드 (추천)
```bash
# GitHub Releases에서 최신 버전 다운로드
https://github.com/yourname/f1-racing-game/releases

# JAR 파일 실행
java -jar f1-racing-1.0.0.jar
```

#### 2. 소스에서 빌드
```bash
# 저장소 클론
git clone https://github.com/yourname/f1-racing-game.git
cd f1-racing-game

# 빌드 및 실행 (Gradle)
./gradlew lwjgl3:run

# 배포용 JAR 생성
./gradlew lwjgl3:dist
# 출력: lwjgl3/build/libs/f1-racing-1.0.0.jar
```

### 첫 실행
1. 게임 실행 시 자동으로 `data/game.db` 데이터베이스 생성
2. 로그인 화면에서 "Guest" 버튼 클릭 또는 계정 생성
3. 메인 메뉴 → **Single Player** → 트랙 선택 → 레이스 시작!

---

## 조작법

### 키보드
| 키 | 동작 |
|----|------|
| **W / ↑** | 가속 |
| **S / ↓** | 후진 |
| **A / ←** | 좌회전 |
| **D / →** | 우회전 |
| **SPACE** | 브레이크 |
| **P** | 피트 레인 진입 |
| **ESC** | 일시정지 / 메뉴 |
| **Tab** | 리더보드 표시 |

### 피트 스톱 미니게임
- 피트 레인 진입 후 **SPACE** 키로 타이밍 바 멈추기
- **1/2/3** 키로 타이어 선택 (Soft/Medium/Hard)

### 게임패드 (Xbox 컨트롤러 기준)
| 버튼 | 동작 |
|------|------|
| **RT** | 가속 |
| **LT** | 브레이크 |
| **Left Stick** | 조향 |
| **A** | 확인 / 피트인 |
| **Start** | 일시정지 |

---

## 문서 구조

프로젝트의 모든 문서는 `docs/` 폴더에 정리되어 있습니다.

### 📚 핵심 문서
| 문서 | 설명 | 독자 |
|------|------|------|
| **[PROJECT-OVERVIEW.md](docs/PROJECT-OVERVIEW.md)** | 프로젝트 전체 개요 | 모두 |
| **[PHASES.md](docs/PHASES.md)** | 10주 개발 로드맵 | 개발자 |
| **[AGENTS.md](docs/AGENTS.md)** | AI 개발 가이드 (Codex) | 개발자 |
| **[USER_GUIDE.md](docs/USER_GUIDE.md)** | 사용자 가이드 | 플레이어 |

### 🔧 기술 스펙
| 문서 | 내용 |
|------|------|
| **[VEHICLE-PHYSICS.md](docs/specs/gameplay/VEHICLE-PHYSICS.md)** | 차량 물리 엔진 |
| **[TIRE-SYSTEM.md](docs/specs/gameplay/TIRE-SYSTEM.md)** | 타이어 마모 시스템 |
| **[PITSTOP-MINIGAME.md](docs/specs/gameplay/PITSTOP-MINIGAME.md)** | 피트 스톱 메카닉 |
| **[DAMAGE-MODEL.md](docs/specs/gameplay/DAMAGE-MODEL.md)** | 차량 내구도 |
| **[HUD-SPECIFICATION.md](docs/specs/ui/HUD-SPECIFICATION.md)** | HUD 시스템 |
| **[MULTIPLAYER-SYNC.md](docs/specs/network/MULTIPLAYER-SYNC.md)** | 네트워크 동기화 |
| **[DATABASE-SCHEMA.md](docs/specs/data/DATABASE-SCHEMA.md)** | DB 스키마 |
| **[CAMERA-SYSTEM.md](docs/specs/core/CAMERA-SYSTEM.md)** | 카메라 시스템 |
| **[TRACK-DESIGN.md](docs/specs/assets/TRACK-DESIGN.md)** | Tiled 맵 가이드 |

### 📐 아키텍처
- **[SYSTEM-OVERVIEW.md](docs/architecture/SYSTEM-OVERVIEW.md)**: 전체 시스템 다이어그램
- **[CLASS-DIAGRAM.md](docs/architecture/CLASS-DIAGRAM.md)**: UML 클래스 다이어그램

---

## 개발 가이드

### 🛠️ 개발 환경 설정

#### 필수 도구
- **JDK 17+**: [다운로드](https://adoptium.net/)
- **Gradle 8.5+**: (Gradle Wrapper 포함, 별도 설치 불필요)
- **IDE**: IntelliJ IDEA (권장) 또는 VS Code
- **Git**: 버전 관리

#### IDE 설정
```bash
# IntelliJ IDEA
./gradlew idea       # IDEA 프로젝트 파일 생성
# 그 후 File → Open → build.gradle 선택

# VS Code (with Extension Pack for Java)
code .
```

### 📦 프로젝트 구조
```
f1-racing-game/
├── core/           # 게임 로직 (플랫폼 독립적)
├── lwjgl3/         # 데스크톱 런처
├── assets/         # 게임 에셋 (스프라이트, 사운드, 맵)
├── docs/           # 문서
├── data/           # 런타임 데이터 (DB)
└── migrations/     # DB 마이그레이션 스크립트
```

### 🧪 테스트 실행
```bash
# 모든 테스트
./gradlew test

# 특정 테스트
./gradlew test --tests "VehiclePhysicsTest"

# 커버리지 리포트
./gradlew jacocoTestReport
# 결과: build/reports/jacoco/test/html/index.html
```

### 🏗️ 빌드 명령어
```bash
# 개발 빌드 (빠른 실행)
./gradlew lwjgl3:run

# 프로덕션 JAR
./gradlew lwjgl3:dist

# 플랫폼별 JAR
./gradlew lwjgl3:jarWin    # Windows
./gradlew lwjgl3:jarMac    # macOS
./gradlew lwjgl3:jarLinux  # Linux

# 전체 빌드 + 테스트
./gradlew clean build
```

### 🐛 디버깅
```bash
# Box2D 메모리 디버깅
./gradlew lwjgl3:run -Dbox2d.debugMemory=true

# 상세 로그
./gradlew lwjgl3:run --info

# 프로파일링 (VisualVM)
jvisualvm
```

---

## 기여하기

### 🤝 기여 절차
1. **Fork** 이 저장소
2. **Feature Branch** 생성 (`git checkout -b feature/AmazingFeature`)
3. **Commit** 변경사항 (`git commit -m 'Add some AmazingFeature'`)
4. **Push** to Branch (`git push origin feature/AmazingFeature`)
5. **Pull Request** 생성

### 📝 코딩 컨벤션
- Java 코드: [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 커밋 메시지: [Conventional Commits](https://www.conventionalcommits.org/)
- 브랜치 네이밍: `feature/`, `bugfix/`, `hotfix/`, `docs/`

### 🧪 PR 체크리스트
- [ ] 모든 테스트 통과 (`./gradlew test`)
- [ ] 코드 스타일 준수
- [ ] 관련 문서 업데이트
- [ ] 스크린샷 추가 (UI 변경 시)
- [ ] 변경사항이 `CHANGELOG.md`에 기록됨

### 🐞 버그 리포트
GitHub Issues를 사용하여 버그를 리포트해주세요:
- **제목**: `[BUG] 간단한 설명`
- **내용**:
    - 재현 단계
    - 예상 동작
    - 실제 동작
    - 스크린샷/로그
    - 환경 정보 (OS, Java 버전)

---
