package com.intro.leitormendolo;

import com.intro.leitormendolo.model.Song;
import com.intro.leitormendolo.service.MusicService;
import java.util.List;

public interface MusicPlayerContract {

    interface View {
        void showSongDetails(Song song);
        void updatePlayPauseButton(boolean isPlaying);
        void updateSeekBar(int progress, int max, String currentTime, String totalTime);
        void updateShuffleButton(boolean isActive);
        void updateRepeatButton(boolean isActive);
        void startAlbumArtAnimation();
        void stopAlbumArtAnimation();
        void displaySongList(List<Song> songList);
    }

    interface Presenter {
        void onPlayPauseClicked();
        void onNextClicked();
        void onPrevClicked();
        void onSeekBarMoved(int progress);
        void onShuffleClicked();
        void onRepeatClicked();
        void loadMusic();
        void viewDestroyed();
        void onServiceConnected(MusicService service);
        void onSongClicked(int position);

        // NOVO MÉTODO PARA A BUSCA
        void onSearchQueryChanged(String query);
    }
}