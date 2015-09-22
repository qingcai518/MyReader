package org.kaka.myreader.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Toast;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.task.UserAddTask;

import java.util.HashMap;
import java.util.Map;

public class RegisterInfoActivity extends Activity {
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_info);

        init();
    }

    private void init() {
        ImageButton btnBack = (ImageButton) findViewById(R.id.back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterInfoActivity.this.finish();
            }
        });

        Intent intent = getIntent();
        final String userId = intent.getStringExtra(AppConstants.KEY_INFO);
        final int userType = intent.getIntExtra(AppConstants.KEY_REGIST_TYPE, AppConstants.TYPE_PHONE);

        final EditText userNameText = (EditText) findViewById(R.id.userName);
        final EditText passwordText = (EditText) findViewById(R.id.password);
        final EditText confirmPasswordText = (EditText) findViewById(R.id.confirmPassword);
        final RadioButton btnMale = (RadioButton) findViewById(R.id.male);
        btnMale.setChecked(true);

        Button btnFinish = (Button) findViewById(R.id.finish);
        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = userNameText.getText().toString();
                String password = passwordText.getText().toString();
                String confirm = confirmPasswordText.getText().toString();
                if (userName.trim().length() == 0) {
                    userNameText.setFocusable(true);
                    userNameText.requestFocus();
                    Toast.makeText(RegisterInfoActivity.this, "请输入昵称", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.trim().length() == 0) {
                    passwordText.setFocusable(true);
                    passwordText.requestFocus();
                    Toast.makeText(RegisterInfoActivity.this, "请输入密码", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!confirm.equals(password)) {
                    confirmPasswordText.setFocusable(true);
                    confirmPasswordText.requestFocus();
                    Toast.makeText(RegisterInfoActivity.this, "两次密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }

                String sex = btnMale.isChecked() ? "male" : "female";

                Map<String, String> map = new HashMap<>();
                map.put("userId", userId);
                map.put("userType", String.valueOf(userType));
                map.put("userName", userName);
                map.put("password", password);
                map.put("sex", sex);

                dialog = ProgressDialog.show(RegisterInfoActivity.this, "请稍等", "用户登录中");
                new UserAddTask(RegisterInfoActivity.this).execute(map);
            }
        });
    }

    public void finishTask(int result) {
        if (dialog != null) {
            dialog.dismiss();
        }

        if (result == 0) {
            Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();
//            Intent intent = new Intent(this, MainActivity.class);
//            startActivity(intent);
            setResult(Activity.RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "注册失败,请检查网络连接", Toast.LENGTH_SHORT).show();
        }
    }
}
