package org.kaka.myreader.opengl;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

public class MyRenderer implements GLSurfaceView.Renderer {
    public enum CurlState {
        None, Left, Right
    }

    // Set to true for checking quickly how perspective projection looks.
    private static final boolean USE_PERSPECTIVE_PROJECTION = false;

    // Rect for render area.
    private RectF viewRect = new RectF();
    private RectF margins = new RectF();
    // Screen size.
    private int viewportWidth;
    private int viewportHeight;

    // Curl meshes used for static and dynamic rendering.
    private Vector<CurlMesh> meshList;

    private boolean isBackgroundColorChanged = false;
    private int backgroundColor;

    private Observer observer;

    private RectF pageRectLeft;
    private RectF pageRectRight;

    /**
     * Basic constructor.
     */
    public MyRenderer(Observer observer) {
        this.observer = observer;
        meshList = new Vector<>();
        pageRectLeft = new RectF();
        pageRectRight = new RectF();
    }

    /**
     * Adds CurlMesh to this renderer.
     */
    public synchronized void addCurlMesh(CurlMesh mesh) {
        removeCurlMesh(mesh);
        meshList.add(mesh);
    }

    /**
     * Removes CurlMesh from this renderer.
     */
    public synchronized void removeCurlMesh(CurlMesh mesh) {
        meshList.remove(mesh);
    }

    /**
     * Returns rect reserved for left or right page.
     */
    public RectF getPageRect(CurlState state) {
        if (state == CurlState.Left) {
            return pageRectLeft;
        } else if (state == CurlState.Right) {
            return pageRectRight;
        }
        return null;
    }

    @Override
    public synchronized void onDrawFrame(GL10 gl) {
        observer.onDrawFrame();

        if (isBackgroundColorChanged) {
            gl.glClearColor(Color.red(backgroundColor) / 255f,
                    Color.green(backgroundColor) / 255f,
                    Color.blue(backgroundColor) / 255f,
                    Color.alpha(backgroundColor) / 255f);
            isBackgroundColorChanged = false;
        }

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        if (USE_PERSPECTIVE_PROJECTION) {
            gl.glTranslatef(0, 0, -6f);
        }

        for (int i = 0; i < meshList.size(); ++i) {
            meshList.get(i).draw(gl);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        viewportWidth = width;
        viewportHeight = height;

        float ratio = (float) width / height;
        viewRect.top = 1.0f;
        viewRect.bottom = -1.0f;
        viewRect.left = -ratio;
        viewRect.right = ratio;
        updatePageRects();

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        if (USE_PERSPECTIVE_PROJECTION) {
            GLU.gluPerspective(gl, 20f, (float) width / height, .1f, 100f);
        } else {
            GLU.gluOrtho2D(gl, viewRect.left, viewRect.right,
                    viewRect.bottom, viewRect.top);
        }

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0f, 0f, 0f, 1f);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
        gl.glHint(GL10.GL_POLYGON_SMOOTH_HINT, GL10.GL_NICEST);
        gl.glEnable(GL10.GL_LINE_SMOOTH);
        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glDisable(GL10.GL_CULL_FACE);

        observer.onSurfaceCreated();
    }


    /**
     * Change background/clear color.
     */
    public void setBackgroundColor(int color) {
        backgroundColor = color;
        isBackgroundColorChanged = true;
    }

    /**
     * Translates screen coordinates into view coordinates.
     */
    public void translate(PointF pt) {
        pt.x = viewRect.left + (viewRect.width() * pt.x / viewportWidth);
        pt.y = viewRect.top - (-viewRect.height() * pt.y / viewportHeight);
    }

    /**
     * Recalculates page rectangles.
     */
    private void updatePageRects() {
        if (viewRect.width() == 0 || viewRect.height() == 0) {
            return;
        }

        pageRectRight.set(viewRect);
        pageRectRight.left += viewRect.width() * margins.left;
        pageRectRight.right -= viewRect.width() * margins.right;
        pageRectRight.top += viewRect.height() * margins.top;
        pageRectRight.bottom -= viewRect.height() * margins.bottom;

        pageRectLeft.set(pageRectRight);
        pageRectLeft.offset(-pageRectRight.width(), 0);

        int bitmapW = (int) ((pageRectRight.width() * viewportWidth) / viewRect
                .width());
        int bitmapH = (int) ((pageRectRight.height() * viewportHeight) / viewRect
                .height());
        observer.onPageSizeChanged(bitmapW, bitmapH);

    }
}
