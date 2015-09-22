package org.kaka.myreader.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.kaka.myreader.R;
import org.kaka.myreader.composite.ClipZoomImageView;
import org.kaka.myreader.task.UserUpdateTask;

public class PhotoClipActivity extends Activity {
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clipimage);

        init();
    }

    private void init() {
        Intent intent = getIntent();
        String path = intent.getStringExtra(PhotoPickerActivity.IMAGEPATH);
        final ClipZoomImageView view = (ClipZoomImageView) findViewById(R.id.myImage);

        Uri uri = Uri.parse("file://" + path);
        view.setImageURI(uri);

        Button btnOK = (Button) findViewById(R.id.ok);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = ProgressDialog.show(PhotoClipActivity.this, "请稍等", "正在上传头像");
                Bitmap bitmap = view.clip();
                new UserUpdateTask(PhotoClipActivity.this).execute(bitmap);
            }
        });
    }

    public void finishTask() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        setResult(0);
        finish();
    }
}
