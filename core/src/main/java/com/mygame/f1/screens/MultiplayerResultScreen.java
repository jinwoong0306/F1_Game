package com.mygame.f1.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygame.f1.Main;
import com.mygame.f1.network.LobbyClient;
import com.mygame.f1.shared.Packets;

/**
 * 멀티플레이 레이스 결과 화면
 * 순위, 완주 시간, 랩 타임 및 FAIL 처리된 플레이어 표시
 */
public class MultiplayerResultScreen implements Screen {
    private final Main game;
    private final LobbyClient lobbyClient;
    private final String roomId;
    private final Packets.RaceResultsPacket results;

    private Stage stage;
    private Skin skin;
    private Texture bgTexture;

    public MultiplayerResultScreen(Main game, LobbyClient client, String roomId, Packets.RaceResultsPacket results) {
        this.game = game;
        this.lobbyClient = client;
        this.roomId = roomId;
        this.results = results;
    }

    @Override
    public void show() {
        skin = Main.getSharedSkin();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // 배경 생성
        bgTexture = createBackground();
        Image bg = new Image(bgTexture);
        bg.setFillParent(true);
        stage.addActor(bg);

        // 메인 테이블
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // 결과 카드
        Table card = new Table();
        card.setBackground(skin.getDrawable("bg"));
        card.pad(30);

        // 타이틀
        Label title = new Label("RACE RESULTS", skin, "title");
        title.setColor(Color.WHITE);
        card.add(title).padBottom(20).row();

        // 구분선
        Table divider = new Table();
        divider.setBackground(skin.getDrawable("divider"));
        card.add(divider).height(2).growX().padBottom(20).row();

        // 순위 테이블
        Table rankingsTable = new Table();
        rankingsTable.defaults().pad(8);

        // 헤더
        addRankingHeader(rankingsTable);

        // 완주자 순위
        if (results.results != null && results.results.length > 0) {
            for (Packets.PlayerResult result : results.results) {
                addPlayerResult(rankingsTable, result, false);
            }
        }

        // FAIL 처리된 플레이어
        if (results.failedPlayerIds != null && results.failedPlayerIds.length > 0) {
            // FAIL 구분선
            Label failLabel = new Label("--- DID NOT FINISH ---", skin, "default");
            failLabel.setColor(Color.RED);
            rankingsTable.add(failLabel).colspan(4).padTop(15).padBottom(10).row();

            for (int failedId : results.failedPlayerIds) {
                addFailedPlayer(rankingsTable, failedId);
            }
        }

        ScrollPane scroll = new ScrollPane(rankingsTable, skin);
        scroll.setFadeScrollBars(false);
        card.add(scroll).grow().padBottom(20).row();

        // 하단 버튼
        Table buttonTable = new Table();
        buttonTable.defaults().pad(8).width(180);

        ImageTextButton lobbyBtn = new ImageTextButton("Return to Lobby", skin, "primary");
        lobbyBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                returnToLobby();
            }
        });
        buttonTable.add(lobbyBtn);

        ImageTextButton exitBtn = new ImageTextButton("Exit", skin, "secondary");
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                exitToMenu();
            }
        });
        buttonTable.add(exitBtn);

        card.add(buttonTable).row();

        root.add(card).width(800).height(600);

        Gdx.app.log("MultiplayerResultScreen", String.format("Showing results: %d finished, %d failed",
            results.results != null ? results.results.length : 0,
            results.failedPlayerIds != null ? results.failedPlayerIds.length : 0));
    }

    private void addRankingHeader(Table table) {
        Label rankLabel = new Label("RANK", skin, "default");
        rankLabel.setColor(Color.GOLD);
        table.add(rankLabel).width(60);

        Label nameLabel = new Label("PLAYER", skin, "default");
        nameLabel.setColor(Color.GOLD);
        table.add(nameLabel).width(200).align(Align.left);

        Label timeLabel = new Label("TIME", skin, "default");
        timeLabel.setColor(Color.GOLD);
        table.add(timeLabel).width(120);

        Label lapsLabel = new Label("BEST LAP", skin, "default");
        lapsLabel.setColor(Color.GOLD);
        table.add(lapsLabel).width(120);

        table.row();

        // 구분선
        Table divider = new Table();
        divider.setBackground(skin.getDrawable("divider"));
        table.add(divider).colspan(4).height(2).growX().padTop(5).padBottom(10).row();
    }

    private void addPlayerResult(Table table, Packets.PlayerResult result, boolean failed) {
        Color textColor = failed ? Color.GRAY : Color.WHITE;

        // 순위
        String rankText = failed ? "FAIL" : getRankText(result.rank);
        Label rankLabel = new Label(rankText, skin, "default");
        rankLabel.setColor(result.rank == 1 ? Color.GOLD : result.rank == 2 ? Color.LIGHT_GRAY : result.rank == 3 ? new Color(0.8f, 0.5f, 0.2f, 1f) : textColor);
        table.add(rankLabel).width(60);

        // 플레이어 이름
        Label nameLabel = new Label(result.username, skin, "default");
        nameLabel.setColor(textColor);
        table.add(nameLabel).width(200).align(Align.left);

        // 총 시간
        String timeText = failed ? "DNF" : formatTime(result.totalTime);
        Label timeLabel = new Label(timeText, skin, "default");
        timeLabel.setColor(textColor);
        table.add(timeLabel).width(120);

        // 최고 랩 타임
        String bestLapText = failed ? "--" : formatTime(getBestLapTime(result.lapTimes));
        Label lapLabel = new Label(bestLapText, skin, "default");
        lapLabel.setColor(textColor);
        table.add(lapLabel).width(120);

        table.row();
    }

    private void addFailedPlayer(Table table, int playerId) {
        // FAIL 플레이어 표시
        Label failLabel = new Label("DNF", skin, "default");
        failLabel.setColor(Color.RED);
        table.add(failLabel).width(60);

        Label nameLabel = new Label("Player " + playerId, skin, "default");
        nameLabel.setColor(Color.GRAY);
        table.add(nameLabel).width(200).align(Align.left);

        Label dnfLabel = new Label("Did Not Finish", skin, "default");
        dnfLabel.setColor(Color.GRAY);
        table.add(dnfLabel).colspan(2);

        table.row();
    }

    private String getRankText(int rank) {
        switch (rank) {
            case 1: return "1ST";
            case 2: return "2ND";
            case 3: return "3RD";
            default: return rank + "TH";
        }
    }

    private String formatTime(float seconds) {
        int minutes = (int) (seconds / 60);
        float secs = seconds % 60;
        return String.format("%d:%06.3f", minutes, secs);
    }

    private float getBestLapTime(float[] lapTimes) {
        if (lapTimes == null || lapTimes.length == 0) return 0f;
        float best = lapTimes[0];
        for (float time : lapTimes) {
            if (time < best) best = time;
        }
        return best;
    }

    private Texture createBackground() {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGB888);
        pixmap.setColor(0.1f, 0.1f, 0.15f, 1f); // 어두운 파란색 배경
        pixmap.fill();
        Texture tex = new Texture(pixmap);
        pixmap.dispose();
        return tex;
    }

    private void returnToLobby() {
        Gdx.app.log("MultiplayerResultScreen", "Returning to lobby");
        // 로비 화면으로 돌아가기 (새로운 로비 생성)
        game.setScreen(new MultiplayerPlaceholderScreen(game));
    }

    private void exitToMenu() {
        Gdx.app.log("MultiplayerResultScreen", "Exiting to main menu");
        // 연결 종료 및 메인 메뉴로
        if (lobbyClient != null && roomId != null) {
            lobbyClient.leaveRoom(roomId);
        }
        game.setScreen(new MainMenuScreen(game));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (bgTexture != null) bgTexture.dispose();
    }
}
