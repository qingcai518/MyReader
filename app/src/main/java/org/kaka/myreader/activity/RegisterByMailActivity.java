package org.kaka.myreader.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
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
import org.kaka.myreader.common.AppUtility;
import org.kaka.myreader.task.MailSendTask;

public class RegisterByMailActivity extends Activity {
    private String mailTo;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_mail);

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
        TextView registByPhone = (TextView) findViewById(R.id.registByPhone);
        registByPhone.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        registByPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterByMailActivity.this, RegisterByPhoneActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
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
                    Toast.makeText(RegisterByMailActivity.this, "邮箱不正确ַ", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog = ProgressDialog.show(RegisterByMailActivity.this, "请稍等", "正在为您发送验证码");
                new MailSendTask(RegisterByMailActivity.this).execute(mailTo);
            }
        });
    }

    public void finishSendMail(int result) {
        if (dialog != null) {
            dialog.dismiss();
        }
        if (result != 0) {
            Toast.makeText(this, "发送失败，请检查网络连接或邮箱是否可用", Toast.LENGTH_SHORT).show();
        } else {
            // next activity.
            Intent intent = new Intent(this, RegisterAuthCodeActivity.class);
            intent.putExtra(AppConstants.KEY_REGIST_TYPE, AppConstants.TYPE_MAIL);
            intent.putExtra(AppConstants.KEY_INFO, mailTo);
            startActivityForResult(intent, 0);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        }
    }
}
