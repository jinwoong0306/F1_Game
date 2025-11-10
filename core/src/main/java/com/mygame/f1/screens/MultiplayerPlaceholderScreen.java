package com.mygame.f1.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygame.f1.Main;
import com.mygame.f1.ui.SkinFactory;

public class MultiplayerPlaceholderScreen implements Screen {
    private final Main game;
    private Stage stage;
    private Skin skin;

    public MultiplayerPlaceholderScreen(Main game) { this.game = game; }

    @Override
    public void show() {
        skin = SkinFactory.createDefaultSkin();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        root.defaults().pad(8).width(320).height(44);
        stage.addActor(root);

        TextButton back = new TextButton("Back", skin);
        back.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x, float y){ game.setScreen(new MainMenuScreen(game)); }});

        root.add(new TextButton("Multiplayer (TBD)", skin)).row();
        root.add(new TextButton("Coming soon", skin)).row();
        root.add(back).row();
    }

    @Override public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { game.setScreen(new MainMenuScreen(game)); return; }
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { if (stage!=null) stage.getViewport().update(w,h,true);} 
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { if (stage!=null) stage.dispose(); if (skin!=null) skin.dispose(); }
}
