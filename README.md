# 🏎️ F1 레이싱 게임

> libGDX 기반 2D 탑다운 F1 레이싱 게임 with 전략적 타이어 관리 & 멀티플레이

[![Java](https://img.shields.io/badge/java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![libGDX](https://img.shields.io/badge/libGDX-1.13.1-red.svg)](https://libgdx.com/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

---

## 📖 목차

1. [프로젝트 소개](#프로젝트-소개)
2. [주요 기능](#주요-기능)
3. [빠른 시작](#빠른-시작)
4. [조작법](#조작법)
5. [개발 가이드](#개발-가이드)
6. [프로젝트 문서](#프로젝트-문서)

---

## 프로젝트 소개

**F1 레이싱 게임**은 실제 Formula 1의 전략적 요소(타이어 관리, 피트 스톱)와 박진감 넘치는 레이싱을 2D 탑다운 뷰로 구현한 게임입니다.

### 🎯 핵심 컨셉
- **현실적인 물리 엔진**: Box2D 기반으로 실제 차량의 가속, 드리프트, 충돌 시뮬레이션
- **전략적 깊이**: 3종 타이어 선택, 마모 관리, 타이밍 기반 피트 스톱
- **온라인 멀티플레이**: 최대 4인 실시간 레이스 (KryoNet)
- **몰입형 카메라**: 차량을 따라 회전하는 트래킹 카메라

---

## 주요 기능

### 🏁 게임 모드
- ✅ **싱글플레이 타임 어택**: 개인 최고 기록 도전
- 🚧 **멀티플레이**: 2-4인 온라인 대전 (개발 중)
- ✅ **연습 모드**: 자유 주행 및 트랙 숙지

### 🔧 전략 시스템
| 타이어 | 성능 보너스 | 내구도 | 최적 상황 |
|--------|------------|--------|-----------|
| **Soft (빨강)** | 속도 +10%, 그립 +15% | 30초 | 짧은 스틴트 |
| **Medium (노랑)** | 속도 +5%, 그립 +5% | 60초 | 균형 전략 |
| **Hard (흰색)** | 기본 성능 | 100초 | 긴 스틴트 |

- **타이어 마모**: 실시간 성능 저하
- **차량 내구도**: 충돌/오프로드 시 손상
- **피트 스톱 미니게임**: 타이밍 바 정확도로 정비 시간 결정

### 🎮 물리 & 제어
- **Box2D 물리 엔진**: 현실적인 마찰, 드리프트
- **트래킹 카메라**: 차량 중심 회전 뷰
- **체크포인트 시스템**: 10개 체크포인트 순차 검증
- **Grass 영역**: 오프트랙 속도 페널티 (30% 감속)

### 🖥️ HUD 시스템
- 속도계 + 기어 표시
- 랩타임 (베스트/최근/현재)
- 타이어 마모도 + 컴파운드
- 차량 내구도
- 미니맵

---

## 빠른 시작

### 시스템 요구사항
- **OS**: Windows 10+, macOS 10.15+, Linux (Ubuntu 20.04+)
- **Java**: JDK 17 이상
- **메모리**: 최소 4GB RAM
- **그래픽**: OpenGL 2.0 지원

### 설치 및 실행

```bash
# 저장소 클론
git clone https://github.com/H0GUN3/F1_Game.git
cd F1_Game

# 빌드 및 실행 (Gradle)
./gradlew lwjgl3:run

# 서버 실행 (멀티플레이용)
./gradlew server:run
```

### 첫 실행
1. 게임 실행 시 자동으로 데이터베이스 생성
2. 로그인 화면에서 "Guest" 또는 계정 생성
3. 메인 메뉴 → **Single Play** → 트랙 선택 → 레이스 시작!

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

### 피트 스톱
- 피트 레인 진입 후 **SPACE** 키로 타이밍 바 멈추기
- **1/2/3** 키로 타이어 선택 (Soft/Medium/Hard)

---

## 개발 가이드

### 🛠️ 개발 환경 설정

#### 필수 도구
- **JDK 17+**: [다운로드](https://adoptium.net/)
- **Gradle 8.10+**: (Gradle Wrapper 포함)
- **IDE**: IntelliJ IDEA (권장) 또는 VS Code
- **Git**: 버전 관리

#### IDE 설정
```bash
# IntelliJ IDEA
# File → Open → F1_Game 폴더 선택
# Gradle 자동 import 대기

# VS Code
code .
```

### 📦 프로젝트 구조
```
F1_Game/
├── core/               # 게임 로직 (플랫폼 독립적)
│   └── src/main/java/com/mygame/f1/
│       ├── Main.java
│       ├── GameScreen.java
│       ├── screens/
│       ├── ui/
│       └── network/    # 멀티플레이 (개발 중)
├── lwjgl3/             # 데스크톱 런처
├── server/             # 게임 서버
├── shared/             # 공용 코드 (클라이언트/서버)
├── assets/             # 게임 에셋
│   ├── atlas/          # TextureAtlas
│   ├── cars/           # 차량 이미지
│   ├── fonts/          # 폰트
│   └── *.tmx           # Tiled 맵 파일
└── docs/               # 문서
```

### 🧪 빌드 & 테스트
```bash
# 개발 빌드 (빠른 실행)
./gradlew lwjgl3:run

# 서버 실행
./gradlew server:run

# 전체 빌드 + 테스트
./gradlew clean build

# 배포용 JAR 생성
./gradlew lwjgl3:dist
# 출력: lwjgl3/build/libs/F1_Game-1.0.0.jar
```

---

## 프로젝트 문서

### 📚 필독 문서
| 문서 | 설명 | 독자 |
|------|------|------|
| **[협업가이드.md](협업가이드.md)** | 개발 워크플로우, Git 전략 | 개발자 필독 |
| **[진행상황.md](진행상황.md)** | 현재 개발 상태, 주간 목표 | 모든 팀원 |
| **[PHASES.md](PHASES.md)** | Phase별 상세 로드맵 | 개발자 |

### 🔧 기술 스펙 (docs/ 폴더)
- **차량 물리**: `docs/specs/gameplay/VEHICLE-PHYSICS.md`
- **타이어 시스템**: `docs/specs/gameplay/TIRE-SYSTEM.md`
- **피트 스톱**: `docs/specs/gameplay/PITSTOP-MINIGAME.md`
- **HUD 시스템**: `docs/specs/ui/HUD-SPECIFICATION.md`
- **멀티플레이 동기화**: `docs/specs/network/MULTIPLAYER-SYNC.md`

---

## 개발 현황

### 전체 진행률
```
Phase 1: 기반 구축         ████████████████████ 100% ✅
Phase 2: 싱글플레이 완성   █████░░░░░░░░░░░░░░░  25% 🚧
Phase 3: 멀티플레이 기반   ██░░░░░░░░░░░░░░░░░░  10% 🚧
Phase 4: 멀티플레이 완성   ░░░░░░░░░░░░░░░░░░░░   0% 📅
```

### 최근 업데이트 (2025-12-03)
- ✅ 체크포인트 회전 객체 버그 수정
- ✅ LAP HUD 대문자 표시 (P1, P2, P3, P4)
- ✅ 싱글플레이 UI 개선 (차량 선택 화면)
- ✅ 협업 가이드 문서 작성
- 🚧 멀티플레이 네트워크 구조 설계 중

---

## 기여하기

### 🤝 기여 절차
1. **브랜치 확인**: [협업가이드.md](협업가이드.md) 참고
2. **Feature Branch** 생성
   - 싱글플레이: `feature/singleplay-polish`
   - 멀티플레이: `feature/multiplayer-core`
3. **커밋 메시지 규칙**:
   ```
   feat: 새로운 기능 추가
   fix: 버그 수정
   refactor: 리팩토링
   docs: 문서 수정
   ```
4. **Pull Request** 생성

### 📝 코딩 컨벤션
- Java 코드 스타일: camelCase 변수, PascalCase 클래스
- 주석: 한글 가능, 명확한 설명
- 자세한 내용은 [협업가이드.md](협업가이드.md) 참고

---

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

---

## 연락처

- **GitHub**: https://github.com/H0GUN3/F1_Game
- **Issues**: https://github.com/H0GUN3/F1_Game/issues
- **Pull Requests**: https://github.com/H0GUN3/F1_Game/pulls

---

**마지막 업데이트**: 2025-12-03
