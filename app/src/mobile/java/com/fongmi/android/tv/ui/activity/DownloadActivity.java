package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.databinding.ActivityDetailBinding;
import com.fongmi.android.tv.event.RefreshEvent;
//import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import com.fongmi.android.tv.ui.activity.DetailActivity;

import com.fongmi.android.tv.databinding.ActivityDownloadBinding;

import java.io.File;
import com.fongmi.android.tv.ui.adapter.IdmAdapter;
import java.util.ArrayList;
import java.util.List;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;

public class DownloadActivity extends BaseActivity implements IdmAdapter.OnClickListener {

    private ActivityDownloadBinding mBinding;
    private IdmAdapter mAdapter;

    String IDMDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, DownloadActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityDownloadBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        setRecyclerView();
        setView();
    }

    @Override
    protected void initEvent() {
        mBinding.delete.setOnClickListener(this::onDelete);
        mBinding.cancel.setOnClickListener(this::onCancel);
    }

    private void setRecyclerView() {
        List<File> IDMFiles = getAllVideoFiles(new File(IDMDir));
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.getItemAnimator().setChangeDuration(0);
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, 1));
        mBinding.recycler.setAdapter(mAdapter = new IdmAdapter(IDMFiles, this));
    }

    private void setView() {
        mBinding.delete.setVisibility(mAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
        mBinding.cancel.setVisibility(mAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void onDelete(View view) {
        if (mAdapter.isDelete()) {
            new MaterialAlertDialogBuilder(this).setTitle(R.string.dialog_delete_record).setMessage(R.string.dialog_delete_keep).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> mAdapter.clear()).show();
        } else if (mAdapter.getItemCount() > 0) {
            mAdapter.setDelete(true);
        } else {
            mBinding.delete.setVisibility(View.GONE);
            mBinding.cancel.setVisibility(View.GONE);
        }
    }

    private void loadConfig(Config config, Keep item) {
        ApiConfig.load(config, new Callback() {
            @Override
            public void success() {
                DetailActivity.start(getActivity(), item.getSiteKey(), item.getVodId(), item.getVodName());
                RefreshEvent.config();
                RefreshEvent.video();
            }

            //@Override
            public void error(int resId) {
                Notify.show(resId);
            }
        });
    }

    @Override
    public boolean onLongClick() {
        mAdapter.setDelete(!mAdapter.isDelete());
        return true;
    }

    @Override
    public void onItemClick(File item) {
        // 处理点击文件的逻辑
        String path = item.getAbsolutePath();
        String name = item.getName();
        DetailActivity.start(this, "本地", path, name);
    }

    @Override

    public void onItemDelete(File item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setMessage("您确定要删除该文件吗？");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean deleted = item.delete();
                if (deleted) {
                    mAdapter.remove(item);
                    if (mAdapter.getItemCount() == 0) {
                        mBinding.delete.setVisibility(View.GONE);
                        mBinding.cancel.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(DownloadActivity.this, "删除文件失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }


    @Override
    public void onBackPressed() {
        if (mAdapter.isDelete()) mAdapter.setDelete(false);
        else super.onBackPressed();
    }

    private void onCancel(View view) {
        onBackPressed();
    }


    private List<File> getAllVideoFiles(File dir) {
        List<File> IDMFiles = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    IDMFiles.addAll(getAllVideoFiles(file));
                } else {
                    String fileName = file.getName();
                    if (fileName.endsWith(".mp4") || fileName.endsWith(".avi")) {
                        IDMFiles.add(file);
                    }
                }
            }
        }
        return IDMFiles;
    }

}
