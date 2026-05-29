package com.smsalert.parking;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_SMS = 1;

    private SharedPreferences prefs;
    private ListView senderListView;
    private ListView keywordListView;
    private TextView logTextView;
    private ScrollView logScroll;
    private List<String> senderList;
    private List<String> keywordList;
    private ArrayAdapter<String> senderAdapter;
    private ArrayAdapter<String> keywordAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("sms_alert_prefs", MODE_PRIVATE);

        senderListView = findViewById(R.id.sender_list);
        keywordListView = findViewById(R.id.keyword_list);
        logTextView = findViewById(R.id.log_text);
        logScroll = findViewById(R.id.log_scroll);

        // 发件人列表：点击展开/收起
        senderListView.setOnItemClickListener((parent, view, position, id) -> toggleListHeight(senderListView));
        // 关键词列表：点击展开/收起
        keywordListView.setOnItemClickListener((parent, view, position, id) -> toggleListHeight(keywordListView));

        EditText senderInput = findViewById(R.id.sender_input);
        Button addSenderBtn = findViewById(R.id.add_sender_btn);
        EditText keywordInput = findViewById(R.id.keyword_input);
        Button addKeywordBtn = findViewById(R.id.add_keyword_btn);
        Button checkPermsBtn = findViewById(R.id.check_perms_btn);
        Button clearLogBtn = findViewById(R.id.clear_log_btn);

        loadRules();

        senderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, senderList);
        senderListView.setAdapter(senderAdapter);

        keywordAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, keywordList);
        keywordListView.setAdapter(keywordAdapter);

        addSenderBtn.setOnClickListener(v -> {
            String sender = senderInput.getText().toString().trim();
            if (!TextUtils.isEmpty(sender) && !senderList.contains(sender)) {
                senderList.add(sender);
                saveRules();
                senderAdapter.notifyDataSetChanged();
                senderInput.setText("");
                Toast.makeText(this, "已添加发件人: " + sender, Toast.LENGTH_SHORT).show();
            }
        });

        senderListView.setOnItemLongClickListener((parent, view, position, id) -> {
            String removed = senderList.remove(position);
            saveRules();
            senderAdapter.notifyDataSetChanged();
            Toast.makeText(this, "已移除: " + removed, Toast.LENGTH_SHORT).show();
            return true;
        });

        addKeywordBtn.setOnClickListener(v -> {
            String keyword = keywordInput.getText().toString().trim();
            if (!TextUtils.isEmpty(keyword) && !keywordList.contains(keyword)) {
                keywordList.add(keyword);
                saveRules();
                keywordAdapter.notifyDataSetChanged();
                keywordInput.setText("");
                Toast.makeText(this, "已添加关键词: " + keyword, Toast.LENGTH_SHORT).show();
            }
        });

        keywordListView.setOnItemLongClickListener((parent, view, position, id) -> {
            String removed = keywordList.remove(position);
            saveRules();
            keywordAdapter.notifyDataSetChanged();
            Toast.makeText(this, "已移除: " + removed, Toast.LENGTH_SHORT).show();
            return true;
        });

        checkPermsBtn.setOnClickListener(v -> checkAndRequestPermissions());

        clearLogBtn.setOnClickListener(v -> {
            prefs.edit().putString("alert_log", "").apply();
            logTextView.setText("暂无匹配记录");
        });

        updateLogDisplay();
        checkAndRequestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLogDisplay();
    }

    private void loadRules() {
        String savedSenders = prefs.getString("senders", "");
        senderList = new ArrayList<>();
        if (!TextUtils.isEmpty(savedSenders)) {
            String[] parts = savedSenders.split("\\|\\|");
            for (String p : parts) {
                if (!TextUtils.isEmpty(p)) senderList.add(p);
            }
        }
        if (senderList.isEmpty()) {
            senderList.add("交警");
        }

        String savedKeywords = prefs.getString("keywords", "");
        keywordList = new ArrayList<>();
        if (!TextUtils.isEmpty(savedKeywords)) {
            String[] parts = savedKeywords.split("\\|\\|");
            for (String p : parts) {
                if (!TextUtils.isEmpty(p)) keywordList.add(p);
            }
        }
        if (keywordList.isEmpty()) {
            keywordList.add("未按规定停放");
            keywordList.add("请立即驶离");
        }
    }

    private void saveRules() {
        prefs.edit()
                .putString("senders", TextUtils.join("||", senderList))
                .putString("keywords", TextUtils.join("||", keywordList))
                .apply();
    }

    private void updateLogDisplay() {
        String log = prefs.getString("alert_log", "");
        logTextView.setText(TextUtils.isEmpty(log) ? "暂无匹配记录" : log);
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void checkAndRequestPermissions() {
        List<String> needed = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECEIVE_SMS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    needed.toArray(new String[0]), REQUEST_SMS);
        } else {
            showOptimizationTips();
        }
    }

    private void showOptimizationTips() {
        new AlertDialog.Builder(this)
                .setTitle("后台保活建议")
                .setMessage("为确保 App 能 7×24 小时监听短信，建议您手动设置以下两项：\n\n" +
                        "1. 允许 App 自启动\n" +
                        "   （手机设置 → 应用 → 短信提醒 → 允许自启动）\n\n" +
                        "2. 关闭电池优化\n" +
                        "   （手机设置 → 应用 → 短信提醒 → 电池 → 无限制）\n\n" +
                        "点击下方按钮可跳转到设置页。")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("稍后", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SMS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, "权限已授权，短信监听已启用", Toast.LENGTH_SHORT).show();
                startKeepAliveService();
                showOptimizationTips();
            } else {
                Toast.makeText(this, "短信权限未授权，App 无法监听短信", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void toggleListHeight(ListView listView) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) listView.getLayoutParams();
        if (params.height == LinearLayout.LayoutParams.WRAP_CONTENT) {
            params.height = (int) (120 * getResources().getDisplayMetrics().density);
        } else {
            params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        }
        listView.setLayoutParams(params);
    }

    private void startKeepAliveService() {
        Intent serviceIntent = new Intent(this, KeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
