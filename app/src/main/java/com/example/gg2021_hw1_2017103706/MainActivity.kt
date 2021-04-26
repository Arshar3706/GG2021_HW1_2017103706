package com.example.gg2021_hw1_2017103706

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.opengl.Matrix.setIdentityM
import android.opengl.Matrix.translateM
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.core.graphics.rotationMatrix
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan


class MainActivity : AppCompatActivity() {
    private lateinit var glView: GLSurfaceView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glView = MyGLSurfaceView(this)

        setContentView(glView)
    }
}

class MyGLSurfaceView(context: Context): GLSurfaceView(context){
    private val renderer: MyGLRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = MyGLRenderer(context)
        setRenderer(renderer)
    }
}

class MyGLRenderer(context: Context): GLSurfaceView.Renderer{
    private val mContext: Context = context
    private var projectionMatrix = FloatArray(16)
    private var viewMatrix = FloatArray(16)
    //P. model matrix & 매 프레임 변화 matrix 선언
    // Model Matrix
    // Cube.OBJ
    private var cubeMatrix = FloatArray(16)
    private var cubeMVMatrix = FloatArray(16)
    private var cubeMVPMatrix = FloatArray(16)
    // Person.OBJ
    private var personMatrix = FloatArray(16)
    private var personMVMatrix = FloatArray(16)
    private var personMVPMatrix = FloatArray(16)
    // Teapot.OBJ
    private var teapotMatrix = FloatArray(16)
    private var teapotMVMatrix = FloatArray(16)
    private var teapotMVPMatrix = FloatArray(16)

    // 매 프레임 변화 Matrix
    private var scaleMatrix = FloatArray(16)
    private var rotationMatrix = FloatArray(16)

    //P. object 선언
    private lateinit var cube: Object
    private lateinit var person: Object
    private lateinit var teapot: Object

    //C.테스트용 Identity Matrix
    private val identityMatrix = FloatArray(16)

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        //P. object 초기화
        cube = Object(mContext, "cube.obj")
        teapot = Object(mContext, "teapot.obj")
        person = Object(mContext, "person.obj")

        //P. model matrix & 매 프레임 변화 matrix 초기화
        // Model Matrix 초기화
        cubeMatrix[0] = 0.5f; cubeMatrix[5] = 1f; cubeMatrix[10] = 0.5f; cubeMatrix[15] = 1f;
        /**
         * JAVA Matrix의 특징인가? 열벡터 기준임
         * Matrices are 4 x 4 column-vector matrices stored in column-major order
         * ...
         */
        personMatrix[0] = 1f; personMatrix[5] = 1f; personMatrix[10] = 1f; personMatrix[12] = 2f;personMatrix[15] = 1f;
        teapotMatrix[0] = -0.2f; teapotMatrix[5] = 0.2f; teapotMatrix[10] = -0.2f; teapotMatrix[12] = 1.25f; teapotMatrix[13] = 0.4f; teapotMatrix[15] = 1f;

        // 매 프레임 변화 Matrix 초기화
        scaleMatrix[0] = 1.001f; scaleMatrix[5] = 1.002f; scaleMatrix[10] = 1.001f; scaleMatrix[15] = 1f;
        rotationMatrix[0] = cos(0.0139626f); rotationMatrix[2] = sin(0.0139626f); rotationMatrix[5] = 1f; rotationMatrix[8] = -sin(0.0139626f); rotationMatrix[10] = cos(0.0139626f); rotationMatrix[15] = 1f;
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        //P. 아래 구현한 mySetLookAtM function 으로 수정
        // Matrix.setLookAtM(viewMatrix, 0, 1.5f, 1.5f, -9f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        mySetLookAtM(viewMatrix, 1.5f, 1.5f, -9f, 0f, 0f, 0f, 0f, 1f, 0f)

        /**
         * 오른쪽 행렬부터 곱하고, 왼쪽 행렬을 곱해서 결과값을 전달함, 오른쪽에서 왼쪽으로 읽는다 생각하면 될듯?
         * Offset은 해당 행렬에서 시작 지점
         */
        //P. 각 object 별 매 프레임 변화 matrix 와 model matrix 를 multiply
        // Cube
        if(cubeMatrix[5] < 3.0f)
        {
            Matrix.multiplyMM(cubeMatrix, 0, scaleMatrix, 0, cubeMatrix, 0)
        }
        // Person
        Matrix.multiplyMM(personMatrix, 0, rotationMatrix, 0, personMatrix, 0)
        // Teapot
        Matrix.multiplyMM(teapotMatrix, 0, rotationMatrix, 0, teapotMatrix, 0)

        /**
         * 정리할 겸 써 봅니다.
         * 시야에 넣는 작업이다.
         * 첫 번째는 World Space 내에 있는 Objec를 Camera Space로 옮기기 위한 View Transform (결과값 = MV Matrix)
         * 두 번째는 그렇게 옮겨온 Objec를 Frustum Space로 옮기는 Projection Transform (결과값 = MVP Matrix), 참고로 Frustum의 의미는 '절두체'이고, 그 강의에서 자주 본 형태를 말하는겅
         * Object Space에서 World Space로 옮기는 World Transform도 있지만, 그건 초기화에서 이미 했고 편의상 그냥 표시함
         */
        // Cube
        Matrix.multiplyMM(cubeMVMatrix, 0, viewMatrix, 0, cubeMatrix, 0)
        Matrix.multiplyMM(cubeMVPMatrix, 0, projectionMatrix, 0, cubeMVMatrix, 0)
        // Person
        Matrix.multiplyMM(personMVMatrix, 0, viewMatrix, 0, personMatrix, 0)
        Matrix.multiplyMM(personMVPMatrix, 0, projectionMatrix, 0, personMVMatrix, 0)
        // Teapot
        Matrix.multiplyMM(teapotMVMatrix, 0, viewMatrix, 0, teapotMatrix, 0)
        Matrix.multiplyMM(teapotMVPMatrix, 0, projectionMatrix, 0, teapotMVMatrix, 0)

        //P. object draw
        cube.draw(cubeMVPMatrix)
        person.draw(personMVPMatrix)
        teapot.draw(teapotMVPMatrix)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0,0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        //P.  아래 구현한 myFrustumM function 으로 수정
        myFrustumM(projectionMatrix, ratio, 60f, 2f, 12f)
        // Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 12f)
    }
}

/**
 * Kotlin에서 함수 쓰는 법은 일단 검색하고...
 * Kotlin 함수의 특징은 파라미터가 자동으로 val로 지정됨
 * 따라서 파라미터로 받은 값을 함수 내부에서 수정하는 것도 불가능 = Reference로 반환 당연히 안됨
 * 대신 DataClass를 활용하여 여러 개의 값을 반환할 수 있다.
 * 기본적으로 두 개는 Pair, 세 개는 Triple을 사용하며, 접근은 .first, .second, .third 이렇게 함
 * 아니면 list로 만들 수도 있음
 */
//P. vecNormalize function 구현: 벡터 정규화 함수 (mySetLookAtM function 구현 시 사용)
fun vecNormalize(tempX: Float, tempY: Float, tempZ: Float): Triple<Float, Float, Float>
{
    val vectorLength = sqrt(tempX*tempX + tempY*tempY + tempZ*tempZ)

    val normalX = tempX/vectorLength
    val normalY = tempY/vectorLength
    val normalZ = tempZ/vectorLength

    // return 내에 식 쓸 수 있긴 한데, 이게 좀 더 보기 편해서
    return Triple(normalX, normalY, normalZ)

}

//P. mySetLookAtM function 구현: viewMatrix 구하는 함수 (Matrix library function 중 multiplyMM 만 사용 가능)
fun mySetLookAtM(tempMatrix: FloatArray, eyeX: Float, eyeY: Float, eyeZ: Float, atX: Float, atY: Float, atZ: Float, upX: Float, upY: Float, upZ: Float)
{
    // Basis n 만들기
    // Camera의 위치에서 at을 뺀다.
    var nX = eyeX - atX
    var nY = eyeY - atY
    var nZ = eyeZ - atZ


    // 뺀 결과를 Normalize
    var normalizeF = vecNormalize(nX, nY, nZ)
    nX = normalizeF.first
    nY = normalizeF.second
    nZ = normalizeF.third

    /**
     * Matrix 내장 함수에선 f가 focus(물체 방향)로 n이랑 방향이 반대다.
     * 따라서 계산 방법도 많이 바뀌긴 하는데 내장 함수에서도 마지막에 -1을 곱해준다.
     * 찾아보면 화면으로 나오는 방향이 n이 맞긴함
     */
    // Basis u 만들기
    //
    // n과 Up을 외적
    var uX = upY*nZ - upZ*nY
    var uY = upZ*nX - upX*nZ
    var uZ = upX*nY - upY*nX

    // 외적의 결과를 Normalize
    var normalizeS = vecNormalize(uX, uY, uZ)
    uX = normalizeS.first
    uY = normalizeS.second
    uZ = normalizeS.third

    // Basis v 만들기
    // u와 n을 외적
    var vX = nY*uZ - nZ*uY
    var vY = nZ*uX - nX*uZ
    var vZ = nX*uY - nY*uX

    // View Transform 만들기
    tempMatrix[0] = uX
    tempMatrix[1] = vX
    tempMatrix[2] = nX
    tempMatrix[3] = 0.0f

    tempMatrix[4] = uY
    tempMatrix[5] = vY
    tempMatrix[6] = nY
    tempMatrix[7] = 0.0f

    tempMatrix[8] = uZ
    tempMatrix[9] = vZ
    tempMatrix[10] = nZ
    tempMatrix[11] = 0.0f

    tempMatrix[12] = -(uX*eyeX + uY*eyeY + uZ*eyeZ)
    tempMatrix[13] = -(vX*eyeX + vY*eyeY + vZ*eyeZ)
    tempMatrix[14] = -(nX*eyeX + nY*eyeY + nZ*eyeZ)
    tempMatrix[15] = 1.0f
}

//P. myFrustumM function 구현: projectionMatrix 구하는 함수 (Matrix library function 중 multiplyMM 만 사용 가능)
fun myFrustumM(tempMatrix: FloatArray, aspect: Float, fov: Float, near: Float, far: Float)
{
    /**
     * 180 degrees = PI radians
     * 1 degree = PI / 180 radians
     * Math.PI
     * but I use Math.toRadians(), it's parameter is double, and also, it returns double type.
     */
    val tempRadians = Math.toRadians(fov.toDouble())
    val tempX = (1.0f/(aspect* tan(tempRadians/2))).toFloat()
    val tempY = (1.0f/tan(tempRadians/2)).toFloat()
    val tempM3 = far/(far-near)
    val tempM4 = (far*near)/(far-near)

    // Matrix 할당
    tempMatrix[0] = tempX;
    tempMatrix[1] = 0.0f;
    tempMatrix[2] = 0.0f;
    tempMatrix[3] = 0.0f;

    tempMatrix[4] = 0.0f;
    tempMatrix[5] = tempY;
    tempMatrix[6] = 0.0f;
    tempMatrix[7] = 0.0f;

    tempMatrix[8] = 0.0f;
    tempMatrix[9] = 0.0f;
    tempMatrix[10] = tempM3;
    // 기존의 Z 값 저장용이라고 한다. + NDC로 넘어오면 Z 축이 뒤집힌다고도 함. 그래서 Z가 크면 멀리 있는거
    tempMatrix[11] = -1.0f;

    tempMatrix[12] = 0.0f;
    tempMatrix[13] = 0.0f;
    tempMatrix[14] = tempM4;
    tempMatrix[15] = 0.0f;
    /**
     * 배운거 기록용
     * View Transform을 통해 생성된 결과물을 EYE라고 했을 떄,
     * Mprojection * (EYE) = (Clip)이 된다.
     * 그니까, Near Plane으로 다 Projection 되는거지 = Z' 값은 어떤 점이든 같다. 대신, W 크기가 Z가 됨
     * 여기까진 모든 점이 Near Plane 평면상에 존재하는거고,
     * 이걸 NDC 공간으로 뿌려주는데,
     * 그게 (Clip / W Clip)이다 = 이러면 homogeneous 값 W가 다시 1이 되고,
     * Z가 컸던 애들 = 멀리 존재하던 애들의 Vertex 크기가 줄어든다 = 원근감에 맞게 NDC 공간으로 뿌려진다.
     * 참고 : http://www.songho.ca/opengl/gl_projectionmatrix.html#perspective
     */
}


//PP. cube, person, teapot 모두 포함할 수 있는 Object class 로 수정
class Object(context: Context, fileName: String) {

    //P. 아래 shader code string 지우고, res/raw 에 위치한 vertex.glsl , fragment.glsl 로드해서 vertexShaderCode, fragmentShaderCode 에 넣기
    private val vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}"

    private val fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}"

    // 여기서부터 오류임
    private val idVertex = context.resources.getIdentifier("res/vertex.glsl", null, context.packageName)
    private val idFragment = context.resources.getIdentifier("res/fragment.glsl", null, context.packageName)

    //P. model matrix handle 변수 추가 선언
    private var vPMatrixHandle: Int = 0
    private var modelMatrixHandle: String = fileName

    val color = floatArrayOf(1.0f, 0.980392f, 0.980392f, 0.3f)

    private var mProgram: Int

    private var vertices = mutableListOf<Float>()
    private var faces = mutableListOf<Short>()
    private lateinit var verticesBuffer: FloatBuffer
    private lateinit var facesBuffer: ShortBuffer

    init {

        val tempVertexShaderCode = loadFile(context, idVertex)
        val tempFragmentShaderCode = loadFile(context, idFragment)

        try {
            val scanner = Scanner(context.assets.open(modelMatrixHandle))
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                if (line.startsWith("v  ")) {
                    val vertex = line.split(" ")
                    val x = vertex[2].toFloat()
                    val y = vertex[3].toFloat()
                    val z = vertex[4].toFloat()
                    vertices.add(x)
                    vertices.add(y)
                    vertices.add(z)
                } else if (line.startsWith("f ")) {
                    val face = line.split(" ")
                    val vertex1 = face[1].split("/")[0].toShort()
                    val vertex2 = face[2].split("/")[0].toShort()
                    val vertex3 = face[3].split("/")[0].toShort()
                    faces.add(vertex1)
                    faces.add(vertex2)
                    faces.add(vertex3)
                }
            }

            verticesBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    for (vertex in vertices) {
                        put(vertex)
                    }
                    position(0)
                }
            }

            facesBuffer = ByteBuffer.allocateDirect(faces.size * 2).run {
                order(ByteOrder.nativeOrder())
                asShortBuffer().apply {
                    for (face in faces) {
                        put((face - 1).toShort())
                    }
                    position(0)
                }
            }
        } catch (e: Exception) {
            Log.e("file_read", e.message.toString())
        }

        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, tempVertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, tempFragmentShaderCode)

        mProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    val COORDS_PER_VERTEX = 3

    private var positionHandle: Int = 0
    private var mColorHandle: Int = 0

    private val vertexStride: Int = COORDS_PER_VERTEX * 4

    //PP. cube, person, teapot 의 world transform 및 매 프레임 변화를 반영할 수 있는 draw function 으로 수정
    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(mProgram)

        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also {
            GLES20.glEnableVertexAttribArray(it)
            GLES20.glVertexAttribPointer(
                    it,
                    COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT,
                    false,
                    vertexStride,
                    verticesBuffer
            )

            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
            GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

            GLES20.glDrawElements(GLES20.GL_TRIANGLES, faces.size, GLES20.GL_UNSIGNED_SHORT, facesBuffer)

            GLES20.glDisableVertexAttribArray(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    private fun loadFile(context: Context, fileID: Int): String {
        lateinit var tempLine: String
        val scanner = Scanner(context.resources.openRawResource(fileID))
        try {
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                tempLine += line
            }
        } catch (e: Exception) {
            Log.e("file_read", e.message.toString())
        }
        return tempLine
    }
}