# PITSTOP-MINIGAME.md

## Overview
피트 레인 진입 시 발동되는 타이밍 기반 미니게임 명세서입니다. 플레이어의 순발력에 따라 정비 시간이 달라져 레이스 결과에 영향을 줍니다.

**Owner**: Gameplay Lead  
**Status**: Draft  
**Last Updated**: 2025-01-15  
**Related Specs**: `TIRE-SYSTEM.md`, `DAMAGE-MODEL.md`, `HUD-SPECIFICATION.md`

---

## 1. Pit Stop Flow (피트 스톱 흐름)

```
차량 주행 → 피트 레인 진입 감지 → 차량 감속/정지
    ↓
미니게임 시작 (타이밍 바)
    ↓
플레이어 입력 (SPACE BAR)
    ↓
결과 판정 (Perfect / Good / Bad)
    ↓
타이어 선택 UI 표시
    ↓
정비 시간 대기 (애니메이션)
    ↓
차량 복귀 → 피트 레인 출구
```

---

## 2. Pit Lane Detection (피트 레인 감지)

### 2.1 Tiled 맵 설정

```xml
<!-- track.tmx 에서 Object Layer 추가 -->
<objectgroup name="PitStops">
  <object id="1" name="PitEntry" x="1024" y="512" width="128" height="64">
    <properties>
      <property name="type" value="pit_entry"/>
    </properties>
  </object>
  
  <object id="2" name="PitZone" x="1152" y="512" width="256" height="64">
    <properties>
      <property name="type" value="pit_zone"/>
    </properties>
  </object>
  
  <object id="3" name="PitExit" x="1408" y="512" width="128" height="64">
    <properties>
      <property name="type" value="pit_exit"/>
    </properties>
  </object>
</objectgroup>
```

### 2.2 Box2D 센서 생성

```java
public class TrackLoader {
    private void createPitLaneSensors(MapObject object) {
        RectangleMapObject rectObject = (RectangleMapObject) object;
        Rectangle rect = rectObject.getRectangle();
        
        // Box2D Body 생성 (Static, Sensor)
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(
            (rect.x + rect.width / 2) / PPM,
            (rect.y + rect.height / 2) / PPM
        );
        
        Body sensorBody = world.createBody(bodyDef);
        
        // Sensor Fixture
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(rect.width / 2 / PPM, rect.height / 2 / PPM);
        
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.isSensor = true; // 충돌하지 않음
        
        Fixture fixture = sensorBody.createFixture(fixtureDef);
        fixture.setUserData(object.getProperties().get("type")); // "pit_entry", "pit_zone", etc.
        
        shape.dispose();
    }
}
```

### 2.3 충돌 감지

```java
public class PitStopContactListener implements ContactListener {
    private PitStopManager pitStopManager;
    
    @Override
    public void beginContact(Contact contact) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();
        
        // 차량이 피트 존에 진입했는지 확인
        if (isPitZoneContact(fixtureA, fixtureB)) {
            Body vehicleBody = getVehicleBody(fixtureA, fixtureB);
            String zoneType = (String) getPitZoneFixture(fixtureA, fixtureB).getUserData();
            
            if ("pit_zone".equals(zoneType)) {
                pitStopManager.triggerPitStop(vehicleBody);
            }
        }
    }
}
```

---

## 3. Minigame Mechanics (미니게임 메카닉)

### 3.1 Timing Bar System

```
┌────────────────────────────────────────────┐
│                                            │
│   ┌──────────────────────────────────┐    │
│   │░░░░░░░░█████░░░░░░░░░░░░░░░░░░░░│    │  ← 타이밍 바 (배경)
│   └──────────────────────────────────┘    │
│              ↑                             │
│            커서 (이동 중)                    │
│                                            │
│   ┌─BAD──┬─GOOD─┬PERFECT┬─GOOD─┬─BAD──┐  │  ← 구역 표시
│                                            │
│        Press SPACE to stop!                │
└────────────────────────────────────────────┘
```

#### 구역 정의
```java
public class TimingBar {
    private static final float TOTAL_WIDTH = 800f;
    private static final float PERFECT_WIDTH = 80f;   // 10% (중앙)
    private static final float GOOD_WIDTH = 200f;     // 25% (양쪽)
    private static final float BAD_WIDTH = 520f;      // 65% (나머지)
    
    // 구역 경계 (0.0 ~ 1.0)
    private static final float PERFECT_START = 0.45f;
    private static final float PERFECT_END = 0.55f;
    private static final float GOOD_START = 0.35f;
    private static final float GOOD_END = 0.65f;
    
    private float cursorPosition = 0f;  // 0.0 (왼쪽) ~ 1.0 (오른쪽)
    private float cursorSpeed = 0.8f;   // 단위/초
    private boolean movingRight = true;
    
    public void update(float delta) {
        if (movingRight) {
            cursorPosition += cursorSpeed * delta;
            if (cursorPosition >= 1.0f) {
                cursorPosition = 1.0f;
                movingRight = false;
            }
        } else {
            cursorPosition -= cursorSpeed * delta;
            if (cursorPosition <= 0.0f) {
                cursorPosition = 0.0f;
                movingRight = true;
            }
        }
    }
    
    public PitStopResult checkInput() {
        if (cursorPosition >= PERFECT_START && cursorPosition <= PERFECT_END) {
            return PitStopResult.PERFECT;
        } else if (cursorPosition >= GOOD_START && cursorPosition <= GOOD_END) {
            return PitStopResult.GOOD;
        } else {
            return PitStopResult.BAD;
        }
    }
}
```

### 3.2 결과 판정

```java
public enum PitStopResult {
    PERFECT(3.0f, Color.GOLD, "PERFECT!"),    // 3초
    GOOD(5.0f, Color.GREEN, "GOOD"),          // 5초
    BAD(8.0f, Color.RED, "BAD");              // 8초
    
    public final float stopTime;
    public final Color feedbackColor;
    public final String feedbackText;
    
    PitStopResult(float time, Color color, String text) {
        this.stopTime = time;
        this.feedbackColor = color;
        this.feedbackText = text;
    }
}
```

---

## 4. Tire Selection UI (타이어 선택 UI)

### 4.1 레이아웃

```
┌────────────────────────────────────────┐
│      SELECT NEW TIRES                  │
│                                        │
│  ┌────────┐  ┌────────┐  ┌────────┐  │
│  │  SOFT  │  │ MEDIUM │  │  HARD  │  │
│  │  [●]   │  │  [●]   │  │  [●]   │  │
│  │ +10%   │  │  +5%   │  │  Base  │  │
│  │ 30 sec │  │ 60 sec │  │ 100sec │  │
│  └────────┘  └────────┘  └────────┘  │
│     [1]         [2]         [3]       │
│                                        │
│  Current: MEDIUM (Worn: 20%)          │
└────────────────────────────────────────┘
```

### 4.2 구현

```java
public class TireSelectionDialog extends Dialog {
    private TireType selectedTire;
    private TireManager tireManager;
    
    public TireSelectionDialog(Skin skin, TireManager tireManager) {
        super("SELECT NEW TIRES", skin);
        this.tireManager = tireManager;
        
        // 타이어 선택 버튼
        TextButton softButton = createTireButton(TireType.SOFT, "1");
        TextButton mediumButton = createTireButton(TireType.MEDIUM, "2");
        TextButton hardButton = createTireButton(TireType.HARD, "3");
        
        // 레이아웃
        getContentTable().row();
        getContentTable().add(softButton).pad(10);
        getContentTable().add(mediumButton).pad(10);
        getContentTable().add(hardButton).pad(10);
        
        // 현재 타이어 정보
        Label currentTireLabel = new Label(
            String.format("Current: %s (Condition: %.0f%%)", 
                tireManager.getCurrentTireType().name(),
                tireManager.getCondition() * 100),
            skin
        );
        getContentTable().row();
        getContentTable().add(currentTireLabel).colspan(3).padTop(20);
        
        // 키보드 단축키
        addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                switch(keycode) {
                    case Input.Keys.NUM_1: selectTire(TireType.SOFT); return true;
                    case Input.Keys.NUM_2: selectTire(TireType.MEDIUM); return true;
                    case Input.Keys.NUM_3: selectTire(TireType.HARD); return true;
                }
                return false;
            }
        });
    }
    
    private TextButton createTireButton(TireType type, String hotkey) {
        String text = String.format("%s\n%s\n+%.0f%%\n%ds",
            type.name(),
            "[" + hotkey + "]",
            type.speedBonus * 100,
            (int)type.durabilitySeconds
        );
        
        TextButton button = new TextButton(text, getSkin());
        button.getLabel().setColor(type.displayColor);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectTire(type);
            }
        });
        
        return button;
    }
    
    private void selectTire(TireType type) {
        selectedTire = type;
        hide(); // 다이얼로그 닫기
    }
    
    public TireType getSelectedTire() {
        return selectedTire;
    }
}
```

---

## 5. Pit Stop Sequence (피트 스톱 시퀀스)

### 5.1 전체 흐름

```java
public class PitStopManager {
    private enum PitStopState {
        IDLE,
        ENTERING,
        MINIGAME,
        TIRE_SELECTION,
        SERVICING,
        EXITING
    }
    
    private PitStopState currentState = PitStopState.IDLE;
    private float stateTimer = 0f;
    private PitStopResult minigameResult;
    
    public void update(float delta) {
        switch(currentState) {
            case ENTERING:
                handleEntering(delta);
                break;
            case MINIGAME:
                handleMinigame(delta);
                break;
            case TIRE_SELECTION:
                handleTireSelection(delta);
                break;
            case SERVICING:
                handleServicing(delta);
                break;
            case EXITING:
                handleExiting(delta);
                break;
        }
    }
    
    public void triggerPitStop(Body vehicleBody) {
        if (currentState != PitStopState.IDLE) return;
        
        // 차량 정지
        vehicleBody.setLinearVelocity(0, 0);
        vehicleBody.setAngularVelocity(0);
        
        // 미니게임 시작
        currentState = PitStopState.MINIGAME;
        showMinigame();
    }
    
    private void handleMinigame(float delta) {
        timingBar.update(delta);
        
        // 플레이어 입력 감지
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            minigameResult = timingBar.checkInput();
            showResultFeedback(minigameResult);
            
            // 타이어 선택으로 전환
            currentState = PitStopState.TIRE_SELECTION;
            showTireSelectionDialog();
        }
    }
    
    private void handleTireSelection(float delta) {
        // 다이얼로그에서 타이어 선택 대기
        if (tireSelectionDialog.isHidden()) {
            TireType selectedTire = tireSelectionDialog.getSelectedTire();
            tireManager.changeTire(selectedTire);
            
            // 정비 시작
            currentState = PitStopState.SERVICING;
            stateTimer = minigameResult.stopTime;
            startServiceAnimation();
        }
    }
    
    private void handleServicing(float delta) {
        stateTimer -= delta;
        
        // 정비 진행률 표시
        float progress = 1.0f - (stateTimer / minigameResult.stopTime);
        updateServiceProgress(progress);
        
        if (stateTimer <= 0) {
            // 차량 수리 완료
            damageManager.fullyRepair();
            
            // 피트 레인 출구로 이동
            currentState = PitStopState.EXITING;
            releaseVehicle();
        }
    }
    
    private void handleExiting(float delta) {
        // 차량이 피트 레인 출구를 벗어나면 완료
        if (!isInPitLane(vehicleBody)) {
            currentState = PitStopState.IDLE;
        }
    }
}
```

---

## 6. Visual Feedback (시각적 피드백)

### 6.1 타이밍 바 렌더링

```java
public class TimingBarRenderer {
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    
    private static final float BAR_X = 560f;
    private static final float BAR_Y = 400f;
    private static final float BAR_WIDTH = 800f;
    private static final float BAR_HEIGHT = 80f;
    
    public void render(TimingBar timingBar) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // 배경 (어두운 회색)
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.9f);
        shapeRenderer.rect(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT);
        
        // BAD 구역 (빨강)
        shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 0.5f);
        shapeRenderer.rect(BAR_X, BAR_Y, BAR_WIDTH * 0.35f, BAR_HEIGHT);
        shapeRenderer.rect(BAR_X + BAR_WIDTH * 0.65f, BAR_Y, BAR_WIDTH * 0.35f, BAR_HEIGHT);
        
        // GOOD 구역 (노랑)
        shapeRenderer.setColor(0.8f, 0.8f, 0.2f, 0.5f);
        shapeRenderer.rect(BAR_X + BAR_WIDTH * 0.35f, BAR_Y, BAR_WIDTH * 0.1f, BAR_HEIGHT);
        shapeRenderer.rect(BAR_X + BAR_WIDTH * 0.55f, BAR_Y, BAR_WIDTH * 0.1f, BAR_HEIGHT);
        
        // PERFECT 구역 (초록)
        shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 0.5f);
        shapeRenderer.rect(BAR_X + BAR_WIDTH * 0.45f, BAR_Y, BAR_WIDTH * 0.1f, BAR_HEIGHT);
        
        // 커서 (흰색, 이동 중)
        float cursorX = BAR_X + BAR_WIDTH * timingBar.getCursorPosition();
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(cursorX - 5, BAR_Y - 10, 10, BAR_HEIGHT + 20);
        
        shapeRenderer.end();
        
        // 텍스트
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Press SPACE to stop!", BAR_X + 250, BAR_Y + 150);
        batch.end();
    }
    
    public void renderResult(PitStopResult result) {
        batch.begin();
        
        BitmapFont largeFont = new BitmapFont();
        largeFont.getData().setScale(3.0f);
        largeFont.setColor(result.feedbackColor);
        
        GlyphLayout layout = new GlyphLayout(largeFont, result.feedbackText);
        float x = (Gdx.graphics.getWidth() - layout.width) / 2;
        float y = Gdx.graphics.getHeight() / 2;
        
        largeFont.draw(batch, result.feedbackText, x, y);
        
        // 정비 시간 표시
        font.setColor(Color.WHITE);
        String timeText = String.format("Pit Stop Time: %.1f seconds", result.stopTime);
        font.draw(batch, timeText, x, y - 80);
        
        batch.end();
    }
}
```

### 6.2 정비 애니메이션

```java
public class PitCrewAnimation {
    private Array<TextureRegion> frames;
    private Animation<TextureRegion> animation;
    private float stateTime = 0f;
    
    public PitCrewAnimation() {
        // 타이어 교체 애니메이션 프레임 로드
        Texture spriteSheet = new Texture("pitstop/tire_change_anim.png");
        TextureRegion[][] regions = TextureRegion.split(spriteSheet, 128, 128);
        
        frames = new Array<>();
        for (int i = 0; i < 8; i++) {
            frames.add(regions[0][i]);
        }
        
        animation = new Animation<>(0.1f, frames, Animation.PlayMode.LOOP);
    }
    
    public void render(SpriteBatch batch, float delta, float x, float y) {
        stateTime += delta;
        TextureRegion currentFrame = animation.getKeyFrame(stateTime);
        batch.draw(currentFrame, x, y);
    }
}
```

---

## 7. Audio Integration (오디오 통합)

```java
public class PitStopAudioManager {
    private Sound pitEntrySound;
    private Sound wrenchSound;
    private Sound airGunSound;
    private Sound perfectSound;
    private Sound goodSound;
    private Sound badSound;
    
    public void load(AssetManager assetManager) {
        pitEntrySound = assetManager.get("sounds/effects/pit_entry.ogg");
        wrenchSound = assetManager.get("sounds/effects/pit_wrench.ogg");
        airGunSound = assetManager.get("sounds/effects/air_gun.ogg");
        perfectSound = assetManager.get("sounds/ui/perfect.ogg");
        goodSound = assetManager.get("sounds/ui/good.ogg");
        badSound = assetManager.get("sounds/ui/bad.ogg");
    }
    
    public void playPitEntry() {
        pitEntrySound.play(0.6f);
    }
    
    public void playResult(PitStopResult result) {
        switch(result) {
            case PERFECT: perfectSound.play(0.8f); break;
            case GOOD: goodSound.play(0.7f); break;
            case BAD: badSound.play(0.6f); break;
        }
    }
    
    public void playServicing() {
        long wrenchId = wrenchSound.loop(0.5f);
        long airGunId = airGunSound.loop(0.6f);
        
        // 정비 완료 시 중지
        // wrenchSound.stop(wrenchId);
        // airGunSound.stop(airGunId);
    }
}
```

---

## 8. Multiplayer Considerations (멀티플레이어 고려사항)

### 8.1 동기화 패킷

```java
public class PitStopPacket {
    public int playerId;
    public PitStopState state;
    public PitStopResult result;
    public TireType selectedTire;
    public float remainingTime;
}
```

### 8.2 서버 권위 모델

```
Client → [Enter Pit] → Server
Server → Validate → Broadcast to all clients
Clients → Show opponent in pit lane

Client → [Minigame Result] → Server
Server → Calculate stop time → Broadcast
Clients → Update opponent status

Server → [Pit Stop Complete] → All Clients
Clients → Release opponent vehicle
```

---

## 9. Testing & Balance

### 9.1 난이도 조정

```java
// 커서 속도 (쉬움/보통/어려움)
private static final float CURSOR_SPEED_EASY = 0.6f;    // 1.67초 왕복
private static final float CURSOR_SPEED_NORMAL = 0.8f;  // 1.25초 왕복
private static final float CURSOR_SPEED_HARD = 1.0f;    // 1.0초 왕복

// 구역 크기 (쉬움/보통/어려움)
private static final float PERFECT_WIDTH_EASY = 120f;   // 15%
private static final float PERFECT_WIDTH_NORMAL = 80f;  // 10%
private static final float PERFECT_WIDTH_HARD = 40f;    // 5%
```

### 9.2 성공률 목표

- **Perfect**: 20% (숙련된 플레이어)
- **Good**: 60% (대부분의 플레이어)
- **Bad**: 20% (초보자 또는 실수)

### 9.3 테스트 케이스

```java
@Test
@DisplayName("Perfect zone should be in center 10% of bar")
public void testPerfectZoneSize() {
    TimingBar bar = new TimingBar();
    
    // 중앙 위치에서 Perfect 판정
    bar.setCursorPosition(0.5f);
    assertThat(bar.checkInput()).isEqualTo(PitStopResult.PERFECT);
    
    // 경계에서 Good 판정
    bar.setCursorPosition(0.44f);
    assertThat(bar.checkInput()).isEqualTo(PitStopResult.GOOD);
    
    bar.setCursorPosition(0.56f);
    assertThat(bar.checkInput()).isEqualTo(PitStopResult.GOOD);
}

@Test
@DisplayName("Pit stop should add correct time to race timer")
public void testPitStopTime() {
    RaceTimer raceTimer = new RaceTimer();
    PitStopManager pitStop = new PitStopManager();
    
    float timeBeforePit = raceTimer.getCurrentTime();
    pitStop.completePitStop(PitStopResult.PERFECT); // 3초
    
    assertThat(raceTimer.getCurrentTime())
        .isCloseTo(timeBeforePit + 3.0f, within(0.1f));
}
```

---

**Version**: 1.0.0  
**Status**: Ready for Implementation  
**Next Review**: After Phase 3 completion
