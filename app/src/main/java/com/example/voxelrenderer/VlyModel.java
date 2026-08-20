package com.example.voxelrenderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Parser per il formato .vly (Voxel-PLY-like).
 * <p>
 * Formato:
 * grid_size: gx gy gz
 * voxel_num: n
 * (n righe)  x y z colorIndex
 * (righe residue) colorIndex r g b
 * <p>
 * Costruisce:
 * - un array di voxel (posizione + indice colore)
 * - una Bitmap "palette" quadrata NxN dove ogni pixel rappresenta un colore,
 *   usata poi come texture 2D. Per ogni voxel calcoliamo le UV del suo colore
 *   nella palette (centro del texel, per evitare bleeding col filtro nearest/lineare).
 */
public class VlyModel {

    /** Un singolo voxel: coordinate intere nella occupancy grid + indice colore. */
    public static class Voxel {
        public final int x, y, z;
        public final int colorIndex;

        Voxel(int x, int y, int z, int colorIndex) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.colorIndex = colorIndex;
        }
    }

    public int gridX, gridY, gridZ;
    public int voxelNum;
    public List<Voxel> voxels;

    /** Palette come bitmap quadrata (side x side). */
    public Bitmap paletteBitmap;
    public int paletteSide;

    /** UV (centro texel) per ogni colorIndex, calcolate dopo aver costruito la palette. */
    private Map<Integer, float[]> colorIndexToUV;

    /**
     * Carica e parsa un file .vly dagli assets.
     */
    public static VlyModel loadFromAssets(Context context, String assetFileName) throws IOException {
        try (InputStream is = context.getAssets().open(assetFileName)) {
            return parse(is);
        }
    }

    private static VlyModel parse(InputStream is) throws IOException {
        VlyModel model = new VlyModel();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String gridLine = readNonEmptyLine(reader);
        String[] gridParts = splitAfterColon(gridLine);
        StringTokenizer gridTok = new StringTokenizer(gridParts[0]);
        model.gridX = Integer.parseInt(gridTok.nextToken());
        model.gridY = Integer.parseInt(gridTok.nextToken());
        model.gridZ = Integer.parseInt(gridTok.nextToken());

        String numLine = readNonEmptyLine(reader);
        String[] numParts = splitAfterColon(numLine);
        model.voxelNum = Integer.parseInt(numParts[0].trim());

        model.voxels = new ArrayList<>(model.voxelNum);

        for (int i = 0; i < model.voxelNum; i++) {
            String line = readNonEmptyLine(reader);
            StringTokenizer tok = new StringTokenizer(line);
            int x = Integer.parseInt(tok.nextToken());
            int y = Integer.parseInt(tok.nextToken());
            int z = Integer.parseInt(tok.nextToken());
            int c = Integer.parseInt(tok.nextToken());
            model.voxels.add(new Voxel(x, y, z, c));
        }

        // Il resto delle righe è la definizione colori: <Ci, R, G, B>
        Map<Integer, int[]> colorMap = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            StringTokenizer tok = new StringTokenizer(line);
            int ci = Integer.parseInt(tok.nextToken());
            int r = Integer.parseInt(tok.nextToken());
            int g = Integer.parseInt(tok.nextToken());
            int b = Integer.parseInt(tok.nextToken());
            colorMap.put(ci, new int[]{r, g, b});
        }

        model.buildPalette(colorMap);
        return model;
    }

    /**
     * Costruisce una texture-palette quadrata NxN che contiene tutti i colori usati.
     * side = ceil(sqrt(numColori)).
     */
    private void buildPalette(Map<Integer, int[]> colorMap) {
        int numColors = colorMap.size();
        int side = Math.max(1, (int) Math.ceil(Math.sqrt(numColors)));
        this.paletteSide = side;

        Bitmap bmp = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);

        // Ordiniamo le chiavi per avere un mapping deterministico indice->pixel
        List<Integer> keys = new ArrayList<>(colorMap.keySet());
        java.util.Collections.sort(keys);

        colorIndexToUV = new HashMap<>();

        for (int i = 0; i < keys.size(); i++) {
            int colorIndex = keys.get(i);
            int[] rgb = colorMap.get(colorIndex);

            int px = i % side;
            int py = i / side;

            int argb = Color.argb(255, rgb[0], rgb[1], rgb[2]);
            bmp.setPixel(px, py, argb);

            // UV al centro del texel, in [0,1]. V invertita perché le texture
            // OpenGL hanno origine in basso a sinistra mentre Bitmap Android
            // ha origine in alto a sinistra.
            float u = (px + 0.5f) / side;
            float v = 1.0f - (py + 0.5f) / side;
            colorIndexToUV.put(colorIndex, new float[]{u, v});
        }

        this.paletteBitmap = bmp;
    }

    /** Ritorna le UV (u,v) associate a un dato indice colore. */
    public float[] getUVForColorIndex(int colorIndex) {
        float[] uv = colorIndexToUV.get(colorIndex);
        if (uv == null) {
            // fallback difensivo: non dovrebbe accadere con un file .vly valido
            return new float[]{0f, 0f};
        }
        return uv;
    }

    private static String readNonEmptyLine(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) return line;
        }
        throw new IOException("Fine file inattesa durante il parsing del preambolo .vly");
    }

    /** Gestisce righe tipo "grid_size: 1 1 3" restituendo la parte dopo i due punti. */
    private static String[] splitAfterColon(String line) {
        int idx = line.indexOf(':');
        String rest = (idx >= 0) ? line.substring(idx + 1).trim() : line.trim();
        return new String[]{rest};
    }
}