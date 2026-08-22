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
                    "layout(location = 0) in vec3 aLocalPosition;\n" +
                    "layout(location = 1) in vec3 aNormal;\n" +
                    "layout(location = 2) in vec3 aInstanceOffset;\n" +
                    "layout(location = 3) in vec2 aInstanceUV;\n" +
                    "\n" +
                    "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uModelMatrix;\n" +
                    "\n" +
                    "out vec3 vWorldPos;\n" +
                    "out vec3 vNormal;\n" +
                    "out vec2 vUV;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec3 localCombined = aLocalPosition + aInstanceOffset;\n" +
                    "    vec4 worldPos = uModelMatrix * vec4(localCombined, 1.0);\n" +
                    "    gl_Position = uMVPMatrix * vec4(localCombined, 1.0);\n" +
                    "    vWorldPos = worldPos.xyz;\n" +
                    "    vNormal = mat3(uModelMatrix) * aNormal;\n" +
                    "    vUV = aInstanceUV;\n" +
                    "}\n";

    public static final String FRAGMENT_SHADER =
            "#version 300 es\n" +
                    "precision mediump float;\n" +
                    "\n" +
                    "in vec3 vWorldPos;\n" +
                    "in vec3 vNormal;\n" +
                    "in vec2 vUV;\n" +
                    "\n" +
                    "uniform sampler2D uPaletteTexture;\n" +
                    "\n" +
                    "uniform vec3 uLightPosWorld;\n" +
                    "uniform vec3 uCameraPosWorld;\n" +
                    "\n" +
                    "uniform float uAmbientIntensity;\n" +   // es. 0.35
                    "uniform float uSpecularIntensity;\n" +  // es. 0.5
                    "uniform float uShininess;\n" +          // es. 32.0
                    "\n" +
                    "out vec4 fragColor;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec3 baseColor = texture(uPaletteTexture, vUV).rgb;\n" +
                    "    vec3 N = normalize(vNormal);\n" +
                    "    vec3 L = normalize(uLightPosWorld - vWorldPos);\n" +
                    "    vec3 V = normalize(uCameraPosWorld - vWorldPos);\n" +
                    "    vec3 H = normalize(L + V);\n" +
                    "\n" +
                    "    float diff = max(dot(N, L), 0.0);\n" +
                    "    float spec = (diff > 0.0)\n" +
                    "        ? pow(max(dot(N, H), 0.0), uShininess)\n" +
                    "        : 0.0;\n" +
                    "\n" +
                    "    vec3 ambient = baseColor * uAmbientIntensity;\n" +
                    "    vec3 diffuse = baseColor * diff;\n" +
                    "    vec3 specular = vec3(1.0) * spec * uSpecularIntensity;\n" +
                    "\n" +
                    "    fragColor = vec4(ambient + diffuse + specular, 1.0);\n" +
                    "}\n";
    }