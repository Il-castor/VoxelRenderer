package com.example.voxelrenderer;

import android.opengl.GLES30;
import android.util.Log;

/**
 * Utility statiche per compilare e linkare programmi shader GLSL ES 3.0.
 */
public class ShaderCompiler {

    private static final String TAG = "ShaderCompiler";

    public static int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] status = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES30.glGetShaderInfoLog(shader);
            Log.e(TAG, "Errore compilazione shader: " + log);
            GLES30.glDeleteShader(shader);
            throw new RuntimeException("Errore compilazione shader: " + log);
        }
        return shader;
    }

    public static int linkProgram(int vertexShader, int fragmentShader) {
        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        int[] status = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES30.glGetProgramInfoLog(program);
            Log.e(TAG, "Errore link program: " + log);
            GLES30.glDeleteProgram(program);
            throw new RuntimeException("Errore link program: " + log);
        }
        return program;
    }

    public static int buildProgram(String vertexSrc, String fragmentSrc) {
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexSrc);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSrc);
        int program = linkProgram(vs, fs);
        // Una volta linkati nel program, i singoli shader object non servono più
        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);
        return program;
    }
}