package com.fongmi.android.tv.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;
import com.fongmi.android.tv.ui.custom.dialog.SyncDialog;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.databinding.ActivityKeepBinding;
import com.fongmi.android.tv.event.RefreshEvent;
//import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.adapter.KeepAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.databinding.FragmentKeepBinding;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class KeepFragment extends BaseFragment implements KeepAdapter.OnClickListener {

    private FragmentKeepBinding mBinding;
    private KeepAdapter mAdapter;

    public static KeepFragment newInstance() {
        return new KeepFragment();
    }

    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentKeepBinding.inflate(inflater, container, false);
    }

    protected void initView() {
        setRecyclerView();
        //getKeep();
        onResume();
    }


    protected void initEvent() {
        mBinding.sync.setOnClickListener(this::onSync);
        mBinding.delete.setOnClickListener(this::onDelete);
        mBinding.cancel.setOnClickListener(this::onBack);   //jim add
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.getItemAnimator().setChangeDuration(0);
        mBinding.recycler.setLayoutManager(new GridLayoutManager(getContext(), Product.getColumn()));
        mBinding.recycler.setAdapter(mAdapter = new KeepAdapter(this));
        mAdapter.setSize(Product.getSpec(getActivity()));
    }

    @Override
    public void onResume() {
        super.onResume();
        getKeep();
    }
    private void getKeep() {
        mAdapter.addAll(Keep.getVod());
        mBinding.delete.setVisibility(mAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
        mBinding.cancel.setVisibility(mAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);  //jim add
    }

    private void onSync(View view) {
        //SyncDialog.create().keep().show(this);
        //SyncDialog.create().keep().show(getSupportFragmentManager(), "SyncDialog");
        SyncDialog.create().keep().show(getActivity().getSupportFragmentManager(), "SyncDialog");
    }

    private void onDelete(View view) {
        if (mAdapter.isDelete()) {
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle(R.string.dialog_delete_record)
                    .setMessage(R.string.dialog_delete_keep)
                    .setNegativeButton(R.string.dialog_negative, null)
                    .setPositiveButton(R.string.dialog_positive, (dialog, which) -> mAdapter.clear()).show();
        } else if (mAdapter.getItemCount() > 0) {
            mAdapter.setDelete(true);
        } else {
            mBinding.delete.setVisibility(View.GONE);
            mBinding.cancel.setVisibility(View.GONE);   //jim add
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


            public void error(int resId) {
                Notify.show(resId);
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType().equals(RefreshEvent.Type.KEEP)) getKeep();
    }


    public void onItemClick(Keep item) {
        Config config = Config.find(item.getCid());
        if (item.getCid() != ApiConfig.getCid()) loadConfig(config, item);
        else DetailActivity.start(getActivity(), item.getSiteKey(), item.getVodId(), item.getVodName());
    }


    public void onItemDelete(Keep item) {
        mAdapter.remove(item.delete());
        //jim edit
        if (mAdapter.getItemCount() > 0) {
            return;
        } else {
            mBinding.delete.setVisibility(View.GONE);
            mBinding.cancel.setVisibility(View.GONE);   //jim add
        }
        mAdapter.setDelete(false);
    }

    public boolean onLongClick() {
        mAdapter.setDelete(!mAdapter.isDelete());
        return true;
    }

    public void onBack(View view) {
       mAdapter.setDelete(false);
    }
}
