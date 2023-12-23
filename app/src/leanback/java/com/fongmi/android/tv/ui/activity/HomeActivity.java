package com.fongmi.android.tv.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Func;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.event.CastEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.custom.CustomTitleView;
import com.fongmi.android.tv.ui.custom.dialog.SiteDialog;
import com.fongmi.android.tv.ui.presenter.FuncPresenter;
import com.fongmi.android.tv.ui.presenter.HeaderPresenter;
import com.fongmi.android.tv.ui.presenter.HistoryPresenter;
import com.fongmi.android.tv.ui.presenter.ProgressPresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;
import com.github.catvod.utils.Util;
import com.google.common.collect.Lists;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

//bellow add by jim ltv
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Environment;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;
import android.os.AsyncTask;
//end if

public class HomeActivity extends BaseActivity implements CustomTitleView.Listener, VodPresenter.OnClickListener, FuncPresenter.OnClickListener, HistoryPresenter.OnClickListener {

    private ArrayObjectAdapter mHistoryAdapter;
    private ActivityHomeBinding mBinding;
    private HistoryPresenter mPresenter;
    private ArrayObjectAdapter mAdapter;
    private SiteViewModel mViewModel;
    private boolean confirm;
    private Result mResult;
    private Clock mClock;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAction(intent);
    }

    @Override
    protected void initView() {
        mClock = Clock.create(mBinding.time).format("MM/dd HH:mm:ss");
        mBinding.progressLayout.showProgress();
        Updater.get().release().start();
        Server.get().start();
        setRecyclerView();
        setViewModel();
        setAdapter();
        initConfig();

        initWelcomeview();  //jim add
    }

    @Override
    protected void initEvent() {
        mBinding.title.setListener(this);
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                mBinding.toolbar.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                if (mPresenter.isDelete()) setHistoryDelete(false);
            }
        });

        //bellow add by jim
        LoadApiFileTask task = new LoadApiFileTask();
        task.execute();
		//end if
    }

    private void checkAction(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            DetailActivity.push(this, Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT)));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            if ("text/plain".equals(intent.getType()) || Util.path(intent.getData()).endsWith(".m3u")) {
                loadLive("file:/" + FileChooser.getPathFromUri(this, intent.getData()));
            } else {
                DetailActivity.push(this, intent.getData());
            }
        }
    }

    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(Integer.class, new HeaderPresenter());
        selector.addPresenter(String.class, new ProgressPresenter());
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), FuncPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), HistoryPresenter.class);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(this, result -> {
            mAdapter.remove("progress");
            addVideo(mResult = result);
        });
    }

    private void setAdapter() {
        mAdapter.add(getFuncRow());
        mAdapter.add(R.string.home_history);
        mAdapter.add(R.string.home_recommend);
        mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
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
                mBinding.progressLayout.showContent();
                checkAction(getIntent());
                getHistory();
                getVideo();
                setFocus();
            }

            @Override
            public void error(String msg) {
                mBinding.progressLayout.showContent();
                mResult = Result.empty();
                Notify.show(msg);
                setFocus();
            }
        };
    }

    private void loadLive(String url) {
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                LiveActivity.start(getActivity());
            }
        });
    }

    private void setFocus() {
        mBinding.recycler.requestFocus();
        App.post(() -> mBinding.title.setFocusable(true), 500);
    }

    private void getVideo() {
        mResult = Result.empty();
        int index = getRecommendIndex();
        String home = ApiConfig.get().getHome().getName();
        mBinding.title.setText(home.isEmpty() ? ResUtil.getString(R.string.app_name) : home);
        if (mAdapter.size() > index) mAdapter.removeItems(index, mAdapter.size() - index);
        if (ApiConfig.get().getHome().getKey().isEmpty()) return;
        mViewModel.homeContent();
        mAdapter.add("progress");
    }

    private void addVideo(Result result) {
        for (List<Vod> items : Lists.partition(result.getList(), Product.getColumn())) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new VodPresenter(this));
            adapter.setItems(items, null);
            mAdapter.add(new ListRow(adapter));
        }
    }

    private ListRow getFuncRow() {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new FuncPresenter(this));
        adapter.add(Func.create(R.string.home_vod));
        adapter.add(Func.create(R.string.home_live));
        adapter.add(Func.create(R.string.home_search));
        adapter.add(Func.create(R.string.home_keep));
        adapter.add(Func.create(R.string.home_push));
        adapter.add(Func.create(R.string.home_setting));
        return new ListRow(adapter);
    }

    private void getHistory() {
        getHistory(false);
    }

    private void getHistory(boolean renew) {
        List<History> items = History.get();
        int historyIndex = getHistoryIndex();
        int recommendIndex = getRecommendIndex();
        boolean exist = recommendIndex - historyIndex == 2;
        if (renew) mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
        if ((items.isEmpty() && exist) || (renew && exist)) mAdapter.removeItems(historyIndex, 1);
        if ((items.size() > 0 && !exist) || (renew && exist)) mAdapter.add(historyIndex, new ListRow(mHistoryAdapter));
        mHistoryAdapter.setItems(items, null);
    }

    private void setHistoryDelete(boolean delete) {
        mPresenter.setDelete(delete);
        mHistoryAdapter.notifyArrayItemRangeChanged(0, mHistoryAdapter.size());
    }

    private void clearHistory() {
        mAdapter.removeItems(getHistoryIndex(), 1);
        History.delete(ApiConfig.getCid());
        mPresenter.setDelete(false);
        mHistoryAdapter.clear();
    }

    private int getHistoryIndex() {
        for (int i = 0; i < mAdapter.size(); i++) if (mAdapter.get(i).equals(R.string.home_history)) return i + 1;
        return -1;
    }

    private int getRecommendIndex() {
        for (int i = 0; i < mAdapter.size(); i++) if (mAdapter.get(i).equals(R.string.home_recommend)) return i + 1;
        return -1;
    }

    private void setConfirm() {
        confirm = true;
        Notify.show(R.string.app_exit);
        App.post(() -> confirm = false, 5000);
    }

    @Override
    public void onItemClick(Func item) {
        switch (item.getResId()) {
            case R.string.home_vod:
                VodActivity.start(this, mResult.clear());
                break;
            case R.string.home_live:
                LiveActivity.start(this);
                break;
            case R.string.home_search:
                SearchActivity.start(this);
                break;
            case R.string.home_keep:
                KeepActivity.start(this);
                break;
            case R.string.home_push:
                PushActivity.start(this);
                break;
            case R.string.home_setting:
                SettingActivity.start(this);
                break;
        }
    }

    @Override
    public void onItemClick(Vod item) {
        DetailActivity.start(this, item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public boolean onLongClick(Vod item) {
        CollectActivity.start(this, item.getVodName());
        return true;
    }

    @Override
    public void onItemClick(History item) {
        DetailActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onItemDelete(History item) {
        mHistoryAdapter.remove(item.delete());
        if (mHistoryAdapter.size() > 0) return;
        mAdapter.removeItems(getHistoryIndex(), 1);
        mPresenter.setDelete(false);
    }

    @Override
    public boolean onLongClick() {
        if (mPresenter.isDelete()) clearHistory();
        else setHistoryDelete(true);
        return true;
    }

    @Override
    public void showDialog() {
        SiteDialog.create(this).show();
    }

    @Override
    public void setSite(Site item) {
        ApiConfig.get().setHome(item);
        getVideo();
    }

    @Override
    public void onChanged() {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        super.onRefreshEvent(event);
        switch (event.getType()) {
            case VIDEO:
                getVideo();
                break;
            case IMAGE:
                int index = getRecommendIndex();
                mAdapter.notifyArrayItemRangeChanged(index, mAdapter.size() - index);
                break;
            case HISTORY:
                getHistory();
                break;
            case SIZE:
                getVideo();
                getHistory(true);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        switch (event.getType()) {
            case SEARCH:
                CollectActivity.start(this, event.getText(), true);
                break;
            case PUSH:
                DetailActivity.push(this, event.getText(), true);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCastEvent(CastEvent event) {
        if (ApiConfig.get().getConfig().equals(event.getConfig())) {
            DetailActivity.cast(this, event.getHistory().update(ApiConfig.getCid()));
        } else {
            ApiConfig.load(event.getConfig(), getCallback(event));
        }
    }

    private Callback getCallback(CastEvent event) {
        return new Callback() {
            @Override
            public void success() {
                RefreshEvent.history();
                RefreshEvent.video();
                onCastEvent(event);
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        };
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (Utils.isMenuKey(event)) showDialog();
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClock.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClock.stop();
    }

    @Override
    public void onBackPressed() {
        if (mBinding.progressLayout.isProgress()) {
            mBinding.progressLayout.showContent();
        } else if (mPresenter.isDelete()) {
            setHistoryDelete(false);
        } else if (mBinding.recycler.getSelectedPosition() != 0) {
            mBinding.recycler.scrollToPosition(0);
        } else if (!confirm) {
            setConfirm();
        } else {
            finish();
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

    //bellow by jim ltv
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
            if (ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestExternalStoragePermission();
            } else {
                loadApiFile();
            }
            return null;
        }
    }
    //end if
}