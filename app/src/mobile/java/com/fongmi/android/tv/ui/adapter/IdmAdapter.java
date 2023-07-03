package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.fongmi.android.tv.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.fongmi.android.tv.databinding.AdapterIdmBinding;
import com.fongmi.android.tv.databinding.AdapterSiteBinding;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class IdmAdapter extends RecyclerView.Adapter<IdmAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<File> mItems;

    private boolean delete;

    public IdmAdapter(List<File> IDMFiles, OnClickListener listener) {
        this.mItems = IDMFiles;
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onItemClick(File item);

        void onItemDelete(File item);

        boolean onLongClick();
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
        notifyItemRangeChanged(0, mItems.size());
    }

    public void addAll(List<File> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void clear() {
        for (File item : mItems) {
            item.delete();
        }
        mItems.clear();
        setDelete(false);
        notifyDataSetChanged();
    }

    public void remove(File item) {
        int index = mItems.indexOf(item);
        if (index == -1) return;
        mItems.remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(AdapterIdmBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        return holder;
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.delete.setVisibility(!delete ? View.GONE : View.VISIBLE);
        setClickListener(holder.binding.getRoot(), item);

        holder.binding.name.setOnClickListener(v -> mListener.onItemClick(item));

        // 绑定ViewHolder
        if (position == getItemCount() - 1) {
            // 最后一个item不需要分隔线
            holder.dividerView.setVisibility(View.GONE);
        } else {
            holder.dividerView.setVisibility(View.VISIBLE);
        }
    }

    private void setClickListener(View root, File item) {
        root.setOnLongClickListener(view -> mListener.onLongClick());
        root.setOnClickListener(view -> {
            if (isDelete()) mListener.onItemDelete(item);
            else mListener.onItemClick(item);
        });
    }


    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterIdmBinding binding;
        public View dividerView;

        ViewHolder(@NonNull AdapterIdmBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            dividerView = itemView.findViewById(R.id.dividerView);
        }
    }

}
