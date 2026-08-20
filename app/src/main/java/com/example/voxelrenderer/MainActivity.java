package com.example.voxelrenderer;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // Cambia qui il file .vly da visualizzare (deve stare in app/src/main/assets/)
    private static final String VLY_ASSET = "christmas.vly";

    private VoxelSurfaceView glView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        glView = new VoxelSurfaceView(this, VLY_ASSET);
        setContentView(glView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glView.onPause();
    }
}