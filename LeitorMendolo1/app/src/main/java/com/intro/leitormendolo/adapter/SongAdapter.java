package com.intro.leitormendolo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.intro.leitormendolo.R;
import com.intro.leitormendolo.model.Song;
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songList = new ArrayList<>();
    private OnSongClickListener listener; // NOVO: Listener para os cliques

    // NOVO: Interface para comunicar o clique
    public interface OnSongClickListener {
        void onSongClick(int position);
    }

    // NOVO: Construtor que recebe o listener
    public SongAdapter(OnSongClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song_item_layout, parent, false);
        return new SongViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song currentSong = songList.get(position);
        holder.textViewTitle.setText(currentSong.getTitle());
        holder.textViewArtist.setText(currentSong.getArtist());
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public void setSongs(List<Song> songs) {
        this.songList = songs;
        notifyDataSetChanged();
    }

    class SongViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewTitle;
        private TextView textViewArtist;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.itemSongTitle);
            textViewArtist = itemView.findViewById(R.id.itemArtistName);

            // NOVO: Adicionar o listener de clique ao item inteiro
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onSongClick(position);
                }
            });
        }
    }
}