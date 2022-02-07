package com.hola360.backgroundvideorecoder.ui.record.video.base

import androidx.databinding.ViewDataBinding
import com.google.gson.Gson
import com.hola360.backgroundvideorecoder.R
import com.hola360.backgroundvideorecoder.ui.dialog.OnDialogDismiss
import com.hola360.backgroundvideorecoder.ui.dialog.RecordVideoDurationDialog
import com.hola360.backgroundvideorecoder.ui.dialog.VideoIntervalDurationDialog
import com.hola360.backgroundvideorecoder.ui.dialog.listdialog.ListSelectionAdapter
import com.hola360.backgroundvideorecoder.ui.dialog.listdialog.ListSelectionBotDialog
import com.hola360.backgroundvideorecoder.ui.record.BaseRecordPageFragment
import com.hola360.backgroundvideorecoder.ui.record.video.model.VideoRecordConfiguration

abstract class BaseRecordVideoFragment<V: ViewDataBinding?>: BaseRecordPageFragment<V>() {

    protected var videoConfiguration: VideoRecordConfiguration? = null
    protected val cameraSelectionDialog: ListSelectionBotDialog by lazy {
        val title = resources.getString(R.string.video_record_configuration_camera)
        val itemList = resources.getStringArray(R.array.camera_facing).toMutableList()
        ListSelectionBotDialog(title, itemList, object : ListSelectionAdapter.OnItemListSelection {
            override fun onSelection(position: Int) {
                videoConfiguration!!.isBack = position == 0
                saveNewVideoConfiguration()
                applyNewVideoConfiguration()
                cameraSelectionDialog.dialog?.dismiss()
            }
        }, object : OnDialogDismiss {
            override fun onDismiss() {
                showDialog = false
            }
        })
    }
    protected val recordVideoDurationDialog: RecordVideoDurationDialog by lazy {
        RecordVideoDurationDialog(object : RecordVideoDurationDialog.OnSelectDuration {
            override fun onSelectDuration(duration: Long) {
                videoConfiguration!!.totalTime= duration
                saveNewVideoConfiguration()
                applyNewVideoConfiguration()
            }
        },
            object : OnDialogDismiss {
                override fun onDismiss() {
                    showDialog = false
                }
            })
    }
    protected val videoIntervalDurationDialog: VideoIntervalDurationDialog by lazy {
        VideoIntervalDurationDialog(object : RecordVideoDurationDialog.OnSelectDuration{
            override fun onSelectDuration(duration: Long) {
                videoConfiguration!!.timePerVideo= duration
                saveNewVideoConfiguration()
                applyNewVideoConfiguration()
            }
        }, object : OnDialogDismiss {
            override fun onDismiss() {
                showDialog=false
            }
        })
    }
    protected var showDialog = false

    protected fun saveNewVideoConfiguration(){
        val configurationString= Gson().toJson(videoConfiguration!!)
        configurationString?.let {
            dataPref!!.setVideoRecordConfiguration(it)
        }
    }

    abstract fun applyNewVideoConfiguration()
}