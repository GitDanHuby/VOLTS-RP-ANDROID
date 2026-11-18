package com.voltsrp.mobile.oswrapper.view;

import com.voltsrp.mobile.oswrapper.GameActivityBase;

public interface GameViewHandler  {
    void onPause();

    void onResume();

    void setup(GameActivityBase activity);
}
