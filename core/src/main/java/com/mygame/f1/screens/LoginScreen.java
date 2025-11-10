package com.mygame.f1.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygame.f1.Main;
import com.mygame.f1.data.UserStore;
import com.mygame.f1.ui.SkinFactory;

public class LoginScreen implements Screen {
    private final Main game;
    private final UserStore store = new UserStore();
    private Stage stage;
    private Skin skin;
    private TextButton btnLoginRef;
    private Dialog activeDialog;
    private Table modalOverlay;

    public LoginScreen(Main game) {
        this.game = game;
    }

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
        panel.defaults().pad(8).width(420);

        Label title = new Label("Login", skin, "title");
        title.setAlignment(Align.center);

        TextField tfUser = new TextField("", skin);
        tfUser.setMessageText("Username");
        TextField tfPass = new TextField("", skin);
        tfPass.setMessageText("Password");
        tfPass.setPasswordMode(true);
        tfPass.setPasswordCharacter('•');

        TextButton btnLogin = new TextButton("로그인 (Enter)", skin);
        TextButton btnSignup = new TextButton("회원가입", skin);
        TextButton btnBack = new TextButton("뒤로", skin);
        this.btnLoginRef = btnLogin;

        btnLogin.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y){
                String u=tfUser.getText().trim();
                String p=tfPass.getText();
                if (store.verify(u,p)){
                    game.playerName = u;
                    game.setScreen(new MainMenuScreen(game));
                } else {
                    showAlert("로그인 실패", "아이디 또는 비밀번호가 올바르지 않습니다.");
                }
            }
        });

        btnSignup.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y){
                showSignupDialog(tfUser.getText().trim());
            }
        });

        btnBack.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y){
                game.setScreen(new SplashScreen(game));
            }
        });

        panel.add(title).colspan(2).center().padTop(8).row();
        panel.add(new Label("아이디", skin)).left();
        panel.add(tfUser).growX().row();
        panel.add(new Label("비밀번호", skin)).left();
        panel.add(tfPass).growX().row();
        panel.add(btnLogin).left();
        panel.add(btnSignup).right().row();
        panel.add(btnBack).colspan(2).center();

        root.add(panel).center();
    }

    private void showSignupDialog(String initialUser){
        closeActiveDialog();
        final Dialog d = new Dialog("회원가입", skin);
        final Table c = new Table(skin);
        final TextField u = new TextField(initialUser==null?"":initialUser, skin);
        u.setMessageText("아이디");
        final TextField p = new TextField("", skin); p.setMessageText("비밀번호"); p.setPasswordMode(true); p.setPasswordCharacter('•');
        final TextField pc = new TextField("", skin); pc.setMessageText("비밀번호 확인"); pc.setPasswordMode(true); pc.setPasswordCharacter('•');
        final Label msg = new Label("", skin, "error");
        c.defaults().pad(8).width(400).left();
        c.add(new Label("아이디", skin)).left(); c.row();
        c.add(u).growX(); c.row();
        c.add(new Label("비밀번호", skin)).left(); c.row();
        c.add(p).growX(); c.row();
        c.add(new Label("비밀번호 확인", skin)).left(); c.row();
        c.add(pc).growX(); c.row();
        c.add(msg).left();
        d.getContentTable().add(c).pad(10);

        TextButton ok = new TextButton("가입", skin);
        TextButton cancel = new TextButton("취소", skin);
        d.getButtonTable().add(ok);
        d.getButtonTable().add(cancel);
        d.setModal(true);
        d.setMovable(false);
        d.setResizable(false);
        d.setKeepWithinStage(true);

        ok.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y){
                String us = u.getText().trim(); String ps = p.getText(); String pcs = pc.getText();
                if (us.isEmpty()||ps.length()<4){ msg.setText("아이디/비밀번호를 확인하세요(비번 4자 이상)"); return; }
                if (!ps.equals(pcs)){ msg.setText("비밀번호가 일치하지 않습니다"); return; }
                if (!store.register(us, ps)){ msg.setText("이미 존재하는 사용자입니다"); return; }
                game.playerName = us;
                d.hide(); activeDialog = null;
                showAlert("완료", "가입 완료. 로그인 되었습니다.");
                game.setScreen(new MainMenuScreen(game));
            }
        });
        cancel.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x, float y){ closeActiveDialog(); } });
        openModal(d);
    }

    private void showAlert(String title, String message){
        closeActiveDialog();
        Dialog dlg = new Dialog(title, skin);
        Table content = new Table(skin);
        content.defaults().pad(8).width(360).left();
        content.add(new Label(message, skin)).left().row();
        dlg.getContentTable().add(content).pad(10);
        TextButton ok = new TextButton("확인", skin);
        ok.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x, float y){ closeActiveDialog(); }});
        dlg.getButtonTable().add(ok).right().pad(10);
        dlg.setModal(true);
        dlg.setMovable(false);
        dlg.setResizable(false);
        dlg.setKeepWithinStage(true);
        openModal(dlg);
    }

    private void closeActiveDialog(){
        if (activeDialog != null && activeDialog.hasParent()) activeDialog.hide();
        activeDialog = null;
        if (modalOverlay != null && modalOverlay.hasParent()) modalOverlay.remove();
        modalOverlay = null;
    }

    private void openModal(Dialog dlg){
        // create dim overlay
        modalOverlay = new Table();
        modalOverlay.setFillParent(true);
        modalOverlay.setBackground(skin.getDrawable("overlay"));
        modalOverlay.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        stage.addActor(modalOverlay);

        activeDialog = dlg;
        dlg.show(stage);
        dlg.toFront();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)){
            if (btnLoginRef != null){
                InputEvent down = new InputEvent(); down.setType(InputEvent.Type.touchDown); btnLoginRef.fire(down);
                InputEvent up = new InputEvent(); up.setType(InputEvent.Type.touchUp); btnLoginRef.fire(up);
            }
        }
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { if (stage!=null) stage.getViewport().update(width,height,true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { if (stage!=null) stage.dispose(); if (skin!=null) skin.dispose(); }
}
