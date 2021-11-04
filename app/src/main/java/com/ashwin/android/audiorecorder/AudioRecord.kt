package com.ashwin.android.audiorecorder

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "audio_record")
data class AudioRecord(
    var fileName: String,
    var filePath: String,
    var timestamp: Long,
    var duration: String,
    var ampsPath: String
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0

    @Ignore
    var isChecked = false
}
