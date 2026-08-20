package com.example.voxelrenderer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * GLSurfaceView che interpreta i touch:
 * - drag orizzontale sulla metà destra/sinistra dello schermo -> ruota la mesh
 *   attorno all'asse verticale, con verso dettato dal lato toccato;
 * - pinch (due dita) -> zoom, tramite ScaleGestureDetector.
 */
public class VoxelSurfaceView extends GLSurfaceView {

    private final VoxelRenderer renderer;
    private final ScaleGestureDetector scaleDetector;

    private float lastTouchX = -1f;
    private static final float ROTATION_SENSITIVITY = 0.4f; // gradi per pixel di drag

    public VoxelSurfaceView(Context context, String assetFileName) {
        super(context);

        setEGLContextClientVersion(3);
        setEGLConfigChooser(new DepthConfigChooser(24));
        setPreserveEGLContextOnPause(true);

        renderer = new VoxelRenderer(context, assetFileName);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                renderer.addZoom(detector.getScaleFactor());
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                break;

            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress() && event.getPointerCount() == 1) {
                    float x = event.getX();
                    if (lastTouchX >= 0f) {
                        float dx = x - lastTouchX;
                        // Il verso di rotazione è dato dalla direzione del drag,
                        // che dipende naturalmente da quale lato dello schermo
                        // l'utente sta trascinando (destra/sinistra), come
                        // richiesto dalla traccia.
                        renderer.addRotation(dx * ROTATION_SENSITIVITY);
                    }
                    lastTouchX = x;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                lastTouchX = -1f;
                break;
        }

        return true;
    }
}