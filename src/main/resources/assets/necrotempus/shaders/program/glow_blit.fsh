#version 120
// Plain textured blit. Using a shader (rather than fixed-function texturing) means the composite
// samples ONLY the ring texture and ignores the lightmap texture unit, texture-env combine, and
// current glColor left over from world rendering (e.g. the chest tile-entity renderer) — which would
// otherwise darken the outline.
uniform sampler2D uTex;
void main() {
    gl_FragColor = texture2D(uTex, gl_TexCoord[0].st);
}
