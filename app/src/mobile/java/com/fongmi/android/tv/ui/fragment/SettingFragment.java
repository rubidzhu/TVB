package com.fongmi.android.tv.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.FragmentSettingBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.impl.SiteCallback;
//import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.ui.activity.MainActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.custom.dialog.HistoryDialog;
import com.fongmi.android.tv.ui.custom.dialog.LiveDialog;
import com.fongmi.android.tv.ui.custom.dialog.SiteDialog;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;
import com.github.catvod.bean.Doh;
import com.github.catvod.net.OkHttp;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.permissionx.guolindev.PermissionX;
import java.util.ArrayList;
import java.util.List;

//bellow add by jim
import com.fongmi.android.tv.ui.activity.MainActivity;
import com.fongmi.android.tv.ui.activity.DownloadActivity;
import android.view.View;
import android.app.AlertDialog;
import android.content.DialogInterface;
import androidx.core.app.ShareCompat;
//end 

public class SettingFragment extends BaseFragment implements ConfigCallback, SiteCallback, LiveCallback {

    private FragmentSettingBinding mBinding;
    private String[] render;
    private String[] decode;
    private String[] player;
    private String[] scale;
    private String[] size;
    private int type;

    public static SettingFragment newInstance() {
        return new SettingFragment();
    }

    private int getDohIndex() {
        return Math.max(0, ApiConfig.get().getDoh().indexOf(Doh.objectFrom(Prefers.getDoh())));
    }

    private String[] getDohList() {
        List<String> list = new ArrayList<>();
        for (Doh item : ApiConfig.get().getDoh()) list.add(item.getName());
        return list.toArray(new String[0]);
    }

    private MainActivity getRoot() {
        return (MainActivity) getActivity();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mBinding.vodUrl.setText(ApiConfig.getDesc());
        mBinding.liveUrl.setText(LiveConfig.getDesc());
        mBinding.wallUrl.setText(WallConfig.getDesc());
        mBinding.dohText.setText(getDohList()[getDohIndex()]);
        mBinding.versionText.setText(BuildConfig.VERSION_NAME);
        mBinding.sizeText.setText((size = ResUtil.getStringArray(R.array.select_size))[Prefers.getSize()]);
        mBinding.scaleText.setText((scale = ResUtil.getStringArray(R.array.select_scale))[Prefers.getScale()]);
        mBinding.playerText.setText((player = ResUtil.getStringArray(R.array.select_player))[Prefers.getPlayer()]);
        mBinding.decodeText.setText((decode = ResUtil.getStringArray(R.array.select_decode))[Prefers.getDecode()]);
        mBinding.renderText.setText((render = ResUtil.getStringArray(R.array.select_render))[Prefers.getRender()]);
		setCacheText();
        //bellow add by jim
        mBinding.downloadText.setText((download = ResUtil.getStringArray(R.array.select_download))[Prefers.getDownload()]);
		//end if
		}
		
    private void setCacheText() {
        FileUtil.getCacheSize(new Callback() {
            @Override
            public void success(String result) {
                mBinding.cacheText.setText(result);
            }
        });
    }

    @Override
    protected void initEvent() {
        mBinding.vodHome.setOnClickListener(view -> SiteDialog.create(this).all().show());
        mBinding.liveHome.setOnClickListener(view -> LiveDialog.create(this).show());
        mBinding.vod.setOnClickListener(view -> ConfigDialog.create(this).type(type = 0).show());
        mBinding.live.setOnClickListener(view -> ConfigDialog.create(this).type(type = 1).show());
        mBinding.wall.setOnClickListener(view -> ConfigDialog.create(this).type(type = 2).show());
        mBinding.vodHistory.setOnClickListener(view -> HistoryDialog.create(this).type(type = 0).show());
        mBinding.liveHistory.setOnClickListener(view -> HistoryDialog.create(this).type(type = 1).show());
        mBinding.wallDefault.setOnClickListener(view -> setWallDefault());
        mBinding.wallRefresh.setOnClickListener(this::setWallRefresh);
        //mBinding.version.setOnLongClickListener(view -> onVersion(true));
        //mBinding.version.setOnClickListener(view -> onVersion(false));
        mBinding.player.setOnClickListener(view -> setPlayer());
        mBinding.decode.setOnClickListener(view -> setDecode());
        mBinding.render.setOnClickListener(view -> setRender());
        mBinding.scale.setOnClickListener(view -> setScale());
        mBinding.size.setOnClickListener(view -> setSize());
        mBinding.size.setOnClickListener(view -> setSize());
		mBinding.doh.setOnClickListener(this::setDoh);

        //belew add by jim
        mBinding.updateVer.setOnLongClickListener(view -> onVersion(true)); //jim add
        mBinding.updateVer.setOnClickListener(view -> onVersion(false));    //jim add
        mBinding.download.setOnClickListener(this::onDownload);    //jim add
        mBinding.downWay.setOnClickListener(view -> setDownload());
        mBinding.deleteCache.setOnClickListener(this::onCache);
        mBinding.aboutText.setOnClickListener(this::onAboutText);
        //end if
    }

    @Override
    public void setConfig(Config config) {
        if (config.getUrl().startsWith("file") && !Utils.hasPermission(getActivity())) {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> load(config));
        } else {
            load(config);
        }
    }

    private void load(Config config) {
        switch (config.getType()) {
            case 0:
                Notify.progress(getActivity());
                ApiConfig.load(config, getCallback());
                mBinding.vodUrl.setText(config.getDesc());
                break;
            case 1:
                Notify.progress(getActivity());
                LiveConfig.load(config, getCallback());
                mBinding.liveUrl.setText(config.getDesc());
                break;
            case 2:
                Notify.progress(getActivity());
                WallConfig.load(config, getCallback());
                mBinding.wallUrl.setText(config.getDesc());
                break;
        }
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                setConfig();
            }

            @Override
            public void error(int resId) {
                Notify.show(resId);
                setConfig();
            }
        };
    }

    private void setConfig() {
        switch (type) {
            case 0:
                setCacheText();
                Notify.dismiss();
                RefreshEvent.video();
                RefreshEvent.config();
                mBinding.vodUrl.setText(ApiConfig.getDesc());
                mBinding.liveUrl.setText(LiveConfig.getDesc());
                mBinding.wallUrl.setText(WallConfig.getDesc());
                break;
            case 1:
                setCacheText();
                Notify.dismiss();
                RefreshEvent.config();
                mBinding.liveUrl.setText(LiveConfig.getDesc());
                break;
            case 2:
                setCacheText();
                Notify.dismiss();
                mBinding.wallUrl.setText(WallConfig.getDesc());
                break;
        }
    }

    @Override
    public void setSite(Site item) {
        ApiConfig.get().setHome(item);
        RefreshEvent.video();
    }

    @Override
    public void onChanged() {
    }

    @Override
    public void setLive(Live item) {
        LiveConfig.get().setHome(item);
    }

    private boolean onVersion(boolean dev) {
        if (dev) Updater.get().force().dev().start();
        else Updater.get().force().start();
        return true;
    }

    private void setPlayer() {
        int index = Prefers.getPlayer();
        Prefers.putPlayer(index = index == player.length - 1 ? 0 : ++index);
        mBinding.playerText.setText(player[index]);
    }

    private void setDecode() {
        int index = Prefers.getDecode();
        Prefers.putDecode(index = index == decode.length - 1 ? 0 : ++index);
        mBinding.decodeText.setText(decode[index]);
    }

    private void setRender() {
        int index = Prefers.getRender();
        Prefers.putRender(index = index == render.length - 1 ? 0 : ++index);
        mBinding.renderText.setText(render[index]);
    }

    private void setScale() {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_scale).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(scale, Prefers.getScale(), (dialog, which) -> {
            mBinding.scaleText.setText(scale[which]);
            Prefers.putScale(which);
            dialog.dismiss();
        }).show();
    }

    private void setSize() {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_size).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(size, Prefers.getSize(), (dialog, which) -> {
            mBinding.sizeText.setText(size[which]);
            Prefers.putSize(which);
            RefreshEvent.size();
            dialog.dismiss();
        }).show();
    }

    private void setWallDefault() {
        WallConfig.refresh(Prefers.getWall() == 8 ? 1 : Prefers.getWall() + 1);
    }

    private void setWallRefresh(View view) {
        Notify.progress(getActivity());
        WallConfig.get().load(new Callback() {
            @Override
            public void success() {
                Notify.dismiss();
                setCacheText();
            }
        });
    }

    private void setDoh(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle("安全DNS").setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(getDohList(), getDohIndex(), (dialog, which) -> {
            setDoh(ApiConfig.get().getDoh().get(which));
            dialog.dismiss();
        }).show();
    }

    private void setDoh(Doh doh) {
        Notify.progress(getActivity());
        Prefers.putDoh(doh.toString());
        OkHttp.get().setDoh(App.get(), doh);
        mBinding.dohText.setText(doh.getName());
        ApiConfig.load(Config.vod(), getCallback());
    }

    private void onCache(View view) {
        FileUtil.clearCache(new Callback() {
            @Override
            public void success() {
                setCacheText();
            }
        });
    }
	
    private void updateText() {
        //if (player == null || decode == null) return;
        if (player == null || decode == null || download == null) return;
        mBinding.vodUrl.setText(ApiConfig.getDesc());
        mBinding.liveUrl.setText(LiveConfig.getDesc());
        mBinding.wallUrl.setText(WallConfig.getDesc());
        mBinding.playerText.setText(player[Prefers.getPlayer()]);
        mBinding.decodeText.setText(decode[Prefers.getDecode()]);
		setCacheText();

        //bellow add by jim
        mBinding.downloadText.setText(download[Prefers.getDownload()]);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateText();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || requestCode != FileChooser.REQUEST_PICK_FILE) return;
        setConfig(Config.find("file:/" + FileChooser.getPathFromUri(getContext(), data.getData()).replace(FileUtil.getRootPath(), ""), type));
    }

    //bellow add by jim
    private void onDownload(View view) {
        DownloadActivity.start(getActivity());
    }

    private String[] download;

    private void setDownload() {
        int index = Prefers.getDownload();
        Prefers.putDownload(index = index == download.length - 1 ? 0 : ++index);
        mBinding.downloadText.setText(download[index]);
    }


    private void onAboutText (View view) {
        // 加载布局文件
        View welcomeView = getLayoutInflater().inflate(R.layout.adapter_setting_about, null);

        // 创建AlertDialog.Builder对象
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        // 设置弹窗标题、按钮和自定义布局
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                // 用户点击确定按钮后，关闭弹窗
                dialog.dismiss();
            }
        });

        builder.setNeutralButton("分享APP给朋友", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 处理分享逻辑，例如弹出分享界面或执行相关操作
                shareContent();
            }
        });
        builder.setView(welcomeView);

        // 创建并显示弹窗
        builder.create().show();

    }

    // 分享逻辑处理方法
    private void shareContent() {
        // 在这里处理分享逻辑，例如弹出分享界面或执行其他分享操作
        ShareCompat.IntentBuilder builder = new ShareCompat.IntentBuilder(getContext()).setType("text/plain").setText("https://zhuzhuok.zeabur.app/article/tech-1");
        //builder.getIntent().putExtra("title", mBinding.control.title.getText());
        //builder.getIntent().putExtra("name", mBinding.control.title.getText());
        builder.startChooser();
    };
    //end if
}
