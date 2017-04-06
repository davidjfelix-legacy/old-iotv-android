package io.iotv.app

import android.media.MediaRecorder

class HLSMediaRecorder: MediaRecorder(), MediaRecorder.OnInfoListener {
    override fun onInfo(mediaRecorder: MediaRecorder?, info: Int, extra: Int) {
        when(info) {
            MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED -> {}
        }
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}