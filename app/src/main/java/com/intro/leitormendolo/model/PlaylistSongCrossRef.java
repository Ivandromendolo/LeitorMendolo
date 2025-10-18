package com.intro.leitormendolo.model;

import androidx.room.Entity;
import androidx.room.Index;

// Define a tabela de ligação entre Playlist e Song
// Garante que cada par (playlistId, songId) é único
@Entity(tableName = "playlist_songs",
        primaryKeys = {"playlistId", "songId"},
        indices = {@Index("songId")}) // Índice para pesquisas mais rápidas por música
public class PlaylistSongCrossRef {

    public long playlistId;
    public long songId;

    public PlaylistSongCrossRef(long playlistId, long songId) {
        this.playlistId = playlistId;
        this.songId = songId;
    }
}