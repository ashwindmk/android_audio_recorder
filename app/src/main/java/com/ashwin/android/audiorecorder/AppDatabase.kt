package com.ashwin.android.audiorecorder

import androidx.room.Database
import androidx.room.RoomDatabase

const val DB_NAME = "audio_record_db"

@Database(entities = arrayOf(AudioRecord::class), version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioRecordDao(): AudioRecordDao
}
