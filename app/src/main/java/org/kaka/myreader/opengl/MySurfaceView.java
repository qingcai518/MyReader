package org.kaka.myreader.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MySurfaceView extends GLSurfaceView implements Observer {

    private CurlMesh pageCurl;
    private CurlMesh pageLeft;
    private CurlMesh pageRight;

    private MyRenderer.CurlState curlState = MyRenderer.CurlState.None;

    // Current page index. This is always showed on right page.
    // private int currentIndex = 0;

    private int width = -1;
    private int height = -1;

    private PointF dragStartPos = new PointF();
    private PointF pointerPos = new PointF();
    private PointF mCurlPos = new PointF();
    private PointF mCurlDir = new PointF();
    private float maxXfromLeft = 0f;
    private float minXfromRight = 0f;
    private boolean isCurlStart = false;
    private boolean isSettingArea = false;

    private boolean isAnimate = false;
    private PointF animationSource = new PointF();
    private PointF animationTarget = new PointF();
    private long animationStartTime;
    private final static int ANIMATION_DURATION_TIME = 300;
    private int animationTargetEvent;

    private static final int SET_CURL_TO_LEFT = 1;
    private static final int SET_CURL_TO_RIGHT = 2;

    private MyRenderer renderer;
    private BitmapProvider bitmapProvider;

    public MySurfaceView(Context context) {
        super(context);
        init();
    }

    public MySurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    @Override
    public void onDrawFrame() {
        if (!isAnimate) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime >= animationStartTime + ANIMATION_DURATION_TIME) {
            if (animationTargetEvent == SET_CURL_TO_RIGHT) {
                CurlMesh right = pageCurl;
                CurlMesh curl = pageRight;
                right.setRect(renderer.getPageRect(MyRenderer.CurlState.Right));
                right.setFlipTexture(false);
                right.reset();
                renderer.removeCurlMesh(curl);
                pageCurl = curl;
                pageRight = right;
            } else if (animationTargetEvent == SET_CURL_TO_LEFT) {
                // Switch curled page to left.
                CurlMesh left = pageCurl;
                CurlMesh curl = pageLeft;
                left.setRect(renderer.getPageRect(MyRenderer.CurlState.Left));
                left.setFlipTexture(true);
                left.reset();
                renderer.removeCurlMesh(curl);
                pageCurl = curl;
                pageLeft = left;
            }

            bitmapProvider.updateCurrentOffset();

            curlState = MyRenderer.CurlState.None;
            isAnimate = false;
            requestRender();
        } else {
            pointerPos.set(animationSource);
            float t = (float) Math
                    .sqrt((double) (currentTime - animationStartTime)
                            / ANIMATION_DURATION_TIME);
            pointerPos.x += (animationTarget.x - animationSource.x) * t;
            pointerPos.y += (animationTarget.y - animationSource.y) * t;
            updateCurlPos(pointerPos);
        }
    }

    @Override
    public void onPageSizeChanged(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        requestRender();
    }

    @Override
    public void onSurfaceCreated() {
        pageLeft.resetTexture();
        pageRight.resetTexture();
        pageCurl.resetTexture();
    }

    @Override
    public void setBackgroundColor(int color) {
        renderer.setBackgroundColor(color);
        requestRender();
    }

    /**
     * Update/set bitmap provider.
     */
    public void setBitmapProvider(BitmapProvider bitmapProvider) {
        this.bitmapProvider = bitmapProvider;

        updateBitmaps();
        requestRender();
    }

    public void update() {
        updateBitmaps();
        requestRender();
    }

    private void init() {
        renderer = new MyRenderer(this);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (isAnimate || bitmapProvider == null) {
                    return false;
                }

                RectF rightRect = renderer.getPageRect(MyRenderer.CurlState.Right);
                RectF leftRect = renderer.getPageRect(MyRenderer.CurlState.Left);

                pointerPos.set(event.getX(), event.getY());
                renderer.translate(pointerPos);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        dragStartPos.set(pointerPos);

                        if (dragStartPos.y > rightRect.top) {
                            dragStartPos.y = rightRect.top;
                        } else if (dragStartPos.y < rightRect.bottom) {
                            dragStartPos.y = rightRect.bottom;
                        }

                        isCurlStart = false;
                        float halfX = (rightRect.right + rightRect.left) / 2;

                        isSettingArea = pointerPos.x >= halfX - 0.1 && pointerPos.x <= halfX + 0.1;
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        if (!isCurlStart) {

                            if (isSettingArea && Math.abs(pointerPos.x - dragStartPos.x) < 0.01) {
                                break;
                            }

                            float halfX = (rightRect.right + rightRect.left) / 2;
                            if (dragStartPos.x < halfX && bitmapProvider.hasPrePage()) {
                                dragStartPos.x = rightRect.left;
                                maxXfromLeft = dragStartPos.x;
                                startCurl(MyRenderer.CurlState.Left);
                            } else if (dragStartPos.x >= halfX
                                    && bitmapProvider.hasNextPage()) {
                                dragStartPos.x = rightRect.right;
                                minXfromRight = dragStartPos.x;
                                startCurl(MyRenderer.CurlState.Right);
                            }
                            isCurlStart = true;
                        }

                        if (curlState == MyRenderer.CurlState.Left) {
                            maxXfromLeft = maxXfromLeft < pointerPos.x ? pointerPos.x : maxXfromLeft;
                        } else if (curlState == MyRenderer.CurlState.Right) {
                            minXfromRight = minXfromRight > pointerPos.x ? pointerPos.x : minXfromRight;
                        }

                        updateCurlPos(pointerPos);
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP: {
                        if (curlState == MyRenderer.CurlState.Left || curlState == MyRenderer.CurlState.Right) {
                            animationSource.set(pointerPos);
                            animationStartTime = System.currentTimeMillis();

                            if (pointerPos.x > (rightRect.left + rightRect.right) / 2) {
                                animationTarget.set(dragStartPos);
                                if (curlState == MyRenderer.CurlState.Right && minXfromRight >= pointerPos.x) {
                                    animationTarget.x = leftRect.left;
                                    animationTargetEvent = SET_CURL_TO_LEFT;
                                    Log.d("", "111111");
                                } else {
                                    animationTarget.x = renderer
                                            .getPageRect(MyRenderer.CurlState.Right).right;
                                    animationTargetEvent = SET_CURL_TO_RIGHT;
                                    if (curlState == MyRenderer.CurlState.Right) {
                                        bitmapProvider.backToBefore();
                                        Log.d("", "222222");
                                    } else {
                                        Log.d("", "333333");
                                    }
                                }
                            } else {
                                animationTarget.set(dragStartPos);
                                if (curlState == MyRenderer.CurlState.Right) {
                                    animationTarget.x = leftRect.left;
                                    animationTargetEvent = SET_CURL_TO_LEFT;
                                    Log.d("", "444444");
                                } else {
                                    if (maxXfromLeft <= pointerPos.x) {
                                        animationTarget.x = rightRect.right;
                                        animationTargetEvent = SET_CURL_TO_RIGHT;
                                        Log.d("", "555555");
                                    } else {
                                        animationTarget.x = rightRect.left;
                                        animationTargetEvent = SET_CURL_TO_LEFT;
                                        bitmapProvider.backToBefore();
                                        Log.d("", "666666");
                                    }
                                }
                            }
                            isAnimate = true;
                            requestRender();
                        }
                        break;
                    }
                }

                return isCurlStart;
            }
        });

        // Even though left and right pages are static we have to allocate room
        // for curl on them too as we are switching meshes. Another way would be
        // to swap texture ids only.
        pageLeft = new CurlMesh(10);
        pageRight = new CurlMesh(10);
        pageCurl = new CurlMesh(10);
        pageLeft.setFlipTexture(true);
        pageRight.setFlipTexture(false);
    }

    /**
     * Sets pageCurl curl position.
     */
    private void setCurlPos(PointF curlPos, PointF curlDir, double radius) {

        // First reposition curl so that page doesn't 'rip off' from book.
        RectF pageRect = renderer.getPageRect(MyRenderer.CurlState.Right);
        if (curlPos.x >= pageRect.right) {
            pageCurl.reset();
            requestRender();
            return;
        }
        if (curlPos.x < pageRect.left) {
            curlPos.x = pageRect.left;
        }
        if (curlDir.y != 0) {
            float diffX = curlPos.x - pageRect.left;
            float leftY = curlPos.y + (diffX * curlDir.x / curlDir.y);
            if (curlDir.y < 0 && leftY < pageRect.top) {
                curlDir.x = curlPos.y - pageRect.top;
                curlDir.y = pageRect.left - curlPos.x;
            } else if (curlDir.y > 0 && leftY > pageRect.bottom) {
                curlDir.x = pageRect.bottom - curlPos.y;
                curlDir.y = curlPos.x - pageRect.left;
            }
        }

        // Finally normalize direction vector and do rendering.
        double dist = Math.sqrt(curlDir.x * curlDir.x + curlDir.y * curlDir.y);
        if (dist != 0) {
            curlDir.x /= dist;
            curlDir.y /= dist;
            pageCurl.curl(curlPos, curlDir, radius);
        } else {
            pageCurl.reset();
        }

        requestRender();
    }

    private void startCurl(MyRenderer.CurlState state) {
        if (state == MyRenderer.CurlState.Right) {
            renderer.removeCurlMesh(pageLeft);
            renderer.removeCurlMesh(pageRight);
            renderer.removeCurlMesh(pageCurl);

            // We are curling right page.
            CurlMesh curl = pageRight;
            pageRight = pageCurl;
            pageCurl = curl;

            // If there is something to show on left page, simply add it to
            // renderer.
            if (bitmapProvider.hasPrePage()) {
                pageLeft
                        .setRect(renderer.getPageRect(MyRenderer.CurlState.Left));
                pageLeft.reset();
                renderer.addCurlMesh(pageLeft);
            }

            // If there is new/next available, set it to right page.
            if (bitmapProvider.hasNextPage()) {
                Bitmap bitmap = bitmapProvider.getBitmap(BitmapState.ToRight);
                pageRight.setBitmap(bitmap);
                pageRight.setRect(renderer
                        .getPageRect(MyRenderer.CurlState.Right));
                pageRight.setFlipTexture(false);
                pageRight.reset();
                renderer.addCurlMesh(pageRight);
            }

            // Add curled page to renderer.
            pageCurl.setRect(renderer.getPageRect(MyRenderer.CurlState.Right));
            pageCurl.setFlipTexture(false);
            pageCurl.reset();
            renderer.addCurlMesh(pageCurl);

            curlState = MyRenderer.CurlState.Right;
        } else if (state == MyRenderer.CurlState.Left) {
            renderer.removeCurlMesh(pageLeft);
            renderer.removeCurlMesh(pageRight);
            renderer.removeCurlMesh(pageCurl);

            CurlMesh curl = pageLeft;
            pageLeft = pageCurl;
            pageCurl = curl;

            Bitmap bitmap = bitmapProvider.getBitmap(BitmapState.ToPreLeft);
            if (bitmap != null) {
                pageLeft.setBitmap(bitmap);
                pageLeft
                        .setRect(renderer.getPageRect(MyRenderer.CurlState.Left));
                pageLeft.setFlipTexture(true);
                pageLeft.reset();
                renderer.addCurlMesh(pageLeft);
            } else {
                bitmapProvider.resetOffset();
            }

            if (bitmapProvider.hasNextPage()) {
                pageRight.setRect(renderer
                        .getPageRect(MyRenderer.CurlState.Right));
                pageRight.reset();
                renderer.addCurlMesh(pageRight);
            }

            pageCurl.setRect(renderer
                    .getPageRect(MyRenderer.CurlState.Right));
            pageCurl.setFlipTexture(false);

            pageCurl.reset();
            renderer.addCurlMesh(pageCurl);

            curlState = MyRenderer.CurlState.Left;
        }
    }

    /**
     * Updates bitmaps for left and right meshes.
     */
    private void updateBitmaps() {
        if (bitmapProvider == null || width <= 0
                || height <= 0) {
            return;
        }

        // Remove meshes from renderer.
        renderer.removeCurlMesh(pageLeft);
        renderer.removeCurlMesh(pageRight);
        renderer.removeCurlMesh(pageCurl);

        Bitmap bitmapRight = bitmapProvider.getBitmap(BitmapState.Right); // right page.
        pageRight.setBitmap(bitmapRight);
        pageRight.setRect(renderer.getPageRect(MyRenderer.CurlState.Right));
        pageRight.reset();
        renderer.addCurlMesh(pageRight);

        Bitmap bitmapLeft = bitmapProvider.getBitmap(BitmapState.Left); // left page.
        if (bitmapLeft != null) {
            pageLeft.setBitmap(bitmapLeft);
            pageLeft.setRect(renderer.getPageRect(MyRenderer.CurlState.Left));
            pageLeft.reset();
            renderer.addCurlMesh(pageLeft);
        }
    }

    /**
     * Updates curl position.
     */
    private void updateCurlPos(PointF pointerPos) {

        // Default curl radius.
        double radius = renderer.getPageRect(MyRenderer.CurlState.Right).width() / 3;

        mCurlPos.set(pointerPos);

        // If curl happens on right page, or on left page on two page mode,
        // we'll calculate curl position from pointerPos.
        if (curlState == MyRenderer.CurlState.Right) {

            mCurlDir.x = mCurlPos.x - dragStartPos.x;
            mCurlDir.y = mCurlPos.y - dragStartPos.y;
            float dist = (float) Math.sqrt(mCurlDir.x * mCurlDir.x + mCurlDir.y
                    * mCurlDir.y);

            // Adjust curl radius so that if page is dragged far enough on
            // opposite side, radius gets closer to zero.
            float pageWidth = renderer.getPageRect(MyRenderer.CurlState.Right)
                    .width();
            double curlLen = radius * Math.PI;
            if (dist > (pageWidth * 2) - curlLen) {
                curlLen = Math.max((pageWidth * 2) - dist, 0f);
                radius = curlLen / Math.PI;
            }

            // Actual curl position calculation.
            if (dist >= curlLen) {
                double translate = (dist - curlLen) / 2;
                mCurlPos.x -= mCurlDir.x * translate / dist;
                mCurlPos.y -= mCurlDir.y * translate / dist;
            } else {
                double angle = Math.PI * Math.sqrt(dist / curlLen);
                double translate = radius * Math.sin(angle);
                mCurlPos.x += mCurlDir.x * translate / dist;
                mCurlPos.y += mCurlDir.y * translate / dist;
            }

            setCurlPos(mCurlPos, mCurlDir, radius);
        }
        // Otherwise we'll let curl follow pointer position.
        else if (curlState == MyRenderer.CurlState.Left) {

            // Adjust radius regarding how close to page edge we are.
            float pageLeftX = renderer.getPageRect(MyRenderer.CurlState.Right).left;
            radius = Math.max(Math.min(mCurlPos.x - pageLeftX, radius), 0f);

            float pageRightX = renderer.getPageRect(MyRenderer.CurlState.Right).right;
            mCurlPos.x -= Math.min(pageRightX - mCurlPos.x, radius);
            mCurlDir.x = mCurlPos.x + dragStartPos.x;
            mCurlDir.y = mCurlPos.y - dragStartPos.y;

            setCurlPos(mCurlPos, mCurlDir, radius);
        }
    }

    public enum BitmapState {
        ToRight, ToPreLeft, Left, Right
    }

    public interface BitmapProvider {
        Bitmap getBitmap(BitmapState state);

        void resetOffset();

        void backToBefore();

        boolean hasNextPage();

        boolean hasPrePage();

        void updateCurrentOffset();
    }
}
