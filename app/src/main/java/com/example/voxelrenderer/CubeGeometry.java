package com.example.voxelrenderer;

/**
 * Geometria di un cubo unitario centrato nell'origine, lato 1.0.
 * 24 vertici (4 per faccia, non condivisi) per avere normali flat corrette
 * per faccia invece di normali mediate agli spigoli.
 */
public class CubeGeometry {

    // 6 facce * 4 vertici * (3 pos + 3 normal) = 144 float
    public static final float[] VERTEX_DATA = {
            // posX, posY, posZ,   nX, nY, nZ

            // +X face
            0.5f, -0.5f, -0.5f,   1, 0, 0,
            0.5f,  0.5f, -0.5f,   1, 0, 0,
            0.5f,  0.5f,  0.5f,   1, 0, 0,
            0.5f, -0.5f,  0.5f,   1, 0, 0,

            // -X face
            -0.5f, -0.5f,  0.5f,  -1, 0, 0,
            -0.5f,  0.5f,  0.5f,  -1, 0, 0,
            -0.5f,  0.5f, -0.5f,  -1, 0, 0,
            -0.5f, -0.5f, -0.5f,  -1, 0, 0,

            // +Y face (top, dato che Z è up nel formato vly la useremo comunque
            // come "alto" locale del cubo unitario: la trasformazione assi
            // avviene a livello di dati, non di mesh)
            -0.5f, 0.5f, -0.5f,   0, 1, 0,
            -0.5f, 0.5f,  0.5f,   0, 1, 0,
            0.5f, 0.5f,  0.5f,   0, 1, 0,
            0.5f, 0.5f, -0.5f,   0, 1, 0,

            // -Y face
            -0.5f, -0.5f,  0.5f,  0, -1, 0,
            -0.5f, -0.5f, -0.5f,  0, -1, 0,
            0.5f, -0.5f, -0.5f,  0, -1, 0,
            0.5f, -0.5f,  0.5f,  0, -1, 0,

            // +Z face
            -0.5f, -0.5f, 0.5f,   0, 0, 1,
            0.5f, -0.5f, 0.5f,   0, 0, 1,
            0.5f,  0.5f, 0.5f,   0, 0, 1,
            -0.5f,  0.5f, 0.5f,   0, 0, 1,

            // -Z face
            0.5f, -0.5f, -0.5f,  0, 0, -1,
            -0.5f, -0.5f, -0.5f,  0, 0, -1,
            -0.5f,  0.5f, -0.5f,  0, 0, -1,
            0.5f,  0.5f, -0.5f,  0, 0, -1,
    };

    // 6 facce * 2 triangoli * 3 indici = 36 indici
    public static final short[] INDEX_DATA = {
            0, 1, 2,   0, 2, 3,       // +X
            4, 5, 6,   4, 6, 7,       // -X
            8, 9, 10,  8, 10, 11,     // +Y
            12, 13, 14, 12, 14, 15,   // -Y
            16, 17, 18, 16, 18, 19,   // +Z
            20, 21, 22, 20, 22, 23,   // -Z
    };

    public static final int FLOATS_PER_VERTEX = 6; // 3 pos + 3 normal
}