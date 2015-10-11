package org.kaka.myreader.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Layout;
import android.text.StaticLayout;
import android.util.Log;
import android.widget.Button;

import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.common.AppUtility;
import org.kaka.myreader.dlayer.dao.CaptureInfoDao;
import org.kaka.myreader.opengl.MySurfaceView;

import java.util.ArrayList;
import java.util.HashMap;

public class ReaderTxtActivity extends AbstractReaderActivity {
    private int endOffset = 0;
    private int startOffsetBefore = 0;
    private int endOffsetBefore = 0;
    private int prePageStart;
    private int nextPageEnd;
    private ArrayList<Integer> captureOffsets;
    private int currentCapture;

    private FileReadTask task0;
    private FileReadTask task1;
    private FileReadTask task2;

    @Override
    protected void createChapterInfo() {
        int preCaptureOffset = 0;
        int currentCaptureOffset = 0;
        int nextCaptureOffset = 0;

        for (int offset : captureOffsets) {
            if (startOffset < offset) {
                nextCaptureOffset = offset;
                break;
            }
            preCaptureOffset = currentCaptureOffset;
            currentCaptureOffset = offset;
        }

        offsetMap = new HashMap<>();
        offsetOppMap = new HashMap<>();

        if (currentCaptureOffset > 0) {
            makePageOffsets(preCaptureOffset, currentCaptureOffset);
        }

        String currentContent = nextCaptureOffset > 0 ? contents.substring(currentCaptureOffset, nextCaptureOffset) : contents.substring(currentCaptureOffset);
        currentContent = changeText(currentContent);
        StaticLayout currentLayout = new StaticLayout(currentContent, 0, currentContent.length(), paint, width - 100, Layout.Alignment.ALIGN_NORMAL, lineSpacing, 0.0f, false);
        int lineCount = currentLayout.getLineCount();
        boolean hasStartOffset = false;
        int nearestOffset = 0;
        for (int i = 0; i < lineCount; i += line) {
            int start = currentCaptureOffset + currentLayout.getLineStart(i);
            if (start < startOffset) {
                nearestOffset = start;
            } else if (start == startOffset) {
                hasStartOffset = true;
            }
            int endLine = i + line > lineCount ? lineCount : i + line;
            int end = currentCaptureOffset + currentLayout.getLineEnd(endLine - 1);

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

        doReadTask(currentCaptureOffset);

        Log.i("current", currentCaptureOffset + "->" + nextCaptureOffset);
    }

    @Override
    protected void getContents(String filePath) {
        contents = AppUtility.readFile(filePath);
    }

    @Override
    protected void getChapters() {
        CaptureInfoDao captureInfoDao = factory.getCaptureInfoDao();
        chapterMap = captureInfoDao.getCaptureInfo(id);
        if (chapterMap.size() == 0) {
            chapterMap = AppUtility.getCaptureInfo(contents);
        }
        captureNames = chapterMap.values().toArray(new String[chapterMap.size()]);
        captureOffsets = new ArrayList<>(chapterMap.keySet());
    }

    @Override
    protected void getPreChapter(Button btnPre, Button btnNext) {
        int index = captureOffsets.indexOf(currentCapture) - 1;
        if (index < 0) {
            btnPre.setEnabled(false);
            btnPre.setTextColor(Color.GRAY);
            return;
        }
        startOffset = captureOffsets.get(index);
        myView.update();
        btnNext.setEnabled(true);
        btnNext.setTextColor(Color.WHITE);

        doReadTask(currentCapture);
        int preIndex = index - 1;
        if (preIndex >= 0) {
            int preOffset = captureOffsets.get(preIndex);
            doReadTask(preOffset);
        }
    }

    @Override
    protected void getNextChapter(Button btnPre, Button btnNext) {
        int index = captureOffsets.indexOf(currentCapture) + 1;
        if (index >= captureOffsets.size()) {
            btnNext.setEnabled(false);
            btnNext.setTextColor(Color.GRAY);
            return;
        }

        startOffset = captureOffsets.get(index);
        myView.update();
        btnPre.setEnabled(true);
        btnPre.setTextColor(Color.WHITE);
        doReadTask(currentCapture);
    }

    @Override
    protected void setProvider() {
        myView.setBitmapProvider(new BitmapProvider());
    }

    @Override
    protected String getChapterName() {
        return captureNames[captureOffsets.indexOf(currentCapture)];
    }

    @Override
    protected boolean hasNextChapter() {
        return !(currentCapture == captureOffsets.get(captureOffsets.size() - 1));
    }

    @Override
    protected boolean hasPreChapter() {
        return currentCapture > 0;
    }

    @Override
    protected int getDistance() {
        int index = captureOffsets.indexOf(currentCapture) + 1;
        int nextChapterOffset;
        if (index < captureOffsets.size()) {
            nextChapterOffset = captureOffsets.get(index) - 1;
        } else {
            nextChapterOffset = contents.length() - 1;
        }

        return nextChapterOffset - currentCapture;
    }

    @Override
    protected int getProgress(int distance) {

        return (startOffset - currentCapture) * 100 / distance;
    }

    @Override
    protected int getEndOffset(int distance, int progress) {
        return currentCapture + distance * progress / 100;
    }

    protected class BitmapProvider implements MySurfaceView.BitmapProvider {
        private boolean hasNext = true;
        private boolean hasPre = true;
        private boolean hasNextBefore = true;
        private boolean hasPreBefore = true;
        private Bitmap bitmap;
        private boolean isRead;

        public void resetOffset() {
            if (startOffset != 0) {
                endOffset = startOffset;
                startOffset = 0;
            }
            progressRateView.setText(AppConstants.DECIMAL_FORMAT.format(endOffset * 100.0 / contents.length()) + "%");
            hasPre = false;
        }

        public void backToBefore() {
            startOffset = startOffsetBefore;
            endOffset = endOffsetBefore;
            hasNext = hasNextBefore;
            hasPre = hasPreBefore;
            progressRateView.setText(AppConstants.DECIMAL_FORMAT.format(endOffset * 100.0 / contents.length()) + "%");
        }

        public boolean hasNextPage() {
            return hasNext;
        }

        public boolean hasPrePage() {
            return hasPre;
        }

        public void updateCurrentOffset() {
            myBookDao.updateCurrentOffset(id, startOffset);
        }

        @Override
        public Bitmap getBitmap(final MySurfaceView.BitmapState state) {

            bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(currentColor);
            canvas.translate(50, 50);

            String subContent = "";
            if (state == MySurfaceView.BitmapState.Left) {
                // make left bitmap. not curl.
                if (startOffset == 0) {
                    return null;
                }

                int start;
                int end = startOffset;

                if (offsetOppMap.containsKey(end)) {
                    start = offsetOppMap.get(end);
                } else {
                    readPreCaptureInfo(end);
                    start = prePageStart;
                }

                subContent = contents.substring(start, end);
            } else if (state == MySurfaceView.BitmapState.ToPreLeft) {
                startOffsetBefore = startOffset;
                endOffsetBefore = endOffset;
                hasNextBefore = hasNext;
                hasPreBefore = hasPre;
                // when curl to left. need to create pre-left bitmap.
                int preStart;
                int preEnd;

                endOffset = startOffset;
                if (offsetOppMap.containsKey(endOffset)) {
                    startOffset = offsetOppMap.get(endOffset);
                    if (startOffset == 0) {
                        hasPre = false;
                        currentCapture = 0;
                        return null;
                    }

                    preEnd = startOffset;
                    if (offsetOppMap.containsKey(preEnd)) {
                        preStart = offsetOppMap.get(preEnd);
                    } else {
                        readPreCaptureInfo(startOffset);
                        preStart = prePageStart;
                    }
                } else {
                    readPreCaptureInfo(startOffset);
                    startOffset = prePageStart;
                    if (startOffset == 0) {
                        hasPre = false;
                        return null;
                    }
                    preEnd = startOffset;
                    if (offsetOppMap.containsKey(preEnd)) {
                        preStart = offsetOppMap.get(preEnd);
                    } else {
                        readPreCaptureInfo(startOffset);
                        preStart = prePageStart;
                    }
                }

                hasPre = true;
                hasNext = endOffset != contents.length();

                progressRateView.setText(AppConstants.DECIMAL_FORMAT.format(endOffset * 100.0 / contents.length()) + "%");
                Log.d("pre-left", preStart + ", " + preEnd);
                Log.d("left", startOffset + ", " + endOffset);
                subContent = contents.substring(preStart, preEnd);

                // insert to myBooks.
                myBookDao.updateCurrentOffset(id, startOffset);
            } else if (state == MySurfaceView.BitmapState.Right || state == MySurfaceView.BitmapState.ToRight) {
                // make right page
                if (state == MySurfaceView.BitmapState.ToRight) {
                    startOffsetBefore = startOffset;
                    endOffsetBefore = endOffset;
                    hasNextBefore = hasNext;
                    hasPreBefore = hasPre;

                    startOffset = endOffset;
                }
                if (offsetMap.containsKey(startOffset)) {
                    endOffset = offsetMap.get(startOffset);
                } else {
                    readNextCaptureInfo(startOffset);
                    endOffset = nextPageEnd;
                }

                hasNext = endOffset != contents.length();
                hasPre = startOffset != 0;

                progressRateView.setText(AppConstants.DECIMAL_FORMAT.format(endOffset * 100.0 / contents.length()) + "%");
                Log.i("start - end", startOffset + ", " + endOffset);
                subContent = contents.substring(startOffset, endOffset);

                // insert to myBooks.
                myBookDao.updateCurrentOffset(id, startOffset);
            }

            subContent = changeText(subContent);
            StaticLayout staticLayout = new StaticLayout(subContent, 0, subContent.length(), paint, width - 100, Layout.Alignment.ALIGN_NORMAL, lineSpacing, 0.0f, false);

            // current capture position.
            int oldCapture = currentCapture;
            if (state != MySurfaceView.BitmapState.Left) {
                boolean found = false;
                for (int i = 0; i < captureOffsets.size() - 1; i++) {
                    if (startOffset >= captureOffsets.get(i) && startOffset < captureOffsets.get(i + 1)) {
                        currentCapture = captureOffsets.get(i);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    currentCapture = captureOffsets.get(captureOffsets.size() - 1);
                }
            }
            if (oldCapture != currentCapture) {
                isRead = false;
            }
            if (!isRead && (state == MySurfaceView.BitmapState.ToPreLeft || state == MySurfaceView.BitmapState.ToRight)) {
                doReadTask(currentCapture);
                isRead = true;
            }

            staticLayout.draw(canvas);
            Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            drawable.draw(canvas);

            return bitmap;
        }
    }

    private void readPreCaptureInfo(final int startOffset) {
        readChapterDialog = ProgressDialog.show(ReaderTxtActivity.this, "请稍后", "加载上一章中...");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                prePageStart = 0;
                long startTime = System.currentTimeMillis();
                int index = captureOffsets.indexOf(startOffset);
                if (index < 1) {
                    handler.sendEmptyMessage(1);
                    return;
                }
                int preCaptureOffset = captureOffsets.get(index - 1);
                String preContent = contents.substring(preCaptureOffset, startOffset);
                preContent = changeText(preContent);
                StaticLayout preLayout = new StaticLayout(preContent, 0, preContent.length(), paint, width - 100, Layout.Alignment.ALIGN_NORMAL, lineSpacing, 0.0f, false);

                int lineCount = preLayout.getLineCount();
                int start = 0;
                for (int i = 0; i < lineCount; i += line) {
                    start = preCaptureOffset + preLayout.getLineStart(i);
                    int endLine = i + line > lineCount ? lineCount : i + line;
                    int end = preCaptureOffset + preLayout.getLineEnd(endLine - 1);

                    if (start == end) {
                        continue;
                    }
                    offsetMap.put(start, end);
                    offsetOppMap.put(end, start);
                }
                long endTime = System.currentTimeMillis();
                Log.i("readPre time : ", (endTime - startTime) + "ms");

                prePageStart = start;
                //return start;
                handler.sendEmptyMessage(1);
            }
        });
        thread.start();
    }

    private void readNextCaptureInfo(final int startOffset) {
        readChapterDialog = ProgressDialog.show(ReaderTxtActivity.this, "请稍后", "加载下一章中...");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                nextPageEnd = 0;
                int nextCaptureOffset;
                int captureOffset = startOffset;
                int index = captureOffsets.indexOf(captureOffset);
                if (index < 0) {
                    for (int i = 0; i < captureOffsets.size() - 1; i++) {
                        if (captureOffsets.get(i) <= captureOffset && captureOffsets.get(i + 1) > captureOffset) {
                            index = i;
                            break;
                        }
                    }
                    if (index < 0) {
                        index = captureOffsets.size() - 1;
                    }
                    captureOffset = captureOffsets.get(index);
                }
                if (index == captureOffsets.size() - 1) {
                    nextCaptureOffset = contents.length();
                } else {
                    nextCaptureOffset = captureOffsets.get(index + 1);
                }

                long startTime = System.currentTimeMillis();
                String nextContent = contents.substring(captureOffset, nextCaptureOffset);
                nextContent = changeText(nextContent);
                StaticLayout nextLayout = new StaticLayout(nextContent, 0, nextContent.length(), paint, width - 100, Layout.Alignment.ALIGN_NORMAL, lineSpacing, 0.0f, false);

                int lineCount = nextLayout.getLineCount();
                for (int i = 0; i < lineCount; i += line) {
                    int start = captureOffset + nextLayout.getLineStart(i);
                    int endLine = i + line > lineCount ? lineCount : i + line;
                    int end = captureOffset + nextLayout.getLineEnd(endLine - 1);
                    if (start == startOffset) {
                        nextPageEnd = end;
                    }

                    if (start == end) {
                        continue;
                    }
                    offsetMap.put(start, end);
                    offsetOppMap.put(end, start);
                }
                long endTime = System.currentTimeMillis();
                Log.i("readNext lining time : ", (endTime - startTime) + "ms");
                handler.sendEmptyMessage(1);
            }
        });
        thread.start();
    }

    private void makePageOffsets(int startOffset, int endOffset) {
        String content = contents.substring(startOffset, endOffset);
        content = changeText(content);
        StaticLayout currentLayout = new StaticLayout(content, 0, content.length(), paint, width - 100, Layout.Alignment.ALIGN_NORMAL, lineSpacing, 0.0f, false);
        int lineCount = currentLayout.getLineCount();
        for (int i = 0; i < lineCount; i += line) {
            int start = startOffset + currentLayout.getLineStart(i);
            int endLine = i + line > lineCount ? lineCount : i + line;
            int end = startOffset + currentLayout.getLineEnd(endLine - 1);

            if (start == end) {
                continue;
            }
            offsetMap.put(start, end);
            offsetOppMap.put(end, start);
        }
    }

    @Override
    protected void resetOffsetMap() {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        double textHeight = Math.ceil(fontMetrics.descent - fontMetrics.ascent);
        line = (int) (height / (textHeight * lineSpacing)) - 2;

        offsetMap = new HashMap<>();
        offsetOppMap = new HashMap<>();

        int preCaptureOffset = 0;
        int currentCaptureOffset = 0;
        int nextCaptureOffset = 0;

        for (int offset : captureOffsets) {
            if (startOffset < offset) {
                nextCaptureOffset = offset;
                break;
            }
            preCaptureOffset = currentCaptureOffset;
            currentCaptureOffset = offset;
        }

        if (nextCaptureOffset == 0) {
            nextCaptureOffset = contents.length();
        }

        if (currentCaptureOffset > 0) {
            makePageOffsets(preCaptureOffset, currentCaptureOffset);
        }

        if (currentCaptureOffset < startOffset) {
            makePageOffsets(currentCaptureOffset, startOffset);
        }

        makePageOffsets(startOffset, nextCaptureOffset);
        doReadTask(currentCaptureOffset);
    }

    @Override
    protected void doReadTask(int currentChapterOffset) {
        int index = captureOffsets.indexOf(currentChapterOffset);
        int preChapterOffset = 0;
        if (index > 0) {
            preChapterOffset = captureOffsets.get(index - 1);
        }

        int beforePreChapterOffset = 0;
        int beforePreIndex = index - 2;
        if (beforePreIndex >= 0) {
            beforePreChapterOffset = captureOffsets.get(beforePreIndex);
        }

        int nextChapterOffset;
        if (index < captureOffsets.size() - 1) {
            nextChapterOffset = captureOffsets.get(index + 1);
        } else {
            nextChapterOffset = captureOffsets.get(captureOffsets.size() - 1);
        }

        if (!offsetMap.containsKey(beforePreChapterOffset)) {
            if (task0 != null && task0.getStatus() == AsyncTask.Status.RUNNING) {
                task0.cancel(true);
                Log.i("task0", task0.getStatus().toString());
            }
            task0 = new FileReadTask();
            task0.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, beforePreChapterOffset, preChapterOffset);
        }


        if (!offsetMap.containsKey(preChapterOffset)) {
            if (task1 != null && task1.getStatus() == AsyncTask.Status.RUNNING) {
                task1.cancel(true);
                Log.i("task1", task1.getStatus().toString());
            }
            task1 = new FileReadTask();
            task1.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, preChapterOffset, currentChapterOffset);
        }

        if (!offsetMap.containsKey(nextChapterOffset)) {
            int belowChapterOffset;
            int indexNext = captureOffsets.indexOf(nextChapterOffset);
            if (indexNext == -1) {
                return;
            } else if (indexNext == captureOffsets.size() - 1) {
                belowChapterOffset = contents.length();
            } else {
                belowChapterOffset = captureOffsets.get(indexNext + 1);
            }

            if (task2 != null && task2.getStatus() == AsyncTask.Status.RUNNING) {
                task2.cancel(true);
                Log.i("task2", task2.getStatus().toString());
            }
            task2 = new FileReadTask();
            task2.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, nextChapterOffset, belowChapterOffset);

            Log.i("", beforePreChapterOffset + "->" + preChapterOffset);
            Log.i("", preChapterOffset + "->" + currentChapterOffset);
            Log.i("", nextChapterOffset + "->" + belowChapterOffset);
        }
    }

    @Override
    protected void releaseTask() {
        if (task0 != null && task0.getStatus() == AsyncTask.Status.RUNNING) {
            task0.cancel(true);
        }
        if (task1 != null && task1.getStatus() == AsyncTask.Status.RUNNING) {
            task1.cancel(true);
        }
        if (task2 != null && task2.getStatus() == AsyncTask.Status.RUNNING) {
            task2.cancel(true);
        }
    }

    private class FileReadTask extends AsyncTask<Integer, Integer, Integer> {
        @Override
        protected Integer doInBackground(Integer... params) {
            Log.i("[Task]", "Read Task start.");
            long startTime = System.currentTimeMillis();
            int startOffset = params[0];
            int endOffset = params[1];

            if (offsetMap.containsKey(startOffset)) {
                return 1;
            }

            makePageOffsets(startOffset, endOffset);
            long endTime = System.currentTimeMillis();

            Log.i("[Task]", "Read Task done." + (endTime - startTime) + "ms");

            return 0;
        }
    }

    @Override
    protected void setIntentChapterInfo(Intent intent) {
        intent.putIntegerArrayListExtra("offsets", captureOffsets);
        intent.putExtra("currentCapture", currentCapture);
    }
}
