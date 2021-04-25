package com.example.gg2021_hw1_2017103706

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.opengl.Matrix.setIdentityM
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.core.graphics.rotationMatrix
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
    private var cubeMatrix = FloatArray(16)
    private var cubeMVMatrix = FloatArray(16)
    private var cubeMVPMatrix = FloatArray(16)

    private var personMatrix = FloatArray(16)
    private var teapotMatrix = FloatArray(16)
    // 매 프레임 변화 Matrix
    private var scaleMatrix = FloatArray(16)
    private var rotateMatrix = FloatArray(16)
    private var rotationMatrix = FloatArray(16)

    //P. object 선언
    private lateinit var cube: Object
    private lateinit var person: Object
    private lateinit var teapot: Object

    // Frame마다 증가시키기 위해 추가하는 변수입니다.
    var time = 0f;

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        //P. object 초기화
        cube = Object(mContext, "cube.obj")
        teapot = Object(mContext, "teapot.obj")
        person = Object(mContext, "person.obj")

        //P. model matrix & 매 프레임 변화 matrix 초기화
        // Model Matrix 초기화
        cubeMatrix[0] = 0.5f; cubeMatrix[5] = 1f; cubeMatrix[10] = 0.5f; cubeMatrix[15] = 1f;
        personMatrix[0] = 1f; personMatrix[3] = 2f; personMatrix[5] = 1f; personMatrix[7] = 0f; personMatrix[10] = 1f; personMatrix[11] =0f; personMatrix[15] = 1f;
        teapotMatrix[0] = -0.2f; teapotMatrix[3] = 1.25f; teapotMatrix[5] = 0.2f; teapotMatrix[7] = 0.4f; teapotMatrix[10] = -0.2f; teapotMatrix[15] = 1f;


        // 매 프레임 변화 Matrix 초기화
        scaleMatrix[0] = 1.001f; scaleMatrix[5] = 1.002f; scaleMatrix[10] = 1.001f; scaleMatrix[15] = 1f;
        rotateMatrix[0] = cos(0.0139626f); rotateMatrix[2] = sin(0.0139626f); rotateMatrix[5] = 1f; rotateMatrix[8] = sin(0.0139626f); rotateMatrix[10] = cos(0.0139626f); rotateMatrix[15] = 1f;
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        // scratch 의미가 약간 순수하다. 그런 늬앙스래
        val scratchPerson = FloatArray(16)
        val scratchTeapot = FloatArray(16)

        //P. 아래 구현한 mySetLookAtM function 으로 수정
        Matrix.setLookAtM(viewMatrix, 0, 1.5f, 1.5f, -9f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        //P. 각 object 별 매 프레임 변화 matrix 와 model matrix 를 multiply
        if(cubeMatrix[5] < 3.0f)
        {
            Matrix.multiplyMM(cubeMatrix, 0, scaleMatrix, 0, cubeMatrix, 0)
        }

        /*
        Matrix.multiplyMM(cubeMatrix, 0, scaleMatrix, 0, cubeMatrix, 0)
        Matrix.multiplyMM(personMatrix, 0, rotateMatrix, 0, personMatrix, 0)
        Matrix.multiplyMM(teapotMatrix, 0, rotateMatrix, 0, teapotMatrix, 0)

        Matrix.multiplyMM(scaleMatrix, 0, scaleMatrix, 0, scaleMatrix, 0)
        Matrix.multiplyMM(rotateMatrix, 0, rotateMatrix, 0, rotateMatrix, 0)
         */

        // rotate를 위한 작업
        val angle = 0.800f * time.toInt()
        /* time 초기화 해주려고 했는데 일단 제외
        if(angle > 360)
        {
            time = 0f;
        }
        */
        // angle : 움직일 각, 그리고 적용할 축에 1.0f
        Matrix.setRotateM(rotationMatrix, 0, angle, 0f, -1.0f, 0f)

        // 시야에 넣는 작업
        // Cube 먼저
        // cubeMatrix[0] = 0.5f; cubeMatrix[5] = 1f; cubeMatrix[10] = 0.5f; cubeMatrix[15] = 1f;
        Matrix.multiplyMM(cubeMVMatrix, 0, viewMatrix, 0, cubeMatrix, 0)
        Matrix.multiplyMM(cubeMVPMatrix, 0, projectionMatrix, 0, cubeMVMatrix, 0)

        Matrix.multiplyMM(personMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(teapotMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // rotate 적용
        Matrix.multiplyMM(scratchTeapot, 0, teapotMatrix, 0, rotationMatrix, 0)
        Matrix.multiplyMM(scratchPerson, 0, personMatrix, 0, rotationMatrix, 0)

        //P. object draw
        cube.draw(cubeMVPMatrix)
        // person.draw(scratchPerson)
        // teapot.draw(scratchTeapot)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0,0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        //P.  아래 구현한 myFrustumM function 으로 수정
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 12f)
    }
}

//P. vecNormalize function 구현: 벡터 정규화 함수 (mySetLookAtM function 구현 시 사용)

//P. mySetLookAtM function 구현: viewMatrix 구하는 함수 (Matrix library function 중 multiplyMM 만 사용 가능)

//P. myFrustumM function 구현: projectionMatrix 구하는 함수 (Matrix library function 중 multiplyMM 만 사용 가능)


//PP. cube, person, teapot 모두 포함할 수 있는 Object class 로 수정
class Object(context: Context, fileName: String){

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

    //P. model matrix handle 변수 추가 선언
    private var vPMatrixHandle: Int = 0

    val color = floatArrayOf(1.0f, 0.980392f, 0.980392f, 0.3f)

    private var mProgram: Int

    private var vertices = mutableListOf<Float>()
    private var faces = mutableListOf<Short>()
    private lateinit var verticesBuffer: FloatBuffer
    private lateinit var facesBuffer: ShortBuffer

    init {
        try {
            val scanner = Scanner(context.assets.open(fileName))
            while (scanner.hasNextLine()){
                val line = scanner.nextLine()
                if (line.startsWith("v  ")){
                    val vertex = line.split(" ")
                    val x = vertex[2].toFloat()
                    val y = vertex[3].toFloat()
                    val z = vertex[4].toFloat()
                    vertices.add(x)
                    vertices.add(y)
                    vertices.add(z)
                }
                else if (line.startsWith("f ")) {
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
                    for (vertex in vertices){
                        put(vertex)
                    }
                    position(0)
                }
            }

            facesBuffer = ByteBuffer.allocateDirect(faces.size * 2).run {
                order(ByteOrder.nativeOrder())
                asShortBuffer().apply {
                    for (face in faces){
                        put((face-1).toShort())
                    }
                    position(0)
                }
            }
        } catch (e: Exception){
            Log.e("file_read", e.message.toString())
        }

        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

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
    fun draw(mvpMatrix: FloatArray){
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
}