package com.intro.leitormendolo.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import com.intro.leitormendolo.R;
import java.util.ArrayList;
import java.util.Random;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;
    private final IBinder musicBinder = new MusicBinder();
    private ArrayList<Integer> songResourceIds;
    private int currentSongIndex = 0;
    private boolean isShuffleOn = false;
    private boolean isRepeatOn = false;
    private Random random = new Random();

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        songResourceIds = new ArrayList<>();
        songResourceIds.add(R.raw.faded);
        songResourceIds.add(R.raw.anitta);
        songResourceIds.add(R.raw.adele);

        // Prepara a primeira música assim que o serviço é criado.
        prepareSongAtIndex(0);
    }

    private void prepareSongAtIndex(int index) {
        if (index >= 0 && index < songResourceIds.size()) {
            currentSongIndex = index;
            try {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(this, songResourceIds.get(currentSongIndex));
                mediaPlayer.setOnCompletionListener(this::onCompletion);
            } catch (Exception e) {
                Log.e("MusicService", "Erro ao criar media player para o índice " + index, e);
            }
        }
    }

    public void playSongAtIndex(int index) {
        prepareSongAtIndex(index);
        if(mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    public void onCompletion(MediaPlayer mp) {
        if (isRepeatOn) {
            playSongAtIndex(currentSongIndex);
        } else if (isShuffleOn) {
            int nextIndex = random.nextInt(songResourceIds.size());
            if (songResourceIds.size() > 1) {
                while (nextIndex == currentSongIndex) {
                    nextIndex = random.nextInt(songResourceIds.size());
                }
            }
            playSongAtIndex(nextIndex);
        } else {
            int nextIndex = (currentSongIndex + 1) % songResourceIds.size();
            playSongAtIndex(nextIndex);
        }
    }

    public void play() { if(mediaPlayer != null) mediaPlayer.start(); }
    public void pause() { if(mediaPlayer != null) mediaPlayer.pause(); }
    public void seekTo(int position) { if(mediaPlayer != null) mediaPlayer.seekTo(position); }
    public int getCurrentPosition() { return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0; }
    public int getDuration() { return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0; }
    public boolean isPlaying() { return (mediaPlayer != null) && mediaPlayer.isPlaying(); }
    public int getCurrentSongIndex() { return currentSongIndex; }
    public int getPlaylistSize() { return songResourceIds.size(); }
    public void setShuffleMode(boolean isShuffleOn) { this.isShuffleOn = isShuffleOn; }
    public void setRepeatMode(boolean isRepeatOn) { this.isRepeatOn = isRepeatOn; }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return musicBinder; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) mediaPlayer.release();
    }
}