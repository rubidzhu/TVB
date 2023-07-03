package com.fongmi.android.tv.ui.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.databinding.ActivityMainBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.FragmentStateManager;
import com.fongmi.android.tv.ui.fragment.SettingFragment;
import com.fongmi.android.tv.ui.fragment.SettingPlayerFragment;
import com.fongmi.android.tv.ui.fragment.VodFragment;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.navigation.NavigationBarView;

//bellow add by jim
import com.fongmi.android.tv.ui.fragment.KeepFragment;
import android.view.View;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import android.os.Environment;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import android.util.Log;
import com.fongmi.android.tv.BuildConfig;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;
import android.os.AsyncTask;
//end if

public class MainActivity extends BaseActivity implements NavigationBarView.OnItemSelectedListener {

    private FragmentStateManager mManager;
    private ActivityMainBinding mBinding;
    private boolean confirm;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityMainBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAction(intent);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        initFragment(savedInstanceState);
        Updater.get().release().start();
        Server.get().start();
        initConfig();
        initWelcomeview();  //jim add
    }

    @Override
    protected void initEvent() {
        mBinding.navigation.setOnItemSelectedListener(this);

        //bellow add by jim
        LoadApiFileTask task = new LoadApiFileTask();
        task.execute();
    }


    private void checkAction(Intent intent) {
        boolean push = ApiConfig.hasPush() && intent.getAction() != null;
        if (push && intent.getAction().equals(Intent.ACTION_SEND) && intent.getType().equals("text/plain")) {
            DetailActivity.push(this, Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT)));
        } else if (push && intent.getAction().equals(Intent.ACTION_VIEW) && intent.getData().getScheme() != null) {
            DetailActivity.push(this, intent.getData());
        }
    }

    private void initFragment(Bundle savedInstanceState) {
        mManager = new FragmentStateManager(mBinding.container, getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                //bellow Jim add
                if (position == 0) {
                    return VodFragment.newInstance();
                } else if (position == 1) {
                    return SettingFragment.newInstance();
                } else {
                    return KeepFragment.newInstance();
                }
                //end if
            }
        };
        if (savedInstanceState == null) mManager.change(0);
    }

    private void initConfig() {
        WallConfig.get().init();
        LiveConfig.get().init();
        ApiConfig.get().init().load(getCallback());
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                checkAction(getIntent());
                RefreshEvent.config();
                RefreshEvent.video();
            }

            @Override
            public void error(int resId) {
                RefreshEvent.config();
                RefreshEvent.empty();
                Notify.show(resId);
            }
        };
    }

    private void setNavigation() {
        mBinding.navigation.getMenu().findItem(R.id.vod).setVisible(true);
        mBinding.navigation.getMenu().findItem(R.id.setting).setVisible(true);
        mBinding.navigation.getMenu().findItem(R.id.live).setVisible(false);
    }

    private void setConfirm() {
        confirm = true;
        Notify.show(R.string.app_exit);
        App.post(() -> confirm = false, 2000);
    }

    public void change(int position) {
        mManager.change(position);
    }

    @Override
    public void onRefreshEvent(RefreshEvent event) {
        super.onRefreshEvent(event);
        if (event.getType().equals(RefreshEvent.Type.CONFIG)) setNavigation();
        mBinding.navigation.getMenu().findItem(R.id.keepbot).setVisible(true);     //jim add
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (mBinding.navigation.getSelectedItemId() == item.getItemId()) return false;
        if (item.getItemId() == R.id.vod) return mManager.change(0);
        if (item.getItemId() == R.id.setting) return mManager.change(1);
        if (item.getItemId() == R.id.keepbot) return mManager.change(2);    //jim add
        return false;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        RefreshEvent.video();
    }

    @Override
    public void onBackPressed() {
        if (!mBinding.navigation.getMenu().findItem(R.id.vod).isVisible()) {
            setNavigation();
        } else if (mManager.isVisible(2)) {
            change(1);
        } else if (mManager.isVisible(1)) {
            mBinding.navigation.setSelectedItemId(R.id.vod);
        } else if (mManager.canBack(0)) {
            if (!confirm) setConfirm();
            else finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WallConfig.get().clear();
        LiveConfig.get().clear();
        ApiConfig.get().clear();
        Server.get().stop();
    }



    //below add by jim
    private void initWelcomeview () {
        // 获取SharedPreferences对象
        SharedPreferences preferences = getSharedPreferences("app", MODE_PRIVATE);

        // 判断用户是否已经看过欢迎界面
        if (!preferences.getBoolean("is_first_time", false)) {
            // 如果是第一次进入app，显示欢迎界面，并将is_first_time设置为true
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("is_first_time", true);
            editor.apply();

            // 加载布局文件
            View welcomeView = getLayoutInflater().inflate(R.layout.adapter_welcome, null);

            // 创建AlertDialog.Builder对象
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // 设置弹窗标题、按钮和自定义布局
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    // 用户点击确定按钮后，关闭弹窗
                    dialog.dismiss();
                }
            });
            builder.setView(welcomeView);

            // 创建并显示弹窗
            builder.create().show();
        }
    }

    private boolean openKeep() {
        KeepActivity.start(this);
        return true;
    }

    private static final String PREF_NAME = "MyAppPref";
    private static final String VERSION_CODE = "VersionCode";
    private static final String ZIP_FILE_NAME = "localApi.zip";
    private static final String TARGET_DIR = Environment.getExternalStorageDirectory().getPath() + "/zhuzhuyingwo/";

    private void requestExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            loadApiFile();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadApiFile();
            } else {
                // 权限被拒绝，显示提示信息
                Toast.makeText(this, "请赋予应用存储权限后使用", Toast.LENGTH_SHORT).show();
                // 退出应用
                finish();
            }
        }
    }

    public void loadApiFile() {

        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int savedVersionCode = preferences.getInt(VERSION_CODE, -1);
        int currentVersionCode = BuildConfig.VERSION_CODE;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestExternalStoragePermission();
            return;
        }

        if (savedVersionCode < currentVersionCode) {
            File targetDir = new File(TARGET_DIR);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            try {
                InputStream inputStream = getAssets().open(ZIP_FILE_NAME);
                ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                ZipEntry zipEntry = null;

                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    String fileName = zipEntry.getName();
                    File targetFile = new File(TARGET_DIR + fileName);
                    if (zipEntry.isDirectory()) {
                        targetFile.mkdirs();
                    } else {
                        // 处理压缩文件中的重复文件名，如果压缩文件中包含重复的文件名，则先删除原有文件再创建新文件。
                        if (targetFile.exists() && !targetFile.delete()) {
                            continue;
                        }

                        FileOutputStream outputStream = new FileOutputStream(targetFile);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zipInputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                        outputStream.close();
                    }
                }
                zipInputStream.close();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(VERSION_CODE, currentVersionCode);
                editor.apply();
            } catch (IOException e) {
                Log.e("MainActivity", "Error unzip file", e);
            }
        } else {
            Log.e("MainActivity", "same version,no unzip config");
        }

    }

    private class LoadApiFileTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestExternalStoragePermission();
            } else {
                loadApiFile();
            }
            return null;
        }
    }
    //end if
}
