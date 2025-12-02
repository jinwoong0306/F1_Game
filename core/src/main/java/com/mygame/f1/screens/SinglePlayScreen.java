package com.mygame.f1.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygame.f1.GameScreen;
import com.mygame.f1.Main;
import com.mygame.f1.ui.SkinFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 싱글 플레이 진입용 선택 화면: 차량 슬라이드 + 맵 리스트.
 */
public class SinglePlayScreen implements Screen {
    private final Main game;
    private Stage stage;
    private Skin skin;
    private boolean disposed = false;

    private final List<VehicleOption> vehicles = new ArrayList<>();
    private final List<TrackOption> tracks = new ArrayList<>();
    private int vehicleIndex = 0;
    private int trackIndex = 0;

    private Image vehicleImage;
    private Label vehicleLabel;
    private ButtonGroup<ImageTextButton> trackGroup;
    private ProgressBar speedBar, handlingBar, durabilityBar;
    private Label speedLabel, handlingLabel, durabilityLabel;

    public SinglePlayScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        skin = SkinFactory.createDefaultSkin();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        seedVehicles();
        seedTracks();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(30);
        // 전체 배경: 진한 검정
        root.setBackground(skin.getDrawable("bg"));
        stage.addActor(root);

        Label title = new Label("SINGLE PLAY", skin.get("title", Label.LabelStyle.class));
        title.setAlignment(Align.left);
        root.add(title).left().padBottom(20).row();

        Table content = new Table();
        content.defaults().pad(10).grow();
        root.add(content).grow();

        // 차량 선택 패널
        content.add(buildVehiclePanel()).grow();

        // 맵 선택 패널
        content.add(buildTrackPanel()).grow();

        // 액션 버튼
        Table actions = new Table();
        actions.defaults().pad(8);
        TextButton btnStart = new TextButton("START", skin);
        TextButton btnExit = new TextButton("EXIT", skin);
        btnStart.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y) {
                launchGame();
            }
        });
        btnExit.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
        actions.add(btnStart).width(160).height(56);
        actions.add(btnExit).width(140).height(56);
        root.add(actions).padTop(12).left().row();

        // 초기 선택 상태 반영
        refreshVehicle();
    }

    private Table buildVehiclePanel() {
        Table panel = new Table();
        panel.setBackground(skin.getDrawable("panel-dark-round"));
        panel.pad(16);
        panel.defaults().pad(6);

        Table header = new Table();
        Image carIcon = new Image(skin.getDrawable("icon-car"));
        header.add(carIcon).size(22, 22).padRight(8);
        Label headerLabel = new Label("CARS", skin);
        headerLabel.setAlignment(Align.left);
        header.add(headerLabel).left();
        panel.add(header).left().row();

        Table preview = new Table();
        preview.setBackground(skin.getDrawable("slot-bg-normal"));
        preview.pad(12);
        vehicleImage = new Image(loadVehicleTexture(vehicleIndex));
        vehicleImage.setScaling(Scaling.fit);
        vehicleLabel = new Label(vehicles.get(vehicleIndex).name, skin);
        vehicleLabel.setAlignment(Align.center);
        preview.add(vehicleImage).size(260, 140).row();
        preview.add(vehicleLabel).padTop(10);
        panel.add(preview).growX().padTop(10).row();

        // 스탯 그래프
        speedBar = new ProgressBar(0, 100, 1, false, skin, "stat-blue");
        handlingBar = new ProgressBar(0, 100, 1, false, skin, "stat-yellow");
        durabilityBar = new ProgressBar(0, 100, 1, false, skin, "stat-green");
        speedLabel = new Label("", skin);
        handlingLabel = new Label("", skin);
        durabilityLabel = new Label("", skin);

        Table stats = new Table();
        stats.setBackground(skin.getDrawable("panel-dark-round"));
        stats.defaults().pad(4).left();
        stats.add(new Label("SPEED", skin)).left();
        stats.add(speedBar).width(220).padRight(6);
        stats.add(speedLabel).width(40).row();
        stats.add(new Label("HANDLING", skin)).left();
        stats.add(handlingBar).width(220).padRight(6);
        stats.add(handlingLabel).width(40).row();
        stats.add(new Label("DURABILITY", skin)).left();
        stats.add(durabilityBar).width(220).padRight(6);
        stats.add(durabilityLabel).width(40).row();
        panel.add(stats).growX().padTop(10).row();

        Table controls = new Table();
        TextButton prev = new TextButton("<", skin);
        TextButton next = new TextButton(">", skin);
        prev.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y) {
                vehicleIndex = (vehicleIndex - 1 + vehicles.size()) % vehicles.size();
                refreshVehicle();
            }
        });
        next.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y) {
                vehicleIndex = (vehicleIndex + 1) % vehicles.size();
                refreshVehicle();
            }
        });
        controls.add(prev).width(80).height(48).padRight(8);
        controls.add(next).width(80).height(48);
        panel.add(controls).padTop(10).row();
        return panel;
    }

    private Table buildTrackPanel() {
        Table panel = new Table();
        panel.setBackground(skin.getDrawable("panel-dark-round"));
        panel.pad(16);
        panel.defaults().pad(6).growX();

        Table header = new Table();
        Image trackIcon = new Image(skin.getDrawable("icon-track"));
        header.add(trackIcon).size(22, 22).padRight(8);
        Label headerLabel = new Label("MAP", skin);
        headerLabel.setAlignment(Align.left);
        header.add(headerLabel).left();
        header.left();
        panel.add(header).left().row();

        Table list = new Table();
        list.defaults().pad(6).growX();
        trackGroup = new ButtonGroup<>();
        trackGroup.setMaxCheckCount(1);
        trackGroup.setMinCheckCount(1);

        for (int i = 0; i < tracks.size(); i++) {
            TrackOption t = tracks.get(i);
            ImageTextButton card = makeTrackCard(t);
            final int idx = i;
            card.addListener(new ClickListener(){
                @Override public void clicked(InputEvent event, float x, float y) {
                    trackIndex = idx;
                }
            });
            trackGroup.add(card);
            list.add(card).height(72).row();
            if (i == 0) card.setChecked(true);
        }

        panel.add(list).growX().padTop(10);
        return panel;
    }

    private ImageTextButton makeTrackCard(TrackOption track) {
        ImageTextButton.ImageTextButtonStyle style = new ImageTextButton.ImageTextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
        Drawable up = skin.getDrawable("slot-bg-normal");
        Drawable over = skin.newDrawable("white", SkinFactory.Palette.BTN_OVER);
        Drawable checked = skin.newDrawable("white", SkinFactory.Palette.NEON_RED.cpy().mul(1f,1f,1f,0.35f));
        style.up = up; style.over = over; style.down = over; style.checked = checked;
        style.font = skin.getFont("default-font");
        style.fontColor = Color.WHITE;
        style.imageUp = null;
        style.imageOver = null;
        style.imageChecked = null;

        ImageTextButton card = new ImageTextButton(track.name + "  |  " + track.difficulty, style);
        card.getLabel().setAlignment(Align.left);
        card.pad(14, 18, 14, 18);
        return card;
    }

    private void refreshVehicle() {
        VehicleOption v = vehicles.get(vehicleIndex);
        vehicleLabel.setText(v.name);
        vehicleImage.setDrawable(new Image(loadVehicleTexture(vehicleIndex)).getDrawable());
        // 스탯 반영
        speedBar.setValue(v.speed);
        handlingBar.setValue(v.handling);
        durabilityBar.setValue(v.durability);
        speedLabel.setText(String.valueOf(v.speed));
        handlingLabel.setText(String.valueOf(v.handling));
        durabilityLabel.setText(String.valueOf(v.durability));
    }

    private Texture loadVehicleTexture(int idx) {
        String path = vehicles.get(idx).path;
        if (Main.assetManager.isLoaded(path, Texture.class)) {
            return Main.assetManager.get(path, Texture.class);
        }
        return new Texture(Gdx.files.internal(path));
    }

    private void launchGame() {
        TrackOption t = tracks.get(trackIndex);
        VehicleOption v = vehicles.get(vehicleIndex);
        game.setScreen(new GameScreen(game, t.mapPath, v.path));
    }

    @Override public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { dispose(); }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        if (stage != null) stage.dispose();
        // 차량 텍스처는 AssetManager가 관리하므로 별도 dispose 불필요
    }

    private void seedVehicles() {
        vehicles.clear();
        // 멀티룸과 동일한 톤의 스탯을 부여
        vehicles.add(new VehicleOption("ASTRA A4", "cars/Astra A4.png", 90, 80, 60));
        vehicles.add(new VehicleOption("BOLTWORKS RX-1", "cars/Boltworks RX-1.png", 85, 70, 55));
        vehicles.add(new VehicleOption("EMERALD E7", "cars/Emerald E7.png", 70, 65, 90));
        vehicles.add(new VehicleOption("GOLD RUSH GT", "cars/Gold Rush GT.png", 78, 72, 75));
        vehicles.add(new VehicleOption("MIDNIGHT P4", "cars/Midnight P4.png", 88, 77, 65));
        vehicles.add(new VehicleOption("SILVERLINE S11", "cars/Silverline S11.png", 82, 85, 70));
    }

    private void seedTracks() {
        tracks.clear();
        tracks.add(new TrackOption("NEON CITY", "EASY", "f1_racing_map.tmx"));
        tracks.add(new TrackOption("JAPAN CIRCUIT", "NORMAL", "japan.tmx"));
        tracks.add(new TrackOption("AMERICA GP", "HARD", "america.tmx"));
    }

    private static class VehicleOption {
        final String name;
        final String path;
        final int speed;
        final int handling;
        final int durability;
        VehicleOption(String name, String path, int speed, int handling, int durability) {
            this.name = name; this.path = path; this.speed = speed; this.handling = handling; this.durability = durability;
        }
    }

    private static class TrackOption {
        final String name;
        final String difficulty;
        final String mapPath;
        TrackOption(String name, String difficulty, String mapPath) {
            this.name = name; this.difficulty = difficulty; this.mapPath = mapPath;
        }
    }
}
