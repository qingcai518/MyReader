package org.kaka.myreader.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.task.AddCommitTask;

public class AddCommitActivity extends FragmentActivity {
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commit);

        Button backBtn = (Button) findViewById(R.id.back);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddCommitActivity.this.finish();
            }
        });

        Button submitBtn = (Button) findViewById(R.id.submitCommit);
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = getSharedPreferences(AppConstants.PREFERENCE_NAME, Activity.MODE_PRIVATE);
                dialog = ProgressDialog.show(AddCommitActivity.this, "请稍等", "正在提交评论");
                RatingBar ratingBar = (RatingBar) findViewById(R.id.score);
                float ratingValue = ratingBar.getRating();
                EditText textView = (EditText) findViewById(R.id.commit);
                String commitText = textView.getText().toString();
                String userId = preferences.getString(AppConstants.PREF_KEY_USERID, "");

                Intent intent = AddCommitActivity.this.getIntent();
                String bookId = intent.getStringExtra(CloudDetailActivity.KEY_BOOK_ID);

                AddCommitTask task = new AddCommitTask(AddCommitActivity.this);
                task.execute(commitText, String.valueOf(ratingValue), bookId, userId);
            }
        });
    }

    public void finishConnection() {
        if (dialog != null) {
            dialog.dismiss();
        }

        finish();
    }
}
