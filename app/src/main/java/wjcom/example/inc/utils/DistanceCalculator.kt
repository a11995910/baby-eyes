package wjcom.example.inc.utils

import kotlin.math.sqrt

/**
 * 距离计算器
 * 基于人脸检测的眼间距离计算用户与屏幕的距离
 */
class DistanceCalculator {
    
    companion object {
        // 平均人眼间距（厘米）
        private const val AVERAGE_IPD_CM = 6.3f
        
        // 默认焦距（像素）- 这个值会根据实际设备调整
        private const val DEFAULT_FOCAL_LENGTH_PX = 1000f
    }
    
    /**
     * 计算距离
     * @param eyeDistancePx 检测到的眼间距离（像素）
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @return 距离（厘米）
     */
    fun calculateDistance(
        eyeDistancePx: Float,
        imageWidth: Int,
        imageHeight: Int
    ): Float {
        if (eyeDistancePx <= 0) return Float.MAX_VALUE
        
        // 使用基本的针孔相机模型计算距离
        // distance = (focal_length * real_size) / pixel_size
        val focalLength = estimateFocalLength(imageWidth, imageHeight)
        val distance = (focalLength * AVERAGE_IPD_CM) / eyeDistancePx
        
        // 限制距离范围在合理区间内（10cm - 200cm）
        return distance.coerceIn(10f, 200f)
    }
    
    /**
     * 估算焦距
     * 基于图像分辨率估算相机焦距
     */
    private fun estimateFocalLength(imageWidth: Int, imageHeight: Int): Float {
        // 这是一个简化的焦距估算方法
        // 实际应用中应该从相机参数中获取
        val diagonalPixels = sqrt((imageWidth * imageWidth + imageHeight * imageHeight).toFloat())
        
        // 假设视场角大约为 70 度（前置摄像头的典型值）
        val fovRadians = Math.toRadians(70.0)
        val focalLength = (diagonalPixels / 2) / kotlin.math.tan(fovRadians / 2).toFloat()
        
        return focalLength.coerceIn(500f, 2000f) // 限制在合理范围内
    }
    
    /**
     * 计算两点之间的像素距离
     */
    fun calculatePixelDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * 根据人脸边框估算眼间距离
     * 当检测不到眼部特征点时的备用方法
     */
    fun estimateEyeDistanceFromFaceBounds(faceWidth: Float): Float {
        // 眼间距离大约是人脸宽度的 30%
        return faceWidth * 0.3f
    }
    
    /**
     * 使用人脸宽度计算距离的备用方法
     * 当检测不到眼部特征点时使用
     */
    fun calculateDistanceFromFace(
        faceWidth: Float,
        imageWidth: Int,
        imageHeight: Int
    ): Float {
        if (faceWidth <= 0) return Float.MAX_VALUE
        
        // 估算眼间距离，然后使用标准计算方法
        val estimatedEyeDistance = estimateEyeDistanceFromFaceBounds(faceWidth)
        return calculateDistance(estimatedEyeDistance, imageWidth, imageHeight)
    }
}