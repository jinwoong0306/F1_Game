package com.mygame.f1.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygame.f1.Main;

public class MainMenuScreen implements Screen {
    private final Main game;
    private Stage stage;
    private Skin skin;
    private Texture logoTex;
    private Texture stripeTex;
    private Texture gridTex;
    private Texture gradientTex;
    private boolean ownsLogo = false;

    public MainMenuScreen(Main game) { this.game = game; }

    @Override
    public void show() {
        long t0 = System.nanoTime();
        skin = Main.getSharedSkin();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // 배경: 단색 검정으로 통일
        gradientTex = makeGradient();
        Image bg = new Image(gradientTex);
        bg.setFillParent(true);
        stage.addActor(bg);

        // 불필요한 배경 패턴 제거하여 순수 검정 유지

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Table card = new Table();
        card.setBackground(skin.getDrawable("bg")); // 메뉴 카드: 검정, 테두리 없음
        card.defaults().pad(12).growX();
        card.pad(24);

        Table column = new Table();
        column.defaults().pad(12).growX();
        card.add(column).grow();

        // logo
        try {
            if (Main.assetManager.isLoaded("ui/login/logo.png", Texture.class)) {
                logoTex = Main.assetManager.get("ui/login/logo.png", Texture.class);
            } else {
                logoTex = new Texture(Gdx.files.internal("ui/login/logo.png"));
                ownsLogo = true;
            }
        } catch (Exception ignored) {}
        if (logoTex != null) {
            Image logo = new Image(logoTex);
            logo.setScaling(Scaling.fit);
            column.add(logo).width(500 * 1.5f).height(180 * 1.5f).padBottom(32).row();
        }

        // 버튼 (텍스트 흰색, 아이콘 원본 색상 그대로)
        ImageTextButton btnSingle = styledMain("SINGLE PLAY", "icon-single");
        ImageTextButton btnMulti = styledMain("MULTI PLAY", "icon-multi");
        ImageTextButton btnSettings = styledSub("SETTING", "icon-settings");
        ImageTextButton btnExit = styledExit("EXIT", "icon-exit");

        btnSingle.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x, float y){ game.setScreen(new SinglePlayScreen(game)); }});
        btnMulti.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x, float y){ game.setScreen(new MultiplayerPlaceholderScreen(game)); }});
        btnSettings.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x, float y){ game.setScreen(new SettingsScreen(game)); }});
        btnExit.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x, float y){ Gdx.app.exit(); }});

        float btnWidth = 440f;
        float btnHeight = 70f;
        column.add(btnSingle).width(btnWidth).height(btnHeight).padBottom(12).row();
        column.add(btnMulti).width(btnWidth).height(btnHeight).padBottom(12).row();
        column.add(btnSettings).width(btnWidth).height(btnHeight).padBottom(12).row();
        column.add(btnExit).width(btnWidth).height(64f).padBottom(18).row();

        // 카드 래핑: 테두리 없이 그대로 배치
        root.add(card).maxWidth(900).growX().expand().fill();

        // 빌드 버전: 전체 화면 오른쪽 아래
        Label footer = new Label("BUILD 2025.11.25", skin);
        footer.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
        Table footerTable = new Table();
        footerTable.setFillParent(true);
        footerTable.bottom().left();
        footerTable.add(footer).pad(10);
        stage.addActor(footerTable);

        Gdx.app.log("PERF", String.format("MainMenuScreen.show: %.2f ms", (System.nanoTime() - t0) / 1_000_000f));
    }

    private ImageTextButton styledMain(String text, String iconName) {
        ImageTextButton.ImageTextButtonStyle base = new ImageTextButton.ImageTextButtonStyle(skin.get("menu-main-icon", ImageTextButton.ImageTextButtonStyle.class));
        base.fontColor = Color.WHITE;
        Drawable up = makeMenuBg(false);
        Drawable over = makeMenuBg(true);
        base.up = up; base.over = over; base.down = over; base.checked = over;
        base.imageUp = skin.getDrawable(iconName);
        base.imageOver = base.imageUp;
        base.imageDown = base.imageUp;
        base.imageChecked = base.imageUp;
        ImageTextButton b = new ImageTextButton(text, base);
        b.getLabel().setAlignment(Align.left);
        b.getImageCell().size(32, 32).padRight(12);
        b.pad(20, 24, 20, 24);
        return b;
    }

    private ImageTextButton styledSub(String text, String iconName) {
        ImageTextButton.ImageTextButtonStyle base = new ImageTextButton.ImageTextButtonStyle(skin.get("menu-sub-icon", ImageTextButton.ImageTextButtonStyle.class));
        base.fontColor = Color.WHITE;
        Drawable up = makeMenuBg(false);
        Drawable over = makeMenuBg(true);
        base.up = up; base.over = over; base.down = over; base.checked = over;
        base.imageUp = skin.getDrawable(iconName);
        base.imageOver = base.imageUp;
        base.imageDown = base.imageUp;
        base.imageChecked = base.imageUp;
        ImageTextButton b = new ImageTextButton(text, base);
        b.getLabel().setAlignment(Align.left);
        b.getImageCell().size(32, 32).padRight(12);
        b.pad(20, 24, 20, 24);
        return b;
    }

    private ImageTextButton styledExit(String text, String iconName) {
        ImageTextButton.ImageTextButtonStyle base = new ImageTextButton.ImageTextButtonStyle(skin.get("menu-exit-icon", ImageTextButton.ImageTextButtonStyle.class));
        base.fontColor = Color.WHITE;
        Drawable up = makeMenuBg(false);
        Drawable over = makeMenuBg(true);
        base.up = up; base.over = over; base.down = over; base.checked = over;
        base.imageUp = skin.getDrawable(iconName);
        base.imageOver = base.imageUp;
        base.imageDown = base.imageUp;
        base.imageChecked = base.imageUp;
        ImageTextButton b = new ImageTextButton(text, base);
        b.getLabel().setAlignment(Align.center);
        b.getImageCell().size(26, 26).padRight(12);
        b.pad(16, 24, 16, 24);
        return b;
    }

    private Texture makeGradient() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, 1f);
        pm.drawPixel(0, 0);
        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private Texture makeStripeTexture() {
        Pixmap pm = new Pixmap(60, 60, Pixmap.Format.RGBA8888);
        pm.setColor(0f,0f,0f,0f); pm.fill();
        pm.setColor(0.94f, 0.27f, 0.27f, 0.12f); // red strip low alpha
        for (int x = 50; x < 52; x++) {
            for (int y = 0; y < 60; y++) pm.drawPixel(x, y);
        }
        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private Texture makeGridTexture(int w, int h) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(1f,1f,1f,0f); pm.fill();
        // red grid lines (rgba(239,68,68,0.3))
        pm.setColor(0.937f, 0.267f, 0.267f, 0.3f);
        for (int x = 0; x < w; x++) pm.drawPixel(x, 0); // top line
        for (int y = 0; y < h; y++) pm.drawPixel(0, y); // left line
        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private Drawable makeMenuBg(boolean hover) {
        Pixmap pm = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pm.setColor(Color.valueOf("696969")); pm.fill(); // 기본 배경
        if (hover) {
            Color overlay = Color.valueOf("A9A9A9");
            pm.setColor(overlay.r, overlay.g, overlay.b, 0.35f); // 밝은 회색 투명도
            pm.fill();
        }
        pm.setColor(Color.valueOf("696969"));
        pm.drawRectangle(0,0,8,8);
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        NinePatch np = new NinePatch(new com.badlogic.gdx.graphics.g2d.TextureRegion(tex),1,1,1,1);
        return new NinePatchDrawable(np);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { Gdx.app.exit(); return; }
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { if (stage!=null) stage.getViewport().update(w,h,true);}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (stage!=null) stage.dispose();
        // skin은 Main이 관리하므로 dispose 하지 않음
        // 로고는 AssetManager에서 로드했을 가능성이 있어 소유한 경우만 해제
        if (ownsLogo && logoTex!=null) logoTex.dispose();
        if (stripeTex!=null) stripeTex.dispose();
        if (gridTex!=null) gridTex.dispose();
        if (gradientTex!=null) gradientTex.dispose();
    }
}
