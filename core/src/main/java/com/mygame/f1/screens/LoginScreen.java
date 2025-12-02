package com.mygame.f1.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygame.f1.Main;
import com.mygame.f1.data.UserStore;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class LoginScreen implements Screen {
    private final Main game;
    private final UserStore store = new UserStore();
    private Stage stage;
    private Skin skin;
    private TextButton btnLoginRef;
    private Dialog activeDialog;
    private Table modalOverlay;

    private Texture logoTex;
    private Texture stripeTex;
    private Texture checkerTex;
    private Texture gradientTex;

    public LoginScreen(Main game) { this.game = game; }

    @Override
    public void show() {
        long t0 = System.nanoTime();
        skin = Main.getSharedSkin();
        ensureHighlightBorder();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // 배경: 가장 어두운 톤(완전 검정)에 그라디언트 없이 깔끔하게 유지
        gradientTex = makeGradient();
        Image bg = new Image(gradientTex);
        bg.setFillParent(true);
        stage.addActor(bg);

        stripeTex = makeStripeTexture();
        TiledDrawable stripeTile = new TiledDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(stripeTex));
        stripeTile.setMinWidth(52);
        stripeTile.setMinHeight(52);
        Image stripe = new Image(stripeTile);
        stripe.setColor(1f, 1f, 1f, 0.05f);
        stripe.setFillParent(true);
        stage.addActor(stripe);

        checkerTex = makeCheckerTexture();
        Image checker = new Image(checkerTex);
        checker.setColor(1f,1f,1f,0.05f);
        checker.setScaling(Scaling.stretch);
        checker.setSize(256, 256);
        checker.setPosition(stage.getViewport().getWorldWidth() - 276, stage.getViewport().getWorldHeight() - 276);
        stage.addActor(checker);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Table column = new Table();
        column.defaults().pad(16);
        root.add(column).expand().fill();

        try { logoTex = new Texture(Gdx.files.internal("ui/login/logo.png")); } catch (Exception ignored) {}
        if (logoTex != null) {
            Image logo = new Image(logoTex);
            logo.setScaling(Scaling.fit);
            column.add(logo).width(750).height(270).padBottom(32).row(); // 1.5배 확대
        }

        // 한글 스타일: capitolcity 폰트 + 흰색 텍스트
        Label.LabelStyle krLabel = new Label.LabelStyle(skin.getFont("kr-font"), Color.WHITE);
        TextButton.TextButtonStyle krBtn = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
        krBtn.font = skin.getFont("kr-font");
        krBtn.fontColor = Color.WHITE;
        TextField.TextFieldStyle krField = new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class));
        krField.font = skin.getFont("kr-font");
        krField.fontColor = Color.WHITE;
        krField.messageFont = skin.getFont("kr-font");
        krField.messageFontColor = new Color(0.82f,0.82f,0.85f,1f); // placeholder 대비 강화

        // 버튼 스타일: 기본 회색, 로그인은 더 진한 회색 + 빨간 테두리
        TextButton.TextButtonStyle loginStyle = new TextButton.TextButtonStyle(krBtn);
        // 버튼 배경: #FF0000, 둥근 모서리
        loginStyle.up = makeButtonBg(Color.valueOf("ff0000"), Color.valueOf("ff0000"));
        loginStyle.over = makeButtonBg(new Color(1f,0f,0f,0.85f), Color.valueOf("ff0000")); // hover: 약간 투명
        loginStyle.down = makeButtonBg(new Color(1f,0f,0f,0.75f), Color.valueOf("ff0000")); // 눌림: 더 어둡게
        loginStyle.checked = loginStyle.down;
        loginStyle.checkedOver = loginStyle.over;
        loginStyle.fontColor = Color.WHITE;
        loginStyle.checkedFontColor = Color.WHITE;

        TextButton.TextButtonStyle grayStyle = new TextButton.TextButtonStyle(krBtn);
        grayStyle.up = makeButtonBg(new Color(0.20f,0.20f,0.20f,1f), new Color(0.28f,0.28f,0.28f,1f));
        grayStyle.over = makeButtonBg(new Color(0.24f,0.24f,0.24f,1f), new Color(0.32f,0.32f,0.32f,1f));
        grayStyle.down = makeButtonBg(new Color(0.16f,0.16f,0.16f,1f), new Color(0.28f,0.28f,0.28f,1f));
        grayStyle.fontColor = Color.WHITE;

        Table cardOuter = new Table();
        cardOuter.setBackground(buildOutline());
        cardOuter.pad(3);
        Table card = new Table();
        card.setBackground(skin.getDrawable("panel-dark-round")); // 둥근 모서리 카드
        card.defaults().pad(12).growX();
        card.pad(24);

        Table header = new Table();
        Image flagIcon = makeIconFromSkin("icon-flag", 24, 24);
        Label hdr = new Label(" 레이서 로그인", krLabel);
        header.add(flagIcon).size(24).padRight(8);
        header.add(hdr).left();
        header.left();
        card.add(header).growX().left().padBottom(16).row();

        // 사용자명 입력영역: 라벨 + 아이콘 + 입력필드 (배경/테두리 대비 확보)
        Label userLabel = new Label("ID", krLabel);
        card.add(userLabel).left().padBottom(4).row();
        TextField tfUser = new TextField("", krField);
        tfUser.setMessageText(" 레이서명을 입력하세요");
        Table userRow = new Table();
        userRow.setBackground(skin.getDrawable("slot-bg-border")); // 어두운 배경 + 검은 테두리
        userRow.pad(8);
        addHoverAndFocusBorder(userRow, tfUser);
        Image userIcon = makeIconFromSkin("icon-user", 20, 20);
        userRow.add(userIcon).size(20).padRight(8);
        userRow.add(tfUser).growX();
        card.add(userRow).growX().padBottom(12).row();

        Label passLabel = new Label("비밀번호", krLabel);
        card.add(passLabel).left().padBottom(4).row();
        TextField tfPass = new TextField("", krField);
        tfPass.setMessageText(" 비밀번호를 입력하세요");
        tfPass.setPasswordMode(true);
        tfPass.setPasswordCharacter('*');
        Table passRow = new Table();
        passRow.setBackground(skin.getDrawable("slot-bg-border"));
        passRow.pad(8);
        addHoverAndFocusBorder(passRow, tfPass);
        Image lockIcon = makeIconFromSkin("icon-lock", 20, 20);
        passRow.add(lockIcon).size(20).padRight(8);
        passRow.add(tfPass).growX();
        card.add(passRow).growX().padBottom(16).row();

        TextButton btnLogin = new TextButton("게임 시작", loginStyle);
        // 계정 만들기: 박스 없이 텍스트만 클릭되도록 TextButtonTextOnly 스타일
        TextButton.TextButtonStyle linkStyle = new TextButton.TextButtonStyle();
        linkStyle.font = skin.getFont("kr-font");
        linkStyle.fontColor = new Color(0.9f,0.9f,0.9f,1f);
        linkStyle.overFontColor = Color.WHITE;
        TextButton btnSignup = new TextButton("계정 만들기", linkStyle);
        btnLoginRef = btnLogin;

        btnLogin.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y){
                String u=tfUser.getText().trim();
                String p=tfPass.getText();
                if (store.verify(u,p)){
                    game.playerName = u;
                    game.setScreen(new MainMenuScreen(game));
                } else {
                    showAlert("Login Failed", "Check username or password.");
                }
            }
        });
        btnSignup.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y){
                showSignupDialog(tfUser.getText().trim());
            }
        });

        card.add(btnLogin).height(52).padBottom(12).row();
        Table links = new Table();
        links.add(btnSignup).left();
        card.add(links).growX().row();

        cardOuter.add(card).grow();
        column.add(cardOuter).maxWidth(700).growX().row();

        Label ver = new Label("VERSION 1.0.2", krLabel);
        ver.setColor(0.5f,0.5f,0.5f,1f);
        ver.setAlignment(Align.center);
        column.add(ver).padTop(12).center();

        Gdx.app.log("PERF", String.format("LoginScreen.show: %.2f ms", (System.nanoTime() - t0) / 1_000_000f));
    }

    private Image makeIconFromSkin(String drawableName, float w, float h) {
        Drawable d = skin.has(drawableName, Drawable.class) ? skin.getDrawable(drawableName) : skin.getDrawable("white");
        Image img = new Image(d);
        img.setScaling(Scaling.fit);
        img.setSize(w, h);
        return img;
    }

    private Texture makeGradient() {
        // 완전 검정 단색 배경 텍스처 생성 (텍스트 대비 확보용)
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, 1f);
        pm.drawPixel(0, 0);
        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private Texture makeStripeTexture() {
        Pixmap pm = new Pixmap(52, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0f,0f,0f,0f);
        pm.fill();
        pm.setColor(0.94f,0.27f,0.27f,0.12f);
        for (int x = 50; x < 52; x++) pm.drawPixel(x,0);
        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private Texture makeCheckerTexture() {
        Pixmap pm = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pm.setColor(1f,1f,1f,1f);
        for (int y=0;y<8;y++) {
            for (int x=0;x<8;x++) {
                if ((x+y)%2==0) pm.drawPixel(x,y); else pm.setColor(1f,1f,1f,0f);
            }
        }
        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    // 단색 + 테두리 버튼 배경을 만드는 유틸
    // 모서리가 둥근 버튼 배경 생성
    private Drawable makeButtonBg(Color fill, Color border){
        int radius = 3;
        int size = 12;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(0,0,0,0); pm.fill();
        // 채우기
        pm.setColor(fill);
        pm.fillRectangle(radius, 0, size - 2*radius, size);
        pm.fillRectangle(0, radius, size, size - 2*radius);
        pm.fillCircle(radius, radius, radius);
        pm.fillCircle(size - radius - 1, radius, radius);
        pm.fillCircle(radius, size - radius - 1, radius);
        pm.fillCircle(size - radius - 1, size - radius - 1, radius);
        // 테두리
        pm.setColor(border);
        pm.drawCircle(radius, radius, radius);
        pm.drawCircle(size - radius - 1, radius, radius);
        pm.drawCircle(radius, size - radius - 1, radius);
        pm.drawCircle(size - radius - 1, size - radius - 1, radius);
        pm.drawRectangle(radius, 0, size - 2*radius, size-1);
        pm.drawRectangle(0, radius, size-1, size - 2*radius);
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        NinePatch np = new NinePatch(new TextureRegion(tex), radius, radius, radius, radius);
        return new NinePatchDrawable(np);
    }

    // 입력 행 hover/포커스 시 테두리를 빨간색으로, 해제 시 기본 테두리로 복귀
    private void addHoverAndFocusBorder(final Table row, final TextField field){
        final boolean[] hover = {false};
        final boolean[] focus = {false};

        row.addListener(new InputListener(){
            @Override public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor){
                hover[0] = true;
                row.setBackground(skin.getDrawable("slot-bg-border-hover")); // hover 시 테두리만 빨간색(#FF0000)
            }
            @Override public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor){
                hover[0] = false;
                if (!focus[0]) row.setBackground(skin.getDrawable("slot-bg-border"));
            }
        });

        field.addListener(new FocusListener(){
            @Override public void keyboardFocusChanged(FocusEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor, boolean focused){
                focus[0] = focused;
                if (focused) row.setBackground(skin.getDrawable("slot-bg-border-focus"));
                else if (!hover[0]) row.setBackground(skin.getDrawable("slot-bg-border"));
            }
            @Override public void scrollFocusChanged(FocusEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor, boolean focused){
                focus[0] = focused;
                if (focused) row.setBackground(skin.getDrawable("slot-bg-border-focus"));
                else if (!hover[0]) row.setBackground(skin.getDrawable("slot-bg-border"));
            }
        });
    }

    private void showSignupDialog(String initialUser){
        closeActiveDialog();
        ensureHighlightBorder();

        // Dialog 스타일을 로그인 카드와 동일하게 구성
        Window.WindowStyle ws = new Window.WindowStyle(skin.get(Window.WindowStyle.class));
        ws.titleFont = skin.getFont("kr-font");
        ws.titleFontColor = Color.WHITE;
        ws.background = skin.getDrawable("panel-dark-round");
        final Dialog d = new Dialog("", ws); // 제목 텍스트 제거

        // 입력/라벨 스타일
        TextField.TextFieldStyle krField = new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class));
        krField.font = skin.getFont("kr-font");
        krField.fontColor = Color.WHITE;
        krField.messageFont = skin.getFont("kr-font");
        krField.messageFontColor = new Color(0.7f,0.7f,0.75f,1f);
        Label.LabelStyle krLabel = new Label.LabelStyle(skin.getFont("kr-font"), Color.WHITE);

        // 버튼 스타일: 회색 톤 (빨간 테두리 없음)
        TextButton.TextButtonStyle primaryBtn = new TextButton.TextButtonStyle();
        primaryBtn.font = skin.getFont("kr-font");
        primaryBtn.fontColor = Color.WHITE;
        primaryBtn.up = makeButtonBg(new Color(0.20f,0.20f,0.20f,1f), new Color(0.32f,0.32f,0.32f,1f));
        primaryBtn.over = makeButtonBg(new Color(0.24f,0.24f,0.24f,1f), new Color(0.36f,0.36f,0.36f,1f));
        primaryBtn.down = makeButtonBg(new Color(0.16f,0.16f,0.16f,1f), new Color(0.28f,0.28f,0.28f,1f));

        TextButton.TextButtonStyle secondaryBtn = new TextButton.TextButtonStyle(primaryBtn);

        // 내용 카드 (highlight-border + panel-dark-round)
        final Table form = new Table(skin);
        form.defaults().pad(10).width(420).left();
        form.add(new Label("사용자명", krLabel)).left(); form.row();
        final TextField u = new TextField(initialUser==null?"":initialUser, krField); u.setMessageText("아이디를 입력해주세요");
        Table uRow = new Table(); uRow.setBackground(skin.getDrawable("slot-bg-border")); uRow.pad(10); addHoverAndFocusBorder(uRow, u);
        uRow.add(u).growX();
        form.add(uRow).growX(); form.row();

        form.add(new Label("비밀번호", krLabel)).left(); form.row();
        final TextField p = new TextField("", krField); p.setMessageText("비밀번호 입력"); p.setPasswordMode(true); p.setPasswordCharacter('*');
        Table pRow = new Table(); pRow.setBackground(skin.getDrawable("slot-bg-border")); pRow.pad(10); addHoverAndFocusBorder(pRow, p);
        pRow.add(p).growX();
        form.add(pRow).growX(); form.row();

        form.add(new Label("비밀번호 확인", krLabel)).left(); form.row();
        final TextField pc = new TextField("", krField); pc.setMessageText("비밀번호 확인"); pc.setPasswordMode(true); pc.setPasswordCharacter('*');
        Table pcRow = new Table(); pcRow.setBackground(skin.getDrawable("slot-bg-border")); pcRow.pad(10); addHoverAndFocusBorder(pcRow, pc);
        pcRow.add(pc).growX();
        form.add(pcRow).growX(); form.row();

        final Label msg = new Label("", krLabel);
        form.add(msg).left();

        // 버튼 추가
        TextButton ok = new TextButton("확인", primaryBtn);
        TextButton cancel = new TextButton("취소", secondaryBtn);
        Table btnRow = new Table();
        btnRow.defaults().pad(8).height(48).width(160);
        btnRow.add(ok);
        btnRow.add(cancel);

        // 본문 카드
        Table inner = new Table();
        inner.setBackground(skin.getDrawable("panel-dark-round"));
        inner.defaults().pad(8).growX();
        inner.pad(16);
        inner.add(form).grow().row();
        inner.add(btnRow).growX();

        // 외곽 테두리(전체 감싸기)
        Table outer = new Table();
        Drawable highlight = skin.has("highlight-border", Drawable.class) ? skin.getDrawable("highlight-border") : buildOutline();
        if (!skin.has("highlight-border", Drawable.class)) {
            skin.add("highlight-border", highlight);
        }
        outer.setBackground(highlight);
        outer.pad(3);
        outer.add(inner).grow();

        d.getContentTable().clear();
        d.getContentTable().add(outer).grow().pad(8);

        d.setModal(true);
        d.setMovable(false);
        d.setResizable(false);
        d.setKeepWithinStage(true);

        ok.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y){
                String us = u.getText().trim(); String ps = p.getText(); String pcs = pc.getText();
                if (us.isEmpty()||ps.length()<4){ msg.setText("4자 이상 입력"); return; }
                if (!ps.equals(pcs)){ msg.setText("비밀번호 불일치"); return; }
                if (!store.register(us, ps)){ msg.setText("이미 존재하는 계정"); return; }
                game.playerName = us;
                d.hide(); activeDialog = null;
                showAlert("완료", "회원 가입. 로그인되었습니다.");
                game.setScreen(new MainMenuScreen(game));
            }
        });
        cancel.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x, float y){ closeActiveDialog(); } });
        openModal(d);
    }

    private void showAlert(String title, String message){
        closeActiveDialog();
        Dialog dlg = new Dialog(title, skin);
        TextButton.TextButtonStyle krBtnStyle = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
        krBtnStyle.font = skin.getFont("kr-font");
        krBtnStyle.fontColor = Color.WHITE;
        Label.LabelStyle krLabel = new Label.LabelStyle(skin.getFont("kr-font"), Color.WHITE);
        Table content = new Table(skin);
        content.defaults().pad(8).width(360).left();
        content.add(new Label(message, krLabel)).left().row();
        dlg.getContentTable().add(content).pad(10);
        TextButton ok = new TextButton("확인", krBtnStyle);
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
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { if (stage!=null) stage.getViewport().update(width,height,true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        // skin은 Main이 관리하므로 dispose 하지 않음
        if (logoTex != null) logoTex.dispose();
        if (stripeTex != null) stripeTex.dispose();
        if (checkerTex != null) checkerTex.dispose();
        if (gradientTex != null) gradientTex.dispose();
    }
    // dispose는 중복 정의를 피하고 상단 정의(gradientTex 포함)만 유지

    private void ensureHighlightBorder() {
        if (skin == null) return;
        if (skin.has("highlight-border", Drawable.class)) return;
        skin.add("highlight-border", buildOutline());
    }

    private NinePatchDrawable buildOutline() {
        int radius = 6;
        int thickness = 2;
        int size = 16;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(0, 0, 0, 0);
        pm.fill();
        pm.setColor(Color.valueOf("ff0000"));
        for (int t = 0; t < thickness; t++) {
            pm.drawCircle(radius, radius, radius - t);
            pm.drawCircle(size - radius - 1, radius, radius - t);
            pm.drawCircle(radius, size - radius - 1, radius - t);
            pm.drawCircle(size - radius - 1, size - radius - 1, radius - t);
            pm.drawRectangle(radius, t, size - 2 * radius, size - 1 - 2 * t);
            pm.drawRectangle(t, radius, size - 1 - 2 * t, size - 2 * radius);
        }
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        NinePatch np = new NinePatch(new TextureRegion(tex), radius, radius, radius, radius);
        return new NinePatchDrawable(np);
    }

}
