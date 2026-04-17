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
import com.purebeat.model.Song;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songs = new ArrayList<>();
    private OnSongClickListener listener;
    private int currentPlayingIndex = -1;

    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
        void onSongLongClick(Song song, int position);
    }

    public void setOnSongClickListener(OnSongClickListener listener) {
        this.listener = listener;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
        notifyDataSetChanged();
    }

    public void setCurrentPlayingIndex(int index) {
        int oldIndex = currentPlayingIndex;
        currentPlayingIndex = index;
        if (oldIndex != -1) {
            notifyItemChanged(oldIndex);
        }
        if (currentPlayingIndex != -1) {
            notifyItemChanged(currentPlayingIndex);
        }
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.bind(song, position == currentPlayingIndex);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    class SongViewHolder extends RecyclerView.ViewHolder {

        private ImageView ivAlbumArt;
        private TextView tvTitle;
        private TextView tvArtist;
        private ImageView ivPlaying;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlbumArt = itemView.findViewById(R.id.iv_album_art);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtist = itemView.findViewById(R.id.tv_artist);
            ivPlaying = itemView.findViewById(R.id.iv_playing);
        }

        public void bind(Song song, boolean isPlaying) {
            tvTitle.setText(song.getTitle());
            tvArtist.setText(song.getArtist());

            if (isPlaying) {
                ivPlaying.setVisibility(View.VISIBLE);
                tvTitle.setTextColor(itemView.getContext().getColor(R.color.primary));
            } else {
                ivPlaying.setVisibility(View.GONE);
                tvTitle.setTextColor(itemView.getContext().getColor(R.color.text_primary));
            }

            Glide.with(itemView.getContext())
                .load(song.getAlbumArtUri())
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .centerCrop()
                .into(ivAlbumArt);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSongClick(song, getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onSongLongClick(song, getAdapterPosition());
                }
                return true;
            });
        }
    }
}
