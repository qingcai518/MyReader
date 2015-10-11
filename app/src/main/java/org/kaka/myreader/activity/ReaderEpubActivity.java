package org.kaka.myreader.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.widget.Button;

import org.kaka.myreader.common.AppUtility;
import org.kaka.myreader.opengl.MySurfaceView;

import java.util.HashMap;
import java.util.List;

import nl.siegmann.epublib.domain.Resource;

public class ReaderEpubActivity extends AbstractReaderActivity {
    private int currentIndex = 1;
    private List<Resource> resourceList;

    @Override
    protected void getContents(String filePath) {
        resourceList = AppUtility.readEpubFile(filePath);
        contents = AppUtility.getEpubContent(resourceList, currentIndex);
    }

    @Override
    protected void getChapters() {
        captureNames = AppUtility.getChapterNames(resourceList);
    }

    @Override
    protected void setProvider() {
        myView.setBitmapProvider(new BitmapProvider());
    }

    @Override
    protected String getChapterName() {
        return captureNames[currentIndex];
    }

    @Override
    protected void getPreChapter(Button btnPre, Button btnNext) {
        int index = currentIndex  - 1;
        if (index < 1) {
            btnPre.setEnabled(false);
            btnPre.setTextColor(Color.GRAY);
            return;
        }

        myView.update();
        btnNext.setEnabled(true);
        btnNext.setTextColor(Color.WHITE);

//        doReadTask(currentCapture);
//        int preIndex = index - 1;
//        if (preIndex >= 0) {
//            int preOffset = captureOffsets.get(preIndex);
//            doReadTask(preOffset);
//        }
    }

    @Override
    protected void getNextChapter(Button btnPre, Button btnNext) {
        int index = currentIndex + 1;
        if (index >= resourceList.size() - 1) {
            btnNext.setEnabled(false);
            btnNext.setTextColor(Color.GRAY);
            return;
        }

        myView.update();
        btnPre.setEnabled(true);
        btnPre.setTextColor(Color.WHITE);
//        doReadTask(currentCapture);
    }

    @Override
    protected boolean hasNextChapter() {
        return !(currentIndex == resourceList.size() - 1);
    }

    @Override
    protected boolean hasPreChapter() {
        return currentIndex <= 1;
    }

    @Override
    protected int getDistance() {
        // TODO
        return 0;
    }

    @Override
    protected int getProgress(int distance) {
        //TODO
        return 0;
    }

    @Override
    protected int getEndOffset(int distance, int progress) {
        //TODO
        return distance * progress / 100;
    }

    @Override
    protected void createChapterInfo() {
        offsetMap = new HashMap<>();
        offsetOppMap = new HashMap<>();

        if (currentIndex > 1) {
            String preContents = AppUtility.getEpubContent(resourceList, currentIndex - 1);
            makeEpubPageOffsets(preContents);
        }

        String currentContent = changeText(contents);
        StaticLayout currentLayout = new StaticLayout(currentContent, 0, currentContent.length(), paint, width - 100, Layout.Alignment.ALIGN_NORMAL, lineSpacing, 0.0f, false);
        int lineCount = currentLayout.getLineCount();
        boolean hasStartOffset = false;
        int nearestOffset = 0;
        for (int i = 0; i < lineCount; i += line) {
            int start = currentLayout.getLineStart(i);
            if (start < startOffset) {
                nearestOffset = start;
            } else if (start == startOffset) {
                hasStartOffset = true;
            }
            int endLine = i + line > lineCount ? lineCount : i + line;
            int end = currentLayout.getLineEnd(endLine - 1);

            if (start == end) {
                continue;
            }
            offsetMap.put(start, end);
            offsetOppMap.put(end, start);
        }

        // 因为字体变化而导致起点位置变化的时候，找和变化前最近的起点.
        if (!hasStartOffset) {
            startOffset = nearestOffset;
        }

        if (currentIndex + 1 < resourceList.size()) {
            String nextContents = AppUtility.getEpubContent(resourceList, currentIndex + 1);
            makeEpubPageOffsets(nextContents);
        }
    }

    protected void resetOffsetMap() {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        double textHeight = Math.ceil(fontMetrics.descent - fontMetrics.ascent);
        line = (int) (height / (textHeight * lineSpacing)) - 2;

        offsetMap = new HashMap<>();
        offsetOppMap = new HashMap<>();
    }

    private class BitmapProvider implements MySurfaceView.BitmapProvider {
        private boolean hasNext = true;
        private boolean hasPre = true;
        private boolean hasNextBefore = true;
        private boolean hasPreBefore = true;
        private Bitmap bitmap;

        @Override
        public Bitmap getBitmap(MySurfaceView.BitmapState state) {
            bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(currentColor);
            canvas.translate(50, 50);

            String subContent = "";
            if (state == MySurfaceView.BitmapState.Left) {
                if (currentIndex > 1) {
                    subContent = AppUtility.getEpubContent(resourceList, currentIndex - 1);
                }
            } else if (state == MySurfaceView.BitmapState.ToPreLeft) {
//                progressRateView.setText(AppConstants.DECIMAL_FORMAT.format(endOffset * 100.0 / contents.length()) + "%");
                currentIndex = currentIndex - 1;
                subContent = AppUtility.getEpubContent(resourceList, currentIndex);

                // insert to myBooks.
                myBookDao.updateCurrentOffset(id, startOffset);
            } else if (state == MySurfaceView.BitmapState.Right || state == MySurfaceView.BitmapState.ToRight) {
//                progressRateView.setText(AppConstants.DECIMAL_FORMAT.format(endOffset * 100.0 / contents.length()) + "%");
                currentIndex = currentIndex + 1;
                subContent = AppUtility.getEpubContent(resourceList, currentIndex);

                // insert to myBooks.
                myBookDao.updateCurrentOffset(id, startOffset);
            }

            subContent = changeText(subContent);
            StaticLayout staticLayout = new StaticLayout(subContent, 0, subContent.length(), paint, width - 100, Layout.Alignment.ALIGN_NORMAL, lineSpacing, 0.0f, false);

            staticLayout.draw(canvas);
            Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            drawable.draw(canvas);

            return bitmap;
        }

        public void resetOffset() {
//            progressRateView.setText(AppConstants.DECIMAL_FORMAT.format(endOffset * 100.0 / contents.length()) + "%");
            hasPre = false;
        }

        public void backToBefore() {
            hasNext = hasNextBefore;
            hasPre = hasPreBefore;
//            progressRateView.setText(AppConstants.DECIMAL_FORMAT.format(endOffset * 100.0 / contents.length()) + "%");
        }

        public boolean hasNextPage() {
            return hasNext;
        }

        public boolean hasPrePage() {
            return hasPre;
        }

        @Override
        public void updateCurrentOffset() {
            myBookDao.updateCurrentOffset(id, startOffset);
        }
    }

    private void makeEpubPageOffsets(String content) {
        content = changeText(content);
        StaticLayout preLayout = new StaticLayout(content, 0, content.length(), paint, width - 100, Layout.Alignment.ALIGN_NORMAL, lineSpacing, 0.0f, false);
        int lineCount = preLayout.getLineCount();
        for (int i = 0; i < lineCount; i += line) {
            int start = preLayout.getLineStart(i);
            int endLine = i + line > lineCount ? lineCount : i + line;
            int end = preLayout.getLineEnd(endLine - 1);

            if (start == end) {
                continue;
            }
            offsetMap.put(start, end);
            offsetOppMap.put(end, start);
        }
    }

    @Override
    protected void doReadTask(int currentChapterOffset) {
    }

    @Override
    protected void releaseTask() {
    }

    @Override
    protected void setIntentChapterInfo(Intent intent) {
        intent.putExtra("currentIndex", currentIndex);
    }
}
