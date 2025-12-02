package com.mygame.f1.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.Timer;
import com.mygame.f1.Main;

public class SplashScreen implements Screen {
    private final Main game;
    private Stage stage;
    private Skin skin;
    private float timer = 0f;
    private final float duration = 3.0f;
    private Texture logo;
    private Texture gradientTex;
    private Texture dotTex;
    private boolean loadingDone = false;
    private boolean ownsLogo = false;
    private boolean ownsDot = false;
    private boolean renderLogged = false;

    public SplashScreen(Main game) { this.game = game; }

    @Override public void show() {
        long t0 = System.nanoTime();
        log("show() start");
        long tSkin = System.nanoTime();
        skin = Main.getSharedSkin();
        logMs("Skin retrieved", tSkin);

        long tStage = System.nanoTime();
        stage = new Stage(new ScreenViewport());
        logMs("Stage created", tStage);
        Gdx.input.setInputProcessor(stage);
        log("Input processor set");

        // 단색 검정 배경
        long tGrad = System.nanoTime();
        gradientTex = makeVerticalGradient();
        logMs("Gradient tex made", tGrad);
        Image bg = new Image(gradientTex);
        bg.setFillParent(true);
        stage.addActor(bg);
        log("Background added");

        Table panel = new Table();
        panel.setFillParent(true);
        panel.setBackground(skin.getDrawable("bg")); // 전체 검정
        panel.defaults().pad(12).center();
        stage.addActor(panel);
        log("Panel added");

        try {
            if (Main.assetManager.isLoaded("ui/login/logo.png", Texture.class)) {
                logo = Main.assetManager.get("ui/login/logo.png", Texture.class);
            } else {
                logo = new Texture(Gdx.files.internal("ui/login/logo.png")); ownsLogo = true;
            }
            log("Logo load success");
        } catch (Exception ignored) { log("Logo load failed"); }
        if (logo != null) {
            Image logoImg = new Image(logo);
            logoImg.setScaling(Scaling.fit);
            logoImg.getColor().a = 0f;
            logoImg.addAction(Actions.fadeIn(1f));
            panel.add(logoImg).width(1200).height(400).padBottom(24).row(); // 2배 크기
            log("Logo image added");
        } else {
            Label title = new Label("PIXEL FORMULA RACING", skin, "title");
            title.getColor().a = 0f;
            title.addAction(Actions.fadeIn(1f));
            panel.add(title).padBottom(24).row();
            log("Fallback title added");
        }

        // Loading indicator (3 bouncing squares)
        Texture whiteTex = skin.getRegion("white-region").getTexture();
        try {
            if (Main.assetManager.isLoaded("ui/icon/circle.png", Texture.class)) {
                dotTex = Main.assetManager.get("ui/icon/circle.png", Texture.class);
            } else {
                dotTex = new Texture(Gdx.files.internal("ui/icon/circle.png")); ownsDot = true;
            }
            log("Dot texture load success");
        } catch (Exception ignored) { log("Dot texture load failed"); }
        Table loading = new Table();
        for (int i = 0; i < 3; i++) {
            Image dot;
            if (dotTex != null) {
                dot = new Image(dotTex);
            } else {
                dot = new Image(whiteTex);
            }
            dot.setScaling(Scaling.fit);
            dot.setColor(0.86f, 0.15f, 0.2f, 1f); // red-600
            dot.addAction(Actions.sequence(
                    Actions.delay(i * 0.15f),
                    Actions.forever(Actions.sequence(
                            Actions.moveBy(0, 12f, 0.5f, Interpolation.sine),
                            Actions.moveBy(0, -12f, 0.5f, Interpolation.sine)
                    ))
            ));
            loading.add(dot).size(12).pad(4);
        }
        panel.add(loading).center().padBottom(6).row();
        log("Loading dots added");

        Label loadingText = new Label("LOADING...", skin);
        loadingText.setColor(0.44f, 0.44f, 0.48f, 1f); // zinc-500
        panel.add(loadingText).center();
        log("Loading text added");

        Gdx.app.log("PERF", String.format("SplashScreen.show setup: %.2f ms", (System.nanoTime() - t0) / 1_000_000f));
    }

    @Override public void render(float delta) {
        if (!renderLogged) {
            log("render() start");
        }
        timer += delta;
        if (timer >= duration && !loadingDone) {
            if (!renderLogged) log("duration reached, scheduling transition");
            loadingDone = true;
            // 약간의 지연 후 화면 전환을 예약해 로딩 애니메이션이 끝까지 재생되도록 함
            Timer.schedule(new Timer.Task() {
                @Override public void run() { game.setScreen(new LoginScreen(game)); }
            }, 0.1f);
        }
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (!renderLogged) log("stage act/draw");
        stage.act(delta);
        stage.draw();
        renderLogged = true;
    }

    @Override public void resize(int w, int h) { if (stage!=null) stage.getViewport().update(w,h,true);} 
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        if (stage!=null) stage.dispose();
        // skin은 Main이 관리하므로 dispose 하지 않음
        if (ownsLogo && logo!=null) logo.dispose();
        if (gradientTex!=null) gradientTex.dispose();
        if (ownsDot && dotTex!=null) dotTex.dispose();
    }

    private Texture makeVerticalGradient() {
        Pixmap pm = new Pixmap(1, 3, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, 1f); pm.drawPixel(0, 2);
        pm.setColor(0f, 0f, 0f, 1f); pm.drawPixel(0, 1);
        pm.setColor(0f, 0f, 0f, 1f); pm.drawPixel(0, 0);
        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private void log(String msg) {
        Gdx.app.log("SPLASH", msg);
    }

    private void logMs(String msg, long startNanos) {
        Gdx.app.log("SPLASH", String.format("%s: %.2f ms", msg, (System.nanoTime() - startNanos) / 1_000_000f));
    }
}
