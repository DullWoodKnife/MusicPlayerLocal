package com.purebeat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.purebeat.R;
import com.purebeat.model.Folder;

import java.util.ArrayList;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private List<Folder> folders = new ArrayList<>();
    private OnFolderClickListener listener;

    public interface OnFolderClickListener {
        void onFolderClick(Folder folder);
    }

    public void setOnFolderClickListener(OnFolderClickListener listener) {
        this.listener = listener;
    }

    public void setFolders(List<Folder> folders) {
        this.folders = folders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        Folder folder = folders.get(position);
        holder.bind(folder);
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    class FolderViewHolder extends RecyclerView.ViewHolder {

        private ImageView ivFolderIcon;
        private TextView tvFolderName;
        private TextView tvSongCount;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFolderIcon = itemView.findViewById(R.id.iv_folder_icon);
            tvFolderName = itemView.findViewById(R.id.tv_folder_name);
            tvSongCount = itemView.findViewById(R.id.tv_song_count);
        }

        public void bind(Folder folder) {
            tvFolderName.setText(folder.getName());
            tvSongCount.setText(folder.getSongCount() + " 首歌曲");

            // Load first song's album art as folder icon
            if (folder.getSongs() != null && !folder.getSongs().isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(folder.getSongs().get(0).getAlbumArtUri())
                    .placeholder(R.drawable.ic_folder)
                    .error(R.drawable.ic_folder)
                    .centerCrop()
                    .into(ivFolderIcon);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFolderClick(folder);
                }
            });
        }
    }
}
