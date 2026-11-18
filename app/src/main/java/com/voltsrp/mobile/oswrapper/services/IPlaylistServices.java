package com.voltsrp.mobile.oswrapper.services;

public interface IPlaylistServices {
    int playlistCount();

    boolean playlistIsPlaying();

    void playlistOpen(String name);

    void playlistPause();

    void playlistPlay();

    void playlistSetVolume(float volume);

    void playlistStop();
}
