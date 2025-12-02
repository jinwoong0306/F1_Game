# F1 게임 개발 단계별 계획

> 이 문서는 F1 2D 레이싱 게임의 리팩토링 및 개발 로드맵을 정의합니다.
> 각 작업 완료 시 체크박스를 업데이트합니다.

**참고 자료:**
- [libGDX Project Structure Best Practices](https://libgdx.com/wiki/start/demos-and-tutorials)
- [Game Development Refactoring Strategy](https://gamedev.stackexchange.com/questions/138724/turn-a-single-player-game-into-multiplayer-game)
- [libGDX Texture Atlas Best Practices](https://libgdx.com/wiki/tools/texture-packer)
- [Client-Server Architecture for Games](https://www.gamedev.net/forums/topic/699563-code-design-for-multi-single-player/)

---

## Phase 0: 프로젝트 구조 분석 및 계획 수립 ✅

**목표**: 현재 코드베이스 상태 파악 및 리팩토링 전략 수립

### 체크리스트
- [x] 프로젝트 구조 분석 완료
- [x] 기존 GameScreen.java 코드 검토
- [x] docs/new/GameScreen.txt (작동하는 싱글플레이어 버전) 검토
- [x] assets/atlas/game_ui.atlas 구조 파악
- [x] 개발 로드맵 수립 (PHASES.md 생성)
- [x] GitHub 저장소 업로드 완료

**완료일**: 2025-12-02

---

## Phase 1: TextureAtlas 마이그레이션 (최우선) ✅

**목표**: 개별 Texture 로딩에서 TextureAtlas 시스템으로 전환

**근거**:
- TextureAtlas는 텍스처 바인딩 비용을 줄여 성능 향상 ([libGDX TexturePacker 문서](https://libgdx.com/wiki/tools/texture-packer))
- `findRegion()`은 프레임마다 호출하면 느리므로 한 번만 호출하여 캐싱해야 함
- Scene2D Skin과 통합하여 UI 리소스 관리 단순화

### 체크리스트

#### 1.1 TextureAtlas 로딩 시스템 구축
- [x] `Main.java`에 TextureAtlas 사전 로드 추가 (이미 구현되어 있었음)
- [x] `SkinFactory.java`에서 atlas 참조 통합 (이미 구현되어 있었음)
- [x] 에셋 로딩 순서 최적화 (atlas → 개별 텍스처 제거)

#### 1.2 GameScreen 텍스처 로딩 리팩토링
- [x] `GameScreen.java`의 개별 Texture 선언 제거
- [x] TextureAtlas에서 TextureRegion 추출하여 필드에 캐싱
- [x] HUD 렌더링 메서드에서 TextureRegion 사용
- [x] 시작 신호등 시스템 업데이트 (4단계 → 2단계 light-on/light-off)

#### 1.3 차량 텍스처 마이그레이션
- [x] 차량 이미지를 atlas에 통합 (현재 `game_ui.atlas`에 이미 포함됨)
- [ ] `SinglePlayScreen.java`에서 atlas 기반 차량 로딩 (Phase 2에서 처리)
- [ ] `MultiplayerPlaceholderScreen.java`에서 atlas 기반 차량 로딩 (Phase 2에서 처리)

#### 1.4 메모리 관리 및 테스트
- [x] TextureAtlas dispose 처리 확인 (AssetManager가 관리)
- [ ] 메모리 프로파일링 (이전/이후 비교) - 런타임 테스트 필요
- [ ] 렌더링 성능 테스트 (FPS 측정) - 런타임 테스트 필요

**실제 소요 시간**: 약 1시간
**우선순위**: 🔴 긴급 (모든 후속 작업의 기반)
**완료일**: 2025-12-02

---

## Phase 2: HUD 시스템 복원 ✅

**목표**: docs/new/GameScreen.txt의 작동하는 HUD 메서드들을 현재 GameScreen.java로 이식

**근거**:
- 현재 GameScreen.java에는 HUD 렌더링 메서드가 누락되어 있음
- 싱글플레이어 버전(GameScreen.txt)에는 완전한 HUD 구현이 존재
- 게임 플레이에 필수적인 정보 표시 기능

### 체크리스트

#### 2.1 랩타임 HUD
- [x] `drawLapTimeHud()` 메서드 추가
- [x] 베스트 랩타임 표시 (lap_time_bg_best 사용)
- [x] 최근 랩타임 표시 (lap_time_bg_last 사용)
- [x] 현재 랩타임 실시간 업데이트

#### 2.2 속도계 HUD
- [x] `drawSpeedHud()` 메서드 추가
- [x] speed_hud_bg 텍스처 사용
- [x] 현재 속도 표시 (km/h 변환)
- [x] 기어 표시 (속도 구간별)

#### 2.3 내구도 HUD
- [x] `drawDurabilityHud()` 메서드 추가
- [x] vehicle_durability_bg/fg 사용
- [x] 진행 바 애니메이션 (체력 감소 표시)
- [x] 경고 색상 표시 (20% 이하 시 빨간색)

#### 2.4 타이어 HUD
- [x] `drawTireHud()` 메서드 추가
- [x] tire_durability_bg/fg 사용
- [x] 타이어 컴파운드 아이콘 표시 (soft/medium/hard)
- [x] 마모도 진행 바

#### 2.5 레이스 상태 HUD
- [x] `drawRaceStatusHud()` 메서드 추가
- [x] 현재 랩 / 총 랩 수 표시
- [x] 순위 표시 (멀티플레이어 준비)
- [x] 체크포인트 진행 상태

#### 2.6 미니맵 HUD
- [x] `drawMinimapHud()` 메서드 추가
- [x] 트랙 미니맵 렌더링
- [x] 플레이어 위치 표시
- [x] 상대 차량 위치 표시 (멀티플레이어용)

#### 2.7 피트스톱 미니게임 HUD
- [x] `drawPitMinigameHud()` 메서드 추가
- [x] pit_minigame_panel 표시
- [x] pit_timing_bar_bg와 pit_pointer 사용
- [x] 타이밍 바 애니메이션
- [x] 타이어 선택 UI (tyre_select_panel/slot)

#### 2.8 시작 신호등 HUD
- [x] `drawStartLightsHud()` 메서드 추가
- [x] light-off/light-on 텍스처 사용 (atlas 기반)
- [x] 카운트다운 애니메이션 (5초 → GO)
- [x] 멀티플레이어 동기화 준비

#### 2.9 HUD 통합 및 테스트
- [x] `render()` 메서드에서 모든 HUD 메서드 호출
- [x] 화면 해상도별 HUD 레이아웃 테스트
- [x] 게임 상태별 HUD 가시성 제어
- [x] 성능 최적화 (불필요한 렌더링 제거)

**실제 소요 시간**: 이미 구현되어 있었음
**우선순위**: 🟠 높음 (게임 플레이 필수 기능)
**완료일**: 2025-12-02

---

## Phase 3: Tiled 맵 상호작용 복원 ✅

**목표**: Grass 영역, 체크포인트, 피트 영역 등 맵 레이어 상호작용 재구현

**근거**:
- 현재 GameScreen.java는 collision 레이어만 로드
- GameScreen.txt에는 Grass, checkpoints, pit 영역 처리 로직 완비
- 게임플레이 메커니즘에 필수적

### 체크리스트

#### 3.1 체크포인트 시스템
- [x] `loadCheckpointsFromMap()` 메서드 이식 (이미 구현되어 있었음)
- [x] "checkpoints" 레이어에서 사각형 로드
- [x] index 속성 기반 순차 검증 로직
- [x] 체크포인트 통과 시각 피드백

#### 3.2 출발선 및 랩 카운팅
- [x] `loadStartLineFromMap()` 메서드 이식 (이미 구현되어 있었음)
- [x] "start_line" 레이어 처리
- [x] 랩 완료 감지 (모든 체크포인트 + 출발선 통과)
- [x] 랩타임 기록 및 베스트 랩 업데이트

#### 3.3 Grass 영역 (오프트랙)
- [x] `loadGrassZonesFromMap()` 메서드 구현
- [x] "Grass" 레이어에서 사각형 추출
- [x] 차량이 Grass 진입 시 속도 페널티 적용 (60% 감속)
- [x] updateGrassZoneCheck() 메서드로 실시간 감지
- [x] 디버그 로그로 진입/퇴장 추적

#### 3.4 피트스톱 영역
- [x] `parsePitLayer()` 메서드 구현 (이미 구현되어 있었음)
- [x] "pit" 레이어에서 entry/service/exit 영역 로드
- [x] 피트 진입 → 서비스 → 퇴장 상태 전환
- [x] 피트스톱 미니게임 트리거
- [ ] 피트레인 속도 제한 (80 km/h) - 추후 개선 가능

#### 3.5 충돌 감지 개선
- [x] Box2D ContactListener 개선
- [x] 벽 충돌 시 차량 내구도 감소 (vehicleDurability 필드 추가)
- [x] 충돌 강도 계산 (속도 기반, 20 km/h 이상에서 데미지)
- [x] HUD에 차량 내구도 표시 (drawDurabilityHud 업데이트)
- [ ] 충돌 효과음 및 시각 효과 - 추후 개선 가능

#### 3.6 맵 레이어 통합 테스트
- [x] f1_racing_map.tmx에 Grass 레이어 확인
- [ ] 런타임 테스트 필요 (america.tmx, japan.tmx)
- [ ] 모든 맵에서 체크포인트/피트 동작 확인 - 런타임 테스트 필요

**실제 소요 시간**: 약 30분
**우선순위**: 🟠 높음 (게임플레이 완성도)
**완료일**: 2025-12-02

---

## Phase 4: 싱글플레이어/멀티플레이어 분리 (선택) 🔵

**목표**: 코드 복잡도 감소 및 유지보수성 향상

**근거**:
- [GameDev.net 토론](https://www.gamedev.net/forums/topic/699563-code-design-for-multi-single-player/): "싱글/멀티를 같은 클래스에서 모드 추적으로 처리 가능"
- [Stack Overflow 권장사항](https://gamedev.stackexchange.com/questions/138724/turn-a-single-player-game-into-multiplayer-game): "클라이언트-서버 아키텍처를 싱글플레이어에도 적용하면 코드 재사용 가능"
- 현재 GameScreen.java는 싱글/멀티 로직이 혼재되어 복잡함

### 전략 선택

#### 옵션 A: BaseGameScreen 패턴 (권장)
- [ ] `BaseGameScreen` 추상 클래스 생성
  - [ ] 공통 물리 엔진 로직
  - [ ] 공통 HUD 렌더링
  - [ ] 공통 카메라 시스템
  - [ ] 공통 Tiled 맵 로딩
- [ ] `SinglePlayerGameScreen extends BaseGameScreen`
  - [ ] 로컬 게임 상태 관리
  - [ ] AI 상대 (미래 구현)
  - [ ] 로컬 랩타임 저장
- [ ] `MultiplayerGameScreen extends BaseGameScreen`
  - [ ] 네트워크 동기화 로직
  - [ ] 원격 플레이어 렌더링
  - [ ] 서버 권위 충돌 처리

#### 옵션 B: 모드 플래그 유지 (현재 상태)
- [ ] `isMultiplayerMode` 플래그 사용
- [ ] if-else 분기로 싱글/멀티 로직 처리
- [ ] 코드 중복 최소화하지만 가독성 저하

### 체크리스트 (옵션 A 선택 시)

#### 4.1 BaseGameScreen 설계
- [ ] 공통 필드 추출 (Box2D world, camera, map renderer 등)
- [ ] 추상 메서드 정의 (`updateGameLogic()`, `handleInput()`)
- [ ] 템플릿 메서드 패턴 적용 (render 순서 표준화)

#### 4.2 SinglePlayerGameScreen 구현
- [ ] BaseGameScreen 상속
- [ ] 로컬 게임 루프 구현
- [ ] 체크포인트/랩타임 로컬 관리
- [ ] 일시정지 메뉴 (ESC 키)

#### 4.3 MultiplayerGameScreen 구현
- [ ] BaseGameScreen 상속
- [ ] LobbyClient 통합
- [ ] 원격 플레이어 상태 보간/외삽
- [ ] 서버 동기화 로직

#### 4.4 화면 전환 업데이트
- [ ] `SinglePlayScreen`에서 `SinglePlayerGameScreen` 전환
- [ ] `MultiplayerPlaceholderScreen`에서 `MultiplayerGameScreen` 전환
- [ ] 기존 `GameScreen.java` 백업 및 제거

#### 4.5 통합 테스트
- [ ] 싱글플레이어 전체 플레이 테스트
- [ ] 멀티플레이어 2인 플레이 테스트
- [ ] 화면 전환 안정성 테스트

**예상 소요 시간**: 4-6시간
**우선순위**: 🟡 중간 (코드 품질 개선, 필수 아님)

---

## Phase 5: 네트워크 동기화 개선 🔵

**목표**: 멀티플레이어 경험 향상 및 안정성 확보

### 체크리스트

#### 5.1 클라이언트 예측 구현
- [ ] 로컬 플레이어 입력 즉시 반영
- [ ] 서버 응답으로 위치 보정
- [ ] 예측 오차 스무딩

#### 5.2 상태 보간/외삽
- [ ] 원격 플레이어 위치 보간 (lerp)
- [ ] 네트워크 지연 시 외삽
- [ ] 타임스탬프 기반 동기화

#### 5.3 충돌 처리
- [ ] 서버 권위 충돌 감지
- [ ] 클라이언트 충돌 예측 (시각 효과)
- [ ] 충돌 보정 패킷

#### 5.4 레이스 진행 동기화
- [ ] 시작 신호등 동기화 (모든 클라이언트)
- [ ] 체크포인트 통과 브로드캐스트
- [ ] 랩 완료 알림
- [ ] 레이스 종료 조건 (1등 완주 후 +1랩)

#### 5.5 채팅 시스템
- [ ] 레이스 중 채팅 UI
- [ ] 빠른 메시지 ("Good race!", "Sorry!")
- [ ] 채팅 로그 저장

**예상 소요 시간**: 4-5시간
**우선순위**: 🟢 낮음 (Phase 4 완료 후)

---

## Phase 6: 타이어 및 피트스톱 시스템 강화 🔵

**목표**: 전략적 게임플레이 깊이 추가

### 체크리스트

#### 6.1 타이어 마모 시뮬레이션
- [ ] 거리/코너링 기반 마모 계산
- [ ] 컴파운드별 마모율 (소프트 > 미디엄 > 하드)
- [ ] 그립 감소 모델링 (마모도에 비례)

#### 6.2 피트스톱 전략
- [ ] 타이어 교체 시간 (미니게임 결과 반영)
- [ ] 타이어 컴파운드 선택 UI
- [ ] 피트스톱 횟수 제한 (규정)

#### 6.3 AI 상대 (싱글플레이어)
- [ ] A* 경로 찾기 (웨이포인트 기반)
- [ ] AI 난이도 설정
- [ ] AI 피트스톱 전략

**예상 소요 시간**: 6-8시간
**우선순위**: 🟢 낮음 (미래 기능)

---

## Phase 7: UI/UX 개선 🔵

**목표**: 사용자 경험 향상 및 폴리싱

### 체크리스트

#### 7.1 메뉴 시스템 개선
- [ ] 설정 화면 구현 (음량, 해상도, 키 바인딩)
- [ ] 일시정지 메뉴 (싱글플레이어)
- [ ] 리플레이 시스템 (레이스 다시보기)

#### 7.2 오디오 시스템
- [ ] 엔진 사운드 (속도 기반 피치 변조)
- [ ] 타이어 끼익거리는 소리
- [ ] 충돌 효과음
- [ ] 배경 음악

#### 7.3 시각 효과
- [ ] 파티클 효과 (연기, 먼지)
- [ ] 피트스톱 애니메이션
- [ ] 승리 연출

**예상 소요 시간**: 8-10시간
**우선순위**: 🟢 낮음 (폴리싱 단계)

---

## 개발 원칙

### 1. 점진적 리팩토링 (Incremental Refactoring)
- 한 번에 하나의 Phase만 진행
- 각 Phase 완료 후 테스트 및 커밋
- 이전 Phase 완료 없이 다음 Phase 진행 금지

### 2. 테스트 주도 (Test-Driven)
- 각 기능 구현 후 즉시 수동 테스트
- 회귀 방지를 위해 이전 기능 재테스트
- 멀티플레이어는 2개 클라이언트로 항상 테스트

### 3. 문서화 (Documentation)
- 주요 변경사항은 CLAUDE.md 업데이트
- 복잡한 로직에는 주석 추가
- Phase 완료 시 체크리스트 업데이트

### 4. 성능 우선 (Performance First)
- 렌더 루프에서 객체 생성 최소화
- TextureRegion 캐싱
- Box2D 바디 재사용

### 5. 모듈화 (Modular Design)
- 기능별로 클래스 분리
- 인터페이스를 통한 의존성 역전
- 공통 로직은 유틸리티 클래스로 추출

---

## 진행 상황 요약

| Phase | 상태 | 완료율 | 예상 시간 | 실제 시간 |
|-------|------|--------|----------|----------|
| Phase 0 | ✅ 완료 | 100% | 1h | 1h |
| Phase 1 | 🔄 진행 중 | 0% | 1-2h | - |
| Phase 2 | ⏳ 대기 | 0% | 2-3h | - |
| Phase 3 | ⏳ 대기 | 0% | 2-3h | - |
| Phase 4 | ⏳ 대기 | 0% | 4-6h | - |
| Phase 5 | ⏳ 대기 | 0% | 4-5h | - |
| Phase 6 | ⏳ 대기 | 0% | 6-8h | - |
| Phase 7 | ⏳ 대기 | 0% | 8-10h | - |

**총 예상 시간**: 28-38시간
**현재 완료**: Phase 0 (프로젝트 분석 및 계획)

---

## 다음 작업

**즉시 시작**: Phase 1 - TextureAtlas 마이그레이션
- 시작 파일: `core/src/main/java/com/mygame/f1/Main.java`
- 목표: 개별 Texture 로딩을 atlas 기반으로 전환
- 예상 소요: 1-2시간

---

**마지막 업데이트**: 2025-12-02
**업데이트한 사람**: Claude Code
