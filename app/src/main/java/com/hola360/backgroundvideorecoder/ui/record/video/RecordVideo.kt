package com.hola360.backgroundvideorecoder.ui.record.video

import android.os.Bundle
import android.view.OrientationEventListener
import android.view.View
import com.hola360.backgroundvideorecoder.MainActivity
import com.hola360.backgroundvideorecoder.R
import com.hola360.backgroundvideorecoder.databinding.LayoutRecordVideoBinding
import com.hola360.backgroundvideorecoder.service.RecordService
import com.hola360.backgroundvideorecoder.ui.record.video.base.BaseRecordVideoFragment
import com.hola360.backgroundvideorecoder.utils.VideoRecordUtils

class RecordVideo : BaseRecordVideoFragment<LayoutRecordVideoBinding>(), View.OnClickListener, RecordService.Listener {

    override val layoutId: Int = R.layout.layout_record_video
    private var orientationAngle= 0
    private val orientationListener: OrientationEventListener by lazy {
        object : OrientationEventListener(requireContext()){
            override fun onOrientationChanged(orientation: Int) {
                orientationAngle= orientation
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity() as MainActivity).recordService!!.getRecordState().observe(this) {
            when (it) {
                RecordService.RecordState.VideoRecording -> {
                    binding!!.isRecording = true
                }
                else -> {
                    binding!!.isRecording = false
                    binding!!.recordTime.text = getString(R.string.video_record_time_zero)
                }
            }
        }
        orientationListener.enable()
    }

    override fun initView() {
        applyNewVideoConfiguration()
        binding!!.camera.setOnClickListener(this)
        binding!!.recordDuration.setOnClickListener(this)
        binding!!.intervalTime.setOnClickListener(this)
        binding!!.previewMode.setOnClickListener(this)
        binding!!.flash.setOnClickListener(this)
        binding!!.sound.setOnClickListener(this)
        binding!!.start.setOnClickListener(this)
    }

    override fun initViewModel() {
    }

    override fun applyNewVideoConfiguration() {
        binding!!.configuration = videoConfiguration!!
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).recordService!!.registerListener(this)
        checkPreviewMode()
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.camera -> {
                onCameraFacingSelect()
            }
            R.id.recordDuration -> {
                onVideoRecordDurationSelect()
            }
            R.id.intervalTime -> {
                onVideoIntervalSelect()
            }
            R.id.previewMode -> {
                onPreviewModeChange()
            }
            R.id.flash -> {
                onFlashModeChange()
            }
            R.id.sound -> {
                onSoundModeChange()
            }
            R.id.start -> {
                startRecordOrSetSchedule()
            }
        }
    }

    override fun onUpdateTime(fileName: String, duration: Long, curTime: Long) {
        if (!binding!!.isRecording && curTime > 0) {
            binding!!.isRecording = true
        }
        binding!!.recordTime.text = VideoRecordUtils.generateRecordTime(curTime)
    }

    override fun onByteBuffer(buf: ShortArray?, minBufferSize: Int) {
    }

    override fun onBatteryLow() {
        onLowBatteryAction()
    }

    override fun onLowStorage() {
        onLowStorageAction()
    }

    override fun onStopped() {
    }

    override fun startAction() {
        if (!binding!!.isRecording) {
            if (recordSchedule!!.scheduleTime > 0L && System.currentTimeMillis() + videoConfiguration!!.totalTime > recordSchedule!!.scheduleTime) {
                showCancelDialog()
            } else {
                (requireActivity() as MainActivity).recordService!!.startRecordVideo(VideoRecordUtils.getVideoRotation(requireContext(), orientationAngle))
            }
        } else {
            (requireActivity() as MainActivity).recordService!!.stopRecordVideo()
            binding!!.recordTime.text = getString(R.string.video_record_time_zero)
        }
    }

    override fun generateCancelDialogMessages(): String {
        return getString(R.string.video_record_schedule_cancel_message)
    }

    override fun onCancelSchedule() {
        cancelSchedule()
    }

    override fun onDestroy() {
        super.onDestroy()
        orientationListener.disable()
    }
}