package com.mygame.f1.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;

public final class SkinFactory {
    private SkinFactory() {}

    public static final class Palette {
        public static final Color BG = rgba(0x12141Aff);
        public static final Color PANEL = rgba(0x1C1F2Aff);
        public static final Color BTN_NORMAL = rgba(0x222838ff);
        public static final Color BTN_OVER = rgba(0x2A3550ff);
        public static final Color BTN_DOWN = rgba(0x334166ff);
        public static final Color BTN_CHECKED = rgba(0x3D7BFFFF);
        public static final Color TEXT = rgba(0xECEFF4ff);
        public static final Color TEXT_MUTED = rgba(0xB0B7C3ff);
        public static final Color ACCENT = rgba(0x3D7BFFFF);
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

        // base 1x1 white texture
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        Texture white = new Texture(pm);
        pm.dispose();
        skin.add("white", white);
        TextureRegion whiteRegion = new TextureRegion(white);
        skin.add("white-region", whiteRegion);

        // fonts
        BitmapFont font = generateFont("fonts/NotoSansKR-Regular.ttf", 18);
        BitmapFont titleFont = generateFont("fonts/NotoSansKR-Bold.ttf", 28);
        if (font == null) font = new BitmapFont();
        if (titleFont == null) titleFont = font;
        skin.add("default-font", font, BitmapFont.class);
        skin.add("title-font", titleFont, BitmapFont.class);

        // utility: create tiled drawable tinted
        java.util.function.Function<Color, Drawable> tile = (c) -> {
            TiledDrawable td = new TiledDrawable(whiteRegion);
            return td.tint(c);
        };

        // Label styles
        Label.LabelStyle label = new Label.LabelStyle();
        label.font = font;
        label.fontColor = Palette.TEXT;
        skin.add("default", label);

        Label.LabelStyle title = new Label.LabelStyle();
        title.font = titleFont;
        title.fontColor = Palette.TEXT;
        skin.add("title", title);

        // TextButton styles
        TextButton.TextButtonStyle btn = new TextButton.TextButtonStyle();
        btn.font = font;
        btn.up = tile.apply(Palette.BTN_NORMAL);
        btn.over = tile.apply(Palette.BTN_OVER);
        btn.down = tile.apply(Palette.BTN_DOWN);
        btn.checked = tile.apply(Palette.BTN_CHECKED.cpy().mul(1f,1f,1f,0.7f));
        btn.fontColor = Palette.TEXT;
        btn.overFontColor = Palette.TEXT;
        btn.checkedFontColor = Palette.TEXT;
        skin.add("default", btn);

        // TextField style
        TextField.TextFieldStyle tfs = new TextField.TextFieldStyle();
        tfs.font = font;
        tfs.fontColor = Palette.TEXT;
        tfs.cursor = tile.apply(Palette.TEXT);
        tfs.selection = tile.apply(Palette.ACCENT.cpy().mul(1f,1f,1f,0.5f));
        tfs.background = tile.apply(Palette.PANEL);
        skin.add("default", tfs);

        // Window/Dialog style
        Window.WindowStyle ws = new Window.WindowStyle();
        ws.titleFont = titleFont;
        ws.titleFontColor = Palette.TEXT;
        ws.background = tile.apply(Palette.PANEL);
        skin.add("default", ws);

        // Panels / backgrounds
        skin.add("bg", tile.apply(Palette.BG), Drawable.class);
        skin.add("panel", tile.apply(Palette.PANEL), Drawable.class);

        // Overlay (dim background for modals)
        skin.add("overlay", tile.apply(new Color(0f, 0f, 0f, 0.55f)), Drawable.class);

        // Error label style
        Label.LabelStyle error = new Label.LabelStyle();
        error.font = font;
        error.fontColor = new Color(0.88f, 0.36f, 0.27f, 1f); // warm red
        skin.add("error", error);

        return skin;
    }

    private static BitmapFont generateFont(String internalPath, int size) {
        try {
            if (!Gdx.files.internal(internalPath).exists()) return null;
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(internalPath));
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = size;
            p.color = Palette.TEXT;
            // Include ASCII + Hangul ranges to avoid tofu (□) on Korean text
            StringBuilder chars = new StringBuilder(FreeTypeFontGenerator.DEFAULT_CHARS);
            appendRange(chars, 0xAC00, 0xD7A3); // Hangul Syllables
            appendRange(chars, 0x1100, 0x11FF); // Hangul Jamo
            appendRange(chars, 0x3131, 0x318E); // Hangul Compatibility Jamo
            chars.append('•'); // password bullet
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
}
