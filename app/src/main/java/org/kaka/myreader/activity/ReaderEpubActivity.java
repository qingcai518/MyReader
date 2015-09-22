package org.kaka.myreader.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;

import org.kaka.myreader.R;

public class ReaderEpubActivity extends Activity {
    private ProgressDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        dialog = ProgressDialog.show(this, "请稍后", "加载中...");



        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

            }
        });
        thread.start();
    }
}
