package com.example.root.flash

import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Surface
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_flash.*

@Suppress("DEPRECATION")
class FlashActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CAMERA = 1

    private var sX = 0f
    private var sY = 0f
    private lateinit  var button: Button
    private lateinit var camManager: CameraManager
    private lateinit var camDevice: CameraDevice
    private lateinit var request: CaptureRequest.Builder
    private lateinit var camSession: CameraCaptureSession
    private lateinit var texture: SurfaceTexture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flash)
        getScreen()
        setUI()
        init()
    }

    private fun openCamera(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CAMERA)
        }else{
            if(getListCamera().lastIndex > 0)
                camManager.openCamera(getListCamera()[0], OpenCallback(), null)
            else
                Toast.makeText(this, "this device is not support", Toast.LENGTH_SHORT).show()
        }
    }

    private fun init(){
        camManager = this.getSystemService(android.content.Context.CAMERA_SERVICE) as CameraManager
        openCamera()
    }

    private fun getListCamera(): List<String> {
        var list = listOf<String>()
        for (i in camManager.cameraIdList){
            list += i
        }
        return list
    }

    private fun event(){
        var i = 0
        button.setOnClickListener{
            if(i%2 == 0) {
                button.text = "On"
                request.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            }
            else{
                button.text = "Off"
                request.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            }
            camSession.setRepeatingRequest(request.build(), null, null)
            i++
        }
    }

    private fun setUI(){
        button = Button(this)
        button.isActivated = false
        button.text = "Off"
        button.measure(0, 0)
        rl_flash_activity.addView(button)
        button.x = sX*50 - button.measuredWidth/2
        button.y = sY*50
        texture = SurfaceTexture(1)
    }

    private fun getScreen(){
        val manager = windowManager.defaultDisplay
        val point  = Point()
        manager.getSize(point)
        sX = point.x/100f
        sY = point.y/100f
    }

    inner class OpenCallback: CameraDevice.StateCallback(){
        override fun onOpened(camera: CameraDevice) {
            Log.d("camera", "opened")
            camDevice = camera
            request = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

            texture.setDefaultBufferSize(1, 1)
            request.addTarget(Surface(texture))

            val outputSurface = ArrayList<Surface>(1)
            outputSurface.add(Surface(texture))
            try {
                camDevice.createCaptureSession(outputSurface, CameraCaptureSessionCallBack(), null)
            }catch (e: Exception){}
            event()
        }

        override fun onDisconnected(camera: CameraDevice) {
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.d("camera", "err")
        }
    }

    open inner class CameraCaptureSessionCallBack: CameraCaptureSession.StateCallback(){
        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.d("CaptureSession", "false")
        }

        override fun onConfigured(session: CameraCaptureSession) {
            camSession = session
            camSession.setRepeatingRequest(request.build(), null, null)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            // Request for camera permission.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                openCamera()
            } else {
                // Permission request was denied.
                Toast.makeText(this, "permission is not granded", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
