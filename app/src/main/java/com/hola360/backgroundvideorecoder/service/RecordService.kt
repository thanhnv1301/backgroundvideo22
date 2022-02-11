package com.hola360.backgroundvideorecoder.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFormat
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import com.hola360.backgroundvideorecoder.MainActivity
import com.hola360.backgroundvideorecoder.R
import com.hola360.backgroundvideorecoder.app.App
import com.hola360.backgroundvideorecoder.broadcastreciever.ListenRecordScheduleBroadcast
import com.hola360.backgroundvideorecoder.ui.dialog.PreviewVideoWindow
import com.hola360.backgroundvideorecoder.ui.record.audio.utils.AudioRecordUtils
import com.hola360.backgroundvideorecoder.ui.record.audio.utils.RecordHelper
import com.hola360.backgroundvideorecoder.utils.Constants
import com.hola360.backgroundvideorecoder.utils.VideoRecordUtils
import com.zlw.main.recorderlib.recorder.RecordConfig
import com.zlw.main.recorderlib.recorder.RecordConfig.RecordFormat
import com.zlw.main.recorderlib.recorder.listener.*
import com.zlw.main.recorderlib.utils.FileUtils
import java.text.SimpleDateFormat
import java.util.*


class RecordService : Service(), AudioRecordUtils.Listener {

    private val notificationManager: NotificationManager by lazy {
        this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val recordScheduleBroadcast: ListenRecordScheduleBroadcast by lazy {
        ListenRecordScheduleBroadcast()
    }
    private var notificationTitle: String = ""
    private var notificationContent: String = ""
    var recordStatus: Int = MainActivity.NO_RECORDING
    private var listener: Listener? = null
    var mBinder = LocalBinder()
    private var currentConfig = RecordConfig()
    private val previewVideoWindow: PreviewVideoWindow by lazy {
        PreviewVideoWindow(this, object : PreviewVideoWindow.RecordAction {
            override fun onRecording(time: Long, isComplete: Boolean) {
                if (recordStatus != MainActivity.NO_RECORDING) {
                    listener?.updateRecordTime(time, MainActivity.RECORD_VIDEO)
                    notificationContent = VideoRecordUtils.generateRecordTime(time)
                    notificationManager.notify(NOTIFICATION_ID, getNotification())
                }
            }

            override fun onFinishRecord() {
                listener?.onRecordCompleted()
                notificationTitle =
                    this@RecordService.resources.getString(R.string.video_record_complete_prefix)
                VideoRecordUtils.checkScheduleWhenRecordStop(this@RecordService)
                notificationManager.notify(NOTIFICATION_ID, getNotification())
                stopForeground(true)
            }
        })
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()
        val intentFilter = IntentFilter().apply {
            addAction(Constants.SCHEDULE_TYPE)
        }
        registerReceiver(recordScheduleBroadcast, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val status = it.getIntExtra(Constants.VIDEO_STATUS, 0)
            recordVideo(status)

            val statusAudio = it.getIntExtra("Audio", 0)
            if (statusAudio == 0)
                startRecording(
                    getFilePath()!!
                )
        }
        return START_NOT_STICKY
    }

    fun recordVideo(status: Int) {
        when (status) {
            MainActivity.RECORD_VIDEO -> {
                previewVideoWindow.setupVideoConfiguration()
                previewVideoWindow.open()
                previewVideoWindow.startRecording()
                recordStatus = MainActivity.RECORD_VIDEO
                notificationTitle = this.resources.getString(R.string.video_record_is_running)
                startForeground(NOTIFICATION_ID, getNotification())
            }
            MainActivity.STOP_VIDEO_RECORD -> {
                previewVideoWindow.close()
                recordStatus = MainActivity.NO_RECORDING
                VideoRecordUtils.checkScheduleWhenRecordStop(this)
                stopForeground(true)
                notificationManager.cancel(NOTIFICATION_ID)
            }
            MainActivity.SCHEDULE_RECORD_VIDEO -> {
                notificationTitle =
                    this.getString(R.string.video_record_schedule_notification_title)
                notificationContent = VideoRecordUtils.generateScheduleTime(this)
                startForeground(NOTIFICATION_ID, getNotification())
            }
            MainActivity.CANCEL_SCHEDULE_RECORD_VIDEO -> {
                stopForeground(true)
            }
        }
    }

    fun startRecording(path: String) {
        val recordDir = String.format(
            Locale.getDefault(), "%s/Record/backgroundrecord/",
            Environment.getExternalStorageDirectory().absolutePath
        )
        changeRecordDir(recordDir)
        changeRecordConfig(
            RecordConfig(
                RecordFormat.MP3,
                1,
                AudioFormat.ENCODING_PCM_16BIT,
                16000
            )
        )
        RecordHelper().start(path, currentConfig)
        startForeground(NOTIFICATION_ID, getNotification())
    }

    fun stopRecording() {
        RecordHelper().stop()
    }

    fun resumeRecording() {
        RecordHelper().resume()
    }

    fun pauseRecording() {
        RecordHelper().pause()
    }

    /**
     * Change format audio file
     */
    fun changeFormat(recordFormat: RecordFormat?): Boolean {
        if (getState() == RecordHelper.RecordState.IDLE) {
            currentConfig.format = recordFormat
            return true
        }
        return false
    }

    /**
     * Change audio record config
     */
    fun changeRecordConfig(recordConfig: RecordConfig?): Boolean {
        if (getState() == RecordHelper.RecordState.IDLE) {
            if (recordConfig != null) {
                currentConfig = recordConfig
            }
            return true
        }
        return false
    }

    fun getRecordConfig(): RecordConfig {
        return currentConfig
    }

    fun changeRecordDir(recordDir: String?) {
        currentConfig.recordDir = recordDir
    }

    fun setCurrentConfig(currentConfig: RecordConfig?) {
        if (currentConfig != null) {
            this.currentConfig = currentConfig
        }
    }

    fun getState(): RecordHelper.RecordState? {
        return RecordHelper().getState()
    }

    private fun getNotification(): Notification {
        val pendingIntent = NavDeepLinkBuilder(this)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_main_graph)
            .setDestination(R.id.nav_video_record)
            .createPendingIntent()

        return NotificationCompat.Builder(this, App.CHANNEL_SERVICE_ID)
            .setContentTitle(notificationTitle)
            .setContentText(notificationContent)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_schedule)
            .build()
    }

    inner class LocalBinder : Binder() {
        fun getServiceInstance(): RecordService = this@RecordService
    }

    fun registerListener(listener: Listener) {
        this.listener = listener
    }

    interface Listener {
        fun onRecordStarted(status: Int)

        fun updateRecordTime(time: Long, status: Int)

        fun onRecordCompleted()
    }

    override fun updateTimer(time: Long) {
    }

    companion object {
        const val NOTIFICATION_ID = 1
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(recordScheduleBroadcast)
    }

    fun setRecordStateListener(recordStateListener: RecordStateListener?) {
        RecordHelper().setRecordStateListener(recordStateListener)
    }

    fun setRecordDataListener(recordDataListener: RecordDataListener?) {
        RecordHelper().setRecordDataListener(recordDataListener)
    }

    fun setRecordSoundSizeListener(recordSoundSizeListener: RecordSoundSizeListener?) {
        RecordHelper().setRecordSoundSizeListener(recordSoundSizeListener)
    }

    fun setRecordResultListener(recordResultListener: RecordResultListener?) {
        RecordHelper().setRecordResultListener(recordResultListener)
    }

    fun setRecordFftDataListener(recordFftDataListener: RecordFftDataListener?) {
        RecordHelper().setRecordFftDataListener(recordFftDataListener)
    }

    private fun getFilePath(): String? {
        val fileDir = currentConfig.recordDir
        if (!FileUtils.createOrExistsDir(fileDir)) {
            return null
        }
        val fileName = java.lang.String.format(
            Locale.getDefault(),
            "bg_audio_%s",
            FileUtils.getNowString(SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.US))
        )
        return java.lang.String.format(
            Locale.US,
            "%s%s%s",
            fileDir,
            fileName,
            ".mp3"
        )
    }
}