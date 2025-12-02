package com.mygame.f1.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.Gdx;

/**
 * 공용 Skin 생성기. 가능한 atlas/game_ui.atlas의 리전을 우선 사용하고,
 * 없으면 런타임으로 생성합니다.
 *
 * 성능 최적화: Main.java에서 한 번만 createDefaultSkin() 호출하여 싱글톤으로 사용.
 */
public final class SkinFactory {
    private SkinFactory() {}

    private static BitmapFont cachedDefaultFont;
    private static BitmapFont cachedTitleFont;
    private static BitmapFont cachedKrFont;

    public static final class Palette {
        public static final Color BG = Color.BLACK;
        public static final Color PANEL = rgba(0x0f1118ff);
        public static final Color BTN_NORMAL = rgba(0x222838ff);
        public static final Color BTN_OVER = rgba(0x2A3550ff);
        public static final Color BTN_DOWN = rgba(0x334166ff);
        public static final Color BTN_CHECKED = rgba(0x3D7BFFFF);
        public static final Color TEXT = Color.WHITE;
        public static final Color TEXT_MUTED = rgba(0xB0B7C3ff);
        public static final Color ACCENT = rgba(0x3D7BFFFF);
        public static final Color NEON_RED = rgba(0xFF0000ff);
        public static final Color NEON_BLUE = rgba(0x00FFFFFF);
        public static final Color PANEL_DARK = rgba(0x1A1A1Aff);
        public static final Color SLOT = rgba(0x0b0b0bff);
        public static final Color SLOT_BORDER = rgba(0x2c2c2cff);
        private static Color rgba(int hex) {
            float a = (hex & 0xFF) / 255f;
            float b = ((hex >> 8) & 0xFF) / 255f;
            float g = ((hex >> 16) & 0xFF) / 255f;
            float r = ((hex >> 24) & 0xFF) / 255f;
            return new Color(r, g, b, a);
        }
    }

    public static Skin createDefaultSkin() {
        Skin skin = new Skin();

        // base 1x1 white
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE); pm.fill();
        Texture white = new Texture(pm);
        white.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        TextureRegion whiteRegion = new TextureRegion(white);
        skin.add("white", white);
        skin.add("white-region", whiteRegion);

        // atlas 등록 (있을 때만)
        try {
            TextureAtlas atlas = com.mygame.f1.Main.assetManager.get("atlas/game_ui.atlas", TextureAtlas.class);
            skin.addRegions(atlas);
            addAlias(skin, atlas, "icon-single", "single-play");
            addAlias(skin, atlas, "icon-multi", "multi-play");
            addAlias(skin, atlas, "icon-exit", "exit");
            addAlias(skin, atlas, "icon-settings", "setting");
            addAlias(skin, atlas, "icon-start", "start-play");
            addAlias(skin, atlas, "icon-flag", "racing-flag");
            addAlias(skin, atlas, "icon-track", "track");
            addAlias(skin, atlas, "icon-car", "select-car");
            addAlias(skin, atlas, "icon-lap", "lap_time_bg_best"); // fallback to best lap bg if no lap icon
            addAlias(skin, atlas, "icon-lock", "lock");
            addAlias(skin, atlas, "icon-send", "send");
            addAlias(skin, atlas, "icon-chat", "chat-bubbles");
            addAlias(skin, atlas, "icon-ready-on", "on-ready");
            addAlias(skin, atlas, "icon-ready-off", "ready-off");
            addAlias(skin, atlas, "icon-user", "single-play");
        } catch (Exception ignored) {}

        // fonts - 캐싱으로 중복 생성 방지
        if (cachedDefaultFont == null) {
            cachedDefaultFont = generateFont("fonts/capitolcity.ttf", 18);
            if (cachedDefaultFont == null) cachedDefaultFont = new BitmapFont();
        }
        if (cachedTitleFont == null) {
            cachedTitleFont = generateFont("fonts/capitolcity.ttf", 28);
            if (cachedTitleFont == null) cachedTitleFont = cachedDefaultFont;
        }
        if (cachedKrFont == null) {
            cachedKrFont = generateFont("fonts/NotoSansKR-Regular.ttf", 20);
            if (cachedKrFont == null) cachedKrFont = cachedDefaultFont;
        }
        skin.add("default-font", cachedDefaultFont, BitmapFont.class);
        skin.add("title-font", cachedTitleFont, BitmapFont.class);
        skin.add("kr-font", cachedKrFont, BitmapFont.class);

        java.util.function.Function<Color, Drawable> tile = (c) -> new TiledDrawable(whiteRegion).tint(c);

        // Label styles
        Label.LabelStyle label = new Label.LabelStyle();
        label.font = cachedDefaultFont; label.fontColor = Palette.TEXT;
        skin.add("default", label);
        Label.LabelStyle labelKr = new Label.LabelStyle();
        labelKr.font = cachedKrFont; labelKr.fontColor = Palette.TEXT;
        skin.add("kr", labelKr);
        Label.LabelStyle title = new Label.LabelStyle();
        title.font = cachedTitleFont; title.fontColor = Palette.TEXT;
        skin.add("title", title);

        // TextButton
        TextButton.TextButtonStyle btn = new TextButton.TextButtonStyle();
        btn.font = cachedDefaultFont;
        btn.up = tile.apply(Palette.BTN_NORMAL);
        btn.over = tile.apply(Palette.BTN_OVER);
        btn.down = tile.apply(Palette.BTN_DOWN);
        btn.checked = tile.apply(Palette.BTN_CHECKED.cpy().mul(1f,1f,1f,0.7f));
        btn.fontColor = Palette.TEXT;
        skin.add("default", btn);

        // menu styles reuse default but with image slots
        ImageTextButton.ImageTextButtonStyle menuMain = new ImageTextButton.ImageTextButtonStyle(btn);
        skin.add("menu-main-icon", menuMain);
        ImageTextButton.ImageTextButtonStyle menuSub = new ImageTextButton.ImageTextButtonStyle(btn);
        skin.add("menu-sub-icon", menuSub);
        ImageTextButton.ImageTextButtonStyle menuExit = new ImageTextButton.ImageTextButtonStyle(btn);
        skin.add("menu-exit-icon", menuExit);

        // TextField
        TextField.TextFieldStyle tfs = new TextField.TextFieldStyle();
        tfs.font = cachedDefaultFont; tfs.fontColor = Palette.TEXT;
        tfs.cursor = tile.apply(Palette.TEXT);
        tfs.selection = tile.apply(Palette.ACCENT.cpy().mul(1f,1f,1f,0.5f));
        tfs.background = tile.apply(Palette.SLOT);
        skin.add("default", tfs);

        // ScrollPane
        ScrollPane.ScrollPaneStyle sps = new ScrollPane.ScrollPaneStyle();
        sps.background = tile.apply(Palette.PANEL);
        sps.vScroll = tile.apply(Palette.BTN_NORMAL);
        sps.hScroll = tile.apply(Palette.BTN_NORMAL);
        sps.vScrollKnob = tile.apply(Palette.BTN_OVER);
        sps.hScrollKnob = tile.apply(Palette.BTN_OVER);
        skin.add("default", sps);

        // CheckBox
        CheckBox.CheckBoxStyle cb = new CheckBox.CheckBoxStyle();
        cb.checkboxOff = tile.apply(Palette.BTN_NORMAL);
        cb.checkboxOn = tile.apply(Palette.ACCENT);
        cb.checkboxOver = tile.apply(Palette.BTN_OVER);
        cb.font = cachedDefaultFont; cb.fontColor = Palette.TEXT;
        skin.add("default", cb);

        // Slider
        Slider.SliderStyle slider = new Slider.SliderStyle();
        slider.background = tile.apply(Palette.BTN_NORMAL);
        slider.knob = tile.apply(Palette.ACCENT);
        slider.knobOver = tile.apply(Palette.BTN_OVER);
        slider.knobDown = tile.apply(Palette.BTN_DOWN);
        slider.knobBefore = tile.apply(Palette.ACCENT.cpy().mul(1f,1f,1f,0.6f));
        skin.add("default-horizontal", slider);

        // ProgressBar
        ProgressBar.ProgressBarStyle barBase = new ProgressBar.ProgressBarStyle();
        barBase.background = tile.apply(Palette.BTN_NORMAL);
        barBase.knob = null;
        barBase.knobBefore = tile.apply(Palette.ACCENT.cpy().mul(1f,1f,1f,0.7f));
        skin.add("default-horizontal", barBase);
        ProgressBar.ProgressBarStyle barBlue = new ProgressBar.ProgressBarStyle(barBase);
        barBlue.knobBefore = tile.apply(new Color(0f, 0.8f, 1f, 0.8f));
        skin.add("stat-blue", barBlue);
        ProgressBar.ProgressBarStyle barYellow = new ProgressBar.ProgressBarStyle(barBase);
        barYellow.knobBefore = tile.apply(new Color(0.95f, 0.75f, 0.15f, 0.9f));
        skin.add("stat-yellow", barYellow);
        ProgressBar.ProgressBarStyle barGreen = new ProgressBar.ProgressBarStyle(barBase);
        barGreen.knobBefore = tile.apply(new Color(0.3f, 0.8f, 0.4f, 0.9f));
        skin.add("stat-green", barGreen);

        // Window
        Window.WindowStyle ws = new Window.WindowStyle();
        ws.titleFont = cachedTitleFont; ws.titleFontColor = Palette.TEXT;
        ws.background = tile.apply(Palette.PANEL);
        skin.add("default", ws);

        // Panels / backgrounds
        skin.add("bg", tile.apply(Palette.BG), Drawable.class);
        skin.add("panel", tile.apply(Palette.PANEL), Drawable.class);
        skin.add("panel-dark-round", rounded(Palette.PANEL_DARK, Palette.PANEL_DARK, 6, 16, 16, 1), Drawable.class);
        skin.add("slot-bg-normal", rounded(Palette.SLOT, Palette.SLOT, 5, 12, 12, 1), Drawable.class);
        skin.add("slot-bg-border", rounded(Palette.SLOT, Palette.SLOT_BORDER, 6, 12, 12, 1), Drawable.class);
        skin.add("slot-bg-border-hover", rounded(Palette.SLOT, Palette.NEON_RED, 6, 12, 12, 1), Drawable.class);
        skin.add("slot-bg-border-focus", rounded(Palette.SLOT, Palette.NEON_RED, 6, 12, 12, 2), Drawable.class);
        skin.add("overlay", tile.apply(new Color(0f,0f,0f,0.65f)), Drawable.class);

        // Chat
        skin.add("chat-border", rounded(Palette.PANEL_DARK, Palette.PANEL_DARK, 6, 16, 16, 1), Drawable.class);
        Color chatBg = Color.valueOf("1e1e1e");
        skin.add("chat-log-bg", rounded(chatBg, Color.BLACK, 3, 12, 12, 1), Drawable.class);
        skin.add("chat-input-bg", rounded(chatBg, Color.BLACK, 5, 12, 12, 1), Drawable.class);

        // highlight / track selected
        Drawable highlightBorder = rounded(new Color(0,0,0,0), Palette.NEON_RED, 6, 8, 8, 1);
        skin.add("highlight-border", highlightBorder, Drawable.class);
        skin.add("track-selected", rounded(Palette.NEON_RED, Palette.NEON_RED, 6, 12, 12, 1), Drawable.class);

        // badges/status
        skin.add("badge-easy", tile.apply(new Color(0.1f, 0.7f, 0.3f, 1f)), Drawable.class);
        skin.add("badge-normal", tile.apply(new Color(0.25f, 0.6f, 1f, 1f)), Drawable.class);
        skin.add("badge-hard", tile.apply(new Color(0.9f, 0.4f, 0.1f, 1f)), Drawable.class);
        skin.add("status-connected", tile.apply(new Color(0f, 0.8f, 0.3f, 1f)), Drawable.class);

        // Send 버튼 전용 배경
        skin.add("send-btn-up", rounded(Palette.SLOT, Palette.SLOT_BORDER, 5, 12, 12, 1), Drawable.class);
        skin.add("send-btn-over", rounded(Color.valueOf("cc0000"), Color.valueOf("ff0000"), 5, 12, 12, 2), Drawable.class);
        skin.add("send-btn-down", rounded(Color.valueOf("990000"), Color.valueOf("ff0000"), 5, 12, 12, 2), Drawable.class);

        // 아이콘: atlas에 없으면 파일에서 로드
        addIcon(skin, "icon-single", "ui/icon/single-play.png");
        addIcon(skin, "icon-multi", "ui/icon/multi-play.png");
        addIcon(skin, "icon-leaderboard", "ui/icon/tropy.png");
        addIcon(skin, "icon-settings", "ui/icon/setting.png");
        addIcon(skin, "icon-exit", "ui/icon/exit.png");
        addIcon(skin, "icon-right-arrow", "ui/icon/right-arrow.png");
        addIcon(skin, "icon-start", "ui/icon/start-play.png");
        addIcon(skin, "icon-flag", "ui/icon/racing-flag.png");
        addIcon(skin, "icon-user", "ui/icon/single-play.png");
        addIcon(skin, "icon-track", "ui/icon/track.png");
        addIcon(skin, "icon-car", "ui/icon/select-car.png");
        addIcon(skin, "icon-lap", "ui/icon/lap-time.png");
        addIcon(skin, "icon-lock", "ui/icon/lock.png");
        addIcon(skin, "icon-bronze", "ui/icon/bronze-medal.png");
        addIcon(skin, "icon-silver", "ui/icon/silver-medal.png");
        addIcon(skin, "icon-gold", "ui/icon/gold-medal.png");
        addIcon(skin, "icon-chat", "ui/icon/chat-bubbles.png");
        addIcon(skin, "icon-send", "ui/icon/send.png");
        addIcon(skin, "icon-ready-on", "ui/icon/on-ready.png");
        addIcon(skin, "icon-ready-off", "ui/icon/ready-off.png");

        // Error label
        Label.LabelStyle error = new Label.LabelStyle();
        error.font = cachedDefaultFont;
        error.fontColor = new Color(0.88f, 0.36f, 0.27f, 1f);
        skin.add("error", error);

        return skin;
    }

    /**
     * 캐시된 폰트 리소스 정리. Main.dispose()에서 호출됨.
     */
    public static void disposeFonts() {
        if (cachedDefaultFont != null) {
            cachedDefaultFont.dispose();
            cachedDefaultFont = null;
        }
        if (cachedTitleFont != null && cachedTitleFont != cachedDefaultFont) {
            cachedTitleFont.dispose();
            cachedTitleFont = null;
        }
        if (cachedKrFont != null && cachedKrFont != cachedDefaultFont) {
            cachedKrFont.dispose();
            cachedKrFont = null;
        }
    }

    private static BitmapFont generateFont(String internalPath, int size) {
        try {
            if (!Gdx.files.internal(internalPath).exists()) return null;
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(internalPath));
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = size;
            p.color = Palette.TEXT;
            StringBuilder chars = new StringBuilder(FreeTypeFontGenerator.DEFAULT_CHARS);
            appendRange(chars, 0xAC00, 0xD7A3);
            appendRange(chars, 0x1100, 0x11FF);
            appendRange(chars, 0x3131, 0x318E);
            chars.append('?');
            p.characters = chars.toString();
            BitmapFont f = gen.generateFont(p);
            gen.dispose();
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    private static void appendRange(StringBuilder sb, int fromInclusive, int toInclusive) {
        for (int cp = fromInclusive; cp <= toInclusive; cp++) {
            sb.append((char) cp);
        }
    }

    private static Drawable bordered(Color fill, Color border){
        return rounded(fill, border, 3, 8, 8, 1);
    }

    private static Drawable rounded(Color fill, Color border, int radius, int w, int h, int thickness){
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(0,0,0,0); pm.fill();
        pm.setColor(fill);
        pm.fillRectangle(radius,0,w-2*radius,h);
        pm.fillRectangle(0,radius,w,h-2*radius);
        pm.fillCircle(radius,radius,radius);
        pm.fillCircle(w-radius-1,radius,radius);
        pm.fillCircle(radius,h-radius-1,radius);
        pm.fillCircle(w-radius-1,h-radius-1,radius);
        pm.setColor(border);
        for (int t=0; t<thickness; t++){
            pm.drawCircle(radius, radius, radius-t);
            pm.drawCircle(w-radius-1, radius, radius-t);
            pm.drawCircle(radius, h-radius-1, radius-t);
            pm.drawCircle(w-radius-1, h-radius-1, radius-t);
            pm.drawRectangle(radius, t, w-2*radius, h-1-2*t);
            pm.drawRectangle(t, radius, w-1-2*t, h-2*radius);
        }
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        NinePatch np = new NinePatch(new TextureRegion(tex), radius, radius, radius, radius);
        return new NinePatchDrawable(np);
    }

    private static void addIcon(Skin skin, String name, String internalPath) {
        if (skin.has(name, Drawable.class)) return; // atlas에 이미 있을 수 있음
        try {
            if (!Gdx.files.internal(internalPath).exists()) return;
            Texture tex = new Texture(Gdx.files.internal(internalPath));
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            skin.add(name, new TextureRegionDrawable(new TextureRegion(tex)), Drawable.class);
        } catch (Exception ignored) {}
    }

    private static void addAlias(Skin skin, TextureAtlas atlas, String alias, String regionName) {
        if (skin.has(alias, Drawable.class)) return;
        if (atlas == null) return;
        TextureAtlas.AtlasRegion r = atlas.findRegion(regionName);
        if (r != null) {
            skin.add(alias, new TextureRegionDrawable(r), Drawable.class);
        }
    }
}
