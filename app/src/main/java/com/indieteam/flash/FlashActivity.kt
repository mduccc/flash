package com.example.root.flash

import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flash)
        getScreen()
        setUI()
        init()
    }

    private fun init(){
        camManager = this.getSystemService(android.content.Context.CAMERA_SERVICE) as CameraManager
        openCamera()
    }

    private fun openCamera(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CAMERA)
        }else{
            if(getListCamera().lastIndex > 0)
                try {
                    camManager.openCamera(getListCamera()[0], OpenCallback(), null)
                }catch(e: Exception){
                    Toasty.error(this, "This device is not suport camera2 api")
                }
            else
                Toast.makeText(this, "this device is not support", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getListCamera(): List<String> {
        var list = listOf<String>()
        for (i in camManager.cameraIdList){
            list += i
        }
        return list
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
            it.textSize = sX*1.5f
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
