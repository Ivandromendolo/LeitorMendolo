package com.intro.leitormendolo.view;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.acrcloud.rec.ACRCloudClient;
import com.acrcloud.rec.ACRCloudConfig;
import com.acrcloud.rec.ACRCloudResult;
import com.acrcloud.rec.IACRCloudListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.intro.leitormendolo.AppDatabase; // Importação adicionada para o diálogo
import com.intro.leitormendolo.MusicPlayerContract;
import com.intro.leitormendolo.R;
import com.intro.leitormendolo.adapter.PlaylistAdapter;
import com.intro.leitormendolo.model.Playlist;
import com.intro.leitormendolo.adapter.SongAdapter;
import com.intro.leitormendolo.model.Song;
import com.intro.leitormendolo.presenter.MusicPlayerPresenter;
import com.intro.leitormendolo.service.MusicService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.ArrayList; // Importação adicionada

public class MainActivity extends AppCompatActivity implements MusicPlayerContract.View,
        SongAdapter.OnSongClickListener, // Interface já implementada
        IACRCloudListener,
        PlaylistAdapter.OnPlaylistClickListener {

    private MusicPlayerContract.Presenter presenter;
    private TextView textViewSongTitle, textViewArtistName, textViewCurrentTime, textViewTotalTime;
    private SeekBar seekBar;
    private ImageButton buttonPlayPause, buttonNext, buttonPrev, buttonShuffle, buttonRepeat;
    private ImageView imageViewAlbumArt;
    private ObjectAnimator albumArtAnimator;
    private MusicService musicService;
    private boolean isBound = false;
    private Intent playIntent;
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private SearchView searchView;
    private RecyclerView recyclerViewPlaylists;
    private PlaylistAdapter playlistAdapter;
    private FloatingActionButton fabAddPlaylist;

    private ACRCloudClient mClient;
    private boolean mProcessing = false;
    private final int RECORD_AUDIO_PERMISSION_CODE = 123;
    private final int NOTIFICATION_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        presenter = new MusicPlayerPresenter(this, getApplicationContext());
        initViews();
        setupClickListeners();
        setupAlbumArtAnimator();
        setupRecyclerView();
        setupSearchListener();
        setupPlaylistRecyclerView();
        initAcrCloud();
        requestNotificationPermission();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void initViews() {
        imageViewAlbumArt = findViewById(R.id.imageViewAlbumArt);
        textViewSongTitle = findViewById(R.id.textViewSongTitle);
        textViewArtistName = findViewById(R.id.textViewArtistName);
        buttonPlayPause = findViewById(R.id.buttonPlayPause);
        buttonNext = findViewById(R.id.buttonNext);
        buttonPrev = findViewById(R.id.buttonPrevious);
        seekBar = findViewById(R.id.seekBar);
        textViewCurrentTime = findViewById(R.id.textViewCurrentTime);
        textViewTotalTime = findViewById(R.id.textViewTotalTime);
        buttonShuffle = findViewById(R.id.buttonShuffle);
        buttonRepeat = findViewById(R.id.buttonRepeat);
        recyclerView = findViewById(R.id.recyclerViewSongs);
        searchView = findViewById(R.id.searchView);
        recyclerViewPlaylists = findViewById(R.id.recyclerViewPlaylists);
        fabAddPlaylist = findViewById(R.id.fabAddPlaylist);
    }

    private void initAcrCloud() {
        ACRCloudConfig config = new ACRCloudConfig();
        config.acrcloudListener = this;
        config.context = this;
        config.host = "identify-eu-west-1.acrcloud.com";
        config.accessKey = "8e722f20207eb2d729f48ad4f3791bfc";
        config.accessSecret = "HgiAKtLirnS4zkLnBJs7VE2Qo9TCGOyIhqDO3cso";
        config.recorderConfig.rate = 8000;
        config.recorderConfig.channels = 1;
        mClient = new ACRCloudClient();
        if (!mClient.initWithConfig(config)) {
            Toast.makeText(this, "Falha ao inicializar ACRCloud", Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "ACRCloud initialization failed");
        } else {
            Log.d("MainActivity", "ACRCloud initialized successfully");
        }
    }

    private void setupClickListeners() {
        buttonPlayPause.setOnClickListener(v -> presenter.onPlayPauseClicked());
        buttonNext.setOnClickListener(v -> presenter.onNextClicked());
        buttonPrev.setOnClickListener(v -> presenter.onPrevClicked());
        buttonShuffle.setOnClickListener(v -> presenter.onShuffleClicked());
        buttonRepeat.setOnClickListener(v -> presenter.onRepeatClicked());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { presenter.onSeekBarMoved(seekBar.getProgress()); }
        });
        if (fabAddPlaylist != null) {
            fabAddPlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
        }
    }

    private void setupSearchListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) { presenter.onSearchQueryChanged(newText); return true; }
        });
    }

    private void setupRecyclerView() {
        songAdapter = new SongAdapter(this); // Passa 'this' como listener
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(songAdapter);
    }

    private void setupPlaylistRecyclerView() {
        playlistAdapter = new PlaylistAdapter(this);
        recyclerViewPlaylists.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPlaylists.setAdapter(playlistAdapter);
    }

    private void setupAlbumArtAnimator() {
        albumArtAnimator = ObjectAnimator.ofFloat(imageViewAlbumArt, "rotation", 0f, 360f);
        albumArtAnimator.setDuration(20000);
        albumArtAnimator.setRepeatCount(ValueAnimator.INFINITE);
        albumArtAnimator.setInterpolator(new LinearInterpolator());
    }

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nova Playlist");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nome da Playlist");
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int marginInDp = 20;
        int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density);
        lp.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
        input.setLayoutParams(lp);
        container.addView(input);
        builder.setView(container);
        builder.setPositiveButton("Criar", (dialog, which) -> {
            String playlistName = input.getText().toString();
            if (!playlistName.trim().isEmpty()) {
                presenter.createPlaylist(playlistName);
            } else {
                Toast.makeText(MainActivity.this, "O nome não pode ser vazio", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeletePlaylistConfirmationDialog(Playlist playlist) {
        if (playlist == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Apagar Playlist")
                .setMessage("Tem a certeza que quer apagar a playlist '" + playlist.getName() + "'?\nEsta ação não pode ser desfeita.")
                .setPositiveButton("Apagar", (dialog, which) -> {
                    presenter.deletePlaylist(playlist);
                })
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    // Mostra um diálogo com a lista de playlists para adicionar a música
    private void showAddToPlaylistDialog(Song song) {
        if (presenter == null || song == null) return; // Segurança

        // --- ATENÇÃO: Acesso à BD na Main Thread (simplificação) ---
        // Idealmente, pediríamos a lista ao Presenter (que a tem em memória)
        // ou o Presenter carregaria em background e retornaria via callback/LiveData.
        List<Playlist> currentPlaylists = new ArrayList<>();
        try {
            currentPlaylists = AppDatabase.getDatabase(this).playlistDao().getAllPlaylists();
        } catch (Exception e) {
            Log.e("MainActivity", "Erro ao buscar playlists para diálogo", e);
            Toast.makeText(this, "Erro ao carregar playlists", Toast.LENGTH_SHORT).show();
            return; // Sai se não conseguir buscar
        }
        // --- FIM DA ATENÇÃO ---


        if (currentPlaylists.isEmpty()) {
            Toast.makeText(this, "Nenhuma playlist criada ainda.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Converte a lista de Playlists para um array de Nomes
        final List<Playlist> playlistsFinal = currentPlaylists; // Cópia final para lambda
        CharSequence[] playlistNames = new CharSequence[playlistsFinal.size()];
        for (int i = 0; i < playlistsFinal.size(); i++) {
            playlistNames[i] = playlistsFinal.get(i).getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Adicionar '" + song.getTitle() + "' a:");
        builder.setItems(playlistNames, (dialog, which) -> {
            // 'which' é o índice da playlist selecionada
            Playlist selectedPlaylist = playlistsFinal.get(which);
            presenter.addSongToPlaylist(song, selectedPlaylist); // Chama o Presenter
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }



    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            presenter.onServiceConnected(musicService);
            isBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        presenter.viewDestroyed();
        if (mClient != null) {
            mClient.release();
            mClient = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_recognize) {
            startRecognition();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startRecognition() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_CODE);
        } else {
            performRecognitionStart();
        }
    }

    private void performRecognitionStart() {
        if (!mProcessing) {
            mProcessing = true;
            Toast.makeText(this, "A ouvir...", Toast.LENGTH_SHORT).show();
            if (mClient == null || !mClient.startRecognize()) {
                mProcessing = false;
                Toast.makeText(this, "Erro ao iniciar o reconhecimento.", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "ACRCloud startRecognize failed or mClient is null");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performRecognitionStart();
            } else {
                Toast.makeText(this, "Permissão de microfone negada.", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Permissão de notificação negada.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResult(ACRCloudResult results) {
        if (mClient != null) {
            mClient.cancel();
        }
        mProcessing = false;
        String result = results.getResult();
        runOnUiThread(() -> parseJsonResult(result));
    }

    private void parseJsonResult(String jsonString) {
        if (jsonString == null) {
            showResultDialog("Erro", "Não foi recebido nenhum resultado.");
            return;
        }
        try {
            JSONObject json = new JSONObject(jsonString);
            JSONObject status = json.getJSONObject("status");
            if (status.getInt("code") == 0) {
                JSONObject metadata = json.getJSONObject("metadata");
                if (metadata.has("music")) {
                    JSONArray music = metadata.getJSONArray("music");
                    if (music.length() > 0) {
                        JSONObject song = music.getJSONObject(0);
                        String title = song.optString("title");
                        String artist = "Desconhecido";
                        if (song.has("artists")) {
                            JSONArray artists = song.getJSONArray("artists");
                            if (artists.length() > 0) {
                                artist = artists.getJSONObject(0).optString("name");
                            }
                        }
                        showResultDialog("Música Reconhecida", "Título: " + title + "\nArtista: " + artist);
                    } else {
                        showResultDialog("Resultado", "Nenhuma música encontrada.");
                    }
                } else {
                    showResultDialog("Resultado", "Nenhuma música encontrada.");
                }
            } else {
                String msg = status.getString("msg");
                showResultDialog("Erro", "Não foi possível reconhecer: " + msg);
            }
        } catch (JSONException e) {
            Log.e("MainActivity", "Erro ao processar JSON do ACRCloud", e);
            showResultDialog("Erro", "Erro ao processar o resultado.");
        }
    }

    private void showResultDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onVolumeChanged(double volume) { /* Não usado */ }

    @Override
    public void showSongDetails(Song song) {
        if (song != null) {
            textViewSongTitle.setText(song.getTitle());
            textViewArtistName.setText(song.getArtist());
        } else {
            textViewSongTitle.setText("A carregar..."); // Ou texto padrão
            textViewArtistName.setText("");
        }
    }

    @Override
    public void updatePlayPauseButton(boolean isPlaying) {
        buttonPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    @Override
    public void updateSeekBar(int progress, int max, String currentTime, String totalTime) {
        seekBar.setMax(Math.max(max, 0));
        // Evita atualizar para 0 se o progresso real for maior (ex: ao voltar para app)
        if (progress > seekBar.getProgress() || max == 0) {
            seekBar.setProgress(progress);
        }
        textViewCurrentTime.setText(currentTime);
        textViewTotalTime.setText(totalTime);
    }

    @Override
    public void updateShuffleButton(boolean isActive) {
        buttonShuffle.setAlpha(isActive ? 1.0f : 0.5f);
    }

    @Override
    public void updateRepeatButton(boolean isActive) {
        buttonRepeat.setAlpha(isActive ? 1.0f : 0.5f);
    }

    @Override
    public void startAlbumArtAnimation() {
        if (!albumArtAnimator.isStarted()) {
            albumArtAnimator.start();
        } else if (albumArtAnimator.isPaused()) {
            albumArtAnimator.resume();
        }
    }

    @Override
    public void stopAlbumArtAnimation() {
        if (albumArtAnimator.isRunning()) {
            albumArtAnimator.pause();
        }
    }

    @Override
    public void displaySongList(List<Song> songList) {
        if (songAdapter != null) {
            songAdapter.setSongs(songList);
        }
    }


    @Override
    public void onSongLongClick(Song song) {
        showAddToPlaylistDialog(song); // Mostra o diálogo para adicionar à playlist
    }


    @Override
    public void onSongClick(int position) {
        presenter.onSongClicked(position);
    }

    @Override
    public void displayPlaylists(List<Playlist> playlists) {
        Log.d("MainActivity", "displayPlaylists chamado com " + (playlists == null ? 0 : playlists.size()) + " playlists.");
        if (playlistAdapter != null) {
            playlistAdapter.setPlaylists(playlists);
        }
    }

    @Override
    public void onPlaylistClick(Playlist playlist) {
        Toast.makeText(this, "Clicou na playlist: " + playlist.getName(), Toast.LENGTH_SHORT).show();
        // Futuro: Abrir tela de detalhes
    }

    @Override
    public void onPlaylistLongClick(Playlist playlist) {
        showDeletePlaylistConfirmationDialog(playlist);
    }

}