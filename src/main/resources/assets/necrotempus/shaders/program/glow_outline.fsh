#version 120
// The ring uses the colour stored in the glow mask. A separate, fixed-width black stroke surrounds
// its outer edge.
uniform sampler2D uTex;
uniform vec2 uTexel;   // 1/width, 1/height
uniform float uWidth;  // outline thickness in framebuffer pixels
uniform float uStrokeWidth;   // maximum black-stroke thickness in framebuffer pixels
uniform float uStrokeFeather; // outer antialiasing width in framebuffer pixels

void main() {
    vec2 uv = gl_TexCoord[0].st;
    float strokeWidth = min(uStrokeWidth, uWidth * 0.5);
    float innerWidth = max(0.0, uWidth - strokeWidth);
    float maxAlpha = 0.0;
    float maxInnerAlpha = 0.0;
    vec3 ringColor = vec3(1.0);
    // Keep the coverage from the filtered silhouette instead of reducing every sample to a
    // binary hit. This preserves fractional edge coverage for the final blend.
    float minAlpha1px = 1.0;
    const int R = 5;   // constant loop bound (GLSL 120); covers max uWidth plus the AA margin
    for (int dx = -R; dx <= R; dx++) {
        for (int dy = -R; dy <= R; dy++) {
            vec4 s = texture2D(uTex, uv + vec2(float(dx), float(dy)) * uTexel);
            float d = length(vec2(float(dx), float(dy)));
            float feather = max(0.5, uStrokeFeather * 0.5);
            float outer = 1.0 - smoothstep(uWidth - feather, uWidth + feather, d);
            float inner = 1.0 - smoothstep(innerWidth - feather, innerWidth + feather, d);
            float coverage = s.a * outer;
            if (coverage > maxAlpha) {
                maxAlpha = coverage;
                ringColor = s.rgb;
            }
            maxInnerAlpha = max(maxInnerAlpha, s.a * inner);
            if (abs(dx) <= 1 && abs(dy) <= 1) {
                minAlpha1px = min(minAlpha1px, s.a);
            }
        }
    }
    // Cut the ring only when the whole local neighbourhood is solid, keeping a narrow inner fringe.
    float edgeMask = 1.0 - smoothstep(0.4, 0.6, minAlpha1px);
    float ring = maxAlpha * edgeMask;

    // The black band is the outer-minus-inner coverage, so it follows the same smooth edge as the
    // coloured ring instead of switching at a quantized distance.
    float stroke = clamp((maxAlpha - maxInnerAlpha) * edgeMask, 0.0, 1.0);
    float finalAlpha = ring;
    if (finalAlpha <= 0.01) discard;
    gl_FragColor = vec4(mix(ringColor, vec3(0.0), stroke), finalAlpha);
}
