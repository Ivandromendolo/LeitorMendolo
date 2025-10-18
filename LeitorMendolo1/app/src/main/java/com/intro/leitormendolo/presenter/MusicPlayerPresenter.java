package com.intro.leitormendolo.presenter;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.intro.leitormendolo.AppDatabase;
import com.intro.leitormendolo.MusicPlayerContract;
import com.intro.leitormendolo.model.Song;
import com.intro.leitormendolo.service.MusicService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MusicPlayerPresenter implements MusicPlayerContract.Presenter {

    private MusicPlayerContract.View view;
    private MusicService musicService;
    private boolean isServiceConnected = false;
    private Handler handler = new Handler();
    private Runnable updateSeekBarAction;
    private List<Song> songList;
    private Context context;
    private int lastKnownSongIndex = -1;
    private boolean isShuffleOn = false;
    private boolean isRepeatOn = false;
    private List<Song> fullSongList = new ArrayList<>();

    public MusicPlayerPresenter(MusicPlayerContract.View view, Context context) {
        this.view = view;
        this.context = context;
        initializeSeekBarRunnable();
    }

    @Override
    public void onServiceConnected(MusicService service) {
        this.musicService = service;
        isServiceConnected = true;
        loadMusic();
        updateSongDetails(musicService.getCurrentSongIndex());
        updateInitialUI();
    }

    @Override
    public void loadMusic() {
        AppDatabase db = AppDatabase.getDatabase(context);
        fullSongList = db.songDao().getAll();
        if (fullSongList.isEmpty()) {
            db.songDao().insertAll(
                    new Song("Faded", "Alan Walker", ""),
                    new Song("Envolver", "Anitta", ""),
                    new Song("Hello", "Adele", "")
            );
            fullSongList = db.songDao().getAll();
        }

        this.songList = new ArrayList<>(fullSongList);

        if (view != null) {
            view.displaySongList(this.songList);
        }
    }

    @Override
    public void onSearchQueryChanged(String query) {
        List<Song> filteredList = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(fullSongList);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (Song song : fullSongList) {
                if (song.getTitle().toLowerCase().contains(filterPattern) ||
                        song.getArtist().toLowerCase().contains(filterPattern)) {
                    filteredList.add(song);
                }
            }
        }
        if (view != null) {
            view.displaySongList(filteredList);
        }
        this.songList = filteredList;
    }

    private void updateSongDetails(int index) {
        if (view != null && !songList.isEmpty() && index < songList.size()) {
            view.showSongDetails(songList.get(index));
            if (musicService != null && !musicService.isPlaying()) {
                view.stopAlbumArtAnimation();
            }
            lastKnownSongIndex = index;
        }
    }

    private void initializeSeekBarRunnable() {
        updateSeekBarAction = new Runnable() {
            @Override
            public void run() {
                if (isServiceConnected && view != null) {
                    int currentServiceIndex = musicService.getCurrentSongIndex();
                    if (currentServiceIndex != lastKnownSongIndex) {
                        updateSongDetails(currentServiceIndex);
                    }
                    int currentPosition = musicService.getCurrentPosition();
                    int duration = musicService.getDuration();
                    view.updateSeekBar(currentPosition, duration, formatTime(currentPosition), formatTime(duration));
                    if (musicService.isPlaying()) {
                        handler.postDelayed(this, 1000);
                    }
                }
            }
        };
    }

    private void updateInitialUI() {
        if (!isServiceConnected || view == null) return;
        view.updatePlayPauseButton(musicService.isPlaying());
        int duration = musicService.getDuration();
        view.updateSeekBar(0, duration, "00:00", formatTime(duration));
        view.updateShuffleButton(isShuffleOn);
        view.updateRepeatButton(isRepeatOn);
    }

    @Override
    public void onPlayPauseClicked() {
        if (!isServiceConnected) return;
        if (musicService.isPlaying()) {
            musicService.pause();
            handler.removeCallbacks(updateSeekBarAction);
            if (view != null) view.stopAlbumArtAnimation();
        } else {
            musicService.play();
            handler.post(updateSeekBarAction);
            if (view != null) view.startAlbumArtAnimation();
        }
        if (view != null) {
            view.updatePlayPauseButton(musicService.isPlaying());
        }
    }

    @Override
    public void onNextClicked() {
        if (isServiceConnected) {
            musicService.onCompletion(null);
        }
    }

    @Override
    public void onPrevClicked() {
        if (!isServiceConnected) return;
        int prevIndex = musicService.getCurrentSongIndex() - 1;
        if (prevIndex < 0) {
            prevIndex = songList.size() - 1;
        }
        if (view != null) {
            view.updatePlayPauseButton(true);
            view.startAlbumArtAnimation();
        }
        handler.removeCallbacks(updateSeekBarAction);
        handler.post(updateSeekBarAction);
        musicService.playSongAtIndex(prevIndex);
        updateSongDetails(prevIndex);
    }

    @Override
    public void onShuffleClicked() {
        isShuffleOn = !isShuffleOn;
        if (isServiceConnected) {
            musicService.setShuffleMode(isShuffleOn);
        }
        if (view != null) {
            view.updateShuffleButton(isShuffleOn);
        }
    }

    @Override
    public void onRepeatClicked() {
        isRepeatOn = !isRepeatOn;
        if (isServiceConnected) {
            musicService.setRepeatMode(isRepeatOn);
        }
        if (view != null) {
            view.updateRepeatButton(isRepeatOn);
        }
    }

    @Override
    public void onSeekBarMoved(int progress) {
        if (isServiceConnected) {
            musicService.seekTo(progress);
        }
    }

    @Override
    public void onSongClicked(int position) {
        if (isServiceConnected) {
            Song clickedSong = this.songList.get(position);
            // Precisamos encontrar o índice real desta música na lista do service
            // Por agora, vamos assumir que os índices correspondem
            int serviceIndex = position; // Esta é uma simplificação

            musicService.playSongAtIndex(serviceIndex);
            updateSongDetails(serviceIndex);
            if (view != null) {
                view.updatePlayPauseButton(true);
                view.startAlbumArtAnimation();
            }
            handler.removeCallbacks(updateSeekBarAction);
            handler.post(updateSeekBarAction);
        }
    }

    private String formatTime(int millis) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    @Override
    public void viewDestroyed() {
        handler.removeCallbacks(updateSeekBarAction);
        this.view = null;
    }
}