package com.mygame.f1.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygame.f1.Main;
import com.mygame.f1.ui.SkinFactory;

public class SplashScreen implements Screen {
    private final Main game;
    private Stage stage;
    private Skin skin;
    private float timer = 0f;
    private final float duration = 1.0f;
    private com.badlogic.gdx.graphics.Texture logo;

    public SplashScreen(Main game) { this.game = game; }

    @Override public void show() {
        skin = SkinFactory.createDefaultSkin();
        stage = new Stage(new ScreenViewport());
        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(skin.getDrawable("bg"));
        stage.addActor(root);

        Table panel = new Table();
        panel.defaults().pad(8);
        panel.setBackground(skin.getDrawable("panel"));

        try { logo = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("ui/login/logo.png")); } catch (Exception ignored) {}
        if (logo != null) {
            panel.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(logo)).pad(10).row();
        }
        panel.add(new Label("F1 Racing Game", skin, "title")).row();
        panel.add(new Label("Loading...", skin)).row();

        root.add(panel).center();
    }

    @Override public void render(float delta) {
        timer += delta;
        if (timer >= duration) { game.setScreen(new LoginScreen(game)); return; }
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { if (stage!=null) stage.getViewport().update(w,h,true);} 
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { if (stage!=null) stage.dispose(); if (skin!=null) skin.dispose(); if (logo!=null) logo.dispose(); }
}
