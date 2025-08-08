package wjcom.example.inc

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.snackbar.Snackbar
import wjcom.example.inc.databinding.ActivityMainBinding
import wjcom.example.inc.service.EyeProtectionService
import wjcom.example.inc.ui.FloatingWindowManager
import wjcom.example.inc.utils.SharedPrefsManager

class MainActivity : AppCompatActivity(), FloatingWindowManager.OnActionListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: SharedPrefsManager
    private lateinit var floatingWindowManager: FloatingWindowManager
    
    // 权限请求回调
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkOverlayPermission()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    // 悬浮窗权限请求回调
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            onPermissionsGranted()
        } else {
            showOverlayPermissionDeniedDialog()
        }
    }
    
    // 接收警告蒙层显示广播
    private val warningReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.eyeprotection.SHOW_WARNING" -> {
                    val message = intent.getStringExtra("message") ?: "离手机太近，注意护眼！"
                    floatingWindowManager.showWarning(message)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefsManager = SharedPrefsManager(this)
        floatingWindowManager = FloatingWindowManager(this)
        floatingWindowManager.setOnActionListener(this)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // 隐藏原来的悬浮按钮，因为现在在Fragment中处理
        binding.fab.hide()
        
        // 注册广播接收器
        val filter = IntentFilter().apply {
            addAction("com.eyeprotection.SHOW_WARNING")
        }
        registerReceiver(warningReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        
        // 首次启动显示欢迎对话框
        if (prefsManager.isFirstLaunch()) {
            showWelcomeDialog()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(warningReceiver)
        floatingWindowManager.cleanup()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                floatingWindowManager.showSettingsPanel()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
    
    private fun showWelcomeDialog() {
        AlertDialog.Builder(this)
            .setTitle("欢迎使用宝宝护眼卫士")
            .setMessage("这是一款保护您眼睛健康的应用。\n\n使用前需要授权摄像头和悬浮窗权限。")
            .setPositiveButton("开始使用") { _, _ ->
                prefsManager.setFirstLaunch(false)
                checkPermissions()
            }
            .setNegativeButton("稍后") { _, _ ->
                prefsManager.setFirstLaunch(false)
            }
            .setCancelable(false)
            .show()
    }
    
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            checkOverlayPermission()
        }
    }
    
    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            onPermissionsGranted()
        }
    }
    
    private fun onPermissionsGranted() {
        prefsManager.setPermissionsGranted(true)
        Snackbar.make(binding.root, "权限授权完成，可以开始使用护眼功能", Snackbar.LENGTH_LONG).show()
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限被拒绝")
            .setMessage("摄像头权限是护眼功能的必需权限，请在设置中手动授权。")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showOverlayPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限被拒绝")
            .setMessage("悬浮窗权限是显示护眼提醒的必需权限，请在设置中手动授权。")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    // 供Fragment调用的权限检查方法
    fun checkAppPermissions() {
        checkPermissions()
    }
    
    // 启动护眼检测服务
    private fun startEyeProtectionService() {
        val intent = Intent(this, EyeProtectionService::class.java).apply {
            action = "ACTION_START_DETECTION"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    // 停止护眼检测服务
    private fun stopEyeProtectionService() {
        val intent = Intent(this, EyeProtectionService::class.java).apply {
            action = "ACTION_STOP_DETECTION"
        }
        startService(intent)
    }
    
    // FloatingWindowManager.OnActionListener 实现
    override fun onStopDetection() {
        stopEyeProtectionService()
    }
    
    override fun onStartDetection() {
        startEyeProtectionService()
    }
    
    override fun onFloatingBallRemoved() {
        stopEyeProtectionService()
    }
    
    override fun onDetectionStateChanged(isActive: Boolean) {
        // 可以在这里添加全局状态更新逻辑
    }
}