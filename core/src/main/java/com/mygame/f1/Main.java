package com.mygame.f1;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.mygame.f1.screens.SplashScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    public static final AssetManager assetManager = new AssetManager();
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
        setScreen(new SplashScreen(this));
    }

    @Override
    public void dispose() {
        assetManager.dispose();
        super.dispose();
    }
}
