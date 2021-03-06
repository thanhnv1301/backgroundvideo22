package com.hola360.backgroundvideorecoder.ui.record.audio.audioschedule

import android.app.Application
import androidx.lifecycle.*
import com.google.gson.Gson
import com.hola360.backgroundvideorecoder.data.model.audio.AudioMode
import com.hola360.backgroundvideorecoder.data.model.audio.AudioModel
import com.hola360.backgroundvideorecoder.data.model.audio.AudioQuality
import com.hola360.backgroundvideorecoder.ui.record.RecordSchedule
import com.hola360.backgroundvideorecoder.utils.SharedPreferenceUtils
import kotlinx.coroutines.launch

class ScheduleAudioViewModel(val application: Application) : ViewModel() {

    private var audioModel: AudioModel? = null
    private var recordSchedule: RecordSchedule? = null
    val recordAudioLiveData = MutableLiveData<AudioModel>()
    val recordScheduleLiveData = MutableLiveData<RecordSchedule>()
    val isRecordScheduleLiveData = MutableLiveData<Boolean>()
    private val dataSharedPreferenceUtil = SharedPreferenceUtils.getInstance(application)

    init {
        isRecordScheduleLiveData.value = false
    }

    fun updateQuality(audioQuality: AudioQuality) {
        viewModelScope.launch {
            audioModel?.quality = audioQuality
            dataSharedPreferenceUtil!!.setAudioConfig(Gson().toJson(audioModel))
            recordAudioLiveData.value = audioModel!!
        }
    }

    fun updateMode(audioMode: AudioMode) {
        viewModelScope.launch {
            audioModel?.mode = audioMode
            dataSharedPreferenceUtil!!.setAudioConfig(Gson().toJson(audioModel))
            recordAudioLiveData.value = audioModel!!
        }
    }

    fun updateDuration(duration: Long) {
        viewModelScope.launch {
            audioModel?.duration = duration
            dataSharedPreferenceUtil!!.setAudioConfig(Gson().toJson(audioModel))
            recordAudioLiveData.value = audioModel!!
        }
    }

    fun updateMuted() {
        viewModelScope.launch {
            audioModel?.isMuted = !audioModel?.isMuted!!
            dataSharedPreferenceUtil!!.setAudioConfig(Gson().toJson(audioModel))
            recordAudioLiveData.value = audioModel!!
        }
    }

    fun updateDate(date: Long) {
        viewModelScope.launch {
            recordSchedule?.scheduleTime = date
            recordScheduleLiveData.value = recordSchedule!!
        }
    }

    fun updateTime(time: Long) {
        viewModelScope.launch {
            recordSchedule?.scheduleTime = time
            recordScheduleLiveData.value = recordSchedule!!
        }
    }

    fun getAudioConfig() {
        viewModelScope.launch {
            audioModel = if (!dataSharedPreferenceUtil!!.getAudioConfig().isNullOrEmpty()) {
                Gson().fromJson(dataSharedPreferenceUtil.getAudioConfig(), AudioModel::class.java)
            } else {
                AudioModel()
            }
            recordAudioLiveData.value = audioModel!!
        }
    }

    fun getAudioScheduleConfig() {
        viewModelScope.launch {
            recordSchedule = if (!dataSharedPreferenceUtil!!.getSchedule().isNullOrEmpty()) {
                Gson().fromJson(
                    dataSharedPreferenceUtil.getSchedule(),
                    RecordSchedule::class.java
                )
            } else {
                RecordSchedule()
            }
            recordScheduleLiveData.value = recordSchedule!!
        }
    }

    fun getSavedSchedule() {
        viewModelScope.launch {
            isRecordScheduleLiveData.value =
                !dataSharedPreferenceUtil!!.getSchedule().isNullOrEmpty()
        }
    }

    fun setSchedule() {
        viewModelScope.launch {
            dataSharedPreferenceUtil!!.putSchedule(Gson().toJson(recordSchedule))
            isRecordScheduleLiveData.value = true
        }
    }

    fun cancelSchedule() {
        viewModelScope.launch {
            dataSharedPreferenceUtil!!.putSchedule(null)
            isRecordScheduleLiveData.value = false
        }
    }

    class Factory(private val application: Application) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScheduleAudioViewModel::class.java)) {
                return ScheduleAudioViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}