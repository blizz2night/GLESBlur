precision mediump float;

uniform float u_Offset[5];
uniform float u_Weight[5];
uniform float u_Vertical;
uniform sampler2D u_InputImageTexture;
uniform float u_Width;
uniform float u_Height;

varying vec2 v_TexCoord;

void main() {
    vec4 color;
    vec2 offset;
    if(u_Vertical<0.0){
        color = texture2D(u_InputImageTexture, v_TexCoord);
    }else{
        color = texture2D(u_InputImageTexture, v_TexCoord) * u_Weight[0];
        if(u_Vertical>0.5){
            for (int i=1; i<5; i++) {
                offset = vec2(0.0, u_Offset[i]) / u_Height;
                color += texture2D(u_InputImageTexture, (v_TexCoord + offset)) * u_Weight[i];
                color += texture2D(u_InputImageTexture, (v_TexCoord - offset)) * u_Weight[i];
            }
        }else{
            for (int i=1; i<5; i++) {
                offset = vec2(u_Offset[i], 0.0) / u_Width;
                color += texture2D(u_InputImageTexture, (v_TexCoord + offset)) * u_Weight[i];
                color += texture2D(u_InputImageTexture, (v_TexCoord - offset)) * u_Weight[i];
            }
        }
    }
    gl_FragColor = color;
}