package org.kaka.myreader.task;

import android.content.Context;
import android.os.AsyncTask;

import org.kaka.myreader.activity.RegisterAuthCodeActivity;
import org.kaka.myreader.activity.RegisterByMailActivity;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.common.AppUtility;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailSendTask extends AsyncTask<String, Integer, Integer> {
    private final static String MAIL_ACCOUNT = "info@myreader.main.jp";
    private final static String MAIL_SMTP = "smtp.lolipop.jp";
    private Context context;

    public MailSendTask(Context context) {
        this.context = context;
    }

    @Override
    protected Integer doInBackground(String... params) {
        String mailTo = params[0];

        Properties properties = new Properties();
        properties.put("mail.smtp.host", MAIL_SMTP);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(properties);
        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(MAIL_ACCOUNT));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(mailTo));

            message.setSubject("MyReader 验证码");
            AppConstants.CurrentAuthCode = AppUtility.getAuthCode(5);
            message.setText("您的验证码为 : " + AppConstants.CurrentAuthCode, "UTF-8");

            Transport transport = session.getTransport("smtp");
            transport.connect(MAIL_ACCOUNT, "wy03237462");
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        } catch (Exception e) {
            return 1;
        }

        return 0;
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (context instanceof RegisterByMailActivity) {
            ((RegisterByMailActivity) context).finishSendMail(result);
        } else if (context instanceof RegisterAuthCodeActivity) {
            ((RegisterAuthCodeActivity) context).beginCount();
        }
    }
}
