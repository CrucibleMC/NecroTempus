#version 120
uniform sampler2D uTex;
uniform vec3 uColor;
void main() {
    float a = texture2D(uTex, gl_TexCoord[0].st).a;
    if (a < 0.1) discard;          // preserve alpha-cutout shape (capes, etc.)
    gl_FragColor = vec4(uColor, 1.0);
}
