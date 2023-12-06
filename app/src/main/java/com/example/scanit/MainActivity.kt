package com.example.scanit

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.scanit.ml.Model
import com.example.scanit.ml.ModelFruitsjpg
import com.example.scanit.ml.ModelHands
import com.example.scanit.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class MainActivity : AppCompatActivity() {
    //Todo ประกาศตัวแปร
    lateinit var textureView: TextureView
    lateinit var txtLabl: TextView
    lateinit var imageView: ImageView
    lateinit var cameraManager: CameraManager
    lateinit var cameraDevice: CameraDevice
    lateinit var handler: Handler

    lateinit var bitmap: Bitmap
    lateinit var imageProcessor: ImageProcessor
    lateinit var model: ModelFruitsjpg

    lateinit var labels:List<String>
    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY,
        Color.BLACK,Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)
    val paint = Paint()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //Todo เวลา Implement Code
        textureView = findViewById<TextureView>(R.id.textureView)
        //imageView = findViewById<ImageView>(R.id.imageView)
        txtLabl = findViewById<TextView>(R.id.txtLabel)
        //Background Process
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        labels = FileUtil.loadLabels(this, "labels.txt")
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = ModelFruitsjpg.newInstance(this)

        textureView.surfaceTextureListener = object :TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                bitmap = textureView.bitmap!!

                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)

                val outputs = model.process(image)
                val score = outputs.scoreAsCategoryList.apply {
                    sortByDescending {
                        it.score
                    }
                }
                //txtLabl.text = ""+score[0].label
                val label = score[0].label
                val percent = "${(score[0].score * 100).toInt()}%" // Convert score to percentage
                val displayText = "$label $percent"

                txtLabl.text = displayText


            }
        }
        cameraManager = getSystemService(Context. CAMERA_SERVICE) as CameraManager
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0], object:CameraDevice.StateCallback(){
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                var surfaceTexture = textureView.surfaceTexture
                var surface = Surface(surfaceTexture)
                var captureRequest =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)
                cameraDevice.createCaptureSession(listOf(surface),
                    object: CameraCaptureSession.StateCallback(){
                        override fun onConfigured(session: CameraCaptureSession) {
                            session.setRepeatingRequest(captureRequest.build(), null, null)
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {

                        }
                    },handler)
            }

            override fun onDisconnected(camera: CameraDevice) {

            }

            override fun onError(camera: CameraDevice, error: Int) {

            }
        },handler)
    }


    //Todo ถ้าต้องสร้าง function (fun)
    private fun grantPermission () {
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED){
            requestPermissions( arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult (
        requestCode: Int ,
        permissions: Array< out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode , permissions , grantResults)
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
            grantPermission()
        }
    }

}