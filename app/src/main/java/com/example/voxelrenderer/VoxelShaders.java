package com.example.voxelrenderer;

/**
 * Sorgenti GLSL ES 3.0 per il rendering instanziato dei voxel.
 * <p>
 * Idea:
 * - la mesh di base è UN SOLO CUBO unitario, centrato nell'origine (attributi
 *   per-vertice: posizione locale + normale).
 * - ogni istanza (voxel) fornisce, tramite un VBO separato con
 *   glVertexAttribDivisor(1), un offset 3D (dove piazzare il cubo nello
 *   spazio) e le UV del suo colore nella palette texture.
 * - il vertex shader somma offset + posizione locale, applica MVP.
 * - il fragment shader legge il colore dalla palette texture usando le UV
 *   costanti per istanza (stesse per tutti i vertici di quel cubo) e applica
 *   uno shading lambertiano molto semplice basato sulla normale.
 */
public class VoxelShaders {

    public static final String VERTEX_SHADER =
            "#version 300 es\n" +
                    "layout(location = 0) in vec3 aLocalPosition;\n" +   // vertice del cubo unitario
                    "layout(location = 1) in vec3 aNormal;\n" +          // normale della faccia
                    "layout(location = 2) in vec3 aInstanceOffset;\n" +  // offset per-istanza (voxel)
                    "layout(location = 3) in vec2 aInstanceUV;\n" +      // UV colore per-istanza
                    "\n" +
                    "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uModelMatrix;\n" +
                    "\n" +
                    "out vec3 vNormal;\n" +
                    "out vec2 vUV;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec3 worldLocal = aLocalPosition + aInstanceOffset;\n" +
                    "    gl_Position = uMVPMatrix * vec4(worldLocal, 1.0);\n" +
                    "    vNormal = mat3(uModelMatrix) * aNormal;\n" +
                    "    vUV = aInstanceUV;\n" +
                    "}\n";

    public static final String FRAGMENT_SHADER =
            "#version 300 es\n" +
                    "precision mediump float;\n" +
                    "\n" +
                    "in vec3 vNormal;\n" +
                    "in vec2 vUV;\n" +
                    "\n" +
                    "uniform sampler2D uPaletteTexture;\n" +
                    "\n" +
                    "out vec4 fragColor;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec3 baseColor = texture(uPaletteTexture, vUV).rgb;\n" +
                    "\n" +
                    "    // Illuminazione direzionale semplice (lambert) + ambient,\n" +
                    "    // giusto per dare volume ai cubi senza shading imposto dalla traccia.\n" +
                    "    vec3 lightDir = normalize(vec3(0.5, 0.8, 0.6));\n" +
                    "    float diff = max(dot(normalize(vNormal), lightDir), 0.0);\n" +
                    "    vec3 ambient = baseColor * 0.35;\n" +
                    "    vec3 diffuse = baseColor * diff * 0.65;\n" +
                    "\n" +
                    "    fragColor = vec4(ambient + diffuse, 1.0);\n" +
                    "}\n";
}