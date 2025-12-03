package com.mygame.f1.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygame.f1.Main;
import com.mygame.f1.GameScreen;
import com.mygame.f1.network.LobbyClient;
import com.mygame.f1.shared.Packets;
import com.mygame.f1.ui.SkinFactory;
import com.badlogic.gdx.utils.IntIntMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Cyberpunk-styled multiplayer lobby with garage panel and chat. */
public class MultiplayerPlaceholderScreen implements Screen {
    private final Main game;
    private Stage stage;
    private com.badlogic.gdx.scenes.scene2d.ui.Skin skin;

    private Label statusLabel;
    private Label playerHeader;
    private Table playersTable;
    private TextButton readyBtn;
    private TextButton startBtn;
    private Slider maxPlayersSlider;
    private Label maxPlayersValue;
    private CheckBox privateRoomToggle;

    private Image vehicleImage;
    private Label vehicleNameLabel;
    private Label vehicleDescLabel;
    private ProgressBar speedBar;
    private ProgressBar handlingBar;
    private ProgressBar durabilityBar;
    private Label speedValLabel;
    private Label handlingValLabel;
    private Label durabilityValLabel;
    private String selectedVehicleName = "";

    private int vehicleIndex = 0;
    private int trackIndex = 0;
    private final List<Table> trackWrappers = new ArrayList<>();
    private Table chatTable;
    private ScrollPane chatScroll;

    private LobbyClient client;
    private String currentRoomId;
    private String selectedRoomId;
    private int selfId = -1;
    private boolean isReady = false;
    private Packets.RoomState lastRoomState;
    private boolean creatingRoom = false;
    private boolean joiningRoom = false;

    private final List<VehicleOption> vehicles = new ArrayList<>();
    private final List<TrackOption> tracks = new ArrayList<>();
    private Thread connectThread;

    public MultiplayerPlaceholderScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        skin = Main.getSharedSkin();
        // FitViewport로 가상 해상도 고정(1920x1080)하여 UI 잘림 방지
        stage = new Stage(new FitViewport(1920, 1080));
        Gdx.input.setInputProcessor(stage);
        ensureHighlightBorder();

        seedVehicles();
        seedTracks();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(30);
        root.setBackground(skin.getDrawable("bg"));
        stage.addActor(root);

        // Top panel (no background, left aligned)
        ImageTextButton.ImageTextButtonStyle backStyle = new ImageTextButton.ImageTextButtonStyle(skin.get("menu-sub-icon", ImageTextButton.ImageTextButtonStyle.class));
        backStyle.imageUp = skin.getDrawable("icon-right-arrow");
        backStyle.imageOver = backStyle.imageUp;
        backStyle.imageDown = backStyle.imageUp;
        backStyle.imageChecked = backStyle.imageUp;
        backStyle.font = skin.getFont("kr-font");
        ImageTextButton backBtn = new ImageTextButton("", backStyle);
        backBtn.getLabel().setText("");
        backBtn.getImageCell().size(20, 20);
        backBtn.setSize(44, 44);

        Image multiIcon = new Image(skin, "icon-multi");
        multiIcon.setScaling(Scaling.fit);
        Label title = new Label("MULTIPLAYER ROOM", skin, "title");
        title.setColor(Color.valueOf("ff0000"));
        title.setEllipsis(true);
        title.setWrap(false);

        Table top = new Table();
        top.setBackground((Drawable) null); // 배경 제거
        top.add(backBtn).size(44,44).padRight(20).padLeft(18).left(); // 오른쪽으로 추가 이동 + 버튼-아이콘 간격 20px
        top.add(multiIcon).size(24,24).padRight(10).left(); // 아이콘-타이틀 간 간격 10px 유지
        top.add(title).left().expandX().fillX();
        root.add(top).expandX().fillX().left().padBottom(10).row();

        Table center = new Table();
        center.defaults().pad(6).top().left(); // 전체 여백을 줄여 밀도 높임
        root.add(center).expand().fill().row();

        // Left panel: players grid + actions + chat
        Table left = new Table();
        left.defaults().pad(8).left();
        playersTable = new Table();
        playersTable.defaults().pad(6).growX();
        playersTable.columnDefaults(0).growX();
        playersTable.columnDefaults(1).growX();
        playersTable.align(Align.topLeft);
        // players frame with border/background
        Table playersFrame = new Table();
        playersFrame.setBackground(skin.getDrawable("panel-dark-round"));
        playersFrame.defaults().pad(6).growX();
        Table playersHeader = new Table();
        Image playersIcon = new Image(skin, "icon-multi");
        playersIcon.setScaling(Scaling.fit);
        playersIcon.setColor(Color.WHITE);
        playersHeader.add(playersIcon).size(30, 30).padRight(8);
        playerHeader = new Label("PLAYERS (0/4)", skin, "title");
        playerHeader.setAlignment(Align.left);
        playersHeader.add(playerHeader).left();
        playersFrame.add(playersHeader).growX().row();
        playersFrame.add(playersTable).growX();
        left.add(playersFrame).growX().padBottom(8).top().row();

        // Ready / Start action cards
        Table actions = new Table();
        actions.setBackground(skin.getDrawable("panel-dark-round"));
        actions.defaults().pad(6).height(64).growX();
        readyBtn = new TextButton("READY", skin);
        startBtn = new TextButton("START", skin);
        actions.add(readyBtn).padRight(8);
        actions.add(startBtn);
        left.add(actions).growX().padBottom(8).top().row();

        // Chat system
        Table chatWrapper = new Table();
        chatWrapper.setBackground(skin.getDrawable("chat-border")); // players 프레임과 동일한 진한 회색
        chatWrapper.defaults().left().pad(4);

        // Title 붙이기
        Table chatHeader = new Table();
        Image chatIcon = new Image(skin, "icon-chat");
        chatIcon.setScaling(Scaling.fit);
        chatIcon.setColor(Color.WHITE);
        chatHeader.add(chatIcon).size(30, 30).padRight(6).top().left();
        chatHeader.add(new Label("CHAT", skin, "title")).left().top();
        chatWrapper.add(chatHeader).left().top().padBottom(4).row();

        // 로그 영역: 타이틀 바로 아래부터 입력창 직전까지 채우기
        this.chatTable = new Table(skin);
        chatTable.top().left();
        chatTable.defaults().pad(4).left(); // 줄 간격 확대
        this.chatScroll = new ScrollPane(chatTable, skin);
        chatScroll.setFadeScrollBars(false);
        // ScrollPane은 setBackground가 없으므로 스타일 복제 후 배경 지정
        ScrollPane.ScrollPaneStyle chatStyle = new ScrollPane.ScrollPaneStyle(chatScroll.getStyle());
        chatStyle.background = skin.getDrawable("chat-log-bg"); // 로그 배경 약간 밝은 검정
        chatScroll.setStyle(chatStyle);
        chatWrapper.add(chatScroll).grow().top().row();

        // 입력 영역: 한글 지원을 위해 Label + 네이티브 입력 사용
        final Label chatInputLabel = new Label(" 메세지를 입력하세요...", skin, "kr");
        chatInputLabel.setColor(0.6f, 0.6f, 0.6f, 1f);
        chatInputLabel.setEllipsis(true);
        chatInputLabel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled); // Label이 터치 이벤트 차단하지 않도록

        // 입력창 배경 스타일
        Table chatInputBox = new Table();
        chatInputBox.setBackground(skin.getDrawable("chat-input-bg"));
        chatInputBox.pad(8);
        chatInputBox.add(chatInputLabel).growX().left();
        chatInputBox.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled); // Table 클릭 활성화

        // 클릭 시 네이티브 입력 다이얼로그 표시
        chatInputBox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.input.getTextInput(new com.badlogic.gdx.Input.TextInputListener() {
                    @Override
                    public void input(String text) {
                        if (text != null && !text.trim().isEmpty()) {
                            String txt = text.trim();
                            chatInputLabel.setText(txt);
                            chatInputLabel.setColor(Color.WHITE);
                            if (client != null && currentRoomId != null) {
                                client.sendChat(currentRoomId, safeName(game.playerName), txt);
                            } else {
                                appendChat(chatTable, chatScroll, safeName(game.playerName), txt, System.currentTimeMillis());
                            }
                            // 입력 후 라벨 초기화
                            Gdx.app.postRunnable(() -> {
                                chatInputLabel.setText(" 메세지를 입력하세요...");
                                chatInputLabel.setColor(0.6f, 0.6f, 0.6f, 1f);
                            });
                        }
                    }
                    @Override
                    public void canceled() {}
                }, "채팅 메시지 입력", "", "메시지를 입력하세요");
            }
        });

        ImageTextButton sendBtn = new ImageTextButton("", skin, "menu-main-icon");
        sendBtn.getImage().setDrawable(skin.getDrawable("icon-send"));
        sendBtn.getImageCell().size(18, 18);
        sendBtn.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y){
                // Send 버튼도 동일한 네이티브 입력 다이얼로그 표시
                Gdx.input.getTextInput(new com.badlogic.gdx.Input.TextInputListener() {
                    @Override
                    public void input(String text) {
                        if (text != null && !text.trim().isEmpty()) {
                            String txt = text.trim();
                            if (client != null && currentRoomId != null) {
                                client.sendChat(currentRoomId, safeName(game.playerName), txt);
                            } else {
                                appendChat(chatTable, chatScroll, safeName(game.playerName), txt, System.currentTimeMillis());
                            }
                        }
                    }
                    @Override
                    public void canceled() {}
                }, "채팅 메시지 입력", "", "메시지를 입력하세요");
            }
        });

        // 전송 버튼: hover/focus #FF0000, 아이콘 send.png 그대로 사용
        ImageTextButton.ImageTextButtonStyle sendStyle = new ImageTextButton.ImageTextButtonStyle(skin.get("menu-sub-icon", ImageTextButton.ImageTextButtonStyle.class));
        sendStyle.up = skin.getDrawable("send-btn-up");
        sendStyle.over = skin.getDrawable("send-btn-over");   // hover: 짙은 빨강
        sendStyle.down = skin.getDrawable("send-btn-down");   // 눌림: 더 짙은 빨강
        sendStyle.checked = sendStyle.over;                   // 포커스 시에도 동일 효과
        sendStyle.checkedOver = sendStyle.over;
        sendStyle.imageUp = skin.getDrawable("icon-send");
        sendStyle.imageOver = sendStyle.imageUp;
        sendStyle.imageDown = sendStyle.imageUp;
        sendStyle.imageChecked = sendStyle.imageUp;
        sendBtn.setStyle(sendStyle);

        Table chatInputRow = new Table();
        chatInputRow.defaults().height(50);
        chatInputRow.add(chatInputBox).growX().growY().padRight(12);
        chatInputRow.add(sendBtn).width(56).growY();
        chatWrapper.add(chatInputRow).growX().height(50).top().row();

        // 채팅 전체를 위로 붙이기
        left.add(chatWrapper).grow().top().row();

        // Right panel: map + garage + start
        Table right = new Table();
        right.defaults().pad(6).left().top(); // 여백 소폭 축소
        right.setBackground(skin.getDrawable("panel-dark-round")); // 부모에 진한 회색 배경/테두리 적용

        // MAP 섹션 (부모에 배경/테두리, 내부 리스트는 배경 없음)
        Table mapTable = new Table();
        mapTable.defaults().left().top();
        mapTable.top().left();
        mapTable.setBackground(skin.getDrawable("panel-dark-round")); // 섹션 배경 #1A1A1A
        mapTable.pad(8);
        Table mapTitle = new Table();
        mapTitle.pad(4);
        Image mapIcon = new Image(skin, "icon-track");
        mapIcon.setScaling(Scaling.fit);
        mapIcon.setColor(Color.WHITE);
        mapTitle.add(mapIcon).size(30, 30).padRight(8);
        mapTitle.add(new Label("MAP", skin, "title")).left();
        mapTable.add(mapTitle).left().padBottom(4).row();

        Table mapListBox = new Table();
        mapListBox.defaults().pad(5).growX().top(); // 내부 여백 최소화
        mapListBox.top().left();
        Table trackList = new Table();
        trackList.defaults().padBottom(12).growX().top(); // 카드 간격 12px
        trackList.top().left();
        trackWrappers.clear();
        for (int i = 0; i < tracks.size(); i++) {
            final int idx = i;
            TrackOption t = tracks.get(i);
            Table card = new Table(skin);
            card.setBackground(skin.getDrawable("slot-bg-normal")); // 진한 검정 배경 (비선택)
            card.pad(10);
            card.align(Align.left);
            Label name = new Label(t.name, skin);
            name.setAlignment(Align.left);
            Label diff = new Label(t.difficulty, skin);
            diff.setAlignment(Align.left);
            Color diffColor = Color.WHITE;
            if ("EASY".equalsIgnoreCase(t.difficulty)) diffColor = new Color(0.1f,0.8f,0.3f,1f);
            else if ("NORMAL".equalsIgnoreCase(t.difficulty)) diffColor = new Color(0.2f,0.6f,1f,1f);
            else if ("HARD".equalsIgnoreCase(t.difficulty)) diffColor = new Color(0.95f,0.6f,0.2f,1f);
            diff.setColor(diffColor);
            card.add(name).left().row();
            diff.getColor().a = 0.8f;
            card.add(diff).left().padTop(4).row();

            Table wrapper = new Table();
            wrapper.add(card).growX();
            wrapper.defaults().growX();
            wrapper.top().left();
            trackWrappers.add(wrapper);

            wrapper.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    selectTrack(idx);
                }
            });

            trackList.add(wrapper).growX();
            trackList.row();
        }
        selectTrack(trackIndex);
        mapListBox.add(trackList).growX().top().row();
        mapTable.add(mapListBox).growX().top().row();

        // CARS 섹션 (부모에 배경/테두리, 제목 테두리 제거)
        Table carTable = new Table();
        carTable.defaults().left().top();
        carTable.top().left();
        carTable.setBackground(skin.getDrawable("panel-dark-round")); // 섹션 배경 #1A1A1A
        carTable.pad(8);
        Table carTitle = new Table();
        carTitle.pad(4);
        Image carsIcon = new Image(skin, "icon-car");
        carsIcon.setScaling(Scaling.fit);
        carsIcon.setColor(Color.WHITE);
        carTitle.add(carsIcon).size(35, 35).padRight(8);
        carTitle.add(new Label("CARS", skin, "title")).left();
        carTable.add(carTitle).left().padBottom(4).row();

        Table carsBox = new Table();
        carsBox.setBackground(skin.getDrawable("panel-dark-round")); // 내부만 어두운 배경
        carsBox.defaults().pad(5).growX().left().top(); // 내부 여백 최소화
        carsBox.top().left();
        carsBox.padTop(10).padBottom(10); // 상하 여백 10px 추가

        Table carCarousel = new Table();
        carCarousel.setBackground(skin.getDrawable("slot-bg-normal")); // 자동차 선택 배경을 진한 검정색으로
        TextButton prevCar = new TextButton("<", skin);
        TextButton nextCar = new TextButton(">", skin);
        vehicleImage = new Image();
        vehicleImage.setScaling(Scaling.fit);
        carCarousel.add(prevCar).width(44).height(44).padRight(6);
        carCarousel.add(vehicleImage).size(240, 160).expandX();
        carCarousel.add(nextCar).width(44).height(44).padLeft(6);
        carsBox.add(carCarousel).growX().top().row();

        vehicleNameLabel = new Label("", skin, "title");
        vehicleDescLabel = new Label("", skin);
        vehicleDescLabel.setWrap(true);
        carsBox.add(vehicleNameLabel).left().padTop(4).row();
        carsBox.add(vehicleDescLabel).growX().width(360).padBottom(8).row();

        Table stats = new Table();
        stats.setBackground(skin.getDrawable("panel-dark-round"));
        stats.defaults().pad(4).left();
        speedBar = new ProgressBar(0, 100, 1, false, skin, "stat-blue");
        handlingBar = new ProgressBar(0, 100, 1, false, skin, "stat-yellow");
        durabilityBar = new ProgressBar(0, 100, 1, false, skin, "stat-green");
        speedValLabel = new Label("", skin);
        handlingValLabel = new Label("", skin);
        durabilityValLabel = new Label("", skin);

        stats.add(new Label("SPEED", skin)).left();
        stats.add(speedBar).width(220).padRight(6);
        stats.add(speedValLabel).width(40).row();
        stats.add(new Label("HANDLING", skin)).left();
        stats.add(handlingBar).width(220).padRight(6);
        stats.add(handlingValLabel).width(40).row();
        stats.add(new Label("DURABILITY", skin)).left();
        stats.add(durabilityBar).width(220).padRight(6);
        stats.add(durabilityValLabel).width(40).row();
        carsBox.add(stats).left().growX().row();

        carTable.add(carsBox).growX().top().row();

        // Right에 분리된 두 테이블을 상단에 배치 (서로 분리되도록 간격 추가)
        right.add(mapTable).growX().top().padBottom(30).row(); // MAP과 CARS 사이 간격 30px
        right.add(carTable).growX().top().row();

        center.add(left)
              .width(Value.percentWidth(0.65f, root)) // 화면 65% 강제
              .expandY().fillY()
              .top()
              .left();
        center.add(right)
              .width(Value.percentWidth(0.30f, root)) // 화면 30% 강제
              .padLeft(Value.percentWidth(0.02f, root)) // 2% 간격
              .expandY().fillY()
              .top()
              .right();

        // Bottom bar
        Table bottom = new Table();
        statusLabel = new Label("Connecting...", skin);
        statusLabel.setWrap(true);
        bottom.add(statusLabel).left().width(Value.percentWidth(0.9f, root)).fillX();
        root.add(bottom).expandX().fillX().padTop(8);

        hookEvents(backBtn, readyBtn, startBtn, prevCar, nextCar);
        updateReadyButton();
        updateStartButton();
        applyVehicleSelection();
        renderPlayers(null);

        initClient();
    }

    private void hookEvents(ImageTextButton backBtn, TextButton readyBtn, TextButton startBtn, TextButton prevCar, TextButton nextCar) {
        backBtn.addListener(new ClickListener(){ @Override public void clicked(InputEvent event, float x, float y){ leaveRoom(); game.setScreen(new MainMenuScreen(game)); }});
        readyBtn.addListener(new ClickListener(){ @Override public void clicked(InputEvent event, float x, float y){ toggleReady(); }});
        startBtn.addListener(new ClickListener(){ @Override public void clicked(InputEvent event, float x, float y){ startRace(); }});
        prevCar.addListener(new ClickListener(){ @Override public void clicked(InputEvent event, float x, float y){ changeVehicle(-1); }});
        nextCar.addListener(new ClickListener(){ @Override public void clicked(InputEvent event, float x, float y){ changeVehicle(1); }});
    }

    private void initClient() {
        client = new LobbyClient();
        client.start();
        setStatus("Connecting...");
        connectThread = new Thread(() -> {
            try {
                // 환경변수로 서버 IP 설정 가능 (기본값: localhost)
                String serverHost = System.getenv("F1_SERVER_HOST");
                if (serverHost == null || serverHost.trim().isEmpty()) {
                    serverHost = "localhost"; // 기본값
                }
                Gdx.app.log("MultiplayerScreen", "Connecting to server: " + serverHost);
                client.connect(serverHost, 54555, 54777, 3000);
                Gdx.app.postRunnable(() -> {
                    setStatus("Connected");
                    refreshRooms();
                });
            } catch (IOException e) {
                Gdx.app.postRunnable(() -> setStatus("Connect failed: " + e.getMessage()));
            }
        }, "lobby-connect");
        connectThread.setDaemon(true);
        connectThread.start();

        client.onRoomList(rooms -> Gdx.app.postRunnable(() -> handleRoomList(rooms)));
        client.onRoomState(state -> Gdx.app.postRunnable(() -> {
            lastRoomState = state;
            if (state != null) {
                int idx = Math.max(0, Math.min(state.selectedTrackIndex, tracks.size() - 1));
                selectTrack(idx, false);
                if (state.players != null) {
                    for (Packets.PlayerInfo p : state.players) {
                        if (p.playerId == selfId && p.vehicleIndex >= 0 && p.vehicleIndex < vehicles.size()) {
                            vehicleIndex = p.vehicleIndex;
                            applyVehicleSelection();
                            break;
                        }
                    }
                }
            }
            renderPlayers(state);
            updateStartButton();
        }));
        client.onRaceStart(pkt -> Gdx.app.postRunnable(() -> {
            setStatus("Race starting in " + pkt.countdownSeconds + "s");
            String mapPath = getTrackPath(pkt.trackIndex);
            String carPath = vehicles.isEmpty() ? null : vehicles.get(vehicleIndex).texturePath;
            IntIntMap vehicleMap = new IntIntMap();
            if (pkt.playerIds != null && pkt.vehicleIndices != null) {
                int len = Math.min(pkt.playerIds.length, pkt.vehicleIndices.length);
                for (int i = 0; i < len; i++) {
                    vehicleMap.put(pkt.playerIds[i], pkt.vehicleIndices[i]);
                }
            } else if (lastRoomState != null && lastRoomState.players != null) {
                for (Packets.PlayerInfo p : lastRoomState.players) {
                    vehicleMap.put(p.playerId, p.vehicleIndex);
                }
            }
            com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
                @Override public void run() {
                    game.setScreen(new GameScreen(game, mapPath, carPath, client, currentRoomId, selfId, vehicleMap));
                }
            }, pkt.countdownSeconds);
        }));
        client.onChat(msg -> Gdx.app.postRunnable(() -> appendChat(chatTable, chatScroll, msg.sender, msg.text, msg.ts)));
        client.onError(msg -> Gdx.app.postRunnable(() -> setStatus("Error: " + msg)));
    }

    private void handleRoomList(List<Packets.RoomState> rooms) {
        if (currentRoomId != null) return;
        if (creatingRoom || joiningRoom) return;
        if (rooms == null || rooms.isEmpty()) {
            createRoomAuto();
        } else {
            Packets.RoomState r = rooms.get(0);
            joinRoom(r.roomId, safeName(game.playerName));
        }
    }

    private void createRoomAuto() {
        if (!client.isConnected()) { setStatus("Not connected"); return; }
        creatingRoom = true;
        String user = safeName(game.playerName);
        client.createRoom("AutoRoom", user, 4).whenComplete((res, err) -> Gdx.app.postRunnable(() -> {
            creatingRoom = false;
            if (err != null || res == null || !res.ok) {
                setStatus("Create failed: " + (err != null ? err.getMessage() : res != null ? res.message : ""));
                return;
            }
            currentRoomId = res.roomId;
            selectedRoomId = res.roomId;
            selfId = res.self.playerId;
            isReady = false;
            setStatus("Created room " + res.roomId);
            refreshRooms();
        }));
    }

    private void joinRoom(String roomId, String user) {
        if (!client.isConnected()) { setStatus("Not connected"); return; }
        joiningRoom = true;
        client.joinRoom(roomId, user).whenComplete((res, err) -> Gdx.app.postRunnable(() -> {
            joiningRoom = false;
            if (err != null || res == null || !res.ok) {
                setStatus("Join failed: " + (err != null ? err.getMessage() : res != null ? res.message : ""));
                return;
            }
            currentRoomId = roomId;
            selectedRoomId = roomId;
            selfId = res.self.playerId;
            isReady = false;
            lastRoomState = res.state;
            setStatus("Joined room " + roomId);
            if (res.state != null) {
                int idx = Math.max(0, Math.min(res.state.selectedTrackIndex, tracks.size() - 1));
                selectTrack(idx, false);
            }
            renderPlayers(res.state);
            updateStartButton();
        }));
    }

    private void refreshRooms() {
        if (!client.isConnected()) { setStatus("Not connected"); return; }
        client.requestRoomList();
    }

    private void toggleReady() {
        if (currentRoomId == null) { setStatus("Join or create a room first"); return; }
        isReady = !isReady;
        client.setReady(currentRoomId, isReady);
        // ready 토글 시 현재 선택 상태도 서버에 한번 더 전달하여 초기화 방지
        client.sendSelection(currentRoomId, trackIndex, vehicleIndex);
        updateReadyButton();
    }

    private void startRace() {
        if (currentRoomId == null) { setStatus("No room"); return; }
        if (!isHost()) { setStatus("Only host can start"); return; }
        if (!canStart()) { setStatus("All players must be ready (2+ required)"); return; }
        String user = safeName(game.playerName);
        client.startRace(currentRoomId, user, 5, trackIndex, vehicleIndex);
        setStatus("Start requested");
    }

    private void leaveRoom() {
        if (currentRoomId != null) {
            client.leaveRoom(currentRoomId);
        }
        currentRoomId = null;
        selectedRoomId = null;
        selfId = -1;
        isReady = false;
        lastRoomState = null;
        renderPlayers(null);
        updateReadyButton();
        updateStartButton();
    }

    private boolean isHost() {
        if (lastRoomState == null || lastRoomState.players == null || lastRoomState.players.isEmpty()) return false;
        return lastRoomState.players.get(0).playerId == selfId;
    }

    private boolean canStart() {
        if (lastRoomState == null || lastRoomState.players == null) return false;
        if (lastRoomState.players.size() < 2) return false;
        for (Packets.PlayerInfo p : lastRoomState.players) {
            if (!p.ready) return false;
        }
        return true;
    }

    private void renderPlayers(Packets.RoomState state) {
        playersTable.clear();
        int maxPlayers = 4;
        int currentPlayers = 0;
        if (state != null) {
            maxPlayers = state.maxPlayers > 0 ? state.maxPlayers : 4;
            currentPlayers = state.players != null ? state.players.size() : 0;
        }
        playerHeader.setText("PLAYERS (" + currentPlayers + "/" + maxPlayers + ")");

        int cols = 2;
        for (int i = 0; i < maxPlayers; i++) {
            Table card = new Table(skin);
            card.setBackground(skin.getDrawable("panel-dark-round"));
            // thick black stroke-like tint
            card.setColor(new Color(0f, 0f, 0f, 1f));
            card.pad(10);
            card.defaults().left();
            card.left();
            if (state != null && state.players != null && i < state.players.size()) {
                Packets.PlayerInfo p = state.players.get(i);
                Label name = new Label(p.username != null ? p.username : ("Player" + p.playerId), skin);
                if (i == 0) name.setColor(new Color(0.92f, 0.8f, 0.2f, 1f)); // host yellow
                String carName = "Vehicle";
                if (p.vehicleIndex >= 0 && p.vehicleIndex < vehicles.size()) {
                    carName = vehicles.get(p.vehicleIndex).name;
                }
                if (p.playerId == selfId && vehicleIndex >= 0 && vehicleIndex < vehicles.size()) {
                    carName = vehicles.get(vehicleIndex).name;
                }
                Label car = new Label(carName, skin);
                car.setColor(new Color(0.8f, 0.8f, 0.8f, 1f));
                Image readyIcon = new Image(skin.getDrawable(p.ready ? "icon-ready-on" : "icon-ready-off"));
                readyIcon.setColor(p.ready ? Color.WHITE : Color.LIGHT_GRAY);
                Label readyLabel = new Label(p.ready ? "READY" : "NOT READY", skin);
                readyLabel.setColor(p.ready ? Color.GREEN : Color.GRAY);

                card.add(name).left().colspan(2).row();
                card.add(car).left().colspan(2).padTop(4).row();
                Table readyRow = new Table();
                readyRow.left();
                readyRow.add(readyIcon).size(14, 14).padRight(6);
                readyRow.add(readyLabel).left();
                card.add(readyRow).left().colspan(2).padTop(6);
            } else {
                Image waitIcon = new Image(skin, "icon-single");
                waitIcon.setScaling(Scaling.fit);
                Label wait = new Label("Waiting...", skin);
                wait.getColor().a = 0.7f;
                wait.addAction(Actions.forever(Actions.sequence(Actions.alpha(0.4f, 0.6f), Actions.alpha(0.9f, 0.6f))));
                card.add(waitIcon).size(26, 26).padBottom(6).row();
                card.add(wait).center();
            }
            playersTable.add(card).growX().height(130).padBottom(6).uniformX();
            if ((i+1) % cols == 0) playersTable.row();
        }
    }

    private void updateReadyButton() {
        if (readyBtn != null) {
            readyBtn.setText("READY");
            readyBtn.getLabel().setColor(isReady ? Color.GREEN : Color.WHITE);
            readyBtn.setColor(isReady ? new Color(0.4f, 1f, 0.4f, 1f) : Color.WHITE);
        }
    }

    private void updateStartButton() {
        if (startBtn == null) return;
        boolean enabled = isHost() && canStart();
        startBtn.setDisabled(!enabled);
        startBtn.setText("START");
    }

    private void changeVehicle(int delta) {
        vehicleIndex = (vehicleIndex + delta + vehicles.size()) % vehicles.size();
        applyVehicleSelection();
        if (client != null && currentRoomId != null) {
            client.sendSelection(currentRoomId, trackIndex, vehicleIndex);
        }
    }

    private void applyVehicleSelection() {
        if (vehicles.isEmpty()) return;
        VehicleOption v = vehicles.get(vehicleIndex);
        selectedVehicleName = v.name;
        Texture tex = null;
        try {
            tex = Main.assetManager.get(v.texturePath, Texture.class);
        } catch (Exception e) { /* ignore */ }
        if (tex != null) {
            vehicleImage.setDrawable(new Image(tex).getDrawable());
            vehicleImage.setScaling(Scaling.fit);
        }
        vehicleNameLabel.setText(v.name);
        vehicleDescLabel.setText(v.description);
        speedBar.setValue(v.speed);
        handlingBar.setValue(v.handling);
        durabilityBar.setValue(v.durability);
        speedValLabel.setText(String.valueOf(v.speed));
        handlingValLabel.setText(String.valueOf(v.handling));
        durabilityValLabel.setText(String.valueOf(v.durability));
        renderPlayers(lastRoomState); // update self slot car name
    }

    private String safeName(String s) {
        String n = s == null ? "" : s.trim();
        if (n.isEmpty()) n = "Player";
        if (n.length() > 24) n = n.substring(0, 24);
        return n;
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { leaveRoom(); game.setScreen(new MainMenuScreen(game)); return; }
        stage.act(delta);
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1);
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();
    }

    @Override public void resize(int width, int height) { if (stage != null) stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (client != null) client.close();
        if (stage != null) stage.dispose();
        // skin은 Main이 관리하므로 dispose 하지 않음
        if (connectThread != null && connectThread.isAlive()) {
            connectThread.interrupt();
        }
    }

    private void ensureHighlightBorder() {
        if (skin == null) return;
        if (skin.has("highlight-border", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) return;
        Pixmap pm = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        pm.setColor(0, 0, 0, 0);
        pm.fill();
        pm.setColor(Color.valueOf("dc2626"));
        pm.drawRectangle(0, 0, 4, 4);
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        NinePatch np = new NinePatch(new TextureRegion(tex), 1, 1, 1, 1);
        skin.add("highlight-border-adhoc-tex", tex, Texture.class);
        skin.add("highlight-border", new NinePatchDrawable(np));
    }

    private Drawable getOutlineDrawable() {
        if (skin != null && skin.has("highlight-border", Drawable.class)) {
            return skin.getDrawable("highlight-border");
        }
        // Fallback: simple red tile to avoid crash
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.valueOf("dc2626"));
        pm.fill();
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        TextureRegion region = new TextureRegion(tex);
        NinePatch np = new NinePatch(region, 0, 0, 0, 0);
        return new NinePatchDrawable(np);
    }

    private void seedVehicles() {
        vehicles.clear();
        vehicles.add(new VehicleOption("NEON PHANTOM", "High speed, nimble handling.", "cars/Astra A4.png", 90, 80, 60));
        vehicles.add(new VehicleOption("CRIMSON HAWK", "Aggressive accel, lighter frame.", "cars/Boltworks RX-1.png", 85, 70, 55));
        vehicles.add(new VehicleOption("GAIA BEAST", "Tanky build, stable control.", "cars/Emerald E7.png", 70, 65, 90));
    }

    private void seedTracks() {
        tracks.clear();
        // 1: 기본 트랙(기존 맵)
        tracks.add(new TrackOption("NEON CITY", "EASY", "f1_racing_map.tmx"));
        // 2: 일본 트랙
        tracks.add(new TrackOption("JAPAN CIRCUIT", "NORMAL", "japan.tmx"));
        // 3: 아메리카 트랙
        tracks.add(new TrackOption("AMERICA GP", "HARD", "america.tmx"));
    }

    private static class VehicleOption {
        final String name;
        final String description;
        final String texturePath;
        final int speed;
        final int handling;
        final int durability;
        VehicleOption(String name, String description, String texturePath, int speed, int handling, int durability) {
            this.name = name;
            this.description = description;
            this.texturePath = texturePath;
            this.speed = speed;
            this.handling = handling;
            this.durability = durability;
        }
    }

    private static class TrackOption {
        final String name;
        final String difficulty;
        final String mapPath;
        TrackOption(String name, String difficulty, String mapPath) {
            this.name = name;
            this.difficulty = difficulty;
            this.mapPath = mapPath;
        }
    }

    private void selectTrack(int idx) { selectTrack(idx, true); }

    private void selectTrack(int idx, boolean notifyServer) {
        if (idx < 0 || idx >= tracks.size()) return;
        trackIndex = idx;
        updateTrackSelection(trackWrappers);
        if (notifyServer && !tracks.isEmpty()) {
            TrackOption t = tracks.get(trackIndex);
            setStatus("MAP 선택: " + t.name);
        }
        if (notifyServer && client != null && currentRoomId != null && isHost()) {
            client.sendSelection(currentRoomId, trackIndex, vehicleIndex);
        }
    }

    private String getTrackPath(int idx) {
        if (idx >= 0 && idx < tracks.size() && tracks.get(idx).mapPath != null) {
            return tracks.get(idx).mapPath;
        }
        return "f1_racing_map.tmx";
    }

    private void appendChat(Table chatTable, ScrollPane chatScroll, String sender, String text, long ts) {
        String time = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(ts == 0 ? System.currentTimeMillis() : ts));
        String line = "[" + time + "] " + sender + ": " + text;
        chatTable.add(new Label(line, skin)).left().row();
        chatScroll.layout();
        chatScroll.setScrollPercentY(1f);
    }

    private void updateTrackSelection(List<Table> wrappers) {
        for (int i = 0; i < wrappers.size(); i++) {
            Table w = wrappers.get(i);
            if (w.getChildren().size > 0 && w.getChildren().first() instanceof Table) {
                Table card = (Table) w.getChildren().first();
                // 기본 배경/색 초기화
                card.setBackground(skin.getDrawable("slot-bg-normal"));
                card.setColor(Color.WHITE);
                for (com.badlogic.gdx.scenes.scene2d.Actor child : card.getChildren()) {
                    if (child instanceof Label lbl) {
                        String txt = lbl.getText().toString();
                        // 이름은 흰색, 난이도는 색상 유지
                        if ("EASY".equalsIgnoreCase(txt) || "NORMAL".equalsIgnoreCase(txt) || "HARD".equalsIgnoreCase(txt)) {
                            lbl.setColor(getDifficultyColor(txt));
                        } else {
                            lbl.setColor(Color.WHITE);
                        }
                    }
                }
            }
            if (i == trackIndex) {
                // 선택 시: 카드 배경을 빨간색으로
                if (w.getChildren().size > 0 && w.getChildren().first() instanceof Table) {
                    Table card = (Table) w.getChildren().first();
                    card.setBackground(skin.getDrawable("track-selected")); // 붉은 배경+테두리
                    card.setColor(Color.WHITE); // 텍스트 선명 유지
                }
            } else {
                // 비선택: wrapper 배경 없음
            }
            w.setBackground((Drawable) null); // wrapper는 항상 배경 제거하여 highlight-border 영향 제거
        }
    }

    private Color getDifficultyColor(String diffText) {
        if (diffText == null) return Color.WHITE;
        String d = diffText.trim().toUpperCase();
        if ("EASY".equals(d)) return new Color(0.1f,0.8f,0.3f,1f);
        if ("NORMAL".equals(d)) return new Color(0.2f,0.6f,1f,1f);
        if ("HARD".equals(d)) return new Color(0.95f,0.6f,0.2f,1f);
        return Color.WHITE;
    }
}
