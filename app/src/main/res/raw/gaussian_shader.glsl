uniform sampler2D u_InputImageTexture;

uniform float offset[5] = float[](0.0, 1.0, 2.0, 3.0, 4.0);
uniform float weight[5] = float[](0.2270270270, 0.1945945946, 0.1216216216,
0.0540540541, 0.0162162162);
uniform float width,height;
varying vec2 textureCoordinate;

void main(void) {
    vec4 color = texture2D(u_InputImageTexture, vec2(textureCoordinate) / height) * weight[0];

    for (int i=1; i<5; i++) {
        color +=
        texture2D(u_InputImageTexture, (vec2(textureCoordinate) + vec2(0.0, offset[i])) / height)
        * weight[i];

        color +=
        texture2D(u_InputImageTexture, (vec2(textureCoordinate) - vec2(0.0, offset[i])) / height)
        * weight[i];
    }
    gl_FragColor = color;
}