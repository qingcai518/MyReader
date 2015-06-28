package org.kaka.myreader.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.common.AppUtility;
import org.kaka.myreader.dlayer.dao.BookmarkInfoDao;
import org.kaka.myreader.dlayer.dao.CaptureInfoDao;
import org.kaka.myreader.dlayer.dao.DaoFactory;
import org.kaka.myreader.dlayer.dao.MyBookDao;
import org.kaka.myreader.dlayer.entities.BookmarkInfoEntity;
import org.kaka.myreader.opengl.MySurfaceView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ReaderActivity extends Activity {
    private SharedPreferences preferences;
    private ProgressDialog dialog;
    private ProgressDialog readCaptureDialog;
    private String contents;
    private int line;
    private int width;
    private int height;
    private float lineSpacing = 1.2f;
    private TextPaint paint;
    private int currentColor = Color.WHITE;
    private int startOffset = 0;
    private int endOffset = 0;
    private int startOffsetBefore = 0;
    private int endOffsetBefore = 0;
    private int id;
    private DaoFactory factory;
    private MyBookDao myBookDao;
    private BookmarkInfoDao bookmarkInfoDao;
    private MyHandler handler = new MyHandler(this);
    private BroadcastReceiver receiver;
    private MySurfaceView myView;
    private TextView progressRateView;
    private TextView batteryInfoView;
    private TextView currentTimeView;
    private PopupWindow popupTopWindow;
    private PopupWindow popupBottomWindow;
    private PopupWindow settingWindow;
    private boolean isSettingPopup;
    private boolean isLighting;
    private boolean isBold;
    private int currentType;
    private boolean isDefaultType = true;
    int virtualButtonHeight = 0;

    private Map<Integer, String> captureMap;
    private ArrayList<Integer> captureOffsets;
    private String[] captureNames;
    private int currentCapture;
    private int prePageStart;
    private int nextPageEnd;
    private float fontSize;

    private ReadTask readTask;

    private Map<Integer, Integer> offsetMap;
    private Map<Integer, Integer> offsetOppMap;

    private static int VISIBLE_HIDE = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
    private static int VISIBLE_SHOW = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_LAYOUT_FLAGS;

    private final static int[] CANVAS_COLORS = new int[]{
            R.color.white,
            R.color.powderblue,
            R.color.rosybrown,
            R.color.sienna,
            R.color.darkkhaki,
            R.color.tan,
            R.color.olivedrab,
            R.color.black
    };

    private final static int[] PAINT_COLORS = new int[]{
            R.color.black,
            R.color.black,
            R.color.darkgreen,
            R.color.white,
            R.color.black,
            R.color.black,
            R.color.white,
            R.color.darkgray
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        Point size = new Point();
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        display.getSize(size);
        width = size.x;
        height = size.y;
        getWindow().getDecorView().setSystemUiVisibility(VISIBLE_HIDE);
        virtualButtonHeight = AppUtility.getVirtualButtonHeight(this, display, height);

        addBroadcastReceiver();
        paint = new TextPaint();

        dialog = ProgressDialog.show(this, "请稍后", "加载中...");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // init setting
                initSettings();

                // readFile
                Paint.FontMetrics fontMetrics = paint.getFontMetrics();
                double textHeight = Math.ceil(fontMetrics.descent - fontMetrics.ascent);
                line = (int) (height / (textHeight * lineSpacing)) - 2;
                String filePath = getIntent().getStringExtra("path");
                contents = AppUtility.readFile(filePath);

                // get Data from db.
                id = getIntent().getIntExtra("id", 0);
                factory = new DaoFactory(ReaderActivity.this);
                myBookDao = factory.getMyBookDao();
                startOffset = myBookDao.getCurrentOffset(id);
                // endOffset = startOffset;

                // capture info
                CaptureInfoDao captureInfoDao = factory.getCaptureInfoDao();
                captureMap = captureInfoDao.getCaptureInfo(id);
                if (captureMap.size() == 0) {
                    captureMap = AppUtility.getCaptureInfo(contents);
                }

                captureOffsets = new ArrayList<>(captureMap.keySet());
                captureNames = captureMap.values().toArray(new String[captureMap.size()]);
                createChapterInfo();

                // bookmarkInfo.
                bookmarkInfoDao = factory.getBookmarkInfoDao();
                handler.sendEmptyMessage(0);
            }
        });
        thread.start();
    }

    @Override
    public void onResume() {
        if (myView != null) {
            myView.onResume();
            getWindow().getDecorView().setSystemUiVisibility(VISIBLE_HIDE);
        }
        if (readTask != null) {
            readTask.stopTasks();
            readTask.interrupt();
            readTask = null;
        }
        super.onResume();
    }

    @Override
    public void onRestart() {
        if (myView != null) {
            myView.onResume();
            getWindow().getDecorView().setSystemUiVisibility(VISIBLE_HIDE);
        }
        if (readTask != null) {
            readTask.stopTasks();
            readTask.interrupt();
            readTask = null;
        }
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        if (dialog != null) {
            dialog.dismiss();
        }

        if (readCaptureDialog != null) {
            readCaptureDialog.dismiss();
        }

        if (popupTopWindow != null && popupTopWindow.isShowing()) {
            popupTopWindow.dismiss();
        }

        if (popupBottomWindow != null && popupBottomWindow.isShowing()) {
            popupBottomWindow.dismiss();
        }

        if (settingWindow != null && settingWindow.isShowing()) {
            settingWindow.dismiss();
        }

        if (factory != null) {
            factory.closeDB();
        }

        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                startOffset = intent.getIntExtra("currentOffset", 0);
                createChapterInfo();
                Log.i("currentOffset", startOffset + "");
                myView.update();
            }
        }
    }

    private void initSettings() {
        preferences = getSharedPreferences(AppConstants.PREFERENCE_NAME, MODE_PRIVATE);
        // bold
        isBold = preferences.getBoolean(AppConstants.PREF_KEY_BOLD, false);
        paint.setFakeBoldText(isBold);

        // font size
        fontSize = preferences.getFloat(AppConstants.PREF_KEY_FONTSIZE, AppConstants.FONT_SIZE_DEFAULT);
        Log.i("font size", fontSize + "");
        paint.setTextSize(fontSize);

        // lighting
        isLighting = preferences.getBoolean(AppConstants.PREF_KEY_LIGHT, false);
        if (isLighting) {
            paint.setColor(Color.DKGRAY);
            batteryInfoView.setTextColor(Color.DKGRAY);
            currentTimeView.setTextColor(Color.DKGRAY);
            progressRateView.setTextColor(Color.DKGRAY);
            currentColor = Color.BLACK;
        } else {
            // font color
            int fontColor = preferences.getInt(AppConstants.PREF_KEY_FONTCOLOR, Color.BLACK);
            paint.setColor(fontColor);
            batteryInfoView.setTextColor(fontColor);
            currentTimeView.setTextColor(fontColor);
            progressRateView.setTextColor(fontColor);
            // background color
            currentColor = preferences.getInt(AppConstants.PREF_KEY_COLOR, Color.WHITE);
        }

        // Chinese type
        currentType = preferences.getInt(AppConstants.PREF_KEY_TYPE, AppConstants.TYPE_ZH);

        // isDefaultType
        isDefaultType = preferences.getBoolean(AppConstants.PREF_KEY_DEFUALTTYPE, true);
    }

    private void getView() {
        myView = (MySurfaceView) findViewById(R.id.myView);
        myView.setBackgroundColor(currentColor);
        myView.setBitmapProvider(new BitmapProvider());
        myView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View decorView = getWindow().getDecorView();
                if (decorView.getSystemUiVisibility() == VISIBLE_HIDE && !isSettingPopup) {
                    decorView.setSystemUiVisibility(VISIBLE_SHOW);
                    showSettingTopWindow();
                    showSettingBottomWindow();
                } else {
                    decorView.setSystemUiVisibility(VISIBLE_HIDE);
                }
                if (isSettingPopup) {
                    isSettingPopup = false;
                }
            }
        });
        dialog.dismiss();
    }

    private void createChapterInfo() {
        long startTime = System.currentTimeMillis();
        int currentCaptureOffset = 0;
        //int preCaptureOffset = 0;
        int nextCaptureOffset = 0;

        for (int offset : captureOffsets) {
            if (startOffset < offset) {
                nextCaptureOffset = offset;
                break;
            }

            //preCaptureOffset = currentCaptureOffset;
            currentCaptureOffset = offset;
        }

        offsetMap = new HashMap<>();
        offsetOppMap = new HashMap<>();

//        if (currentCaptureOffset > 0) {
//            makePageOffsets(preCaptureOffset, currentCaptureOffset);
//        }

        if (readTask != null) {
            readTask.stopTasks();
            readTask.interrupt();
        }
        readTask = new ReadTask(currentCaptureOffset);
        readTask.start();

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

            offsetMap.put(start, end);
            offsetOppMap.put(end, start);
        }

        // 因为字体变化而导致起点位置变化的时候，找和变化前最近的起点.
        if (!hasStartOffset) {
            startOffset = nearestOffset;
        }

        long endTime = System.currentTimeMillis();
        Log.i("Lining time : ", endTime - startTime + "ms");
    }

    private void addBroadcastReceiver() {
        batteryInfoView = (TextView) findViewById(R.id.batteryInfo);
        currentTimeView = (TextView) findViewById(R.id.currentTime);
        progressRateView = (TextView) findViewById(R.id.progressRate);
        currentTimeView.setText(AppConstants.DATE_FORMAT.format(new Date()));

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                    batteryInfoView.setText("电量:" + intent.getIntExtra("level", 0) + "%");
                } else if (action.equals(Intent.ACTION_TIME_TICK)) {
                    currentTimeView.setText(AppConstants.DATE_FORMAT.format(new Date()));
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(receiver, filter);
    }

    private void showSettingTopWindow() {
        popupTopWindow = new PopupWindow(ReaderActivity.this);

        View popupView = getLayoutInflater().inflate(R.layout.setting_top, null);
        Button backBtn = (Button) popupView.findViewById(R.id.back);
        backBtn.setText(ReaderActivity.this.getIntent().getStringExtra("name"));
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReaderActivity.this.finish();
            }
        });

        Button bookmarkBtn = (Button) popupView.findViewById(R.id.bookmark);
        bookmarkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BookmarkInfoEntity entity = new BookmarkInfoEntity();
                entity.setId(id);
                entity.setOffset(startOffset);
                entity.setCaptureName(captureNames[captureOffsets.indexOf(currentCapture)]);
                entity.setProgress(progressRateView.getText().toString());
                bookmarkInfoDao.insert(entity);
                if (popupTopWindow != null && popupTopWindow.isShowing()) {
                    popupTopWindow.dismiss();
                }
                if (popupBottomWindow != null && popupBottomWindow.isShowing()) {
                    popupBottomWindow.dismiss();
                }
                Toast.makeText(ReaderActivity.this, "追加书签完毕", Toast.LENGTH_SHORT).show();
            }
        });

        popupTopWindow.setContentView(popupView);
        popupTopWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupTopWindow.setFocusable(true);
        popupTopWindow.setWindowLayoutMode(width, WindowManager.LayoutParams.WRAP_CONTENT);
        popupTopWindow.setWidth(width);
        popupTopWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        int status_height = getResources().getDimensionPixelSize(resourceId);
        popupTopWindow.showAtLocation(getWindow().getDecorView(), Gravity.TOP, 0, status_height);
        popupTopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (popupBottomWindow != null && popupBottomWindow.isShowing()) {
                    popupBottomWindow.dismiss();
                }
                getWindow().getDecorView().setSystemUiVisibility(VISIBLE_HIDE);
            }
        });
    }

    private void showSettingBottomWindow() {
        popupBottomWindow = new PopupWindow(ReaderActivity.this);

        View popupView = getLayoutInflater().inflate(R.layout.setting_bottom, null);
        TextView seekText = (TextView) popupView.findViewById(R.id.seekText);
        seekText.setText(captureNames[captureOffsets.indexOf(currentCapture)]);
        Button btnCat = (Button) popupView.findViewById(R.id.catalogue);
        btnCat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ReaderActivity.this, CaptureActivity.class);
                intent.putExtra("id", id);
                intent.putExtra("captureNames", captureNames);
                intent.putIntegerArrayListExtra("offsets", captureOffsets);
                intent.putExtra("currentCapture", currentCapture);
                intent.putExtra("name", ReaderActivity.this.getIntent().getStringExtra("name"));
                if (popupTopWindow != null && popupTopWindow.isShowing()) {
                    popupTopWindow.dismiss();
                }
                if (popupBottomWindow != null && popupBottomWindow.isShowing()) {
                    popupBottomWindow.dismiss();
                }
                startActivityForResult(intent, 1);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });

        Button btnShare = (Button) popupView.findViewById(R.id.share);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        Button btnSet = (Button) popupView.findViewById(R.id.set);
        btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingWindow();
            }
        });

        final Button btnNight = (Button) popupView.findViewById(R.id.night);
        if (isLighting) {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_action_light);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            btnNight.setCompoundDrawables(null, drawable, null, null);
            btnNight.setTextColor(Color.RED);
        } else {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_action_bulb);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            btnNight.setCompoundDrawables(null, drawable, null, null);
            btnNight.setTextColor(Color.WHITE);
        }
        btnNight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLighting) {
                    int textColor = Color.DKGRAY;
                    paint.setColor(textColor);
                    batteryInfoView.setTextColor(textColor);
                    currentTimeView.setTextColor(textColor);
                    progressRateView.setTextColor(textColor);
                    currentColor = Color.BLACK;
                    isLighting = true;
                } else {
                    int textColor = preferences.getInt(AppConstants.PREF_KEY_FONTCOLOR, Color.BLACK);
                    paint.setColor(textColor);
                    batteryInfoView.setTextColor(textColor);
                    currentTimeView.setTextColor(textColor);
                    progressRateView.setTextColor(textColor);
                    currentColor = preferences.getInt(AppConstants.PREF_KEY_COLOR, Color.WHITE);
                    isLighting = false;
                }

                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(AppConstants.PREF_KEY_LIGHT, isLighting);
                editor.apply();

                myView.update();
                if (popupTopWindow != null && popupTopWindow.isShowing()) {
                    popupTopWindow.dismiss();
                }
                if (popupBottomWindow != null && popupBottomWindow.isShowing()) {
                    popupBottomWindow.dismiss();
                }
                getWindow().getDecorView().setSystemUiVisibility(VISIBLE_HIDE);
            }
        });

        popupBottomWindow.setContentView(popupView);
        popupBottomWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupBottomWindow.setWindowLayoutMode(width, WindowManager.LayoutParams.WRAP_CONTENT);
        popupBottomWindow.setWidth(width);
        popupBottomWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popupBottomWindow.showAtLocation(getWindow().getDecorView(), Gravity.BOTTOM, 0, virtualButtonHeight);
        popupBottomWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (popupTopWindow != null && popupTopWindow.isShowing()) {
                    popupTopWindow.dismiss();
                }
                getWindow().getDecorView().setSystemUiVisibility(VISIBLE_HIDE);
            }
        });
    }

    private void showSettingWindow() {
        if (popupTopWindow != null && popupTopWindow.isShowing()) {
            popupTopWindow.dismiss();
        }
        if (popupBottomWindow != null && popupBottomWindow.isShowing()) {
            popupBottomWindow.dismiss();
        }
        getWindow().getDecorView().setSystemUiVisibility(VISIBLE_HIDE);

        settingWindow = new PopupWindow(this);

        View popupView = getLayoutInflater().inflate(R.layout.setting, null);
        final LinearLayout btnLayout = (LinearLayout) popupView.findViewById(R.id.color_sets);
        int redRingIndex = preferences.getInt(AppConstants.PREF_KEY_REDRINGINDEX, -1);
        for (int i = 0; i < CANVAS_COLORS.length; i++) {
            final LayerDrawable layerList = (LayerDrawable) getResources().getDrawable(R.drawable.colorset);
            final GradientDrawable btnRing = (GradientDrawable) layerList.findDrawableByLayerId(R.id.btn_ring);
            final GradientDrawable btnContent = (GradientDrawable) layerList.findDrawableByLayerId(R.id.btn_content);

            final int index = i;
            final Button btnColorSet = new Button(this);
            btnColorSet.setText("Aa");
            btnColorSet.setTextSize(12);
            // setColor to button.
            btnContent.setColor(getResources().getColor(CANVAS_COLORS[index]));
            btnColorSet.setBackground(layerList);
            btnColorSet.setTextColor(getResources().getColor(PAINT_COLORS[index]));
            if (redRingIndex == index) {
                btnRing.setColor(Color.RED);
            } else {
                btnRing.setColor(Color.WHITE);
            }

            btnColorSet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentColor = getResources().getColor(CANVAS_COLORS[index]);
                    int textColor = getResources().getColor(PAINT_COLORS[index]);
                    paint.setColor(textColor);
                    batteryInfoView.setTextColor(textColor);
                    currentTimeView.setTextColor(textColor);
                    progressRateView.setTextColor(textColor);

                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt(AppConstants.PREF_KEY_COLOR, currentColor);
                    editor.putInt(AppConstants.PREF_KEY_FONTCOLOR, textColor);
                    editor.putInt(AppConstants.PREF_KEY_REDRINGINDEX, index);
                    editor.apply();
                    myView.update();

                    btnRing.setColor(getResources().getColor(R.color.red));
                    for (int i = 0; i < btnLayout.getChildCount(); i++) {
                        if (i == index) {
                            continue;
                        }
                        Button btn = (Button) btnLayout.getChildAt(i);
                        GradientDrawable ring = (GradientDrawable) ((LayerDrawable) btn.getBackground()).findDrawableByLayerId(R.id.btn_ring);
                        ring.setColor(Color.WHITE);
                    }
                }
            });
            btnLayout.addView(btnColorSet);
        }

        final Button btnBold = (Button) popupView.findViewById(R.id.bord);
        btnBold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBold) {
                    paint.setFakeBoldText(false);
                    btnBold.setTextColor(Color.WHITE);
                    isBold = false;
                } else {
                    paint.setFakeBoldText(true);
                    btnBold.setTextColor(Color.RED);
                    isBold = true;
                }
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(AppConstants.PREF_KEY_BOLD, isBold);
                editor.apply();
                myView.update();
            }
        });

        final Button btnPlus = (Button) popupView.findViewById(R.id.plus);
        final Button btnMinus = (Button) popupView.findViewById(R.id.minus);
        btnPlus.setEnabled(preferences.getBoolean(AppConstants.PREF_KEY_PLUS_ENABLE, true));
        btnMinus.setEnabled(preferences.getBoolean(AppConstants.PREF_KEY_MINUS_ENABLE, true));
        if (!btnPlus.isEnabled()) {
            btnPlus.setTextColor(Color.GRAY);
        }
        if (!btnMinus.isEnabled()) {
            btnMinus.setTextColor(Color.GRAY);
        }
        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fontSize += 2;
                paint.setTextSize(fontSize);
                if (fontSize >= AppConstants.FONT_SIZE_MAX) {
                    btnPlus.setEnabled(false);
                    btnPlus.setTextColor(Color.GRAY);
                }
                if (!btnMinus.isEnabled()) {
                    btnMinus.setEnabled(true);
                    btnMinus.setTextColor(Color.WHITE);
                }

                SharedPreferences.Editor editor = preferences.edit();
                editor.putFloat(AppConstants.PREF_KEY_FONTSIZE, fontSize);
                editor.putBoolean(AppConstants.PREF_KEY_PLUS_ENABLE, btnPlus.isEnabled());
                editor.putBoolean(AppConstants.PREF_KEY_MINUS_ENABLE, btnMinus.isEnabled());
                editor.apply();

                resetOffsetMap();
                myView.update();
            }
        });
        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fontSize -= 2;
                paint.setTextSize(fontSize);
                if (fontSize <= AppConstants.FONT_SIZE_MIN) {
                    btnMinus.setEnabled(false);
                    btnMinus.setTextColor(Color.GRAY);
                }
                if (!btnPlus.isEnabled()) {
                    btnPlus.setEnabled(true);
                    btnPlus.setTextColor(Color.WHITE);
                }

                SharedPreferences.Editor editor = preferences.edit();
                editor.putFloat(AppConstants.PREF_KEY_FONTSIZE, fontSize);
                editor.putBoolean(AppConstants.PREF_KEY_PLUS_ENABLE, btnPlus.isEnabled());
                editor.putBoolean(AppConstants.PREF_KEY_MINUS_ENABLE, btnMinus.isEnabled());
                editor.apply();

                resetOffsetMap();
                myView.update();
            }
        });

        Button btnTransfer = (Button) popupView.findViewById(R.id.chinese);
        btnTransfer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDefaultType = !isDefaultType;
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(AppConstants.PREF_KEY_DEFUALTTYPE, isDefaultType);
                editor.apply();
                myView.update();
            }
        });

        settingWindow.setContentView(popupView);
        settingWindow.setOutsideTouchable(true);
        settingWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        settingWindow.setWindowLayoutMode(width, WindowManager.LayoutParams.WRAP_CONTENT);
        settingWindow.setWidth(width);
        settingWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        settingWindow.showAtLocation(getWindow().getDecorView(), Gravity.BOTTOM, 0, virtualButtonHeight);
        isSettingPopup = true;
    }

    private String changeText(String str) {
        if (!isDefaultType && currentType == AppConstants.TYPE_ZH) {
            str = AppUtility.convertS2J(str);
        } else if (!isDefaultType && currentType == AppConstants.TYPE_TW) {
            str = AppUtility.convertJ2S(str);
        }
        return str;
    }

    private void resetOffsetMap() {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        double textHeight = Math.ceil(fontMetrics.descent - fontMetrics.ascent);
        line = (int) (height / (textHeight * lineSpacing)) - 2;

        offsetMap = new HashMap<>();
        offsetOppMap = new HashMap<>();

        int currentCaptureOffset = 0;
        //int preCaptureOffset = 0;
        int nextCaptureOffset = 0;

        for (int offset : captureOffsets) {
            if (startOffset < offset) {
                nextCaptureOffset = offset;
                break;
            }

            //preCaptureOffset = currentCaptureOffset;
            currentCaptureOffset = offset;
        }

//        if (currentCaptureOffset > 0) {
//            makePageOffsets(preCaptureOffset, currentCaptureOffset);
//        }
        if (readTask != null) {
            readTask.stopTasks();
            readTask.interrupt();
        }
        readTask = new ReadTask(currentCaptureOffset);
        readTask.start();

        if (currentCaptureOffset < startOffset) {
            makePageOffsets(currentCaptureOffset, startOffset);
        }

        makePageOffsets(startOffset, nextCaptureOffset);
    }

    /**
     * Bitmap provider.
     */
    private class BitmapProvider implements MySurfaceView.BitmapProvider {
        private boolean hasNext = true;
        private boolean hasPre = true;
        private boolean hasNextBefore = true;
        private boolean hasPreBefore = true;
        private Bitmap bitmap;
        private boolean readed;

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
                Log.i("pre-left", preStart + ", " + preEnd);
                Log.i("left", startOffset + ", " + endOffset);
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
                Log.i("right", startOffset + ", " + endOffset);
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
                readed = false;
            }
            if (!readed && (state == MySurfaceView.BitmapState.ToPreLeft || state == MySurfaceView.BitmapState.ToRight)) {
                if (readTask != null) {
                    readTask.stopTasks();
                    readTask.interrupt();
                    Log.i("[Task page]", "task stopped");
                }
                readTask = new ReadTask(currentCapture);
                readTask.start();
                readed = true;
            }

            staticLayout.draw(canvas);
            Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            drawable.draw(canvas);

            return bitmap;
        }
    }

    private void readPreCaptureInfo(final int startOffset) {
        readCaptureDialog = ProgressDialog.show(ReaderActivity.this, "请稍后", "加载上一章中...");
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

                    offsetMap.put(start, end);
                    offsetOppMap.put(end, start);
                }
                long endTime = System.currentTimeMillis();
                Log.i("Lineing time : ", (endTime - startTime) / 1000 + "s");

                prePageStart = start;
                //return start;
                handler.sendEmptyMessage(1);
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void readNextCaptureInfo(final int startOffset) {
        readCaptureDialog = ProgressDialog.show(ReaderActivity.this, "请稍后", "加载下一章中...");
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

                    offsetMap.put(start, end);
                    offsetOppMap.put(end, start);
                }
                long endTime = System.currentTimeMillis();
                Log.i("Lineing time : ", (endTime - startTime) / 1000 + "s");

                // preCapture read.
                if (!offsetOppMap.containsKey(captureOffset) && index > 0) {
                    int preCaptureOffset = captureOffsets.get(index - 1);
                    String preContent = contents.substring(preCaptureOffset, captureOffset);
                    preContent = changeText(preContent);
                    StaticLayout preLayout = new StaticLayout(preContent, 0, preContent.length(), paint, width - 100, Layout.Alignment.ALIGN_NORMAL, lineSpacing, 0.0f, false);

                    int preLineCount = preLayout.getLineCount();
                    for (int i = 0; i < preLineCount; i += line) {
                        int start = preCaptureOffset + preLayout.getLineStart(i);
                        int endLine = i + line > preLineCount ? preLineCount : i + line;
                        int end = preCaptureOffset + preLayout.getLineEnd(endLine - 1);

                        offsetMap.put(start, end);
                        offsetOppMap.put(end, start);
                    }
                }

                handler.sendEmptyMessage(1);
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<ReaderActivity> myWeakReference;

        public MyHandler(ReaderActivity activity) {
            myWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ReaderActivity activity = myWeakReference.get();
            if (activity == null) {
                return;
            }
            int code = msg.what;
            switch (code) {
                case 0:
                    activity.getView();
                    break;
                case 1:
                    activity.readCaptureDialog.dismiss();
                    activity.getWindow().getDecorView().setSystemUiVisibility(VISIBLE_HIDE);
                    break;
            }
        }
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

    public class ReadTask extends Thread {
        private int currentCaptureOffset;
        private int preCaptureOffset;
        private int nextCaptureOffset;
        private Thread thread1;
        private Thread thread2;

        public ReadTask(int currentCaptureOffset) {
            this.currentCaptureOffset = currentCaptureOffset;
            int index = captureOffsets.indexOf(currentCaptureOffset);
            if (index > 0) {
                preCaptureOffset = captureOffsets.get(index - 1);
            } else {
                preCaptureOffset = 0;
            }

            if (index < captureOffsets.size() - 1) {
                nextCaptureOffset = captureOffsets.get(index + 1);
            } else {
                nextCaptureOffset = captureOffsets.get(captureOffsets.size() - 1);
            }
        }

        public void stopTasks() {
            if (thread1 != null && thread1.isAlive()) {
                thread1.interrupt();
            }
            if (thread2 != null && thread2.isAlive()) {
                thread2.interrupt();
            }
        }

        @Override
        public void run() {
            Log.i("[Task]", "Read Task start.");
            if (preCaptureOffset != currentCaptureOffset && !offsetMap.containsKey(preCaptureOffset)) {
                thread1 = new Thread(new Runnable() {
                    public void run() {
                        Log.i("[Task]", "Task1 start.");
                        long startTime = System.currentTimeMillis();
                        makePageOffsets(preCaptureOffset, currentCaptureOffset);
                        long endTime = System.currentTimeMillis();
                        Log.i("[Task]", "Task1 end." + (endTime - startTime) + "ms");
                    }
                });
                thread1.start();
            }

            if (currentCaptureOffset != nextCaptureOffset && !offsetMap.containsKey(nextCaptureOffset)) {
                thread2 = new Thread(new Runnable() {
                    public void run() {
                        Log.i("[Task]", "Task2 start.");
                        long startTime = System.currentTimeMillis();
                        int nextOffset;
                        int index = captureOffsets.indexOf(nextCaptureOffset);
                        if (index == -1) {
                            return;
                        } else if (index == captureOffsets.size() - 1) {
                            nextOffset = contents.length();
                        } else {
                            nextOffset = captureOffsets.get(index + 1);
                        }
                        makePageOffsets(nextCaptureOffset, nextOffset);
                        long endTime = System.currentTimeMillis();
                        Log.i("[Task]", "Task2 end." + (endTime - startTime) + "ms");
                    }
                });
                thread2.start();
            }
            Log.i("[Task]", "Read Task done.");
        }
    }
}
