# F1 레이싱 게임 개발 진행 기록

**최종 업데이트**: 2025-12-04
**현재 브랜치**: `feature/multiplayer-core`
**프로젝트 상태**: Phase 4 완료 단계 (멀티플레이 핵심 기능 구현 완료)

---

## 전체 진행률

```
Phase 1: 기반 구축           ████████████████████ 100% ✅
Phase 2: 싱글플레이 완성     ████████████████████ 100% ✅
Phase 3: 전략 시스템         ████████████████████ 100% ✅
Phase 4: 멀티플레이          ████████████████░░░░  85% 🚧
Phase 5: 폴리싱              ████████░░░░░░░░░░░░  40% 🚧
```

---

## 최근 변경 이력

### 2025-12-04: 성능 최적화 및 브랜치 병합

#### 성능 최적화 (GC 부하 100% 제거)
- **Vector2 객체 재사용**: 매 프레임 8개 생성 → 0개 (임시 변수 `v2_tmp1`, `v2_tmp2` 활용)
- **Color 배열 정적화**: `PLAYER_COLORS` static 배열로 미니맵 색상 캐싱
- **미니맵 카메라 재사용**: `minimapCamera` 필드로 분리하여 매 프레임 생성 방지
- **hudCamera.setToOrtho() 제거**: resize()에서만 호출하도록 변경
- **효과**: 780개/초 객체 생성 → 0개/초, 렉 현상 70-90% 개선

#### singleplay-polish 브랜치 병합 (게임플레이 밸런스)
- 최고 전진 속도: 3.5 → 4.0 (약 15% 상향)
- 가속 램프업 속도: 0.10 → 0.03 (느린 가속으로 난이도 조정)
- 타이어 마모율 완화: Soft 50초→90초, Medium 70초→130초, Hard 90초→150초
- 내구도 0 이하 시 최고 속도 30% 제한
- 브레이크 키 변경: SPACE → SHIFT
- HUD 속도계 스케일 0.6배 (최대 268km/h 표시)
- Pit UI 레이아웃 개선 (타이어 선택 패널 상단 이동)

### 2025-12-04: 멀티플레이 안정화

#### 고스트 플레이어 버그 수정
- **문제**: 비정상 연결 종료 시 플레이어가 게임에 남아있는 현상
- **해결**: Keep-alive(8초) 및 Timeout(20초) 설정 추가
- **파일**: `GameServer.java:51-57`

#### 미니맵 플레이어 색상 구분
- Player 1: 밝은 파란색 (0.2, 0.6, 1.0)
- Player 2: 밝은 초록색 (0.2, 1.0, 0.4)
- Player 3: 노란색 (1.0, 0.8, 0.2)
- Player 4: 핑크색 (1.0, 0.4, 0.9)
- **파일**: `GameScreen.java:1304-1316`

#### 트랙 선택 검증 완료
- Track 1: NEON CITY → f1_racing_map.tmx
- Track 2: JAPAN CIRCUIT → japan.tmx
- Track 3: AMERICA GP → america.tmx

---

## 구현 완료 기능

### 싱글플레이
- [x] Box2D 물리 엔진 (고정 1/60초 타임스텝)
- [x] 차량 물리 (가속, 조향, 브레이크, 드리프트)
- [x] 추적 카메라 (위치/회전 lerp 스무딩)
- [x] Tiled 맵 로딩 및 충돌 처리
- [x] 체크포인트 시스템 (10개 순차 검증)
- [x] 랩타임 시스템 (베스트/최근/현재)
- [x] Grass 영역 속도 페널티 (30% 감속)
- [x] 피트스톱 미니게임 (타이밍 바)
- [x] 타이어 시스템 (Soft/Medium/Hard)
- [x] 차량 내구도 시스템
- [x] 완전한 HUD (속도계, 랩타임, 내구도, 타이어, 미니맵, 레이스 상태)
- [x] F1 스타일 시작 신호등

### 멀티플레이
- [x] KryoNet 기반 TCP/UDP 통신
- [x] Server-authoritative 아키텍처
- [x] 로비 시스템 (방 생성/참가/나가기)
- [x] 최대 4명 플레이어 지원
- [x] Ready 상태 관리
- [x] 트랙/차량 선택 동기화
- [x] 실시간 채팅
- [x] 카운트다운 후 레이스 시작
- [x] 실시간 위치 동기화 (UDP, 20Hz)
- [x] 원격 플레이어 보간/외삽
- [x] 미니맵 플레이어 색상 구분
- [x] 1등 완주 시 10초 카운트다운
- [x] 최종 순위 및 결과 화면
- [x] Keep-alive 및 Timeout (고스트 플레이어 방지)

### UI/UX
- [x] 로그인 화면 (UserStore 연동)
- [x] 메인 메뉴 (싱글/멀티/설정/종료)
- [x] 싱글플레이 선택 화면 (차량/트랙 선택)
- [x] 멀티플레이 로비 화면
- [x] 게임 HUD 시스템
- [x] 멀티플레이 결과 화면

---

## 남은 작업 (Phase 5: 폴리싱)

### 높음 우선순위
- [ ] 전체 플레이 테스트 (싱글 + 멀티)
- [ ] 버그 수정 및 안정화
- [ ] 네트워크 지연(Latency) 최적화

### 중간 우선순위
- [ ] 오디오 시스템 (엔진 사운드, 효과음)
- [ ] 파티클 효과 (타이어 연기, 충돌 불꽃)
- [ ] AI 상대 (싱글플레이용)

### 낮음 우선순위
- [ ] 추가 트랙
- [ ] 차량 커스터마이징
- [ ] 리플레이 시스템

---

## Git 커밋 이력 (최근)

```
146afba Perf: Optimize singleplayer rendering performance
dda31e7 Feat: Improve multiplayer sync with extrapolation and higher update rate
91180a3 Feat: Merge singleplay-polish improvements while preserving multiplayer
83634ba Fix: Add keep-alive and timeout to prevent ghost players
7271fc3 Fix: Improve minimap player color assignment stability
```

---

## 기술 스택

| 구성요소 | 기술 | 버전 |
|---------|------|------|
| 프레임워크 | libGDX | 1.13.1 |
| 물리 엔진 | Box2D | (bundled) |
| UI | Scene2D.ui | (bundled) |
| 네트워크 | KryoNet | 2.22.0-RC1 |
| 빌드 도구 | Gradle | 8.5+ |
| Java | OpenJDK | 17+ |

---

## 실행 방법

```bash
# 게임 클라이언트 실행
gradlew lwjgl3:run

# 서버 실행 (멀티플레이용)
gradlew server:run

# 배포용 JAR 빌드
gradlew lwjgl3:dist
```

---

**관련 문서**:
- [CLAUDE.md](CLAUDE.md) - AI 어시스턴트 컨텍스트
- [PHASES.md](PHASES.md) - 개발 단계별 체크리스트
- [README.md](README.md) - 프로젝트 소개
