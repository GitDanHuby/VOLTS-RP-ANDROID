package com.rockstargames.oswrapper;

import com.voltsrp.mobile.oswrapper.GameActivityBase;
import com.voltsrp.mobile.oswrapper.GameView;

/**
 * Compatibility adapter for the class name resolved by the bundled libGame.so.
 * Service methods are inherited from the app implementation.
 */
public final class GamePlatformServices extends com.voltsrp.mobile.oswrapper.GamePlatformServices {
    public GamePlatformServices(GameActivityBase activity, GameView view) {
        super(activity, view);
    }
}
