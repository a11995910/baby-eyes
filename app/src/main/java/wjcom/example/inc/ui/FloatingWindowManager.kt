package wjcom.example.inc.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import wjcom.example.inc.R
import wjcom.example.inc.utils.SharedPrefsManager

class FloatingWindowManager(private val context: Context) {
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefsManager = SharedPrefsManager(context)
    
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
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        
        // 设置警告信息
        val textWarning = warningOverlay!!.findViewById<TextView>(R.id.text_warning)
        textWarning.text = message
        
        // 设置确认按钮
        val btnKnow = warningOverlay!!.findViewById<Button>(R.id.btn_know)
        btnKnow.setOnClickListener {
            hideWarning()
        }
        
        // 5秒后自动消失
        warningOverlay!!.postDelayed({
            hideWarning()
        }, 5000)
        
        windowManager.addView(warningOverlay, params)
    }
    
    fun hideWarning() {
        warningOverlay?.let {
            windowManager.removeView(it)
            warningOverlay = null
        }
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
    }
}