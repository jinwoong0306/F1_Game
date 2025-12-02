package com.mygame.f1.tools;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

/**
 * Atlas 패커 도구 (실행용).
 * raw 이미지를 한 번에 패킹해 assets/atlas/game_ui.atlas 를 생성합니다.
 *
 * 실행 방법:
 *   ./gradlew :lwjgl3:packAtlas
 */
public final class TexturePackerTool {
    private TexturePackerTool() {}

    public static void main(String[] args) throws Exception {
        TexturePacker.Settings s = new TexturePacker.Settings();
        s.maxWidth = 2048;
        s.maxHeight = 2048;
        s.pot = true;              // 2의 제곱수 크기
        s.paddingX = 2;
        s.paddingY = 2;
        s.combineSubdirectories = true;
        s.duplicatePadding = true;

        String inputDir = "C:/Users/BILabGun/workspace/F1/assets/raw-ui";
        String outputDir = "C:/Users/BILabGun/workspace/F1/assets/atlas";
        String packFileName = "game_ui";

        TexturePacker.process(s, inputDir, outputDir, packFileName);
        System.out.println("Packing Finished! -> " + outputDir + "/" + packFileName + ".atlas");
    }
}
