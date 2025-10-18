package com.intro.leitormendolo.presenter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.intro.leitormendolo.AppDatabase;
import com.intro.leitormendolo.MusicPlayerContract;
import com.intro.leitormendolo.model.Playlist;
import com.intro.leitormendolo.model.PlaylistSongCrossRef; // Importação adicionada
import com.intro.leitormendolo.model.Song;
import com.intro.leitormendolo.service.MusicService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MusicPlayerPresenter implements MusicPlayerContract.Presenter {

    private MusicPlayerContract.View view;
    private MusicService musicService;
    private boolean isServiceConnected = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBarAction;
    private List<Song> songList; // Lista exibida (pode ser filtrada)
    private Context context;
    private int lastKnownSongIndex = -1; // Índice na lista COMPLETA (fullSongList)
    private boolean isShuffleOn = false;
    private boolean isRepeatOn = false;
    private List<Song> fullSongList = new ArrayList<>(); // Lista completa de músicas
    private List<Playlist> playlistList = new ArrayList<>();
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    public MusicPlayerPresenter(MusicPlayerContract.View view, Context context) {
        this.view = view;
        this.context = context.getApplicationContext();
        initializeSeekBarRunnable();
    }

    @Override
    public void onServiceConnected(MusicService service) {
        this.musicService = service;
        isServiceConnected = true;
        loadMusic();
        loadPlaylists();
    }

    @Override
    public void loadMusic() {
        databaseExecutor.execute(() -> {
            List<Song> loadedSongs;
            try {
                AppDatabase db = AppDatabase.getDatabase(context);
                loadedSongs = db.songDao().getAll();
                if (loadedSongs.isEmpty()) {
                    db.songDao().insertAll(
                            new Song("Faded", "Alan Walker", ""),
                            new Song("Envolver", "Anitta", ""),
                            new Song("Hello", "Adele", "")
                    );
                    loadedSongs = db.songDao().getAll();
                }
                Log.d("Presenter", "Músicas carregadas: " + loadedSongs.size());

                List<Song> finalLoadedSongs = new ArrayList<>(loadedSongs);
                handler.post(() -> {
                    fullSongList = finalLoadedSongs;
                    songList = new ArrayList<>(fullSongList);
                    if (view != null) {
                        view.displaySongList(songList);
                    }
                    if (isServiceConnected && musicService != null && fullSongList != null) {
                        musicService.setSongList(fullSongList);
                    }
                    updateInitialUI();
                    int initialIndex = (musicService != null) ? musicService.getCurrentSongIndex() : 0;
                    if (initialIndex < 0 || initialIndex >= fullSongList.size()) {
                        initialIndex = 0;
                    }
                    updateSongDetails(initialIndex);
                });
            } catch (Exception e) {
                Log.e("Presenter", "Erro ao carregar músicas", e);
                handler.post(() -> { if (view != null) view.showToast("Erro ao carregar músicas"); });
            }
        });
    }


    @Override
    public void loadPlaylists() {
        databaseExecutor.execute(() -> {
            List<Playlist> loadedPlaylists;
            try {
                AppDatabase db = AppDatabase.getDatabase(context);
                loadedPlaylists = db.playlistDao().getAllPlaylists();
                Log.d("Presenter", "Playlists carregadas da BD: " + loadedPlaylists.size());
            } catch (Exception e) {
                Log.e("Presenter", "Erro ao carregar playlists da BD", e);
                loadedPlaylists = new ArrayList<>();
            }
            List<Playlist> finalLoadedPlaylists = loadedPlaylists;
            handler.post(() -> {
                this.playlistList = finalLoadedPlaylists;
                if (view != null) {
                    view.displayPlaylists(this.playlistList);
                    Log.d("Presenter", "Chamou displayPlaylists na View");
                }
            });
        });
    }

    @Override
    public void createPlaylist(String playlistName) {
        if (playlistName == null || playlistName.trim().isEmpty()) {
            Log.w("Presenter", "Tentativa de criar playlist com nome vazio.");
            return;
        }
        final String finalPlaylistName = playlistName.trim();
        databaseExecutor.execute(() -> {
            try {
                Playlist newPlaylist = new Playlist(finalPlaylistName);
                AppDatabase db = AppDatabase.getDatabase(context);
                long newId = db.playlistDao().insertPlaylist(newPlaylist);
                Log.i("Presenter", "Playlist '" + finalPlaylistName + "' criada com ID: " + newId);
                List<Playlist> updatedPlaylists = db.playlistDao().getAllPlaylists();
                handler.post(() -> {
                    this.playlistList = updatedPlaylists;
                    if (view != null) {
                        view.displayPlaylists(this.playlistList);
                        Log.d("Presenter", "Chamou displayPlaylists após criar playlist");
                        view.showToast("Playlist '" + finalPlaylistName + "' criada!");
                    }
                });
            } catch (Exception e) {
                Log.e("Presenter", "Erro ao inserir playlist '" + finalPlaylistName + "'", e);
                handler.post(() -> { if (view != null) view.showToast("Erro ao criar playlist"); });
            }
        });
    }

    @Override
    public void deletePlaylist(Playlist playlist) {
        if (playlist == null) {
            Log.w("Presenter", "Tentativa de apagar playlist nula.");
            return;
        }
        databaseExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(context);
                db.playlistDao().deleteAllSongsFromPlaylist(playlist.id);
                db.playlistDao().deletePlaylist(playlist);
                Log.i("Presenter", "Playlist '" + playlist.getName() + "' apagada.");
                List<Playlist> updatedPlaylists = db.playlistDao().getAllPlaylists();
                handler.post(() -> {
                    this.playlistList = updatedPlaylists;
                    if (view != null) {
                        view.displayPlaylists(this.playlistList);
                        Log.d("Presenter", "Chamou displayPlaylists após apagar playlist");
                        view.showToast("Playlist '" + playlist.getName() + "' apagada.");
                    }
                });
            } catch (Exception e) {
                Log.e("Presenter", "Erro ao apagar playlist '" + playlist.getName() + "'", e);
                handler.post(() -> { if (view != null) view.showToast("Erro ao apagar playlist"); });
            }
        });
    }


    @Override
    public void addSongToPlaylist(Song song, Playlist playlist) {
        if (song == null || playlist == null) {
            Log.w("Presenter", "Tentativa de adicionar música ou playlist nula.");
            return;
        }


        Song songFromFullList = findSongInFullListByIdOrDetails(song);

        if (songFromFullList == null) {
            Log.e("Presenter", "Não foi possível encontrar a música '" + song.getTitle() + "' na lista completa para obter o ID.");
            handler.post(() -> { if (view != null) view.showToast("Erro ao encontrar ID da música"); });
            return;
        }

        final long songId = songFromFullList.id;
        final long playlistId = playlist.id;

        databaseExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(context);
                PlaylistSongCrossRef crossRef = new PlaylistSongCrossRef(playlistId, songId);
                db.playlistDao().insertPlaylistSongCrossRef(crossRef);
                Log.i("Presenter", "Música ID " + songId + " ("+ song.getTitle() +") adicionada à Playlist ID " + playlistId + " ("+ playlist.getName() +")");
                handler.post(() -> {
                    if (view != null) {
                        view.showToast("'" + song.getTitle() + "' adicionada a '" + playlist.getName() + "'");
                    }
                });
            } catch (Exception e) {
                Log.e("Presenter", "Erro ao adicionar música ID " + songId + " à playlist ID " + playlistId, e);
                handler.post(() -> {
                    if (view != null) {
                        if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                            view.showToast("Música já existe nessa playlist");
                        } else {
                            view.showToast("Erro ao adicionar música à playlist");
                        }
                    }
                });
            }
        });
    }



    @Override
    public void onSearchQueryChanged(String query) {
        List<Song> filteredList = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(fullSongList);
        } else {
            String filterPattern = query.toLowerCase(Locale.ROOT).trim();
            for (Song song : fullSongList) {
                boolean titleMatches = song.getTitle() != null && song.getTitle().toLowerCase(Locale.ROOT).contains(filterPattern);
                boolean artistMatches = song.getArtist() != null && song.getArtist().toLowerCase(Locale.ROOT).contains(filterPattern);
                if (titleMatches || artistMatches) {
                    filteredList.add(song);
                }
            }
        }
        this.songList = filteredList;
        if (view != null) {
            handler.post(() -> view.displaySongList(this.songList));
        }
    }

    private void updateSongDetails(int serviceIndex) {
        if (view != null && fullSongList != null && !fullSongList.isEmpty() && serviceIndex >= 0 && serviceIndex < fullSongList.size()) {
            Song song = fullSongList.get(serviceIndex);
            if (song != null) {
                view.showSongDetails(song);
                if (musicService != null && !musicService.isPlaying()) {
                    if (view != null) view.stopAlbumArtAnimation();
                }
                lastKnownSongIndex = serviceIndex;
            } else {
                Log.w("Presenter", "Música (lista completa) na posição " + serviceIndex + " é null.");
                if (view != null) view.showSongDetails(null);
            }
        } else {
            if (serviceIndex != -1) {
                Log.w("Presenter", "Índice inválido (" + serviceIndex + ") ou lista completa vazia ao atualizar detalhes.");
            }
            if(view != null) view.showSongDetails(null);
        }
    }

    private void initializeSeekBarRunnable() {
        updateSeekBarAction = new Runnable() {
            @Override
            public void run() {
                if (isServiceConnected && view != null && musicService != null && musicService.isPlaying()) {
                    int currentServiceIndex = musicService.getCurrentSongIndex();
                    if (currentServiceIndex != lastKnownSongIndex) {
                        updateSongDetails(currentServiceIndex);
                    }
                    int currentPosition = musicService.getCurrentPosition();
                    int duration = musicService.getDuration();
                    if (duration > 0) {
                        view.updateSeekBar(currentPosition, duration, formatTime(currentPosition), formatTime(duration));
                    } else {
                        view.updateSeekBar(currentPosition, 0, formatTime(currentPosition), "00:00");
                    }
                    if (musicService.isPlaying()) {
                        handler.postDelayed(this, 1000);
                    }
                }
            }
        };
    }

    private void updateInitialUI() {
        handler.post(()-> {
            if (!isServiceConnected || view == null || musicService == null) return;
            view.updatePlayPauseButton(musicService.isPlaying());
            int duration = musicService.getDuration();
            view.updateSeekBar(0, Math.max(0, duration), "00:00", formatTime(Math.max(0, duration)));
            view.updateShuffleButton(isShuffleOn);
            view.updateRepeatButton(isRepeatOn);
        });
    }

    @Override
    public void onPlayPauseClicked() {
        if (!isServiceConnected || musicService == null) return;
        boolean isCurrentlyPlaying = musicService.isPlaying();
        if (isCurrentlyPlaying) {
            musicService.pause();
            handler.removeCallbacks(updateSeekBarAction);
            if (view != null) view.stopAlbumArtAnimation();
        } else {
            if (fullSongList != null && !fullSongList.isEmpty()) {
                if (musicService.getCurrentPosition() > 0) {
                    musicService.play();
                } else {
                    int indexToPlay = (lastKnownSongIndex >= 0 && lastKnownSongIndex < fullSongList.size()) ? lastKnownSongIndex : 0;
                    musicService.playSongAtIndex(indexToPlay);
                }
                handler.post(updateSeekBarAction);
                if (view != null) view.startAlbumArtAnimation();
            } else {
                Log.w("Presenter", "Play clicado, mas a lista de músicas está vazia.");
            }
        }
        if (view != null) {
            view.updatePlayPauseButton(!isCurrentlyPlaying);
        }
    }


    @Override
    public void onNextClicked() {
        if (isServiceConnected && musicService != null && fullSongList != null && !fullSongList.isEmpty()) {
            musicService.playNext();
            handler.removeCallbacks(updateSeekBarAction);
            handler.post(updateSeekBarAction);
        }
    }

    @Override
    public void onPrevClicked() {
        if (isServiceConnected && musicService != null && fullSongList != null && !fullSongList.isEmpty()) {
            musicService.playPrevious();
            handler.removeCallbacks(updateSeekBarAction);
            handler.post(updateSeekBarAction);
        }
    }

    @Override
    public void onShuffleClicked() {
        isShuffleOn = !isShuffleOn;
        if (isServiceConnected && musicService != null) {
            musicService.setShuffleMode(isShuffleOn);
        }
        if (view != null) {
            view.updateShuffleButton(isShuffleOn);
        }
    }

    @Override
    public void onRepeatClicked() {
        isRepeatOn = !isRepeatOn;
        if (isServiceConnected && musicService != null) {
            musicService.setRepeatMode(isRepeatOn);
        }
        if (view != null) {
            view.updateRepeatButton(isRepeatOn);
        }
    }

    @Override
    public void onSeekBarMoved(int progress) {
        if (isServiceConnected && musicService != null) {
            musicService.seekTo(progress);
            int duration = musicService.getDuration();
            if (view != null && duration > 0) {
                view.updateSeekBar(progress, duration, formatTime(progress), formatTime(duration));
            }
            handler.removeCallbacks(updateSeekBarAction);
            handler.post(updateSeekBarAction);
        }
    }

    @Override
    public void onSongClicked(int position) {
        if (isServiceConnected && musicService != null && songList != null && position >= 0 && position < songList.size()) {
            Song clickedSong = songList.get(position);

            // --- CORREÇÃO APLICADA AQUI ---
            Song songFromFullList = findSongInFullListByIdOrDetails(clickedSong);

            if (songFromFullList != null) {
                int serviceIndex = fullSongList.indexOf(songFromFullList);
                if (serviceIndex != -1) {
                    musicService.playSongAtIndex(serviceIndex);
                    updateSongDetails(serviceIndex);
                    if (view != null) {
                        view.updatePlayPauseButton(true);
                        view.startAlbumArtAnimation();
                    }
                    handler.removeCallbacks(updateSeekBarAction);
                    handler.post(updateSeekBarAction);
                } else {
                    Log.e("Presenter", "Erro interno: Música encontrada mas índice não localizado na fullSongList.");
                }
            } else {
                Log.e("Presenter", "Erro: Música clicada (" + (clickedSong != null ? clickedSong.getTitle() : "null") + ") não encontrada na lista completa.");
            }
        }
    }


    private Song findSongInFullListByIdOrDetails(Song songToFind) {
        if (songToFind == null || fullSongList == null || fullSongList.isEmpty()) return null;

        // 1. Tenta encontrar por ID (o mais fiável)
        // (O ID pode ser 0 se a música veio da inserção inicial e não foi recarregada)
        if (songToFind.id > 0) {
            for (Song s : fullSongList) {
                if (s != null && s.id == songToFind.id) return s;
            }
        }

        // 2. Se não encontrou por ID (ou ID é 0), tenta por Título/Artista
        Log.w("Presenter", "Não encontrou música por ID ("+songToFind.id+"), tentando por Título/Artista...");
        for (Song s : fullSongList) {
            if (s != null && s.getTitle() != null && s.getTitle().equals(songToFind.getTitle()) &&
                    ((s.getArtist() == null && songToFind.getArtist() == null) ||
                            (s.getArtist() != null && s.getArtist().equals(songToFind.getArtist())))) {
                Log.i("Presenter", "Encontrou por Título/Artista. ID da BD é: " + s.id);
                return s; // Retorna a música da lista completa (que tem o ID da BD)
            }
        }

        Log.e("Presenter", "Música '" + songToFind.getTitle() + "' não encontrada na lista completa por ID nem por detalhes.");
        return null; // Não encontrado
    }


    private String formatTime(int millis) {
        if (millis < 0) millis = 0;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    public void viewDestroyed() {
        handler.removeCallbacks(updateSeekBarAction);
        this.view = null;
        databaseExecutor.shutdown();
    }
}