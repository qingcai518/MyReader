package org.kaka.myreader.activity;

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
import org.kaka.myreader.opengl.MySurfaceView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.siegmann.epublib.domain.Resource;

public class ReaderEpubActivity extends AbstractReaderActivity {
    private int currentIndex = 1;
    private List<Resource> resourceList;
    private Map<Integer, List<Integer>> chapterOffsetMap = new HashMap<>();
    private Map<Integer, String> chapterContentMap = new HashMap<>();
    private FileReadTask task0, task1, task2;

    @Override
    protected void getContents(String filePath) {
        resourceList = AppUtility.readEpubFile(filePath);
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
        int index = currentIndex - 1;
        if (index < 1) {
            btnPre.setEnabled(false);
            btnPre.setTextColor(Color.GRAY);
            return;
        }

        myView.update();
        btnNext.setEnabled(true);
        btnNext.setTextColor(Color.WHITE);

        doReadTask();
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
        doReadTask();
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

        if (currentIndex > 1) {
            makePageOffsets(currentIndex - 1);
        }

        String currentContent = changeText(AppUtility.getEpubContent(resourceList, currentIndex));
        StaticLayout currentLayout = new StaticLayout(currentContent, 0, currentContent.length(), paint, width - 100, Layout.Alignment.ALIGN_NORMAL, lineSpacing, 0.0f, false);
        int lineCount = currentLayout.getLineCount();
        boolean hasStartOffset = false;
        int nearestOffset = 0;
        List<Integer> list = new ArrayList<>();
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
            if (!list.contains(start)) {
                list.add(start);
            }
            if (!list.contains(end)) {
                list.add(end);
            }
        }
        if (!chapterOffsetMap.containsKey(currentIndex)) {
            chapterOffsetMap.put(currentIndex, list);
            chapterContentMap.put(currentIndex, currentContent);
        }

        // 因为字体变化而导致起点位置变化的时候，找和变化前最近的起点.
        if (!hasStartOffset) {
            startOffset = nearestOffset;
        }

        if (currentIndex + 1 < resourceList.size()) {
            makePageOffsets(currentIndex + 1);
        }

        doReadTask();
    }

    protected void resetOffsetMap() {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        double textHeight = Math.ceil(fontMetrics.descent - fontMetrics.ascent);
        line = (int) (height / (textHeight * lineSpacing)) - 2;

        chapterOffsetMap = new HashMap<>();
        chapterContentMap = new HashMap<>();
    }

    private class BitmapProvider implements MySurfaceView.BitmapProvider {
        private boolean hasNext = true;
        private boolean hasPre = true;
        private boolean hasNextBefore = true;
        private boolean hasPreBefore = true;
        private int startOffsetBefore;
        private Bitmap bitmap;

        public void resetOffset() {
            startOffset = 0;
            List<Integer> offsetList = chapterOffsetMap.get(currentIndex);
            String contents = chapterContentMap.get(currentIndex);
            int endOffset = offsetList.get(1);
            progressRateView.setText(AppConstants.DECIMAL_FORMAT.format(endOffset * 100.0 / contents.length()) + "%");
            hasPre = false;
        }

        public void backToBefore() {
            startOffset = startOffsetBefore;
            List<Integer> offsetList = chapterOffsetMap.get(currentIndex);
            String contents = chapterContentMap.get(currentIndex);
            int endOffset = offsetList.get(offsetList.indexOf(startOffset) + 1);
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
        public Bitmap getBitmap(MySurfaceView.BitmapState state) {
            bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(currentColor);
            canvas.translate(50, 50);

            String subContent = "";
            List<Integer> list = chapterOffsetMap.get(currentIndex);
            String contents = chapterContentMap.get(currentIndex);
            int index = list.indexOf(startOffset);
            if (state == MySurfaceView.BitmapState.Left) {
                if (index <= 0) {
                    return null;
                }
                int preOffset = list.get(index - 1);
                subContent = contents.substring(preOffset, startOffset);

            } else if (state == MySurfaceView.BitmapState.ToPreLeft) {
                startOffsetBefore = startOffset;
                hasNextBefore = hasNext;
                hasPreBefore = hasPre;
                // when curl to left.
                int preOffset;
                int endOffset = startOffset;
                if (index <= 0) {
                    hasPre = false;
                    return null;
                } else {
                    startOffset = list.get(index - 1);
                    if (index > 1) {
                        preOffset = list.get(index - 2);
                    } else {
                        // TODO should read pre chapter?
                        hasPre = false;
                        return null;
                    }
                }
                hasPre = true;
                subContent = contents.substring(preOffset, startOffset);
                progressRateView.setText(AppConstants.DECIMAL_FORMAT.format(endOffset * 100.0 / contents.length()) + "%");

                // insert to myBooks.
                myBookDao.updateCurrentOffset(id, startOffset);
            } else if (state == MySurfaceView.BitmapState.Right) {
                int nextIndex = list.indexOf(startOffset) + 1;
                if (nextIndex >= list.size()) {
                    return null;
                }
                int endOffset = list.get(nextIndex);

                subContent = contents.substring(startOffset, endOffset);
                progressRateView.setText(AppConstants.DECIMAL_FORMAT.format(endOffset * 100.0 / contents.length()) + "%");

                hasNext = currentIndex < resourceList.size() - 1;
                hasPre = currentIndex != 0;
            } else if (state == MySurfaceView.BitmapState.ToRight) {
                startOffsetBefore = startOffset;
                hasNextBefore = hasNext;
                hasPreBefore = hasPre;
                if (index + 1 >= list.size()) {
                    return null;
                }
                startOffset = list.get(index + 1);
                int endOffset;
                int nextIndex = list.indexOf(startOffset) + 1;
                if (nextIndex < list.size()) {
                    endOffset = list.get(nextIndex);
                } else {
                    if (!chapterOffsetMap.containsKey(currentIndex + 1)) {
                        return null;
                    }
                    List<Integer> nextList = chapterOffsetMap.get(currentIndex + 1);
                    contents = chapterContentMap.get(currentIndex + 1);
                    startOffset = nextList.get(0);
                    endOffset = nextList.get(1);
                }

                subContent = contents.substring(startOffset, endOffset);
                progressRateView.setText(AppConstants.DECIMAL_FORMAT.format(endOffset * 100.0 / contents.length()) + "%");

                hasNext = currentIndex < resourceList.size() - 1;
                hasPre = startOffset != 0;

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
    }

    private void makePageOffsets(int index) {
        String content = AppUtility.getEpubContent(resourceList, index);
        content = changeText(content);
        StaticLayout preLayout = new StaticLayout(content, 0, content.length(), paint, width - 100, Layout.Alignment.ALIGN_NORMAL, lineSpacing, 0.0f, false);
        int lineCount = preLayout.getLineCount();
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < lineCount; i += line) {
            int start = preLayout.getLineStart(i);
            int endLine = i + line > lineCount ? lineCount : i + line;
            int end = preLayout.getLineEnd(endLine - 1);

            if (start == end) {
                continue;
            }
            if (!list.contains(start)) {
                list.add(start);
            }
            if (!list.contains(end)) {
                list.add(end);
            }
        }
        if (!chapterOffsetMap.containsKey(index)) {
            chapterOffsetMap.put(index, list);
            chapterContentMap.put(index, content);
        }
    }

    private void doReadTask() {
        int preIndex = currentIndex - 1;
        if (preIndex > 0 && !chapterOffsetMap.containsKey(preIndex)) {
            if (task0 != null && task0.getStatus() == AsyncTask.Status.RUNNING) {
                task0.cancel(true);
                Log.i("task0", task0.getStatus().toString());
            }
            task0 = new FileReadTask();
            task0.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, preIndex);
        }

        if (!chapterOffsetMap.containsKey(currentIndex)) {
            if (task1 != null && task1.getStatus() == AsyncTask.Status.RUNNING) {
                task1.cancel(true);
                Log.i("task1", task1.getStatus().toString());
            }
            task1 = new FileReadTask();
            task1.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, currentIndex);
        }

        int nextIndex = currentIndex + 1;
        if (nextIndex < resourceList.size())
            if (!chapterOffsetMap.containsKey(nextIndex)) {
                if (task2 != null && task2.getStatus() == AsyncTask.Status.RUNNING) {
                    task2.cancel(true);
                    Log.i("task2", task2.getStatus().toString());
                }
                task2 = new FileReadTask();
                task2.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, nextIndex);
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
            int index = params[0];

            if (chapterOffsetMap.containsKey(index)) {
                return 1;
            }

            makePageOffsets(index);
            long endTime = System.currentTimeMillis();

            Log.i("[Task]", "Read Task done." + (endTime - startTime) + "ms");

            return 0;
        }
    }

    @Override
    protected void setIntentChapterInfo(Intent intent) {
        intent.putExtra("currentIndex", currentIndex);
    }
}
