package wjcom.example.inc.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * 音频录制和播放管理器
 */
class AudioRecorderManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioRecorderManager"
        private const val AUDIO_FILE_NAME = "voice_warning.3gp"
        private const val MAX_RECORDING_DURATION = 4000L // 4秒
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var isPlaying = false
    
    private val audioFile: File by lazy {
        File(context.filesDir, AUDIO_FILE_NAME)
    }
    
    /**
     * 开始录音
     */
    fun startRecording(): Boolean {
        return try {
            if (isRecording) {
                Log.w(TAG, "录音已在进行中")
                return false
            }
            
            // 停止可能正在播放的音频
            stopPlaying()
            
            // 删除之前的录音文件
            if (audioFile.exists()) {
                audioFile.delete()
            }
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile.absolutePath)
                setMaxDuration(MAX_RECORDING_DURATION.toInt())
                
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        Log.d(TAG, "达到最大录音时长，自动停止")
                        stopRecording()
                    }
                }
                
                prepare()
                start()
            }
            
            isRecording = true
            Log.d(TAG, "开始录音")
            true
        } catch (e: Exception) {
            Log.e(TAG, "启动录音失败", e)
            releaseRecorder()
            false
        }
    }
    
    /**
     * 停止录音
     */
    fun stopRecording(): Boolean {
        return try {
            if (!isRecording) {
                Log.w(TAG, "没有正在进行的录音")
                return false
            }
            
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            Log.d(TAG, "录音停止，文件大小: ${audioFile.length()} bytes")
            true
        } catch (e: Exception) {
            Log.e(TAG, "停止录音失败", e)
            releaseRecorder()
            false
        }
    }
    
    /**
     * 播放录音
     */
    fun playRecording(): Boolean {
        return try {
            if (!hasRecording()) {
                Log.w(TAG, "没有录音文件")
                return false
            }
            
            if (isPlaying) {
                Log.w(TAG, "音频已在播放中")
                return false
            }
            
            // 停止可能正在进行的录音
            if (isRecording) {
                stopRecording()
            }
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setOnCompletionListener {
                    this@AudioRecorderManager.isPlaying = false
                    Log.d(TAG, "音频播放完成")
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "音频播放错误: what=$what, extra=$extra")
                    this@AudioRecorderManager.isPlaying = false
                    true
                }
                prepare()
                start()
            }
            
            isPlaying = true
            Log.d(TAG, "开始播放录音")
            true
        } catch (e: Exception) {
            Log.e(TAG, "播放录音失败", e)
            releasePlayer()
            false
        }
    }
    
    /**
     * 停止播放
     */
    fun stopPlaying() {
        try {
            if (isPlaying && mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
                Log.d(TAG, "停止播放")
            }
            releasePlayer()
        } catch (e: Exception) {
            Log.e(TAG, "停止播放失败", e)
            releasePlayer()
        }
    }
    
    /**
     * 检查是否有录音文件
     */
    fun hasRecording(): Boolean {
        return audioFile.exists() && audioFile.length() > 0
    }
    
    /**
     * 获取录音文件路径
     */
    fun getRecordingPath(): String? {
        return if (hasRecording()) audioFile.absolutePath else null
    }
    
    /**
     * 删除录音文件
     */
    fun deleteRecording(): Boolean {
        stopRecording()
        stopPlaying()
        return try {
            if (audioFile.exists()) {
                audioFile.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除录音文件失败", e)
            false
        }
    }
    
    /**
     * 释放所有资源
     */
    fun release() {
        stopRecording()
        stopPlaying()
    }
    
    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "释放录音器失败", e)
        }
        mediaRecorder = null
        isRecording = false
    }
    
    private fun releasePlayer() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "释放播放器失败", e)
        }
        mediaPlayer = null
        isPlaying = false
    }
    
    /**
     * 当前是否正在录音
     */
    fun isRecording(): Boolean = this.isRecording
    
    /**
     * 当前是否正在播放
     */
    fun isPlaying(): Boolean = this.isPlaying
}