package org.kaka.myreader.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.task.MailSendTask;

import java.util.Timer;
import java.util.TimerTask;

public class RegisterAuthCodeActivity extends Activity {
    private String authCode;
    private TextView countView;
    private int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_authcode);

        init();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    private void init() {
        ImageButton btnBack = (ImageButton) findViewById(R.id.back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        TextView messageView = (TextView) findViewById(R.id.message);
        TextView infoView = (TextView) findViewById(R.id.info);
        Intent intent = getIntent();
        final int type = intent.getIntExtra(AppConstants.KEY_REGIST_TYPE, AppConstants.TYPE_PHONE);
        if (type == AppConstants.TYPE_MAIL) {
            messageView.setText("我们已经发送验证码到这个邮箱：");
        } else {
            messageView.setText("我们已经发送验证码到这个号码：");
        }

        final String info = intent.getStringExtra(AppConstants.KEY_INFO);
        infoView.setText(info);

        Button btnNext = (Button) findViewById(R.id.next);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (authCode == null || authCode.trim().length() == 0) {
                    Toast.makeText(RegisterAuthCodeActivity.this, "请输入验证码", Toast.LENGTH_SHORT).show();
                }
                if (authCode.equals(AppConstants.CurrentAuthCode)) {
                    Intent intent = new Intent(RegisterAuthCodeActivity.this, RegisterInfoActivity.class);
                    intent.putExtra(AppConstants.KEY_INFO, info);
                    intent.putExtra(AppConstants.KEY_REGIST_TYPE, type);
                    RegisterAuthCodeActivity.this.startActivityForResult(intent, 0);
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                } else {
                    Toast.makeText(RegisterAuthCodeActivity.this, "验证码不正确", Toast.LENGTH_SHORT).show();
                }
            }
        });

        EditText authCodeText = (EditText) findViewById(R.id.authCode);
        authCodeText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                authCode = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        countView = (TextView) findViewById(R.id.counter);

        countView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!"重新发送".equals(countView.getText())) {
                    return;
                }

                if (type == AppConstants.TYPE_MAIL) {
                    countView.getPaint().setFlags(Paint.LINEAR_TEXT_FLAG);
                    new MailSendTask(RegisterAuthCodeActivity.this).execute(info);
                } else {

                }
            }
        });
        beginCount();
    }

    public void beginCount() {
        count = 20;
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (count == 0) {
                    countView.setText("重新发送");
                    countView.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
                    handler.removeCallbacks(this);
                } else {
                    countView.setText("约" + count-- + "秒后收到");
                    handler.postDelayed(this, 1000);
                }
            }
        };
        runnable.run();

    }
}
