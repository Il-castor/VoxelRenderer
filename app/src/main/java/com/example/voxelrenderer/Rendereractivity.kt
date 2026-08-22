package com.example.voxelrenderer

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * Ospita il renderer OpenGL a schermo intero (VoxelSurfaceView, riusata da
 * Java) con sovrapposto, in alto a destra, un TextView che mostra gli FPS
 * correnti aggiornati dal renderer via callback.
 */
class RendererActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VLY_FILE_NAME = "extra_vly_file_name"
    }

    private lateinit var glView: VoxelSurfaceView
    private lateinit var fpsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fileName = intent.getStringExtra(EXTRA_VLY_FILE_NAME) ?: "simple.vly"

        glView = VoxelSurfaceView(this, fileName)

        fpsTextView = TextView(this).apply {
            setTextColor(Color.GREEN)
            textSize = 16f
            typeface = Typeface.MONOSPACE
            setPadding(24, 16, 24, 16)
            text = "FPS: --"
        }

        val root = FrameLayout(this)
        root.addView(glView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        root.addView(fpsTextView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.END
        ))

        setContentView(root)

        // Il renderer chiama onFpsUpdated sul thread GL: bisogna tornare
        // sul thread UI per aggiornare la TextView.
        glView.setFpsListener { fps ->
            runOnUiThread {
                fpsTextView.text = String.format(Locale.US, "FPS: %.1f\nVoxel: %d / %d", fps,
                    glView.renderer.instanceCount, glView.renderer.totalVoxelCount);
            }
        }
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()
    }
}