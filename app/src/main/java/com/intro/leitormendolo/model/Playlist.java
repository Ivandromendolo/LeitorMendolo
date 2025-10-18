package com.intro.leitormendolo.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "playlists")
public class Playlist {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "name")
    public String name;

    // Construtor necessário para o Room
    public Playlist(String name) {
        this.name = name;
    }

    // Getters (opcional, mas boa prática)
    public long getId() { return id; }
    public String getName() { return name; }
}