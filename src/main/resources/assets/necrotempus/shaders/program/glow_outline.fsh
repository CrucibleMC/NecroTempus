#version 120
// Rainbow-cycling outline: the coloured ring shifts hue uniformly over time, ignoring the entity's
// original colour. A separate, fixed-width black stroke surrounds its outer edge.
uniform sampler2D uTex;
uniform vec2 uTexel;   // 1/width, 1/height
uniform float uWidth;  // outline thickness in this half-resolution framebuffer (0.5..2)
uniform float uStrokeWidth;   // maximum black-stroke thickness (0.75 = 1.5 final screen pixels)
uniform float uStrokeFeather; // outer antialiasing width (0.5 = 1 final screen pixel)
uniform float uTime;   // seconds, for animation

vec3 hsl2rgb(float h, float s, float l) {
    vec3 rgb = clamp(abs(mod(h * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    return l + s * (rgb - 0.5) * (1.0 - abs(2.0 * l - 1.0));
}

void main() {
    vec2 uv = gl_TexCoord[0].st;
    float minDist = 1.0e9;
    // Minimum alpha in a 1-pixel neighbourhood.  If ANY pixel within 1px is empty
    // (alpha < 0.5) we are within 1px of the silhouette edge, and the ring should
    // NOT be cut — this pushes the ring 1px inside the entity, past the AA fringe.
    float minAlpha1px = 1.0;
    const int R = 5;   // constant loop bound (GLSL 120); covers max uWidth plus the AA margin
    for (int dx = -R; dx <= R; dx++) {
        for (int dy = -R; dy <= R; dy++) {
            vec4 s = texture2D(uTex, uv + vec2(float(dx), float(dy)) * uTexel);
            if (s.a > 0.5) {
                float d = length(vec2(float(dx), float(dy)));
                if (d < minDist) minDist = d;
            }
            if (abs(dx) <= 1 && abs(dy) <= 1) {
                minAlpha1px = min(minAlpha1px, s.a);
            }
        }
    }
    // 1 inside the ring radius, smoothly to 0 across the last pixel of the outer edge...
    float ring = 1.0 - smoothstep(uWidth - 0.5, uWidth + 0.5, minDist);
    // ...cut inside only where EVERY pixel in a 1px radius is solid silhouette (>0.6 alpha),
    // i.e. we are more than 1px deep inside the entity.  This avoids the anti-aliased fringe.
    float edgeMask = 1.0 - smoothstep(0.4, 0.6, minAlpha1px);
    ring *= edgeMask;

    // Single hue for the entire ring, cycling through the rainbow over time
    vec3 rainbow = hsl2rgb(fract(uTime * 0.08), 1.0, 0.5);

    // Keep a solid coloured core and reserve only the outermost band for the black stroke. For very
    // thin configured outlines, cap the stroke at half the ring so it cannot swallow the colour.
    float strokeWidth = min(uStrokeWidth, uWidth * 0.5);
    float strokeStart = uWidth - strokeWidth;
    float stroke = smoothstep(strokeStart - uStrokeFeather, strokeStart + uStrokeFeather, minDist);

    // The old alpha came from `ring`, whose outermost samples are deliberately translucent. Give the
    // black band full coverage through uWidth and fade only beyond it, so distant strokes stay solid.
    float strokeAlpha = (1.0 - smoothstep(uWidth, uWidth + uStrokeFeather, minDist)) * edgeMask;
    float finalAlpha = mix(ring, strokeAlpha, stroke);
    if (finalAlpha <= 0.01) discard;
    gl_FragColor = vec4(mix(rainbow, vec3(0.0), stroke), finalAlpha);
}
