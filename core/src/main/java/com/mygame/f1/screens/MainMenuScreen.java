package com.mygame.f1.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygame.f1.GameScreen;
import com.mygame.f1.Main;
import com.mygame.f1.ui.SkinFactory;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

public class MainMenuScreen implements Screen {
    private final Main game;
    private Stage stage;
    private Skin skin;
    private TextButton[] items;
    private int focus = 0;
    private Texture logoTex;

    public MainMenuScreen(Main game) { this.game = game; }

    @Override
    public void show() {
        skin = SkinFactory.createDefaultSkin();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(skin.getDrawable("bg"));
        stage.addActor(root);

        Table panel = new Table();
        panel.setBackground(skin.getDrawable("panel"));
        panel.defaults().pad(10).width(320).height(48);

        TextButton single = new TextButton("Single Play", skin);
        TextButton multi = new TextButton("Multi Play (TBD)", skin);
        TextButton settings = new TextButton("Settings", skin);
        TextButton exit = new TextButton("Exit", skin);
        items = new TextButton[]{single, multi, settings, exit};

        single.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x, float y){ game.setScreen(new GameScreen(game)); }});
        multi.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x, float y){ game.setScreen(new MultiplayerPlaceholderScreen(game)); }});
        settings.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x, float y){ game.setScreen(new SettingsScreen(game)); }});
        exit.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x, float y){ Gdx.app.exit(); }});

        panel.add(new Label("Main Menu", skin, "title")).row();
        panel.add(single).row();
        panel.add(multi).row();
        panel.add(settings).row();
        panel.add(exit).row();

        Image logo = null;
        try { logoTex = new Texture(Gdx.files.internal("ui/login/logo.png")); logo = new Image(logoTex); } catch (Exception ignored) {}
        Table left = new Table();
        if (logo != null) { left.add(logo).pad(20); }

        root.add(left).expand().left().pad(20);
        root.add(panel).right().pad(20);

        updateFocus();
    }

    private void updateFocus(){
        for (int i=0;i<items.length;i++){
            items[i].setChecked(i==focus);
        }
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) { focus = (focus-1+items.length)%items.length; updateFocus(); }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) { focus = (focus+1)%items.length; updateFocus(); }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) { Actor a = items[focus]; a.fire(new InputEvent(){ { setType(Type.touchDown);} }); a.fire(new InputEvent(){ { setType(Type.touchUp);} }); }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { Gdx.app.exit(); }

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { if (stage!=null) stage.getViewport().update(width,height,true);} 
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { if (stage!=null) stage.dispose(); if (skin!=null) skin.dispose(); if (logoTex!=null) logoTex.dispose(); }
}
