# F1 2D Racing Game - 최종 발표 PPT 구성안 (Codex 핸드오버 중심)

1. 인트로 / 제목
   - 프로젝트명, 버전, 발표자, 날짜
   - 한 줄 미션: "물리 기반 전략 F1 레이스를 2D 톱다운으로"

2. 비전 & 목표
   - 핵심 가치: 현실적 물리, 전략 깊이, 멀티플레이 긴장감
   - 성공 지표: 60fps, 4인 멀티, 타임어택 반복 플레이

3. 플레이 경험 Pillars
   - 실사 기반 주행감(Box2D, 횡방향 마찰, 공기 저항)
   - 전략 의사결정(타이어 선택/마모, 피트 스톱 미니게임)
   - 경쟁성(리더보드, 멀티플레이 로비)

4. 기술 스택 & 모듈
   - LibGDX 1.13.1 + Box2D, Scene2D UI
   - 네트워크: KryoNet 2.24.0 (계획)
   - 데이터: SQLite 3.45+ (계획)
   - 모듈: core(게임 로직), lwjgl3(런처); 에셋 AssetManager 프리로드

5. 현재 빌드 스냅샷 (코드 기준)
   - 차량 물리: 파라미터 기반 최고속/가속/그립/회전력, 횡방향 마찰, 공기 저항, 부드러운 제동
   - 카메라: 차량 추적, 위치/회전 lerp, 전방 오프셋
   - 렌더링/자산: AssetManager 선로딩, GC 최소화, Tiled 맵 렌더러
   - 충돌: Box2D ContactListener로 벽 충돌 감지 및 감속, 트랙/화면 경계 사용
   - 입력: UP/LEFT/RIGHT/SPACE

6. 시스템 상태 요약
   - 핵심 주행 루프, 카메라, 충돌: 구현됨
   - HUD/타임어택 루프: Phase 2 목표(예정)
   - 타이어/데미지/피트 미니게임: Phase 3 목표(예정)
   - 멀티플레이: Phase 4 목표(계획 단계)

7. 전략 시스템 설계 (예정 기능)
   - 타이어: Soft/Medium/Hard (속도/그립 보너스 vs 내구도)
   - 마모/데미지: 주행, 충돌, 오프로드 기반 성능 저하
   - 피트 스톱 미니게임: 타이밍 바 정확도 -> 정비 시간

8. 멀티플레이 로드맵
   - 서버 권위 모델, 클라이언트 예측 + 보간/외삽 (200ms 지연 대응)
   - 로비/매치메이킹, 2-4인 레이스, 부정 행위 방지
   - 네트워크 동기화 스펙: docs/specs/network/MULTIPLAYER-SYNC.md 참조(준비 예정)

9. 아키텍처 & 데이터 흐름
   - 입력 -> 물리(World/Body) -> 카메라 -> HUD/미니맵 -> 렌더
   - 에셋 파이프라인: assets/ + generateAssetList -> AssetManager -> 스프라이트/HUD
   - 빌드 경로: `./gradlew lwjgl3:run` (개발) / `lwjgl3:dist` (배포)

10. 로드맵 & 마일스톤 (PHASES.md 기반)
    - Phase 0: 세팅/스펙 (완료 또는 대부분 완료 가정)
    - Phase 1: 코어 파운데이션 (현 코드가 충돌/카메라/물리 구현으로 커버)
    - Phase 2: 싱글플레이 타임어택/HUD
    - Phase 3: 전략 시스템(타이어/데미지/피트)
    - Phase 4: 멀티플레이
    - Phase 5: 폴리싱/릴리스

11. 리스크 & 대응
    - 물리 튜닝 난이도 -> Box2D 상수/파라미터 표준화, 테스트 트랙 스냅샷
    - 성능/GC -> 오브젝트 풀, 프로파일링(VisualVM), 자산 프리로드
    - 멀티플레이 지연/치트 -> 서버 권위+리플레이 검증, 입력 지터 버퍼
    - 스펙-코드 불일치 -> 변경 시 docs/specs 동기화 + 미니 ADR 남기기
    - QA 공백 -> junit5/AssertJ 테스트 도입, 주행 리그 스크립트

12. Codex 협업 & 핸드오버 베스트 프랙티스
    - Ship/Show/Ask (Martin Fowler, 2025): 변경마다 Ship(즉시 병합)/Show(PR 후 즉시 병합)/Ask(리뷰 대기)로 라벨링해 대화 흐름 유지
    - 의사결정 로그: ADR 포맷(Michael Nygard, 2011; adr.github.io)으로 `docs/adr/NNN-title.md`에 맥락/결정/결과를 짧게 기록
    - 일일 컨텍스트 메모: 목표/진행/다음 행동/위험/링크 5줄을 `docs/new/daily-handoff.md`(제안)에 append -> 새 대화 시작 시 Codex에 붙여넣기
    - 스펙 싱크: 기능 변경 시 해당 `docs/specs/**`와 AGENTS 지침을 동시 갱신, 실행 명령(`gradlew ...`)과 테스트 결과를 PR/메모에 함께 남기기
    - 대화 스타터 템플릿: {오늘 목표, 완료/미완료, 블로커, 최신 ADR/PR 링크, 실행 명령}을 기본 프롬프트로 사용

13. 데모 & 발표 진행
    - 30~60초 플레이 영상/스크린샷(카메라 추적, 충돌 시 감속)을 포함
    - 빌드/실행 데모: `./gradlew lwjgl3:run`, 주요 키 입력 안내
    - Q&A 준비: 물리 파라미터, 멀티플레이 동기화, 타이어 밸런스 질문 대비

14. 부록/참고
    - 소스: Martin Fowler, "Ship / Show / Ask", 2025 (martinfowler.com)
    - 소스: Michael Nygard, "Documenting Architecture Decisions", 2011; adr.github.io (2025 업데이트)
    - 주요 문서: README.md, docs/PHASES.md, docs/AGENTS.md, docs/new/GameScreen.txt
