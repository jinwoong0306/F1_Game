package com.mygame.f1;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.mygame.f1.screens.SplashScreen;
import com.mygame.f1.ui.SkinFactory;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    public static final AssetManager assetManager = new AssetManager();
    private static Skin sharedSkin;
    public String playerName = "Player";

    @Override
    public void create() {
        long t0 = System.nanoTime();
        String[] carTextures = {
                "cars/Astra A4.png",
                "cars/Boltworks RX-1.png",
                "cars/Emerald E7.png",
                "cars/Gold Rush GT.png",
                "cars/Midnight P4.png",
                "cars/Silverline S11.png"
        };
        for (String path : carTextures) {
            assetManager.load(path, Texture.class);
        }
        // 공용 로고/아이콘 사전 로드
        assetManager.load("ui/login/logo.png", Texture.class);
        assetManager.load("ui/icon/circle.png", Texture.class);
        // Atlas 로드
        assetManager.load("atlas/game_ui.atlas", TextureAtlas.class);

        assetManager.finishLoading();
        Gdx.app.log("PERF", String.format("Main asset preloads: %.2f ms", (System.nanoTime() - t0) / 1_000_000f));

        // Skin 싱글톤 초기화 (한 번만 생성)
        long skinStart = System.nanoTime();
        sharedSkin = SkinFactory.createDefaultSkin();
        Gdx.app.log("PERF", String.format("Skin creation: %.2f ms", (System.nanoTime() - skinStart) / 1_000_000f));

        setScreen(new SplashScreen(this));
    }

    @Override
    public void dispose() {
        if (sharedSkin != null) {
            sharedSkin.dispose();
            sharedSkin = null;
        }
        SkinFactory.disposeFonts();
        assetManager.dispose();
        super.dispose();
    }

    /**
     * 공유 Skin 인스턴스 반환. 모든 Screen에서 이 메서드를 사용하여 Skin을 얻어야 함.
     * dispose()를 호출하지 말 것 - Main이 관리함.
     */
    public static Skin getSharedSkin() {
        return sharedSkin;
    }
}
