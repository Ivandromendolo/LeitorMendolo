package com.intro.leitormendolo.view;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import com.acrcloud.rec.sdk.ACRCloudClient;
import com.acrcloud.rec.sdk.ACRCloudConfig;
import com.acrcloud.rec.sdk.ACRCloudResult;
import com.acrcloud.rec.sdk.IACRCloudListener;
import com.intro.leitormendolo.MusicPlayerContract;
import com.intro.leitormendolo.R;
import com.intro.leitormendolo.adapter.SongAdapter;
import com.intro.leitormendolo.model.Song;
import com.intro.leitormendolo.presenter.MusicPlayerPresenter;
import com.intro.leitormendolo.service.MusicService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends AppCompatActivity implements MusicPlayerContract.View, SongAdapter.OnSongClickListener, IACRCloudListener {

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

    private ACRCloudClient mClient;
    private boolean mProcessing = false;
    private final int RECORD_AUDIO_PERMISSION_CODE = 123;

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
        initAcrCloud();
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
    }

    private void initAcrCloud() {
        ACRCloudConfig mConfig = new ACRCloudConfig();

        mConfig.host = "identify-eu-west-1.acrcloud.com";
        mConfig.accessKey = "8e722f20207eb2d729f48ad4f3791bfc";
        mConfig.accessSecret = "HgiAKtLirnS4zkLnBJs7VE2Qo9TCGOyIhqDO3cso";

        mConfig.context = this;
        mConfig.recorderConfig.rate = 8000;
        mConfig.recorderConfig.channels = 1;

        // CORREÇÃO: Esta linha TEM de estar ativa para recebermos o resultado!
        mConfig.listener = this;

        mClient = new ACRCloudClient();
        mClient.initWithConfig(mConfig);
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
    }

    private void setupSearchListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) { presenter.onSearchQueryChanged(newText); return true; }
        });
    }

    private void setupRecyclerView() {
        songAdapter = new SongAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(songAdapter);
    }

    private void setupAlbumArtAnimator() {
        albumArtAnimator = ObjectAnimator.ofFloat(imageViewAlbumArt, "rotation", 0f, 360f);
        albumArtAnimator.setDuration(20000);
        albumArtAnimator.setRepeatCount(ValueAnimator.INFINITE);
        albumArtAnimator.setInterpolator(new LinearInterpolator());
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
        if (item.getItemId() == R.id.action_recognize) {
            startRecognition();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startRecognition() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_CODE);
        } else {
            if (!mProcessing) {
                mProcessing = true;
                Toast.makeText(this, "A ouvir...", Toast.LENGTH_SHORT).show();
                if (mClient != null && !mClient.startRecognize()) {
                    mProcessing = false;
                    Toast.makeText(this, "Erro ao iniciar o reconhecimento.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecognition();
            } else {
                Toast.makeText(this, "Permissão de microfone negada.", Toast.LENGTH_SHORT).show();
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
                    JSONObject song = music.getJSONObject(0);
                    String title = song.getString("title");
                    JSONArray artists = song.getJSONArray("artists");
                    String artist = artists.getJSONObject(0).getString("name");
                    showResultDialog("Música Reconhecida", "Título: " + title + "\nArtista: " + artist);
                } else {
                    showResultDialog("Resultado", "Nenhuma música encontrada no resultado.");
                }
            } else {
                String msg = status.getString("msg");
                showResultDialog("Erro", "Não foi possível reconhecer: " + msg);
            }
        } catch (JSONException e) {
            e.printStackTrace();
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
    public void onVolumeChanged(double volume) { /* Não usado */ }

    @Override
    public void showSongDetails(Song song) {
        textViewSongTitle.setText(song.getTitle());
        textViewArtistName.setText(song.getArtist());
    }

    @Override
    public void updatePlayPauseButton(boolean isPlaying) {
        buttonPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    @Override
    public void updateSeekBar(int progress, int max, String currentTime, String totalTime) {
        seekBar.setMax(max);
        seekBar.setProgress(progress);
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
        songAdapter.setSongs(songList);
    }

    @Override
    public void onSongClick(int position) {
        presenter.onSongClicked(position);
    }
}