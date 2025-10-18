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
    private OnSongClickListener listener;

    // --- INTERFACE ATUALIZADA ---
    public interface OnSongClickListener {
        void onSongClick(int position);

        void onSongLongClick(Song song); // Passa o objeto Song diretamente

    }
    // --- FIM DA ATUALIZAÇÃO ---

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
        // Adiciona verificações de nulidade por segurança
        if (currentSong != null) {
            holder.textViewTitle.setText(currentSong.getTitle() != null ? currentSong.getTitle() : "Título inválido");
            holder.textViewArtist.setText(currentSong.getArtist() != null ? currentSong.getArtist() : "Artista inválido");
        } else {
            holder.textViewTitle.setText("Música inválida");
            holder.textViewArtist.setText("");
        }
    }


    @Override
    public int getItemCount() {
        return songList == null ? 0 : songList.size(); // Adiciona verificação de nulidade
    }

    public void setSongs(List<Song> songs) {
        // Garante que a lista nunca é nula
        this.songList = (songs == null) ? new ArrayList<>() : songs;
        notifyDataSetChanged();
    }

    class SongViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewTitle;
        private TextView textViewArtist;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.itemSongTitle);
            textViewArtist = itemView.findViewById(R.id.itemArtistName);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onSongClick(position);
                }
            });

            // --- NOVO LISTENER DE CLIQUE LONGO ADICIONADO ---
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                // Verifica se listener existe, posição é válida e a lista não é nula/vazia
                if (listener != null && position != RecyclerView.NO_POSITION && songList != null && position < songList.size()) {
                    Song clickedSong = songList.get(position);
                    if (clickedSong != null) { // Verifica se a música não é nula
                        listener.onSongLongClick(clickedSong); // Envia o objeto Song
                        return true; // Clique longo tratado
                    }
                }
                return false; // Clique longo não tratado
            });
            // --- FIM DO NOVO LISTENER ---
        }
    }
}