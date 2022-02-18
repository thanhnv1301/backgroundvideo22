package com.hola360.backgroundvideorecoder.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import android.view.OrientationEventListener
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.hola360.backgroundvideorecoder.R
import com.hola360.backgroundvideorecoder.data.model.audio.AudioModel
import com.hola360.backgroundvideorecoder.service.notification.RecordNotificationManager
import com.hola360.backgroundvideorecoder.ui.dialog.PreviewVideoWindow
import com.hola360.backgroundvideorecoder.ui.record.audio.utils.SoundRecorder
import com.hola360.backgroundvideorecoder.utils.*
import java.util.*

class RecordService : Service() {

    private val mRecordNotificationManager by lazy {
        RecordNotificationManager(this)
    }
    private var notificationTitle: String = ""
    private var notificationContent: String = ""
    var mBinder = LocalBinder()
    private var videoPreviewVideoWindow: PreviewVideoWindow? = null
    private var orientationAngle= 0
    private val orientationListener: OrientationEventListener by lazy {
        object : OrientationEventListener(this){
            override fun onOrientationChanged(orientation: Int) {
                orientationAngle= orientation
                Log.d("abcVideo", "Orientation: $orientation ")
            }
        }
    }
    private var listener: Listener? = null
    var time = 0L
    private var recordStateLiveData = MutableLiveData<RecordState>()
    var mSoundRecorder: SoundRecorder? = null
    private var mServiceManager: ServiceManager? = null
    private var mAudioModel: AudioModel? = null
    private val handler = Handler(Looper.getMainLooper())
    private var mp3Name: String? = null
    var isRecordScheduleStart = false
    private val runnable = Runnable {
        time = time.plus(TIME_LOOP)
        mServiceManager!!.updateProgress(Utils.convertTime(time / 1000))
        listener?.onUpdateTime(if (!mp3Name.isNullOrEmpty()) mp3Name!! else "Audio Record", 0, time)
        nextLoop()
        stopAudioRecordByTime(time)
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    private inner class ServiceManager {
        fun startRecord() {
            val notification = mRecordNotificationManager.getNotification(
                notificationTitle,
                notificationContent
            )
            startForeground(RecordNotificationManager.NOTIFICATION_ID, notification)
        }

        fun startSchedule(time: Long) {
            val notification = mRecordNotificationManager.getNotification(
                notificationTitle,
                notificationContent
            )
            startForeground(RecordNotificationManager.NOTIFICATION_ID, notification)
        }

        fun updateProgress(time: String) {
            val notification = mRecordNotificationManager.getNotification(
                notificationTitle,
                time
            )
            mRecordNotificationManager.notificationManager.notify(
                RecordNotificationManager.NOTIFICATION_ID,
                notification
            )
        }

        fun error() {
            stopForeground(true)
        }

        fun stop() {
            recordStateLiveData.value = RecordState.None
            stopForeground(true)
        }
    }

    override fun onCreate() {
        super.onCreate()
        mServiceManager = ServiceManager()
        recordStateLiveData.value = RecordState.None
        initReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    private fun initReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(RecordNotificationManager.ACTION_STOP)
        intentFilter.addAction(RecordNotificationManager.ACTION_RECORD_FROM_SCHEDULE)
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(globalReceiver, intentFilter)
    }

    private val globalReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                when (intent.action) {
                    RecordNotificationManager.ACTION_STOP -> {
                        stopRecording()
                    }
                    RecordNotificationManager.ACTION_RECORD_FROM_SCHEDULE -> {
                        val type = intent.getBooleanExtra(Constants.SCHEDULE_TYPE, false)
                        if (!type) {
                            startRecordAudio()
                            isRecordScheduleStart = true
                        } else {
                            startRecordVideo(VideoRecordUtils.getVideoRotation(this@RecordService, orientationAngle))
                            time = System.currentTimeMillis()
                        }
                    }
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        val mBattery = level * 100 / scale.toFloat()
                        if (mBattery == BATTERY_LOW) {
                            if (recordStateLiveData.value == RecordState.AudioRecording) {
                                listener?.onBatteryLow(mBattery)
                            }
                        }
                    }
                }

            }
        }

    }

    fun startRecordVideo(videoOrientation:Int) {
        if (recordStateLiveData.value == RecordState.None || recordStateLiveData.value == RecordState.VideoSchedule) {
            mServiceManager!!.startRecord()
            notificationTitle = this.resources.getString(R.string.video_record_notification_title)
            videoPreviewVideoWindow =
                PreviewVideoWindow(this, videoOrientation, object : PreviewVideoWindow.RecordAction {
                    override fun onRecording(recordTime: Long) {
                        if (recordStateLiveData.value == RecordState.VideoRecording) {
                            listener?.onUpdateTime("", 0L, recordTime)
                            notificationContent =
                                VideoRecordUtils.generateRecordTime(recordTime)
                            val notification = mRecordNotificationManager.getNotification(
                                notificationTitle,
                                notificationContent
                            )
                            mRecordNotificationManager.notifyNewStatus(notification)
                        }
                    }

                    override fun onFinishRecord() {
                        notificationTitle =
                            this@RecordService.resources.getString(R.string.video_record_complete_prefix)
                        VideoRecordUtils.checkScheduleWhenRecordStop(this@RecordService)
                        val notification = mRecordNotificationManager.getNotification(
                            notificationTitle,
                            notificationContent
                        )
                        listener?.onStopped()
                        mRecordNotificationManager.notifyNewStatus(notification)
                        mServiceManager!!.stop()
                    }
                })
            videoPreviewVideoWindow!!.setupVideoConfiguration()
            videoPreviewVideoWindow!!.open()
            recordStateLiveData.value = RecordState.VideoRecording
        }
    }

    fun stopRecordVideo() {
        if (recordStateLiveData.value == RecordState.VideoRecording) {
            recordStateLiveData.value = RecordState.None
            videoPreviewVideoWindow?.close()
            videoPreviewVideoWindow = null
            VideoRecordUtils.checkScheduleWhenRecordStop(this@RecordService)
            listener?.onStopped()
            time = 0L
            orientationListener.disable()
            mServiceManager!!.stop()
        }
    }

    fun updatePreviewVideoParams(visibility: Boolean) {
        videoPreviewVideoWindow?.updateLayoutParams(visibility)
    }

    fun startRecordAudio() {
        mAudioModel =
            if (!SharedPreferenceUtils.getInstance(this)?.getAudioConfig()
                    .isNullOrEmpty()
            ) {
                Gson().fromJson(
                    SharedPreferenceUtils.getInstance(this)?.getAudioConfig(),
                    AudioModel::class.java
                )
            } else {
                AudioModel()
            }
        if (!isRecording()) {
            mp3Name = String.format(
                Configurations.TEMPLATE_AUDIO_FILE_NAME,
                DateTimeUtils.getFullDate(Date().time)
            )
            mSoundRecorder =
                SoundRecorder(
                    this,
                    mp3Name,
                    mAudioModel!!,
                    object : SoundRecorder.OnRecorderListener {
                        override fun onBuffer(buf: ShortArray?, minBufferSize: Int) {
                            listener?.onByteBuffer(buf, minBufferSize)
                        }

                    })
            mSoundRecorder!!.setHandle(object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        SoundRecorder.MSG_REC_STARTED -> {
                            mServiceManager!!.startRecord()
                        }
                        SoundRecorder.MSG_REC_STOPPED -> {
                            mServiceManager!!.stop()
                            listener?.onStopped()
                            if (isRecordScheduleStart) {
                                SharedPreferenceUtils.getInstance(this@RecordService)!!
                                    .putSchedule(null)
                            }
                            ToastUtils.getInstance(this@RecordService)!!
                                .showToast(getString(R.string.recording_stop))
                            recordStateLiveData.value = RecordState.None
                        }
                        SoundRecorder.MSG_ERROR_GET_MIN_BUFFER_SIZE, SoundRecorder.MSG_ERROR_CREATE_FILE, SoundRecorder.MSG_ERROR_REC_START, SoundRecorder.MSG_ERROR_AUDIO_RECORD, SoundRecorder.MSG_ERROR_AUDIO_ENCODE, SoundRecorder.MSG_ERROR_WRITE_FILE, SoundRecorder.MSG_ERROR_CLOSE_FILE -> {
                            recordAudioFailed()
                        }
                    }
                }
            })
            time = 0
            recordStateLiveData.value = RecordState.AudioRecording
            mSoundRecorder!!.start()
            nextLoop()
        }

    }

    fun stopRecording() {
        mServiceManager!!.stop()
        if (mSoundRecorder != null && mSoundRecorder!!.isRecording()) {
            mSoundRecorder!!.stop()
        }
        handler.removeCallbacks(runnable)
    }

    private fun nextLoop() {
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, TIME_LOOP)
    }

    private fun recordAudioFailed() {
        ToastUtils.getInstance(this@RecordService)!!.showToast("Error")
        mServiceManager!!.error()
        listener?.onStopped()
        handler.removeCallbacks(runnable)
    }

    fun isRecording(): Boolean {
        return recordStateLiveData.value == RecordState.VideoRecording || recordStateLiveData.value == RecordState.AudioRecording
    }

    fun getRecordState(): MutableLiveData<RecordState> {
        return recordStateLiveData
    }

    private fun stopAudioRecordByTime(time: Long) {
        if (mAudioModel!!.duration != 0L) {
            if (time == mAudioModel!!.duration) {
                stopRecording()
            }
        }
    }

    private fun getBroadcastPendingIntent(
        context: Context,
        isVideo: Boolean
    ): PendingIntent {
        val intent = Intent(RecordNotificationManager.ACTION_RECORD_FROM_SCHEDULE).apply {
            putExtra(Constants.SCHEDULE_TYPE, isVideo)
        }
        return PendingIntent.getBroadcast(
            context, 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun setAlarmSchedule(context: Context, time: Long, isVideo: Boolean) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (SystemUtils.isAndroidO()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, time,
                getBroadcastPendingIntent(context, isVideo)
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                time,
                getBroadcastPendingIntent(context, isVideo)
            )
        }
        recordStateLiveData.value = if (isVideo) {
            RecordState.VideoSchedule
        } else {
            orientationListener.enable()
            RecordState.AudioSchedule
        }
        mServiceManager!!.startSchedule(time)
    }

    fun cancelAlarmSchedule(context: Context, isVideo: Boolean) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(getBroadcastPendingIntent(context, isVideo))
        mServiceManager!!.stop()
    }

    inner class LocalBinder : Binder() {
        fun getServiceInstance(): RecordService = this@RecordService
    }

    fun registerListener(listener: Listener) {
        this.listener = listener
    }

    interface Listener {
        fun onUpdateTime(fileName: String, duration: Long, curTime: Long)

        fun onStopped()

        fun onByteBuffer(buf: ShortArray?, minBufferSize: Int)

        fun onBatteryLow(batteryPer: Float)
    }

    companion object {
        const val TIME_LOOP = 500L
        const val BATTERY_LOW = 20F
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(globalReceiver)
    }

    enum class RecordState {
        None, AudioRecording, VideoRecording, AudioSchedule, VideoSchedule
    }

}