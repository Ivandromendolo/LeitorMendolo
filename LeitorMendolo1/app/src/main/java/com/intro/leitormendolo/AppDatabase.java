package com.intro.leitormendolo;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.intro.leitormendolo.dao.SongDao;
import com.intro.leitormendolo.model.Song;

// Anotação que define as tabelas e a versão da base de dados
@Database(entities = {Song.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract SongDao songDao(); // Método que nos dá acesso ao "porteiro"

    private static volatile AppDatabase INSTANCE;

    // Método para obter a instância da base de dados (garante que só temos uma)
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "music_player_database")
                            // NOTA: Isto é um atalho para o nosso tutorial. Numa app real,
                            // as operações de base de dados devem ser feitas em background.
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}