package com.intro.leitormendolo.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler; // Adicionado
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.intro.leitormendolo.R;
import com.intro.leitormendolo.model.Song;
import com.intro.leitormendolo.view.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;
    private int currentSongIndex = 0;
    private boolean isShuffleOn = false;
    private boolean isRepeatOn = false;
    private Random random = new Random();
    private List<Song> songList = new ArrayList<>();
    private final IBinder musicBinder = new MusicBinder();
    private AudioManager audioManager;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MusicPlayerChannel";
    public static final String ACTION_PLAY = "com.intro.leitormendolo.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.intro.leitormendolo.ACTION_PAUSE";
    public static final String ACTION_NEXT = "com.intro.leitormendolo.ACTION_NEXT";
    public static final String ACTION_PREV = "com.intro.leitormendolo.ACTION_PREV";
    private MediaSessionCompat mediaSession;
    private ActionReceiver actionReceiver;
    private PlaybackStateCompat.Builder stateBuilder;

    // --- NOVO: Handler e Runnable para a atualização da notificação ---
    private Handler notificationHandler = new Handler();
    private Runnable updateNotificationTask;

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (mediaPlayer != null && !mediaPlayer.isPlaying()) play();
                    if (mediaPlayer != null) mediaPlayer.setVolume(1.0f, 1.0f);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.2f, 0.2f);
                    break;
            }
        }
    };

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        songList = new ArrayList<>();

        mediaSession = new MediaSessionCompat(this, "MusicPlayerSession");
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() { play(); }
            @Override
            public void onPause() { pause(); }
            @Override
            public void onSkipToNext() { playNext(); }
            @Override
            public void onSkipToPrevious() { playPrevious(); }
        });
        mediaSession.setActive(true);

        createNotificationChannel();

        actionReceiver = new ActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PREV);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(actionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(actionReceiver, filter);
        }

        // --- NOVO: Inicializa o atualizador ---
        initializeNotificationUpdater();
    }

    private void initializeNotificationUpdater() {
        updateNotificationTask = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    updatePlaybackState(true);
                    notificationHandler.postDelayed(this, 1000); // Reagenda para daqui a 1 segundo
                }
            }
        };
    }

    public void setSongList(List<Song> songs) {
        this.songList = songs;
        if (songList != null && !songList.isEmpty()) {
            prepareSongAtIndex(0);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Music Player Controls", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(boolean isPlaying) {
        if (songList == null || songList.isEmpty() || currentSongIndex >= songList.size()) return null;

        Song currentSong = songList.get(currentSongIndex);
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(), getAlbumArtForSong(currentSongIndex));
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent prevPI = PendingIntent.getBroadcast(this, 1, new Intent(ACTION_PREV), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent nextPI = PendingIntent.getBroadcast(this, 3, new Intent(ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent playPausePI;
        int playPauseIcon;
        if (isPlaying) {
            playPausePI = PendingIntent.getBroadcast(this, 2, new Intent(ACTION_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            playPauseIcon = R.drawable.ic_pause;
        } else {
            playPausePI = PendingIntent.getBroadcast(this, 4, new Intent(ACTION_PLAY), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            playPauseIcon = R.drawable.ic_play;
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentSong.getTitle())
                .setContentText(currentSong.getArtist())
                .setSmallIcon(R.drawable.ic_music_note)
                .setLargeIcon(albumArt)
                .setContentIntent(pIntent)
                .setOnlyAlertOnce(true)
                .addAction(R.drawable.ic_skip_previous, "Previous", prevPI)
                .addAction(playPauseIcon, isPlaying ? "Pause" : "Play", playPausePI)
                .addAction(R.drawable.ic_next, "Next", nextPI)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .build();
    }

    private int getAlbumArtForSong(int songIndex) {
        switch (songIndex) {
            case 0: return R.raw.faded;
            case 1: return R.raw.anitta;
            case 2: return R.raw.adele;
            default: return R.drawable.ic_music_note;
        }
    }

    private int getSongResource(int index) {
        switch (index) {
            case 0: return R.raw.faded;
            case 1: return R.raw.anitta;
            case 2: return R.raw.adele;
            default: return -1;
        }
    }

    private void prepareSongAtIndex(int index) {
        if (songList != null && index >= 0 && index < songList.size()) {
            currentSongIndex = index;
            try {
                if (mediaPlayer != null) mediaPlayer.release();
                int resId = getSongResource(currentSongIndex);
                if (resId != -1) {
                    mediaPlayer = MediaPlayer.create(this, resId);
                    mediaPlayer.setOnCompletionListener(this::onCompletion);
                }
            } catch (Exception e) {
                Log.e("MusicService", "Error creating media player", e);
            }
        }
    }

    private void updatePlaybackState(boolean isPlaying) {
        if (mediaPlayer == null) return;
        long position = mediaPlayer.getCurrentPosition();

        stateBuilder.setState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                position, 1.0f);
        mediaSession.setPlaybackState(stateBuilder.build());
    }

    public void playSongAtIndex(int index) {
        prepareSongAtIndex(index);
        play();
    }

    public void playNext() {
        if (songList == null || songList.isEmpty()) return;
        if (isRepeatOn) {
            playSongAtIndex(currentSongIndex);
        } else if (isShuffleOn) {
            int nextIndex = random.nextInt(songList.size());
            while (songList.size() > 1 && nextIndex == currentSongIndex) {
                nextIndex = random.nextInt(songList.size());
            }
            playSongAtIndex(nextIndex);
        } else {
            int nextIndex = (currentSongIndex + 1) % songList.size();
            playSongAtIndex(nextIndex);
        }
    }

    public void playPrevious() {
        if (songList == null || songList.isEmpty()) return;
        int prevIndex = currentSongIndex - 1;
        if (prevIndex < 0) {
            prevIndex = songList.size() - 1;
        }
        playSongAtIndex(prevIndex);
    }

    public void onCompletion(MediaPlayer mp) {
        notificationHandler.removeCallbacks(updateNotificationTask);
        playNext();
    }

    public void play() {
        if (mediaPlayer != null) {
            int result = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mediaPlayer.start();
                updatePlaybackState(true);
                Notification notification = buildNotification(true);
                if (notification != null) startForeground(NOTIFICATION_ID, notification);
                // Inicia o atualizador
                notificationHandler.post(updateNotificationTask);
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updatePlaybackState(false);
            audioManager.abandonAudioFocus(audioFocusChangeListener);
            stopForeground(false);
            Notification notification = buildNotification(false);
            if (notification != null) NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification);
            // Para o atualizador
            notificationHandler.removeCallbacks(updateNotificationTask);
        }
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
            // Atualiza imediatamente o estado para a nova posição
            updatePlaybackState(mediaPlayer.isPlaying());
        }
    }
    public int getCurrentPosition() { return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0; }
    public int getDuration() { return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0; }
    public boolean isPlaying() { return (mediaPlayer != null) && mediaPlayer.isPlaying(); }
    public int getCurrentSongIndex() { return currentSongIndex; }
    public int getPlaylistSize() { return songList != null ? songList.size() : 0; }
    public void setShuffleMode(boolean isShuffleOn) { this.isShuffleOn = isShuffleOn; }
    public void setRepeatMode(boolean isRepeatOn) { this.isRepeatOn = isRepeatOn; }

    @Nullable @Override
    public IBinder onBind(Intent intent) { return musicBinder; }

    @Override
    public void onDestroy() {
        notificationHandler.removeCallbacks(updateNotificationTask);
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (audioManager != null) {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }
        unregisterReceiver(actionReceiver);
        mediaSession.release();
    }

    public class ActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case ACTION_PLAY: play(); break;
                case ACTION_PAUSE: pause(); break;
                case ACTION_NEXT: playNext(); break;
                case ACTION_PREV: playPrevious(); break;
            }
        }
    }
}