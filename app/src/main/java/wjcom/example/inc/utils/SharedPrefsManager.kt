package wjcom.example.inc.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * 共享首选项管理器
 * 管理应用的设置和配置
 */
class SharedPrefsManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "eye_protection_prefs"
        private const val KEY_DISTANCE_THRESHOLD = "distance_threshold"
        private const val KEY_WARNING_MESSAGE = "warning_message"
        private const val KEY_DETECTION_ENABLED = "detection_enabled"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_PERMISSIONS_GRANTED = "permissions_granted"
        
        // 默认值
        private const val DEFAULT_DISTANCE_THRESHOLD = 30f
        private const val DEFAULT_WARNING_MESSAGE = "离手机太近，注意护眼！"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 获取距离阈值（厘米）
     */
    fun getDistanceThreshold(): Float {
        return prefs.getFloat(KEY_DISTANCE_THRESHOLD, DEFAULT_DISTANCE_THRESHOLD)
    }
    
    /**
     * 设置距离阈值
     */
    fun setDistanceThreshold(threshold: Float) {
        prefs.edit().putFloat(KEY_DISTANCE_THRESHOLD, threshold).apply()
    }
    
    /**
     * 设置距离阈值（整型）
     */
    fun setDistanceThreshold(threshold: Int) {
        setDistanceThreshold(threshold.toFloat())
    }
    
    /**
     * 获取警告消息
     */
    fun getWarningMessage(): String {
        return prefs.getString(KEY_WARNING_MESSAGE, DEFAULT_WARNING_MESSAGE) ?: DEFAULT_WARNING_MESSAGE
    }
    
    /**
     * 设置警告消息
     */
    fun setWarningMessage(message: String) {
        prefs.edit().putString(KEY_WARNING_MESSAGE, message).apply()
    }
    
    /**
     * 获取检测是否启用
     */
    fun isDetectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_DETECTION_ENABLED, false)
    }
    
    /**
     * 设置检测启用状态
     */
    fun setDetectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DETECTION_ENABLED, enabled).apply()
    }
    
    /**
     * 是否首次启动
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    /**
     * 设置首次启动状态
     */
    fun setFirstLaunch(isFirst: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, isFirst).apply()
    }
    
    /**
     * 获取权限是否已授权
     */
    fun arePermissionsGranted(): Boolean {
        return prefs.getBoolean(KEY_PERMISSIONS_GRANTED, false)
    }
    
    /**
     * 设置权限授权状态
     */
    fun setPermissionsGranted(granted: Boolean) {
        prefs.edit().putBoolean(KEY_PERMISSIONS_GRANTED, granted).apply()
    }
}