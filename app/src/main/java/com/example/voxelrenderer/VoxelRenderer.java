package com.example.voxelrenderer;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renderer principale: carica il modello .vly, costruisce i buffer per
 * l'instanced rendering (mesh base = 1 cubo, N istanze = N voxel) e disegna
 * la scena con un'unica drawcall (glDrawElementsInstanced).
 * <p>
 * Rotazione e zoom sono pilotati dall'Activity tramite i metodi
 * {@link #addRotation(float)} e {@link #addZoom(float)}, thread-safe rispetto
 * al thread di rendering GL.
 */
public class VoxelRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "VoxelRenderer";

    /** Callback per notificare l'FPS calcolato alla UI Android (chiamato sul thread GL). */
    public interface FpsListener {
        void onFpsUpdated(float fps);
    }

    private final Context context;
    private final String assetFileName;
    private volatile FpsListener fpsListener;

    private long fpsWindowStartNanos = 0L;
    private int fpsFrameCount = 0;
    private static final long FPS_UPDATE_INTERVAL_NANOS = 500_000_000L; // aggiorna 2 volte al secondo

    public void setFpsListener(FpsListener listener) {
        this.fpsListener = listener;
    }

    private int program;
    private int paletteTextureId;

    // VAO non disponibile via GLES30 "puro" come extension core in tutte le
    // versioni: usiamo bind espliciti di VBO/EBO ad ogni frame (comunque
    // un'unica drawcall, quindi l'overhead è trascurabile).
    private int cubeVbo;
    private int cubeEbo;
    private int instanceVbo;
    private int vaoId;

    private int instanceCount;

    // uniform locations
    private int uMVPMatrixLoc;
    private int uModelMatrixLoc;
    private int uPaletteTextureLoc;


    private int uLightPosWorldLoc;
    private int uCameraPosWorldLoc;
    private int uAmbientIntensityLoc;
    private int uSpecularIntensityLoc;
    private int uShininessLoc;

    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] tempMatrix = new float[16];

    private volatile float rotationAngleDeg = 0f;
    private volatile float zoomFactor = 1f;
    private static final float MIN_ZOOM = 0.3f;
    private static final float MAX_ZOOM = 3.0f;

    private static final float AMBIENT_INTENSITY = 0.35f;
    private static final float SPECULAR_INTENSITY = 0.5f;
    private static final float SHININESS = 32f;
    private static final float[] LIGHT_POS_WORLD = {10f, 15f, 10f};

    private float cameraDistance = 5f;
    private float[] modelCenter = new float[]{0f, 0f, 0f};

    private int surfaceWidth = 1, surfaceHeight = 1;

    private final AtomicBoolean modelReady = new AtomicBoolean(false);

    public VoxelRenderer(Context context, String assetFileName) {
        this.context = context;
        this.assetFileName = assetFileName;
    }

    // ---- Input pubblico dall'Activity (touch / pinch) ----

    public void addRotation(float deltaDeg) {
        rotationAngleDeg += deltaDeg;
    }

    public void addZoom(float factor) {
        zoomFactor *= factor;
        if (zoomFactor < MIN_ZOOM) zoomFactor = MIN_ZOOM;
        if (zoomFactor > MAX_ZOOM) zoomFactor = MAX_ZOOM;
    }

    private void setupVertexArrayObject() {
        int[] vaos = new int[1];
        GLES30.glGenVertexArrays(1, vaos, 0);
        vaoId = vaos[0];

        GLES30.glBindVertexArray(vaoId);

        // Attributi mesh cubo (per-vertice)
        final int cubeStride = CubeGeometry.FLOATS_PER_VERTEX * 4;
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, cubeVbo);
        GLES30.glEnableVertexAttribArray(0); // aLocalPosition
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, cubeStride, 0);
        GLES30.glEnableVertexAttribArray(1); // aNormal
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, cubeStride, 3 * 4);

        // Attributi per-istanza
        final int instanceStride = 5 * 4;
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVbo);
        GLES30.glEnableVertexAttribArray(2); // aInstanceOffset
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, instanceStride, 0);
        GLES30.glVertexAttribDivisor(2, 1);
        GLES30.glEnableVertexAttribArray(3); // aInstanceUV
        GLES30.glVertexAttribPointer(3, 2, GLES30.GL_FLOAT, false, instanceStride, 3 * 4);
        GLES30.glVertexAttribDivisor(3, 1);

        // L'EBO va bindato mentre il VAO è attivo: il VAO lo ricorda
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, cubeEbo);

        // Sblocca il VAO (l'EBO resta associato ad esso, i VBO no)
        GLES30.glBindVertexArray(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    // ---- GLSurfaceView.Renderer ----

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(0.08f, 0.09f, 0.11f, 1f);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LESS);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);

        program = ShaderCompiler.buildProgram(VoxelShaders.VERTEX_SHADER, VoxelShaders.FRAGMENT_SHADER);
        uMVPMatrixLoc = GLES30.glGetUniformLocation(program, "uMVPMatrix");
        uModelMatrixLoc = GLES30.glGetUniformLocation(program, "uModelMatrix");
        uPaletteTextureLoc = GLES30.glGetUniformLocation(program, "uPaletteTexture");

        uPaletteTextureLoc = GLES30.glGetUniformLocation(program, "uPaletteTexture");
        uLightPosWorldLoc = GLES30.glGetUniformLocation(program, "uLightPosWorld");
        uCameraPosWorldLoc = GLES30.glGetUniformLocation(program, "uCameraPosWorld");
        uAmbientIntensityLoc = GLES30.glGetUniformLocation(program, "uAmbientIntensity");
        uSpecularIntensityLoc = GLES30.glGetUniformLocation(program, "uSpecularIntensity");
        uShininessLoc = GLES30.glGetUniformLocation(program, "uShininess");

        setupCubeBuffers();
        loadModelAndBuildInstances();
        setupVertexArrayObject();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        GLES30.glViewport(0, 0, width, height);

        float aspect = (float) width / (float) Math.max(1, height);
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.1f, 1000f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        updateFpsCounter();

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        if (!modelReady.get()) return;

        GLES30.glUseProgram(program);

        // Camera: guarda il centro del modello da una distanza fissata in
        // base al bounding box, modulata dallo zoom utente.
        float distance = cameraDistance / zoomFactor;
        float eyeX = modelCenter[0];
        float eyeY = modelCenter[1];
        float eyeZ = modelCenter[2] + distance * 0.4f + distance;

        Matrix.setLookAtM(viewMatrix, 0,
               eyeX, eyeY, eyeZ, modelCenter[0], modelCenter[1], modelCenter[2],
                0f, 1f, 0f); // up

        // Nota: la Z è up-axis nel formato vly; costruiamo comunque il modello
        // già "raddrizzato" in setupInstanceBuffer (swap Y/Z), quindi qui la
        // camera usa la convenzione standard Y-up di OpenGL.

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, rotationAngleDeg, 0f, 1f, 0f);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        GLES30.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0);
        GLES30.glUniformMatrix4fv(uModelMatrixLoc, 1, false, modelMatrix, 0);

        GLES30.glUniform3f(uLightPosWorldLoc, LIGHT_POS_WORLD[0], LIGHT_POS_WORLD[1], LIGHT_POS_WORLD[2]);
        GLES30.glUniform3f(uCameraPosWorldLoc, eyeX, eyeY, eyeZ);
        GLES30.glUniform1f(uAmbientIntensityLoc, AMBIENT_INTENSITY);
        GLES30.glUniform1f(uSpecularIntensityLoc, SPECULAR_INTENSITY);
        GLES30.glUniform1f(uShininessLoc, SHININESS);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, paletteTextureId);
        GLES30.glUniform1i(uPaletteTextureLoc, 0);

       GLES30.glBindVertexArray(vaoId);
       GLES30.glDrawElementsInstanced(
               GLES30.GL_TRIANGLES,
               CubeGeometry.INDEX_DATA.length,
               GLES30.GL_UNSIGNED_SHORT,
               0,
               instanceCount);
       GLES30.glBindVertexArray(0);
    }

    /**
     * Conta i frame e, ogni FPS_UPDATE_INTERVAL_NANOS, calcola l'FPS medio
     * nella finestra e lo notifica al listener (se presente). Eseguito sul
     * thread di rendering GL: il listener deve marshallare verso il thread
     * UI se necessario.
     */
    private void updateFpsCounter() {
        long now = System.nanoTime();
        if (fpsWindowStartNanos == 0L) {
            fpsWindowStartNanos = now;
            fpsFrameCount = 0;
            return;
        }

        fpsFrameCount++;
        long elapsed = now - fpsWindowStartNanos;

        if (elapsed >= FPS_UPDATE_INTERVAL_NANOS) {
            float fps = fpsFrameCount / (elapsed / 1_000_000_000f);
            FpsListener listener = fpsListener;
            if (listener != null) {
                listener.onFpsUpdated(fps);
            }
            fpsWindowStartNanos = now;
            fpsFrameCount = 0;
        }
    }

    // ---- Setup buffer statici (mesh del cubo unitario) ----

    private void setupCubeBuffers() {
        FloatBuffer vertexBuffer = ByteBuffer
                .allocateDirect(CubeGeometry.VERTEX_DATA.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(CubeGeometry.VERTEX_DATA).position(0);

        ShortBuffer indexBuffer = ByteBuffer
                .allocateDirect(CubeGeometry.INDEX_DATA.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        indexBuffer.put(CubeGeometry.INDEX_DATA).position(0);

        int[] buffers = new int[2];
        GLES30.glGenBuffers(2, buffers, 0);
        cubeVbo = buffers[0];
        cubeEbo = buffers[1];

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, cubeVbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                CubeGeometry.VERTEX_DATA.length * 4, vertexBuffer, GLES30.GL_STATIC_DRAW);

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, cubeEbo);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER,
                CubeGeometry.INDEX_DATA.length * 2, indexBuffer, GLES30.GL_STATIC_DRAW);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    // ---- Caricamento modello .vly + buffer per-istanza + texture palette ----

    private void loadModelAndBuildInstances() {
        try {
            VlyModel model = VlyModel.loadFromAssets(context, assetFileName);
            buildInstanceBuffer(model);
            uploadPaletteTexture(model);
            computeCameraFraming(model);
            modelReady.set(true);
        } catch (IOException e) {
            Log.e(TAG, "Errore caricamento file .vly: " + assetFileName, e);
        }
    }

    /**
     * Costruisce il VBO per-istanza: per ogni voxel, (offsetX, offsetY, offsetZ, u, v).
     * Convertiamo le coordinate griglia (X,Y,Z con Z up) in spazio locale
     * centrato sull'origine, con Y come up-axis (convenzione OpenGL standard):
     * localX = gridX - centerX
     * localY = gridZ - centerZ   (Z del formato diventa Y "su" nello spazio GL)
     * localZ = gridY - centerY
     */
    private void buildInstanceBuffer(VlyModel model) {
        instanceCount = model.voxels.size();

        final int FLOATS_PER_INSTANCE = 5; // offsetX, offsetY, offsetZ, u, v
        float[] data = new float[instanceCount * FLOATS_PER_INSTANCE];

        float centerX = model.gridX / 2f;
        float centerY = model.gridY / 2f;
        float centerZ = model.gridZ / 2f;

        for (int i = 0; i < instanceCount; i++) {
            VlyModel.Voxel v = model.voxels.get(i);

            float localX = v.x - centerX;
            float localY = v.z - centerZ; // Z (up nel formato) -> Y (up in GL)
            float localZ = v.y - centerY;

            float[] uv = model.getUVForColorIndex(v.colorIndex);

            int base = i * FLOATS_PER_INSTANCE;
            data[base]     = localX;
            data[base + 1] = localY;
            data[base + 2] = localZ;
            data[base + 3] = uv[0];
            data[base + 4] = uv[1];
        }

        FloatBuffer instanceBuffer = ByteBuffer
                .allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        instanceBuffer.put(data).position(0);

        int[] buffers = new int[1];
        GLES30.glGenBuffers(1, buffers, 0);
        instanceVbo = buffers[0];

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, data.length * 4, instanceBuffer, GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    private void uploadPaletteTexture(VlyModel model) {
        int[] tex = new int[1];
        GLES30.glGenTextures(1, tex, 0);
        paletteTextureId = tex[0];

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, paletteTextureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, model.paletteBitmap, 0);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    /**
     * Calcola una distanza di camera che garantisca la visione completa
     * dell'intero bounding box del modello, indipendentemente dal numero di
     * voxel, usando la diagonale della grid e il campo visivo verticale.
     */
    private void computeCameraFraming(VlyModel model) {
        modelCenter[0] = 0f;
        modelCenter[1] = 0f;
        modelCenter[2] = 0f;

        float dx = model.gridX;
        float dy = model.gridZ; // ricorda: Z (up nel formato) -> Y in GL
        float dz = model.gridY;

        float radius = 0.5f * (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float fovRad = (float) Math.toRadians(45f);

        // distanza minima per contenere una sfera di raggio "radius" nel FOV verticale
        cameraDistance = radius / (float) Math.sin(fovRad / 2.0) + radius * 0.3f;
        if (cameraDistance < 2f) cameraDistance = 2f;
    }
}