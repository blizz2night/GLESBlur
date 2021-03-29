attribute vec4 a_Position;
attribute vec4 a_InputTextureCoordinate;

varying vec2 textureCoordinate;

void main() {
    gl_Position = a_Position;
    textureCoordinate =  a_InputTextureCoordinate.xy;
}