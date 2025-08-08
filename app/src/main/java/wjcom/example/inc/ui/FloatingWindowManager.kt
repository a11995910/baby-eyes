package wjcom.example.inc.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import wjcom.example.inc.R
import wjcom.example.inc.utils.AudioRecorderManager
import wjcom.example.inc.utils.SharedPrefsManager

class FloatingWindowManager(private val context: Context) {
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefsManager = SharedPrefsManager(context)
    private val audioRecorderManager = AudioRecorderManager(context)
    
    private var floatingBall: View? = null
    private var settingsPanel: View? = null
    private var warningOverlay: View? = null
    
    private var onActionListener: OnActionListener? = null
    
    interface OnActionListener {
        fun onStopDetection()
        fun onStartDetection()
        fun onFloatingBallRemoved()
        fun onDetectionStateChanged(isActive: Boolean)
    }
    
    fun setOnActionListener(listener: OnActionListener) {
        this.onActionListener = listener
    }
    
    fun showFloatingBall() {
        if (floatingBall != null) return
        
        val layoutInflater = LayoutInflater.from(context)
        floatingBall = layoutInflater.inflate(R.layout.floating_ball, null)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 200
        
        // 设置点击事件
        floatingBall?.setOnClickListener {
            showSettingsPanel()
        }
        
        // 设置长按事件
        floatingBall?.setOnLongClickListener {
            showRemoveDialog()
            true
        }
        
        // 设置拖拽
        setupDragListener(floatingBall!!, params)
        
        windowManager.addView(floatingBall, params)
    }
    
    fun hideFloatingBall() {
        floatingBall?.let {
            windowManager.removeView(it)
            floatingBall = null
        }
    }
    
    fun showSettingsPanel() {
        if (settingsPanel != null) return
        
        val layoutInflater = LayoutInflater.from(context)
        settingsPanel = layoutInflater.inflate(R.layout.settings_panel, null)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.CENTER
        // 放宽设置面板宽度到屏幕宽度的 90%
        val screenWidth = context.resources.displayMetrics.widthPixels
        params.width = (screenWidth * 0.9f).toInt()
        
        // 初始化设置面板控件
        setupSettingsPanel(settingsPanel!!)
        
        windowManager.addView(settingsPanel, params)
    }
    
    private fun setupSettingsPanel(panel: View) {
        // 检测开关
        val switchDetection = panel.findViewById<Switch>(R.id.switch_detection)
        switchDetection.isChecked = true // 默认开启
        switchDetection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                onActionListener?.onStartDetection()
            } else {
                onActionListener?.onStopDetection()
            }
            // 通知状态变化
            onActionListener?.onDetectionStateChanged(isChecked)
        }
        
        // 距离阈值设置
        val seekBarDistance = panel.findViewById<SeekBar>(R.id.seekbar_threshold)
        val textDistance = panel.findViewById<TextView>(R.id.text_threshold)
        
        val currentThreshold = prefsManager.getDistanceThreshold()
        seekBarDistance.progress = (currentThreshold - 20).toInt() // SeekBar从0开始，阈值从20开始
        textDistance.text = "${currentThreshold}cm"
        
        seekBarDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val distance = progress + 20 // 20-50cm范围
                textDistance.text = "${distance}cm"
                prefsManager.setDistanceThreshold(distance)
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 自定义提醒语
        val editWarningMessage = panel.findViewById<EditText>(R.id.edit_warning_message)
        editWarningMessage.setText(prefsManager.getWarningMessage())
        editWarningMessage.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                prefsManager.setWarningMessage(editWarningMessage.text.toString())
            }
        }
        
        // 语音播报设置
        val switchVoiceWarning = panel.findViewById<Switch>(R.id.switch_voice_warning)
        val layoutVoiceRecording = panel.findViewById<LinearLayout>(R.id.layout_voice_recording)
        val btnRecordVoice = panel.findViewById<Button>(R.id.btn_record_voice)
        val btnPlayVoice = panel.findViewById<Button>(R.id.btn_play_voice)
        
        // 初始化语音播报开关状态
        switchVoiceWarning.isChecked = prefsManager.isVoiceWarningEnabled()
        layoutVoiceRecording.visibility = if (switchVoiceWarning.isChecked) View.VISIBLE else View.GONE
        
        // 更新试听按钮状态
        updatePlayButtonState(btnPlayVoice)
        
        // 语音播报开关监听
        switchVoiceWarning.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setVoiceWarningEnabled(isChecked)
            layoutVoiceRecording.visibility = if (isChecked) View.VISIBLE else View.GONE
            
            if (!isChecked) {
                // 关闭语音播报时停止可能正在进行的录音或播放
                audioRecorderManager.release()
            }
        }
        
        // 录音按钮长按监听
        btnRecordVoice.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (checkAudioPermission()) {
                        startRecording(btnRecordVoice, btnPlayVoice)
                    } else {
                        showAudioPermissionDialog()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopRecording(btnRecordVoice, btnPlayVoice)
                    true
                }
                else -> false
            }
        }
        
        // 试听按钮点击监听
        btnPlayVoice.setOnClickListener {
            if (audioRecorderManager.isPlaying()) {
                audioRecorderManager.stopPlaying()
                btnPlayVoice.text = "试听"
            } else {
                if (audioRecorderManager.playRecording()) {
                    btnPlayVoice.text = "停止"
                    // 播放完成后更新按钮状态
                    Handler(Looper.getMainLooper()).postDelayed({
                        btnPlayVoice.text = "试听"
                    }, 4000) // 最长4秒
                }
            }
        }
        
        // 关闭按钮
        val btnCancel = panel.findViewById<Button>(R.id.btn_cancel)
        val btnConfirm = panel.findViewById<Button>(R.id.btn_confirm)
        
        btnCancel.setOnClickListener {
            hideSettingsPanel()
        }
        
        btnConfirm.setOnClickListener {
            // 保存设置并关闭
            val warningMessage = editWarningMessage.text.toString()
            prefsManager.setWarningMessage(warningMessage)
            hideSettingsPanel()
        }
    }
    
    fun hideSettingsPanel() {
        settingsPanel?.let {
            windowManager.removeView(it)
            settingsPanel = null
        }
    }
    
    fun showWarning(message: String) {
        if (warningOverlay != null) return
        
        val layoutInflater = LayoutInflater.from(context)
        warningOverlay = layoutInflater.inflate(R.layout.warning_overlay, null)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            // 不允许触摸透传到后面的窗口，用户必须点击确认区关闭
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        // 显示 PNG 弹框背景
        val imageView = warningOverlay!!.findViewById<ImageView>(R.id.image_popup)
        imageView?.setImageResource(R.drawable.tankuang)

        // 设置警告信息或喇叭图标
        warningOverlay!!.findViewById<TextView>(R.id.text_warning)?.let { textView ->
            if (prefsManager.isVoiceWarningEnabled() && audioRecorderManager.hasRecording()) {
                // 启用语音播报且有录音文件时显示喇叭图标
                textView.text = "🔊"
                textView.textSize = 32f
                
                // 播放录音
                audioRecorderManager.playRecording()
            } else {
                // 显示文字消息
                textView.text = message
                textView.textSize = 16f
            }
        }
        // 设置确认点击区域（图片按钮）或兼容旧按钮
        warningOverlay!!.findViewById<View>(R.id.btn_image_confirm)?.setOnClickListener {
            hideWarning()
        }
        // 兼容旧布局：如果存在名为 btn_know 的资源，则注册点击关闭
        val btnKnowId = context.resources.getIdentifier("btn_know", "id", context.packageName)
        if (btnKnowId != 0) {
            warningOverlay!!.findViewById<View>(btnKnowId)?.setOnClickListener { hideWarning() }
        }
        
        // 移除自动消失，必须用户点击后才关闭
        
        windowManager.addView(warningOverlay, params)
    }
    
    fun hideWarning() {
        warningOverlay?.let {
            windowManager.removeView(it)
            warningOverlay = null
        }
        // 停止播放音频
        audioRecorderManager.stopPlaying()
    }
    
    private fun showRemoveDialog() {
        val layoutInflater = LayoutInflater.from(context)
        val dialogView = layoutInflater.inflate(R.layout.remove_dialog, null)
        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        
        // 设置窗口类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_PHONE)
        }
        
        val btnRemove = dialogView.findViewById<Button>(R.id.btn_remove)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel_remove)
        
        btnRemove.setOnClickListener {
            hideFloatingBall()
            onActionListener?.onFloatingBallRemoved()
            dialog.dismiss()
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun setupDragListener(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.performClick()
                    true
                }
                else -> false
            }
        }
    }
    
    fun cleanup() {
        hideFloatingBall()
        hideSettingsPanel()
        hideWarning()
        audioRecorderManager.release()
    }
    
    // 录音相关辅助方法
    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun showAudioPermissionDialog() {
        Toast.makeText(context, "需要录音权限才能录制语音提醒", Toast.LENGTH_SHORT).show()
    }
    
    private fun startRecording(recordBtn: Button, playBtn: Button) {
        if (audioRecorderManager.startRecording()) {
            recordBtn.text = "录音中..."
            recordBtn.isEnabled = false
            playBtn.isEnabled = false
        } else {
            Toast.makeText(context, "开始录音失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopRecording(recordBtn: Button, playBtn: Button) {
        if (audioRecorderManager.isRecording()) {
            if (audioRecorderManager.stopRecording()) {
                recordBtn.text = "长按录音（最长4秒）"
                recordBtn.isEnabled = true
                updatePlayButtonState(playBtn)
                Toast.makeText(context, "录音完成", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "停止录音失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updatePlayButtonState(playBtn: Button) {
        playBtn.isEnabled = audioRecorderManager.hasRecording()
        playBtn.text = if (audioRecorderManager.isPlaying()) "停止" else "试听"
    }
}