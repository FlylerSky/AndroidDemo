package com.relifes;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends Activity {

    ListView lvSongs;
    TextView tvCurrentSong;
    Button btnPlayPause, btnPrev, btnNext;

    ArrayList<String> songNames = new ArrayList<>();
    ArrayList<String> songPaths = new ArrayList<>();

    MediaPlayer mediaPlayer;
    int currentIndex = -1;
    boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        lvSongs       = (ListView) findViewById(R.id.lv_songs);
        tvCurrentSong = (TextView) findViewById(R.id.tv_current_song);
        btnPlayPause  = (Button)   findViewById(R.id.btn_play_pause);
        btnPrev       = (Button)   findViewById(R.id.btn_prev);
        btnNext       = (Button)   findViewById(R.id.btn_next);

        // Xin quyền runtime (Android 6+)
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            loadSongs();
        }

        // Chọn bài trong danh sách
        lvSongs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                currentIndex = position;
                playSong(currentIndex);
            }
        });

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer == null) return;
                if (isPlaying) {
                    mediaPlayer.pause();
                    isPlaying = false;
                    btnPlayPause.setText("Play");
                } else {
                    mediaPlayer.start();
                    isPlaying = true;
                    btnPlayPause.setText("Pause");
                }
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (songPaths.isEmpty()) return;
                currentIndex = (currentIndex + 1) % songPaths.size();
                playSong(currentIndex);
            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (songPaths.isEmpty()) return;
                currentIndex = (currentIndex - 1 + songPaths.size())
                               % songPaths.size();
                playSong(currentIndex);
            }
        });
    }

    void loadSongs() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor cursor = getContentResolver().query(
            uri, projection, selection, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(0);
                String path  = cursor.getString(1);
                songNames.add(title);
                songPaths.add(path);
            }
            cursor.close();
        }

        if (songNames.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài hát nào!", Toast.LENGTH_SHORT).show();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_list_item_1, songNames);
        lvSongs.setAdapter(adapter);
    }

    void playSong(int index) {
        if (index < 0 || index >= songPaths.size()) return;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(songPaths.get(index));
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            btnPlayPause.setText("Pause");
            tvCurrentSong.setText(songNames.get(index));
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi phát nhạc!", Toast.LENGTH_SHORT).show();
        }

        // Tự động chuyển bài khi kết thúc
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                currentIndex = (currentIndex + 1) % songPaths.size();
                playSong(currentIndex);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongs();
            } else {
                Toast.makeText(this,
                    "Cần quyền truy cập bộ nhớ để đọc nhạc!",
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}