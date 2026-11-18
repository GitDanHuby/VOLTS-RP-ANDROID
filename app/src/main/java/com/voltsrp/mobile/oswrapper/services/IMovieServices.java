package com.voltsrp.mobile.oswrapper.services;

public interface IMovieServices {
    boolean isMoviePlaying();

    void playMovie(String filename, boolean skippable);

    void setMovieText(String text);

    void setMovieTextScale(int scale);

    void stopMovie();
}
