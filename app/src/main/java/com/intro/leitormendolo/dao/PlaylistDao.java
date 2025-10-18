package com.intro.leitormendolo.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import com.intro.leitormendolo.model.Playlist;
import com.intro.leitormendolo.model.PlaylistSongCrossRef;
import com.intro.leitormendolo.model.PlaylistWithSongs;
import java.util.List;

@Dao
public interface PlaylistDao {

    @Insert
    long insertPlaylist(Playlist playlist); // Retorna o ID da playlist inserida

    @Insert
    void insertPlaylistSongCrossRef(PlaylistSongCrossRef crossRef);

    @Delete
    void deletePlaylist(Playlist playlist);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    void deleteSongFromPlaylist(long playlistId, long songId);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    void deleteAllSongsFromPlaylist(long playlistId); // Útil ao apagar uma playlist

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    List<Playlist> getAllPlaylists();

    @Transaction // Garante que a leitura da Playlist e das Songs é feita atomicamente
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    PlaylistWithSongs getPlaylistWithSongs(long playlistId);

    @Transaction
    @Query("SELECT * FROM playlists")
    List<PlaylistWithSongs> getAllPlaylistsWithSongs();
}