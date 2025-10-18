package com.intro.leitormendolo.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.intro.leitormendolo.model.Song;
import java.util.List;

@Dao // Diz ao Room que isto é um Data Access Object
public interface SongDao {

    @Query("SELECT * FROM songs") // Define uma consulta para obter todas as músicas
    List<Song> getAll();

    @Insert // Define uma operação para inserir músicas
    void insertAll(Song... songs);

    // No futuro, podemos adicionar @Delete, @Update, etc.
}