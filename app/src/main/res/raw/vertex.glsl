uniform mat4 uVPMatrix;
uniform mat4 uMFMatrix;
attribute vec4 vPosition;
void main() {
    gl_Position = uMFMatrix * vPosition;
}