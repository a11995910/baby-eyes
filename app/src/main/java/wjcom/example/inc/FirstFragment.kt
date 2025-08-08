package wjcom.example.inc

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import wjcom.example.inc.databinding.FragmentFirstBinding
import wjcom.example.inc.service.EyeProtectionService
import wjcom.example.inc.ui.FloatingWindowManager
import wjcom.example.inc.utils.SharedPrefsManager

class FirstFragment : Fragment(), FloatingWindowManager.OnActionListener {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var floatingWindowManager: FloatingWindowManager
    private lateinit var prefsManager: SharedPrefsManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefsManager = SharedPrefsManager(requireContext())
        floatingWindowManager = FloatingWindowManager(requireContext())
        floatingWindowManager.setOnActionListener(this)

        // 使用说明按钮
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
        
        // 护眼检测开关
        binding.switchProtection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (prefsManager.arePermissionsGranted()) {
                    startEyeProtectionService()
                    floatingWindowManager.showFloatingBall()
                    binding.textDetectionStatus.text = "运行中"
                    Snackbar.make(view, "护眼检测已启动", Snackbar.LENGTH_SHORT).show()
                } else {
                    binding.switchProtection.isChecked = false // 重置开关
                    (activity as? MainActivity)?.let { mainActivity ->
                        mainActivity.checkAppPermissions()
                    }
                }
            } else {
                stopEyeProtectionService()
                floatingWindowManager.hideFloatingBall()
                binding.textDetectionStatus.text = "已关闭"
                Snackbar.make(view, "护眼检测已停止", Snackbar.LENGTH_SHORT).show()
            }
        }
        
        // 设置按钮
        binding.btnSettings.setOnClickListener {
            floatingWindowManager.showSettingsPanel()
        }
    }
    
    private fun startEyeProtectionService() {
        try {
            val intent = Intent(requireContext(), EyeProtectionService::class.java)
            intent.action = EyeProtectionService.ACTION_START_DETECTION
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent)
            } else {
                requireContext().startService(intent)
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "启动服务失败: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }
    
    private fun stopEyeProtectionService() {
        val intent = Intent(requireContext(), EyeProtectionService::class.java)
        intent.action = EyeProtectionService.ACTION_STOP_DETECTION
        requireContext().startService(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // FloatingWindowManager.OnActionListener implementation
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
        // 同步首页开关状态
        view?.let {
            binding.switchProtection.isChecked = isActive
            binding.textDetectionStatus.text = if (isActive) "运行中" else "已关闭"
        }
    }
}