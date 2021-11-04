package com.ashwin.android.audiorecorder

import androidx.room.*

@Dao
interface AudioRecordDao {
    @Query("SELECT * from audio_record")
    fun getAll(): List<AudioRecord>

    @Insert
    fun insert(audioRecord: AudioRecord)

    @Delete
    fun delete(audioRecord: AudioRecord)

    @Delete
    fun delete(audioRecords: Array<AudioRecord>)

    @Update
    fun update(audioRecord: AudioRecord)
}
