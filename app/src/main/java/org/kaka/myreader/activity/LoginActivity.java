package org.kaka.myreader.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.task.LoginTask;

public class LoginActivity extends Activity {
    private ProgressDialog dialog;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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
        TextView textView = (TextView) findViewById(R.id.register);
        textView.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterByPhoneActivity.class);
                LoginActivity.this.startActivityForResult(intent, 0);
                LoginActivity.this.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });

        final EditText userView = (EditText) findViewById(R.id.user);
        final EditText passwordView = (EditText) findViewById(R.id.password);

        Button btn = (Button) findViewById(R.id.login);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userId = userView.getText().toString().trim();
                String password = passwordView.getText().toString();
                if (userId.length() == 0) {
                    Toast.makeText(LoginActivity.this, "请输入用户名", Toast.LENGTH_SHORT).show();
                    userView.requestFocus();
                    return;
                }
                if (password.trim().length() == 0) {
                    Toast.makeText(LoginActivity.this, "请输入密码", Toast.LENGTH_SHORT).show();
                    passwordView.requestFocus();
                    return;
                }

                dialog = ProgressDialog.show(LoginActivity.this, "登录中", "请稍后");
                new LoginTask(LoginActivity.this).execute(userId, password);
            }
        });
    }

    public void finishTask(int result, String[] infos) {
        if (dialog != null) {
            dialog.dismiss();
        }

        switch (result) {
            case 0:
                SharedPreferences preferences = getSharedPreferences(AppConstants.PREFERENCE_NAME, Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(AppConstants.PREF_KEY_LOGIN, true);
                editor.putString(AppConstants.PREF_KEY_USERNAME, infos[0]);
                editor.putString(AppConstants.PREF_KEY_POINT, infos[1]);
                editor.putString(AppConstants.PREF_KEY_SEX, infos[2]);
                editor.putString(AppConstants.PREF_KEY_USER_IMAGE, infos[3]);
                editor.putString(AppConstants.PREF_KEY_USERID, userId);

                editor.apply();
                setResult(RESULT_OK);
                finish();
                break;
            case 1:
                Toast.makeText(this, "用户名或者密码不正确", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(this, "登录失败,请检查网络", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
