package com.codewithkael.webrtcscreenshare.ui

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.codewithkael.webrtcscreenshare.R
import com.codewithkael.webrtcscreenshare.databinding.ActivityMainBinding
import com.codewithkael.webrtcscreenshare.repository.MainRepository
import com.codewithkael.webrtcscreenshare.service.WebrtcService
import com.codewithkael.webrtcscreenshare.service.WebrtcServiceRepository
import com.jenorg.webrtcscreenshare.utils.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.MediaStream
import org.webrtc.RTCStats
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainRepository.Listener {

    private var username:String?=null
    lateinit var views:ActivityMainBinding
    
    lateinit var notificationManager: NotificationManager
    @Inject lateinit var context: Context
    private val USAGE_STATS_INTERVAL_IN_MILLI = 60000L // Update interval (e.g., 1 minute)
    private val REQUEST_CODE_USAGE_ACCESS = 101
    
    @Inject lateinit var webrtcServiceRepository: WebrtcServiceRepository
    private val capturePermissionRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views= ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        init()

    }

    private fun init(){
        username = intent.getStringExtra("username")
        if (username.isNullOrEmpty()){
            finish()
        }
        
        requestUsageAccess()
        
        WebrtcService.surfaceView = views.surfaceView
        WebrtcService.listener = this
        webrtcServiceRepository.startIntent(username!!)
        views.requestBtn.setOnClickListener {
            startScreenCapture()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != capturePermissionRequestCode) return
        WebrtcService.screenPermissionIntent = data
        webrtcServiceRepository.requestConnection(
            views.targetEt.text.toString()
        )
    }

    private fun startScreenCapture(){
        val mediaProjectionManager = application.getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(), capturePermissionRequestCode
        )
    }

    override fun onConnectionRequestReceived(target: String) {
        runOnUiThread{
            views.apply {
                notificationTitle.text = "$target is requesting for connection"
                notificationLayout.isVisible = true
                notificationAcceptBtn.setOnClickListener {
                    webrtcServiceRepository.acceptCAll(target)
                    notificationLayout.isVisible = false
                }
                notificationDeclineBtn.setOnClickListener {
                    notificationLayout.isVisible = false
                }
            }
        }
    }

    override fun onConnectionConnected() {
        runOnUiThread {
            views.apply {
                requestLayout.isVisible = false
                disconnectBtn.isVisible = true
                disconnectBtn.setOnClickListener {
                    webrtcServiceRepository.endCallIntent()
                    restartUi()
                }
            }
        }
    }

    override fun onCallEndReceived() {
        runOnUiThread {
            restartUi()
        }
    }

    override fun onRemoteStreamAdded(stream: MediaStream) {
        runOnUiThread {
            views.surfaceView.isVisible = true
            stream.videoTracks[0].addSink(views.surfaceView)
        }
    }
    
    private fun requestUsageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, request it
                requestPermissions(arrayOf(android.Manifest.permission.PACKAGE_USAGE_STATS), 101)
                Toast.makeText(this, "Requesting Usage Access Permission", Toast.LENGTH_SHORT).show()
            } else {
                // Permission already granted, proceed with monitoring
                startMonitoringForegroundApp()
                Toast.makeText(this, "Usage Access Permission Granted", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Usage Access API not available on older devices
            Toast.makeText(this, "Usage Access not supported", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_USAGE_ACCESS) {
            // Check if permission is granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with monitoring
                startMonitoringForegroundApp()
                Toast.makeText(this, "Usage Access Permission Granted", LENGTH_SHORT).show()
            } else {
                // Permission denied, inform user and potentially request again
                Toast.makeText(this, "Usage Access Permission required for monitoring foreground apps.", LENGTH_SHORT).show()
                shouldShowRequestPermissionRationale(android.Manifest.permission.PACKAGE_USAGE_STATS).let {
                    if (it) {
                        // User denied but can still be persuaded, explain again
                        requestUsageAccess() // Request permission again
                    } else {
                        // User selected "Don't ask again", navigate to app settings
                        Toast.makeText(this, "Please grant Usage Access Permission in App Settings.", LENGTH_SHORT).show()
                        //  You can include an Intent to open App Settings here
                    }
                }
            }
        }
    }
    
    private fun startMonitoringForegroundApp() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - USAGE_STATS_INTERVAL_IN_MILLI
        
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST, beginTime, endTime
        )
        
        if (usageStatsList.isNotEmpty()) {
            val recentUsageStat = usageStatsList[0]
            val foregroundPackageName = recentUsageStat.packageName
            
            // Check if foregroundPackageName is "com.whatsapp"
            if (foregroundPackageName == "com.whatsapp") {
                // WhatsApp is in use
                Toast.makeText(this, "WhatsApp is in use", Toast.LENGTH_SHORT).show()
                // Update UI or perform relevant actions
            } else {
                // Another app is in the foreground
                Toast.makeText(this, "Another app is in use", Toast.LENGTH_SHORT).show()
            }
        } else {
            // No recent usage stats available
            Toast.makeText(this, "No recent usage data", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun restartUi(){
        views.apply {
            disconnectBtn.isVisible=false
            requestLayout.isVisible = true
            notificationLayout.isVisible = false
            surfaceView.isVisible = false
        }
    }

}