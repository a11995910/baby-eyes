package wjcom.example.inc.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Lifecycle
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import wjcom.example.inc.R
import wjcom.example.inc.utils.DistanceCalculator
import wjcom.example.inc.utils.SharedPrefsManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class EyeProtectionService : Service(), LifecycleOwner {
    
    companion object {
        private const val TAG = "EyeProtectionService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "eye_protection_channel"
        
        const val ACTION_START_DETECTION = "ACTION_START_DETECTION"
        const val ACTION_STOP_DETECTION = "ACTION_STOP_DETECTION"
        const val ACTION_TOGGLE_DETECTION = "ACTION_TOGGLE_DETECTION"
    }
    
    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var distanceCalculator: DistanceCalculator
    private lateinit var prefsManager: SharedPrefsManager
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null
    
    private var isDetecting = false
    private var consecutiveCloseFrames = 0
    private val maxConsecutiveFrames = 5 // 连续检测到距离过近的帧数阈值
    
    // ML Kit人脸检测器
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setMinFaceSize(0.1f)
            .enableTracking()
            .build()
    )
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        cameraExecutor = Executors.newSingleThreadExecutor()

        distanceCalculator = DistanceCalculator()
        prefsManager = SharedPrefsManager(this)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_DETECTION -> {
                if (!isDetecting) {
                    startDetection()
                }
            }
            ACTION_STOP_DETECTION -> {
                stopDetection()
            }
            ACTION_TOGGLE_DETECTION -> {
                if (isDetecting) {
                    stopDetection()
                } else {
                    startDetection()
                }
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    
    private fun startDetection() {
        Log.d(TAG, "启动检测")
        
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
                isDetecting = true
                Log.d(TAG, "检测已启动")
            } catch (e: Exception) {
                Log.e(TAG, "启动相机失败", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        
        // 解绑之前的用例
        cameraProvider.unbindAll()
        
        // 配置前置摄像头
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        
        // 配置图像分析
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy)
                }
            }
        
        try {
            // 绑定用例到相机
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                imageAnalysis
            )
            Log.d(TAG, "相机绑定成功")
        } catch (e: Exception) {
            Log.e(TAG, "绑定相机用例失败", e)
        }
    }
    
    @SuppressLint("UnsafeOptInUsageError")
    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }
        
        // 如果检测已暂停，直接返回
        if (!isDetecting) {
            imageProxy.close()
            return
        }
        
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        
        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0] // 取第一个检测到的人脸
                    
                    // 尝试使用眼部特征点计算距离
                    val leftEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)
                    val rightEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)
                    
                    val distance = if (leftEye != null && rightEye != null) {
                        // 使用眼部特征点计算距离
                        val eyeDistance = abs(leftEye.position.x - rightEye.position.x)
                        distanceCalculator.calculateDistance(
                            eyeDistance,
                            imageProxy.width,
                            imageProxy.height
                        )
                    } else {
                        // 备用方案：使用面部边界框
                        val faceWidth = face.boundingBox.width().toFloat()
                        distanceCalculator.calculateDistanceFromFace(
                            faceWidth,
                            imageProxy.width,
                            imageProxy.height
                        )
                    }
                    
                    Log.d(TAG, "检测到人脸，距离: ${distance}cm")
                    
                    // 检查距离是否过近
                    val threshold = prefsManager.getDistanceThreshold()
                    if (distance < threshold) {
                        consecutiveCloseFrames++
                        Log.d(TAG, "距离过近，连续帧数: $consecutiveCloseFrames")
                        
                        if (consecutiveCloseFrames >= maxConsecutiveFrames) {
                            showDistanceWarning()
                            consecutiveCloseFrames = 0 // 重置计数器
                        }
                    } else {
                        consecutiveCloseFrames = 0 // 重置计数器
                    }
                } else {
                    Log.d(TAG, "未检测到人脸")
                    consecutiveCloseFrames = 0 // 重置计数器
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "人脸检测失败", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
    
    private fun stopDetection() {
        Log.d(TAG, "停止检测")
        isDetecting = false
        consecutiveCloseFrames = 0
        
        cameraProvider?.unbindAll()
        stopSelf()
    }
    
    private fun showDistanceWarning() {
        Log.d(TAG, "显示距离警告")
        
        // 暂停检测避免反复弹窗
        isDetecting = false
        
        val warningIntent = Intent("com.eyeprotection.SHOW_WARNING").apply {
            putExtra("message", prefsManager.getWarningMessage())
            setPackage(packageName)
        }
        sendBroadcast(warningIntent)
        
        // 10秒后自动恢复检测
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (cameraProvider != null) {
                isDetecting = true
                Log.d(TAG, "自动恢复检测")
            }
        }, 10000)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        isDetecting = false
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()

        faceDetector.close()
        
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "护眼检测"
            val descriptionText = "实时监测观看距离，保护眼部健康"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, wjcom.example.inc.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("宝宝护眼卫士正在运行")
            .setContentText("正在保护您的眼部健康")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}