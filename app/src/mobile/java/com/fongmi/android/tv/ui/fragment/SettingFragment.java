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

import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogUpdateBinding;
import com.fongmi.android.tv.databinding.FragmentSettingBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.ui.activity.MainActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.custom.dialog.HistoryDialog;
import com.fongmi.android.tv.ui.custom.dialog.LiveDialog;
import com.fongmi.android.tv.ui.custom.dialog.SiteDialog;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;
import com.github.catvod.bean.Doh;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.permissionx.guolindev.PermissionX;

import java.util.ArrayList;
import java.util.List;

//bellow add by jim
import com.fongmi.android.tv.ui.activity.DownloadActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import androidx.core.app.ShareCompat;
import android.widget.Switch;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.Context;
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
        return Math.max(0, ApiConfig.get().getDoh().indexOf(Doh.objectFrom(Setting.getDoh())));
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
        mBinding.sizeText.setText((size = ResUtil.getStringArray(R.array.select_size))[Setting.getSize()]);
        mBinding.scaleText.setText((scale = ResUtil.getStringArray(R.array.select_scale))[Setting.getScale()]);
        mBinding.playerText.setText((player = ResUtil.getStringArray(R.array.select_player))[Setting.getPlayer()]);
        mBinding.decodeText.setText((decode = ResUtil.getStringArray(R.array.select_decode))[Setting.getDecode()]);
        mBinding.renderText.setText((render = ResUtil.getStringArray(R.array.select_render))[Setting.getRender()]);
		setCacheText();
        //bellow add by jim
        mBinding.downloadText.setText((download = ResUtil.getStringArray(R.array.select_download))[Setting.getDownload()]);
        mBinding.liveSwitchText.setText((liveSwitch = ResUtil.getStringArray(R.array.switch_array))[Setting.getLiveSwitch()]);
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
        mBinding.vod.setOnClickListener(this::onVod);
        mBinding.live.setOnClickListener(this::onLive);
        mBinding.wall.setOnClickListener(this::onWall);
        mBinding.cache.setOnClickListener(this::onCache);
        //mBinding.version.setOnClickListener(this::onVersion); jim edit
        mBinding.vodHome.setOnClickListener(this::onVodHome);
        mBinding.liveHome.setOnClickListener(this::onLiveHome);
        mBinding.vodHistory.setOnClickListener(this::onVodHistory);
        mBinding.version.setOnLongClickListener(this::onVersionDev);
        mBinding.liveHistory.setOnClickListener(this::onLiveHistory);
        mBinding.player.setOnLongClickListener(this::onPlayerSetting);
        mBinding.wallDefault.setOnClickListener(this::setWallDefault);
        mBinding.wallRefresh.setOnClickListener(this::setWallRefresh);
        mBinding.player.setOnClickListener(this::setPlayer);
        mBinding.decode.setOnClickListener(this::setDecode);
        mBinding.render.setOnClickListener(this::setRender);
        mBinding.scale.setOnClickListener(this::setScale);
        mBinding.size.setOnClickListener(this::setSize);
		mBinding.doh.setOnClickListener(this::setDoh);

        //belew add by jim
        //mBinding.updateVer.setOnLongClickListener(this::onVersion);
        mBinding.version.setOnClickListener(this::onVersionShow);
        mBinding.updateVer.setOnClickListener(this::onVersion);
        mBinding.download.setOnClickListener(this::onDownload);
        mBinding.downWay.setOnClickListener(view -> setDownload());
        mBinding.deleteCache.setOnClickListener(this::onCache);
        mBinding.aboutText.setOnClickListener(this::onAboutText);
        mBinding.liveSwitch.setOnClickListener(view -> setLiveSwitch());
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
            public void error(String msg) {
                Notify.show(msg);
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

    private void onVod(View view) {
        ConfigDialog.create(this).type(type = 0).show();
    }

    private void onLive(View view) {
        ConfigDialog.create(this).type(type = 1).show();
    }

    private void onWall(View view) {
        ConfigDialog.create(this).type(type = 2).show();
    }

    private void onVodHome(View view) {
        SiteDialog.create(this).all().show();
    }

    private void onLiveHome(View view) {
        LiveDialog.create(this).show();
    }

    private void onVodHistory(View view) {
        HistoryDialog.create(this).type(type = 0).show();
    }

    private void onLiveHistory(View view) {
        HistoryDialog.create(this).type(type = 1).show();
    }
	
	private boolean onPlayerSetting(View view) {
        //getRoot().change(2);
        return true;
    }

    //bellow edit by jim
    private void onVersion(View view) {
        //Updater.get().force().release().start();
        Updater updater = Updater.get();
        updater.setContext(getContext()); // 设置当前活动的 Context
        updater.force().release().start();
    }

    private boolean onVersionDev(View view) {
        //Updater.get().force().dev().start();
        Updater updater = Updater.get();
        updater.setContext(getContext()); // 设置当前活动的 Context
        updater.force().dev().start();
        return true;
    }
    //end if

    private void setWallDefault(View view) {
        WallConfig.refresh(Setting.getWall() == 8 ? 1 : Setting.getWall() + 1);
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

    private void setPlayer(View view) {
        int index = Setting.getPlayer();
        Setting.putPlayer(index = index == player.length - 1 ? 0 : ++index);
        mBinding.playerText.setText(player[index]);
    }

    private void setDecode(View view) {
        int index = Setting.getDecode();
        Setting.putDecode(index = index == decode.length - 1 ? 0 : ++index);
        mBinding.decodeText.setText(decode[index]);
    }

    private void setRender(View view) {
        int index = Setting.getRender();
        Setting.putRender(index = index == render.length - 1 ? 0 : ++index);
        mBinding.renderText.setText(render[index]);
    }

    private void setScale(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_scale).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(scale, Setting.getScale(), (dialog, which) -> {
            mBinding.scaleText.setText(scale[which]);
            Setting.putScale(which);
            dialog.dismiss();
        }).show();
    }

    private void setSize(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_size).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(size, Setting.getSize(), (dialog, which) -> {
            mBinding.sizeText.setText(size[which]);
            Setting.putSize(which);
            RefreshEvent.size();
            dialog.dismiss();
        }).show();
    }

    private void setDoh(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle("安全DNS").setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(getDohList(), getDohIndex(), (dialog, which) -> {
            setDoh(ApiConfig.get().getDoh().get(which));
            dialog.dismiss();
        }).show();
    }

    private void setDoh(Doh doh) {
        OkHttp.get().setDoh(doh);
        Notify.progress(getActivity());
        Setting.putDoh(doh.toString());
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

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden || player == null || decode == null) return;
        mBinding.vodUrl.setText(ApiConfig.getDesc());
        mBinding.liveUrl.setText(LiveConfig.getDesc());
        mBinding.wallUrl.setText(WallConfig.getDesc());
        mBinding.dohText.setText(getDohList()[getDohIndex()]);
        mBinding.playerText.setText(player[Setting.getPlayer()]);
        mBinding.decodeText.setText(decode[Setting.getDecode()]);
        setCacheText();

        //bellow add by jim
        mBinding.downloadText.setText(download[Setting.getDownload()]);
        mBinding.liveSwitchText.setText(liveSwitch[Setting.getLiveSwitch()]);
        //end if
    }

    @Override
    public void onResume() {
        super.onResume();
        onHiddenChanged(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || requestCode != FileChooser.REQUEST_PICK_FILE) return;
        setConfig(Config.find("file:/" + FileChooser.getPathFromUri(getContext(), data.getData()).replace(Path.rootPath(), ""), type));
    }

    //bellow add by jim
    private void onDownload(View view) {
        DownloadActivity.start(getActivity());
    }

    private String[] download;
    private String[] liveSwitch;

    private void setDownload() {
        int index = Setting.getDownload();
        Setting.putDownload(index = index == download.length - 1 ? 0 : ++index);
        mBinding.downloadText.setText(download[index]);
    }

    private void setLiveSwitch() {
        int index = Setting.getLiveSwitch();
        Setting.setLiveSwitch(index = index == liveSwitch.length - 1 ? 0 : ++index);
        mBinding.liveSwitchText.setText(liveSwitch[index]);

        Toast.makeText(getActivity(), "此设置重启APP后将生效", Toast.LENGTH_SHORT).show();
    }

    private void onVersionShow(View view) {
        SharedPreferences preferences = getActivity().getSharedPreferences("versionInfo", Context.MODE_PRIVATE);
        String name =preferences.getString("verName", "");;
        String desc = preferences.getString("verDesc", "");;
        int code = preferences.getInt("verCode", 0);;
        if (name != null && desc != null && code == BuildConfig.VERSION_CODE) {
            // 创建一个框显示 name 和 desc
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("当前版本: " + name);
            builder.setMessage("更新内容: " + desc);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
        }
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
        ShareCompat.IntentBuilder builder = new ShareCompat.IntentBuilder(getContext()).setType("text/plain").setText("https://zhuzhuok.top/article/tech-1");
        builder.startChooser();
    };
    //end if
}
