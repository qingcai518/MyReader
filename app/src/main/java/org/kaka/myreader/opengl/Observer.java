package org.kaka.myreader.opengl;

public interface Observer {
    void onDrawFrame();

    void onPageSizeChanged(int w, int h);

    void onSurfaceCreated();
}
