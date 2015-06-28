package org.kaka.myreader.activity;

import android.app.ProgressDialog;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppUtility;
import org.kaka.myreader.task.MailSendTask;

public class RegisterAuthCodeActivity extends FragmentActivity {
    private String mailTo;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_mail);

        init();
    }

    private void init() {
        TextView registByPhone = (TextView) findViewById(R.id.registByPhone);
        registByPhone.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        registByPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ImageButton btnBack = (ImageButton) findViewById(R.id.back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        EditText editText = (EditText) findViewById(R.id.mail);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mailTo = s.toString().trim();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        Button btnNext = (Button) findViewById(R.id.next);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AppUtility.isMailAddress(mailTo)) {
                    Toast.makeText(RegisterAuthCodeActivity.this, "请输入正确的邮箱地址", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog = ProgressDialog.show(RegisterAuthCodeActivity.this, "请稍后", "正在为您发送验证码");
                new MailSendTask(RegisterAuthCodeActivity.this).execute(mailTo);
            }
        });
    }

    public void finishSendMail(int result) {
        if (dialog != null) {
            dialog.dismiss();
        }
        if (result != 0) {
            Toast.makeText(this, "发送失败，请检查邮箱是否填写正确或网络是否连接", Toast.LENGTH_SHORT).show();
        } else {
            // next activity.
        }
    }
}
