package org.kaka.myreader.activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.common.AppUtility;

public class RegisterByMailActivity extends FragmentActivity {
    private String currentCode;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        init();
        registerReceiver(sendMessage, new IntentFilter(AppConstants.BROADCAST_SENT_SMS));
        registerReceiver(receiver, new IntentFilter(
                AppConstants.BROADCAST_DELIVERED_SMS));
    }

    @Override
    public void onDestroy() {
        if (sendMessage != null) {
            unregisterReceiver(sendMessage);
        }
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    private void init() {
        TextView registByMail = (TextView) findViewById(R.id.registByMail);
        registByMail.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        registByMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        final EditText phoneNumberText = (EditText) findViewById(R.id.phoneNumber);

        Button btnNext = (Button) findViewById(R.id.next);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SmsManager smsManager = SmsManager.getDefault();
                String phoneNumber = phoneNumberText.getText().toString();
                if (!PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)) {
                    return;
                }

                if (currentCode == null) {
                    currentCode = AppUtility.getAuthCode(AppConstants.AUTH_CODE_DIGITS);
                    startTime = System.currentTimeMillis();
                } else {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - startTime > AppConstants.AUTH_CODE_EXPIRE) {
                        currentCode = AppUtility.getAuthCode(AppConstants.AUTH_CODE_DIGITS);
                        startTime = System.currentTimeMillis();
                    }
                }

                Intent sentIntent = new Intent(AppConstants.BROADCAST_SENT_SMS);
                PendingIntent sentPI = PendingIntent.getBroadcast(RegisterByMailActivity.this, 0, sentIntent, 0);

                Intent deliverIntent = new Intent(AppConstants.BROADCAST_DELIVERED_SMS);
                PendingIntent deliverPI = PendingIntent.getBroadcast(RegisterByMailActivity.this, 0, deliverIntent, 0);

                smsManager.sendTextMessage(phoneNumber, null, currentCode, sentPI, deliverPI);
            }
        });
    }

    private BroadcastReceiver sendMessage = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(context, "send success", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(context, "send fail", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "recieve down", Toast.LENGTH_LONG).show();
        }
    };
}
