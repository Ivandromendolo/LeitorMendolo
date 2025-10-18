package com.intro.leitormendolo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.intro.leitormendolo.R;
import com.intro.leitormendolo.model.Playlist;
import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<Playlist> playlistList = new ArrayList<>();
    private OnPlaylistClickListener listener;

    // --- INTERFACE ATUALIZADA ---
    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);

        void onPlaylistLongClick(Playlist playlist);

    }
    // --- FIM DA ATUALIZAÇÃO ---

    public PlaylistAdapter(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.playlist_item_layout, parent, false);
        return new PlaylistViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist currentPlaylist = playlistList.get(position);
        // Adiciona verificação de nulidade por segurança
        if (currentPlaylist != null && currentPlaylist.getName() != null) {
            holder.textViewName.setText(currentPlaylist.getName());
        } else {
            holder.textViewName.setText("Playlist inválida"); // Ou algum texto padrão
        }
    }


    @Override
    public int getItemCount() {
        return playlistList == null ? 0 : playlistList.size(); // Adiciona verificação de nulidade
    }

    public void setPlaylists(List<Playlist> playlists) {
        // Garante que a lista nunca é nula
        this.playlistList = (playlists == null) ? new ArrayList<>() : playlists;
        notifyDataSetChanged();
    }

    class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewName;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.itemPlaylistName);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                // Adiciona verificação para playlistList
                if (listener != null && position != RecyclerView.NO_POSITION && playlistList != null && position < playlistList.size()) {
                    listener.onPlaylistClick(playlistList.get(position));
                }
            });

            // --- NOVO LISTENER DE CLIQUE LONGO ADICIONADO ---
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                // Adiciona verificação para playlistList
                if (listener != null && position != RecyclerView.NO_POSITION && playlistList != null && position < playlistList.size()) {
                    listener.onPlaylistLongClick(playlistList.get(position));
                    return true; // Clique longo tratado
                }
                return false; // Clique longo não tratado
            });
            // --- FIM DO NOVO LISTENER ---
        }
    }
}