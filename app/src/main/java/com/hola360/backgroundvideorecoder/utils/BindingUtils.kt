package com.hola360.backgroundvideorecoder.utils

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.hola360.backgroundvideorecoder.R
import com.hola360.backgroundvideorecoder.data.model.audio.AudioModel
import com.hola360.backgroundvideorecoder.data.model.audio.AudioQuality

object BindingUtils {

    @BindingAdapter("videoRecordDuration")
    @JvmStatic
    fun videoRecordDuration(textView: TextView, duration: Long?) {
        duration?.let {
            textView.text = if (it > 0) {
                val time = it / 1000 / 60
                textView.resources.getQuantityString(
                    R.plurals.video_record_configuration_minute,
                    time.toInt(), time
                )
            } else {
                textView.resources.getString(R.string.video_record_configuration_un_limit)
            }
        }
    }

    @BindingAdapter("timePerVideo")
    @JvmStatic
    fun timePerVideo(textView: TextView, duration: Long?) {
        duration?.let {
            textView.text = if (it > 0) {
                val time = it / 1000 / 60
                textView.resources.getQuantityString(
                    R.plurals.video_record_configuration_minute,
                    time.toInt(), time.toInt()
                )
            } else {
                textView.resources.getString(R.string.video_record_configuration_no_split)
            }
        }
    }

    @BindingAdapter("android:setRecordQuality")
    @JvmStatic
    fun setRecordQuality(textView: TextView, audioModel: AudioModel?) {
        if (audioModel != null) {
            val recordQuality = textView.context.resources.getStringArray(R.array.record_quality)
            textView.text = recordQuality[audioModel.quality.ordinal]
        }
    }

    @BindingAdapter("android:setRecordMode")
    @JvmStatic
    fun setRecordMode(textView: TextView, audioModel: AudioModel?) {
        if (audioModel != null) {
            val recordMode = textView.context.resources.getStringArray(R.array.record_mode)
            textView.text = recordMode[audioModel.mode.ordinal]
        }
    }

}