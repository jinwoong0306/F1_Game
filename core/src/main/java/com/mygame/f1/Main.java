package com.mygame.f1;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.mygame.f1.screens.SplashScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    // 게임 전체에서 유일하게 사용될 static final AssetManager
    public static final AssetManager assetManager = new AssetManager();
    public String playerName = "Player";

    @Override
    public void create() {
        // 에셋 로딩
        assetManager.load("pitstop_car_3.png", Texture.class);
        assetManager.load("new_map2.png", Texture.class);
        assetManager.load("sukit.png", Texture.class);
        assetManager.load("Hockenheim_fix.png", Texture.class);
        assetManager.load("track_3.png", Texture.class);
        assetManager.load("x_track.png", Texture.class);
        assetManager.load("track_grey.png", Texture.class);
        assetManager.load("Track_t2.png", Texture.class);

        assetManager.finishLoading(); // 모든 에셋 로딩이 끝날 때까지 대기

        setScreen(new SplashScreen(this));
    }

    @Override
    public void dispose() {
        assetManager.dispose();
        super.dispose();
    }
}
