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
import com.purebeat.model.Playlist;

import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<Playlist> playlists = new ArrayList<>();
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
        void onPlaylistDeleteClick(Playlist playlist);
    }

    public void setOnPlaylistClickListener(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.bind(playlist);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    class PlaylistViewHolder extends RecyclerView.ViewHolder {

        private ImageView ivPlaylistIcon;
        private TextView tvPlaylistName;
        private TextView tvSongCount;
        private ImageView ivDelete;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlaylistIcon = itemView.findViewById(R.id.iv_playlist_icon);
            tvPlaylistName = itemView.findViewById(R.id.tv_playlist_name);
            tvSongCount = itemView.findViewById(R.id.tv_song_count);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }

        public void bind(Playlist playlist) {
            tvPlaylistName.setText(playlist.getName());
            tvSongCount.setText(playlist.getSongCount() + " 首歌曲");

            if (playlist.getCoverImagePath() != null && !playlist.getCoverImagePath().isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(playlist.getCoverImagePath())
                    .placeholder(R.drawable.ic_playlist)
                    .error(R.drawable.ic_playlist)
                    .centerCrop()
                    .into(ivPlaylistIcon);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlist);
                }
            });

            ivDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistDeleteClick(playlist);
                }
            });
        }
    }
}
