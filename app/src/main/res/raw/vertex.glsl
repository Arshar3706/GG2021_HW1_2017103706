//p. vertex.glsl code 작성

uniform mat4 uVPMatrix;
uniform mat4 uMFMatrix;
attribute vec4 vPosition;
// 행렬 곱이 한 번에 한 번만 되서 만든 임시 행렬
mat4 mvpMatrix;

void main() {
    mvpMatrix = uVPMatrix * uMFMatrix;
    gl_Position = mvpMatrix * vPosition;
}