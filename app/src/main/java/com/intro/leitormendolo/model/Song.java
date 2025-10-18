package com.intro.leitormendolo.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "songs") // Diz ao Room que esta classe é uma tabela chamada "songs"
public class Song {

    @PrimaryKey(autoGenerate = true) // Define o 'id' como a chave primária que se auto-incrementa
    public long id;

    @ColumnInfo(name = "title") // Define o nome da coluna
    public String title;

    @ColumnInfo(name = "artist")
    public String artist;

    @ColumnInfo(name = "path")
    public String path;

    // Um construtor vazio é por vezes útil para o Room
    public Song() {}

    // Construtor que vamos usar
    public Song(String title, String artist, String path) {
        this.title = title;
        this.artist = artist;
        this.path = path;
    }

    // --- Getters ---
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getPath() { return path; }
}