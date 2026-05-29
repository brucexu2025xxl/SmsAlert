package com.smsalert.parking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) return;

        SharedPreferences prefs = context.getSharedPreferences("sms_alert_prefs", Context.MODE_PRIVATE);
        String sendersStr = prefs.getString("senders", "交警");
        String keywordsStr = prefs.getString("keywords", "未按规定停放||请立即驶离");

        String[] senders = TextUtils.isEmpty(sendersStr) ? new String[0] : sendersStr.split("\\|\\|");
        String[] keywords = TextUtils.isEmpty(keywordsStr) ? new String[0] : keywordsStr.split("\\|\\|");

        String format = SmsMessage.createFromPdu((byte[]) pdus[0]).getMessageBody();
        boolean is3gFormat = format != null;

        for (Object pdu : pdus) {
            SmsMessage sms;
            if (is3gFormat) {
                sms = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                sms = SmsMessage.createFromPdu((byte[]) pdu);
            }
            if (sms == null) continue;

            String sender = sms.getDisplayOriginatingAddress();
            String body = sms.getMessageBody();
            if (sender == null || body == null) continue;

            Log.d(TAG, "收到短信 - 发件人: " + sender + " | 内容前30字: " +
                    (body.length() > 30 ? body.substring(0, 30) + "..." : body));

            boolean senderMatch = false;
            // 匹配发件人地址 或 短信内容中含发件人关键词
            for (String s : senders) {
                if (TextUtils.isEmpty(s)) continue;
                if (sender.contains(s) || body.contains(s)) {
                    senderMatch = true;
                    break;
                }
            }

            boolean keywordMatch = false;
            for (String kw : keywords) {
                if (!TextUtils.isEmpty(kw) && body.contains(kw)) {
                    keywordMatch = true;
                    break;
                }
            }

            if (senderMatch && keywordMatch) {
                Log.i(TAG, "===== 匹配成功！触发提醒 =====");
                saveLog(context, sender, body);
                AlertNotificationHelper.showAlert(context, sender, body);
            }
        }
    }

    private void saveLog(Context context, String sender, String body) {
        SharedPreferences prefs = context.getSharedPreferences("sms_alert_prefs", Context.MODE_PRIVATE);
        String existingLog = prefs.getString("alert_log", "");

        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
        String time = sdf.format(new Date());

        String brief = body.length() > 40 ? body.substring(0, 40) + "..." : body;
        String newEntry = "[" + time + "] " + sender + "\n" + brief + "\n\n";

        String updatedLog = newEntry + existingLog;

        String[] lines = updatedLog.split("\n\n");
        StringBuilder sb = new StringBuilder();
        int max = Math.min(lines.length, 20);
        for (int i = 0; i < max; i++) {
            if (i > 0) sb.append("\n\n");
            sb.append(lines[i]);
        }

        prefs.edit().putString("alert_log", sb.toString()).apply();
    }
}
