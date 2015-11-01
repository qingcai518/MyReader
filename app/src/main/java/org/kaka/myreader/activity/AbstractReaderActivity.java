package org.kaka.myreader.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextPaint;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.common.AppUtility;
import org.kaka.myreader.dlayer.dao.BookmarkInfoDao;
import org.kaka.myreader.dlayer.dao.DaoFactory;
import org.kaka.myreader.dlayer.dao.MyBookDao;
import org.kaka.myreader.dlayer.entities.BookmarkInfoEntity;
import org.kaka.myreader.opengl.MySurfaceView;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Map;

public abstract class AbstractReaderActivity extends Activity {
    protected ProgressDialog readChapterDialog;
    protected int line;
    protected int width;
    protected int height;
    protected float lineSpacing = 1.2f;
    protected TextPaint paint;
    protected int currentColor = Color.WHITE;
    protected int startOffset = 0;
    protected int currentIndex = 1;
    protected String id;
    protected DaoFactory factory;
    protected MyBookDao myBookDao;
    protected MyHandler handler = new MyHandler(this);
    protected MySurfaceView myView;
    protected TextView progressRateView;
    protected Map<Integer, String> chapterMap;
    protected String[] captureNames;
    protected Map<Integer, Integer> offsetMap;
    protected Map<Integer, Integer> offsetOppMap;

    private final static String text = "Aa";
    private BookmarkInfoDao bookmarkInfoDao;
    private BroadcastReceiver receiver;
    private SharedPreferences preferences;
    private ProgressDialog dialog;
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
    private int virtualButtonHeight = 0;
    private float fontSize;
    private static int VISIBLE_HIDE = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
            | View.SYSTEM_UI_FLAG_IMMERSIVE;
    private static int VISIBLE_SHOW = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

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
            R.color.darkslategray
    };

    public enum BookType {
        TXT, EPUB
    }

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
        if (virtualButtonHeight > 0) {
            // 为显示status bar的底色
            RelativeLayout layout = (RelativeLayout) findViewById(R.id.title);
            layout.addView(new View(this));
        }

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
                getContents(filePath);

                // get Data from db.
                id = getIntent().getStringExtra("id");
                factory = new DaoFactory(AbstractReaderActivity.this);
                myBookDao = factory.getMyBookDao();
                startOffset = myBookDao.getCurrentOffset(id)[0];
                currentIndex = myBookDao.getCurrentOffset(id)[1];
                myBookDao.updateReadTime(id);

                // capture info
                getChapters();
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
        releaseTask();
        super.onResume();
    }

    @Override
    public void onRestart() {
        if (myView != null) {
            myView.onResume();
            getWindow().getDecorView().setSystemUiVisibility(VISIBLE_HIDE);
        }
        releaseTask();
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        if (dialog != null) {
            dialog.dismiss();
        }

        if (readChapterDialog != null) {
            readChapterDialog.dismiss();
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

    protected void getView() {
        myView = (MySurfaceView) findViewById(R.id.myView);
        myView.setBackgroundColor(currentColor);
        setProvider();
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
                    String info = "电量:" + intent.getIntExtra("level", 0) + "%";
                    batteryInfoView.setText(info);
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
        popupTopWindow = new PopupWindow(this);

        View popupView = getLayoutInflater().inflate(R.layout.setting_top, null);
        Button backBtn = (Button) popupView.findViewById(R.id.back);
        backBtn.setText(AbstractReaderActivity.this.getIntent().getStringExtra("name"));
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AbstractReaderActivity.this.setResult(RESULT_OK);
                AbstractReaderActivity.this.finish();
            }
        });

        Button bookmarkBtn = (Button) popupView.findViewById(R.id.bookmark);
        bookmarkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BookmarkInfoEntity entity = new BookmarkInfoEntity();
                entity.setId(id);
                entity.setChapterIndex(currentIndex);
                entity.setOffset(startOffset);
                entity.setChapterName(getChapterName());
                entity.setProgress(progressRateView.getText().toString());
                bookmarkInfoDao.insert(entity);
                if (popupTopWindow != null && popupTopWindow.isShowing()) {
                    popupTopWindow.dismiss();
                }
                Toast.makeText(AbstractReaderActivity.this, "追加书签完毕", Toast.LENGTH_SHORT).show();
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

    abstract protected String getChapterName();

    private void showSettingBottomWindow() {
        popupBottomWindow = new PopupWindow(this);

        View popupView = getLayoutInflater().inflate(R.layout.setting_bottom, null);
        TextView seekText = (TextView) popupView.findViewById(R.id.seekText);
        seekText.setText(getChapterName());
        Button btnChapter = (Button) popupView.findViewById(R.id.catalogue);
        btnChapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AbstractReaderActivity.this, ChapterActivity.class);
                intent.putExtra("id", id);
                intent.putExtra("captureNames", captureNames);
                setIntentChapterInfo(intent);
                intent.putExtra("name", AbstractReaderActivity.this.getIntent().getStringExtra("name"));
                if (popupTopWindow != null && popupTopWindow.isShowing()) {
                    popupTopWindow.dismiss();
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
            if (drawable != null) {
                drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                btnNight.setCompoundDrawables(null, drawable, null, null);
            }
            btnNight.setTextColor(Color.RED);
        } else {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_action_bulb);
            if (drawable != null) {
                drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                btnNight.setCompoundDrawables(null, drawable, null, null);
            }
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
            }
        });

        final Button btnPre = (Button) popupView.findViewById(R.id.preChapter);
        if (!hasPreChapter()) {
            btnPre.setEnabled(false);
            btnPre.setTextColor(Color.GRAY);
        } else {
            btnPre.setEnabled(true);
            btnPre.setTextColor(Color.WHITE);
        }

        final Button btnNext = (Button) popupView.findViewById(R.id.nextChapter);
        if (!hasNextChapter()) {
            btnNext.setEnabled(false);
            btnNext.setTextColor(Color.GRAY);
        } else {
            btnNext.setEnabled(true);
            btnNext.setTextColor(Color.WHITE);
        }

        btnPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPreChapter(btnPre, btnNext);

                if (popupTopWindow != null && popupTopWindow.isShowing()) {
                    popupTopWindow.dismiss();
                }
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getNextChapter(btnPre, btnNext);

                if (popupTopWindow != null && popupTopWindow.isShowing()) {
                    popupTopWindow.dismiss();
                }
            }
        });

        SeekBar progressSeek = (SeekBar) popupView.findViewById(R.id.progressSeek);
        progressSeek.setMax(100);
        final int distance = getDistance();
        int progress = getProgress(distance);
        progressSeek.setProgress(progress);
        Log.i("seek to", progress + "");

        progressSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int progress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                this.progress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int endOffset = getEndOffset(distance, progress);
                int nearestOffset = 0;
                for (int key : offsetMap.keySet()) {
                    if (key <= endOffset) {
                        nearestOffset = key > nearestOffset ? key : nearestOffset;
                    }
                }
                startOffset = nearestOffset;
                myView.update();
            }
        });

        popupBottomWindow.setContentView(popupView);
        popupBottomWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupBottomWindow.setWindowLayoutMode(width, WindowManager.LayoutParams.WRAP_CONTENT);
        popupBottomWindow.setWidth(width);
        popupBottomWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popupBottomWindow.showAtLocation(getWindow().getDecorView(), Gravity.BOTTOM, 0, virtualButtonHeight);
    }

    private void showSettingWindow() {
        if (popupTopWindow != null && popupTopWindow.isShowing()) {
            popupTopWindow.dismiss();
        }

        settingWindow = new PopupWindow(this);

        View popupView = getLayoutInflater().inflate(R.layout.setting, null);
        final LinearLayout btnLayout = (LinearLayout) popupView.findViewById(R.id.color_sets);
        int redRingIndex = preferences.getInt(AppConstants.PREF_KEY_REDRINGINDEX, -1);
        for (int i = 0; i < CANVAS_COLORS.length; i++) {
            final LayerDrawable layerList = (LayerDrawable) getResources().getDrawable(R.drawable.colorset);
            if (layerList == null) {
                continue;
            }
            final GradientDrawable btnRing = (GradientDrawable) layerList.findDrawableByLayerId(R.id.btn_ring);
            final GradientDrawable btnContent = (GradientDrawable) layerList.findDrawableByLayerId(R.id.btn_content);

            final int index = i;
            final Button btnColorSet = new Button(this);
            btnColorSet.setText(text);
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
                    isLighting = false;
                    editor.putBoolean(AppConstants.PREF_KEY_LIGHT, false);
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

        final SeekBar lightSeek = (SeekBar) popupView.findViewById(R.id.lightProgress);
        lightSeek.setMax(255);
        WindowManager.LayoutParams params = AbstractReaderActivity.this.getWindow().getAttributes();
        Log.i("test", "" + params.screenBrightness);
        int lightNow = (int) (params.screenBrightness * 255);
        if (lightNow == -255) {
            try {
                lightNow = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            } catch (Exception e) {
                Log.e("Light", e.getMessage());
            }
        }
        Log.i("Light Now", lightNow + "");
        lightSeek.setProgress(lightNow);
        lightSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                WindowManager.LayoutParams params = AbstractReaderActivity.this.getWindow().getAttributes();
                params.screenBrightness = progress / 255f;
                AbstractReaderActivity.this.getWindow().setAttributes(params);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

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
        settingWindow.showAtLocation(getWindow().getDecorView(), Gravity.BOTTOM, 0, 0);
        isSettingPopup = true;
    }

    protected String changeText(String str) {
        if (!isDefaultType && currentType == AppConstants.TYPE_ZH) {
            str = AppUtility.convertS2J(str);
        } else if (!isDefaultType && currentType == AppConstants.TYPE_TW) {
            str = AppUtility.convertJ2S(str);
        }
        return str;
    }

    protected static class MyHandler extends Handler {
        private final WeakReference<AbstractReaderActivity> myWeakReference;

        public MyHandler(AbstractReaderActivity activity) {
            myWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            AbstractReaderActivity activity = myWeakReference.get();
            if (activity == null) {
                return;
            }
            int code = msg.what;
            switch (code) {
                case 0:
                    activity.getView();
                    break;
                case 1:
                    activity.readChapterDialog.dismiss();
                    activity.getWindow().getDecorView().setSystemUiVisibility(VISIBLE_HIDE);
                    break;
            }
        }
    }

    abstract protected void resetOffsetMap();

    abstract protected void setProvider();

    abstract protected void createChapterInfo();

    abstract protected void releaseTask();

    abstract protected void getContents(String filePath);

    abstract protected void getChapters();

    abstract protected boolean hasPreChapter();

    abstract protected boolean hasNextChapter();

    abstract protected void getPreChapter(Button btnPre, Button btnNext);

    abstract protected void getNextChapter(Button btnPre, Button btnNext);

    abstract protected int getDistance();

    abstract protected int getProgress(int distance);

    abstract protected int getEndOffset(int distance, int progress);

    abstract protected void setIntentChapterInfo(Intent intent);
}
