package com.intro.leitormendolo;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// --- NOVAS IMPORTAÇÕES ADICIONADAS ---
import com.intro.leitormendolo.dao.PlaylistDao;         // Importa o novo DAO
import com.intro.leitormendolo.model.Playlist;         // Importa a nova entidade Playlist
import com.intro.leitormendolo.model.PlaylistSongCrossRef; // Importa a nova entidade de relação
// --- FIM DAS NOVAS IMPORTAÇÕES ---

import com.intro.leitormendolo.dao.SongDao;
import com.intro.leitormendolo.model.Song;

// --- ALTERAÇÕES NA ANOTAÇÃO @Database ---
// 1. Adiciona Playlist.class e PlaylistSongCrossRef.class à lista de 'entities'
// 2. Incrementa a 'version' de 1 para 2
@Database(entities = {Song.class, Playlist.class, PlaylistSongCrossRef.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {

    public abstract SongDao songDao();
    public abstract PlaylistDao playlistDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "music_player_database")
                            // --- ALTERAÇÃO IMPORTANTE AQUI ---
                            // Adicionado para lidar com a mudança de versão da base de dados.
                            // Isto APAGARÁ a base de dados antiga na primeira execução após a atualização.
                            // Numa app real, usaríamos Migrations para preservar os dados.
                            .fallbackToDestructiveMigration()
                            // --- FIM DA ALTERAÇÃO IMPORTANTE ---
                            .allowMainThreadQueries() // Mantido como estava
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}