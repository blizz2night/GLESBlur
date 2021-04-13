precision mediump float;

uniform vec2 u_Offset[256];
uniform float u_Weight[256];
uniform float u_Vertical;
uniform sampler2D u_InputImageTexture;
uniform int u_Size;
varying vec2 v_TexCoord;

void main() {
    vec4 color;
    if(u_Vertical<0.0){
        color = texture2D(u_InputImageTexture, v_TexCoord);
    }else{
        color = texture2D(u_InputImageTexture, v_TexCoord) * u_Weight[0];
        for (int i=1; i<u_Size; i++) {
            color += texture2D(u_InputImageTexture, (v_TexCoord + u_Offset[i])) * u_Weight[i];
            color += texture2D(u_InputImageTexture, (v_TexCoord - u_Offset[i])) * u_Weight[i];
        }
    }
    gl_FragColor = color;
}