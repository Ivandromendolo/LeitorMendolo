package com.intro.leitormendolo.model;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;
import java.util.List;

// Classe para carregar uma Playlist e todas as suas Songs associadas
public class PlaylistWithSongs {

    @Embedded // Inclui os campos da entidade Playlist diretamente aqui
    public Playlist playlist;

    @Relation(
            parentColumn = "id", // Coluna 'id' da tabela 'playlists'
            entityColumn = "id", // Coluna 'id' da tabela 'songs'
            associateBy = @Junction(
                    value = PlaylistSongCrossRef.class, // Tabela de ligação
                    parentColumn = "playlistId", // Coluna na tabela de ligação que referencia a Playlist
                    entityColumn = "songId" // Coluna na tabela de ligação que referencia a Song
            )
    )
    public List<Song> songs; // Lista de músicas associadas a esta playlist
}