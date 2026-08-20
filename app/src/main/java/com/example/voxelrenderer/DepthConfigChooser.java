package com.example.voxelrenderer;

import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * Sceglie, tra le EGLConfig disponibili, quella con GLES 3 abilitato e il
 * depth buffer più vicino (per eccesso) a quello desiderato, con minimo
 * garantito. Necessario perché con molti voxel sovrapposti un depth buffer
 * a 16 bit produce z-fighting / risultati sbagliati (vedi addendum traccia).
 */
public class DepthConfigChooser implements GLSurfaceView.EGLConfigChooser {

    private static final String TAG = "DepthConfigChooser";
    private static final int EGL_OPENGL_ES3_BIT = 0x00000040;

    private final int desiredDepthSize;

    public DepthConfigChooser(int desiredDepthSize) {
        this.desiredDepthSize = desiredDepthSize;
    }

    @Override
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        int[] numConfigs = new int[1];
        int[] attribs = new int[]{
                EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_NONE
        };

        egl.eglChooseConfig(display, attribs, null, 0, numConfigs);
        int count = numConfigs[0];
        if (count <= 0) {
            throw new RuntimeException("Nessuna EGLConfig ES3 disponibile su questo dispositivo");
        }

        EGLConfig[] configs = new EGLConfig[count];
        egl.eglChooseConfig(display, attribs, configs, count, numConfigs);

        EGLConfig best = null;
        int bestDepth = -1;

        for (EGLConfig cfg : configs) {
            int depth = getAttrib(egl, display, cfg, EGL10.EGL_DEPTH_SIZE);
            Log.v(TAG, "EGLConfig depth size disponibile: " + depth);

            if (depth >= desiredDepthSize) {
                // preferiamo la config valida col depth più basso >= desiderato
                // (evita di sprecare banda con depth eccessivi tipo 32 se non serve)
                if (best == null || depth < bestDepth) {
                    best = cfg;
                    bestDepth = depth;
                }
            }
        }

        if (best == null) {
            // fallback: prendi la config col depth massimo disponibile, anche se < desiderato
            for (EGLConfig cfg : configs) {
                int depth = getAttrib(egl, display, cfg, EGL10.EGL_DEPTH_SIZE);
                if (depth > bestDepth) {
                    bestDepth = depth;
                    best = cfg;
                }
            }
            Log.w(TAG, "Depth desiderato (" + desiredDepthSize + ") non disponibile, uso " + bestDepth + " bit");
        } else {
            Log.i(TAG, "Depth buffer selezionato: " + bestDepth + " bit");
        }

        return best;
    }

    private int getAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute) {
        int[] value = new int[1];
        if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
            return value[0];
        }
        return 0;
    }
}