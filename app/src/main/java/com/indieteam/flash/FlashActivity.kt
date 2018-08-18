package com.example.root.flash

import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.hardware.camera2.*
import android.os.BatteryManager
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
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_flash.*
import java.util.*
import kotlin.concurrent.schedule

@Suppress("DEPRECATION")
class FlashActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CAMERA = 1

    private var sX = 0f
    private var sY = 0f
    private lateinit  var button: Button
    private lateinit var textView: TextView
    private lateinit var camManager: CameraManager
    private lateinit var camDevice: CameraDevice
    private lateinit var request: CaptureRequest.Builder
    private lateinit var camSession: CameraCaptureSession
    private lateinit var texture: SurfaceTexture
    private var cameraBackId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flash)
        getScreen()
        setUI()
        init()
    }

    private fun init(){
        var detectBackCam = 0
        var detectFlash = 0
        if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            camManager = this.getSystemService(android.content.Context.CAMERA_SERVICE) as CameraManager
            for (i in camManager.cameraIdList){
                val camCharacteristics = camManager.getCameraCharacteristics(i.toString())
                if(camCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK){
                    detectBackCam = 1
                    if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                        detectFlash = 1
                        cameraBackId = i
                    }
                }
            }
            if(detectBackCam == 1 && detectFlash == 1)
                openCamera()
            if(detectBackCam == 0)
                Toasty.error(this, "This device is not support back camera", Toast.LENGTH_SHORT).show()
            if(detectFlash == 0)
                Toasty.error(this, "This device is not support led flash", Toast.LENGTH_SHORT).show()

        }else{
            Toasty.error(this, "This device is not support camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CAMERA)
        }else{
            try {
                camManager.openCamera(cameraBackId, OpenCallback(), null)
            }catch(e: Exception){
                Toasty.error(this, "This device is not suport camera2 api", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun battery(): Any{
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let {
            ifilter -> this.registerReceiver(null, ifilter)
        }

        val batteryPct: Float? = batteryStatus?.let {
            intent ->
                val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                level / scale.toFloat()
        }

        batteryPct?.let {
            return (batteryPct*100).toInt()
        }
        return "null"
    }

    private fun event(){
        var i = 0
        button.setOnClickListener{
            if(i%2 == 0) {
                button.let {
                    it.text = "On"
                    it.setTextColor(resources.getColor(R.color.colorAccent))
                    it.setBackgroundResource(R.drawable.flashlight_on)
                }
                textView.setTextColor(resources.getColor(R.color.colorDark))
                rl_flash_activity.setBackgroundResource(R.color.colorWhite)
                request.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            }
            else{
                button.let {
                    it.text = "Off"
                    it.setTextColor(resources.getColor(R.color.colorAccent))
                    it.setBackgroundResource(R.drawable.flashlight_off)
                }
                textView.setTextColor(resources.getColor(R.color.colorAccent))
                rl_flash_activity.setBackgroundResource(R.color.colorDark)
                request.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            }
            camSession.setRepeatingRequest(request.build(), null, null)
            i++
        }
    }

    private fun setUI(){
        button = Button(this)
        val btnWidth = (sX*35).toInt()
        button.let {
            it.isActivated = false
            it.text = "Off"
            it.textSize = sX*0.5f
            it.setTextColor(resources.getColor(R.color.colorAccent))
            it.setBackgroundResource(R.drawable.flashlight_off)
            it.measure(0, 0)
            rl_flash_activity.addView(button)
            it.layoutParams.width = btnWidth
            it.layoutParams.height = btnWidth
            it.x = sX*50 - btnWidth/2
            it.y = sY*48 - btnWidth/2
        }

        textView = TextView(this)

        textView.let {
            it.text = "Power: ${battery()}%"
            it.textSize = 15f
            it.typeface = Typeface.DEFAULT_BOLD
            it.setTextColor(resources.getColor(R.color.colorAccent))
            it.measure(0,0)
            it.x = sX*50 - textView.measuredWidth/2
            it.y = sY*5
            rl_flash_activity.addView(textView)
        }

        texture = SurfaceTexture(1)

        rl_flash_activity.setBackgroundResource(R.color.colorDark)

        Timer().schedule(0, 1000){
            this@FlashActivity.runOnUiThread {
                textView.text = "Power: ${battery()}%"
            }
        }
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

            val characteristics = camManager.getCameraCharacteristics(cameraBackId)
            val configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            for (i in 0 until  configs.getOutputSizes(ImageFormat.JPEG).size){
                if(i == configs.getOutputSizes(ImageFormat.JPEG).size - 1){
                    val size = configs.getOutputSizes(ImageFormat.JPEG)[i]
                    texture.setDefaultBufferSize(size.width, size.height)
                }
            }
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
                Toasty.error(this, "permission is not granded", Toast.LENGTH_SHORT).show()
            }
        }
    }

//    var i = 0
//
//    override fun onResume() {
//        super.onResume()
//        if(i >0){
//            init()
//        }
//        i++
//    }
//
//    override fun onPause() {
//        super.onPause()
//        camDevice.close()
//    }
}
