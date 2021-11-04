package com.ashwin.android.audiorecorder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.ashwin.android.audiorecorder.databinding.ActivityGalleryBinding
import kotlinx.coroutines.*
import java.io.File

class GalleryActivity : AppCompatActivity(), AudioRecordListAdapter.OnItemClickListener {
    private val SUB_TAG = GalleryActivity::class.java.simpleName

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var records: ArrayList<AudioRecord>
    private lateinit var adapter: AudioRecordListAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolBar.setNavigationOnClickListener {
            onBackPressed()
        }

        db = Room.databaseBuilder(this, AppDatabase::class.java, DB_NAME).build()

        records = ArrayList()

        adapter = AudioRecordListAdapter(records, this)

        binding.recyclerView.apply {
            adapter = this@GalleryActivity.adapter
            layoutManager = LinearLayoutManager(context)
        }

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                filter(s.toString())
            }
        })

        fetchAll()
    }

    private fun filter(text: String) {
        adapter.filter.filter(text)
    }

    private fun fetchAll() {
        GlobalScope.launch {
            val queryResult = db.audioRecordDao().getAll()
            records.clear()
            records.addAll(queryResult)
            Log.d(APP_TAG, "$SUB_TAG: fetchAll: records.size: ${records.size}")
            withContext(Dispatchers.Main) {
                adapter.update(records)
            }
        }
    }

    override fun onItemClicked(position: Int) {
//        Toast.makeText(this, "Item $position clicked", Toast.LENGTH_SHORT).show()
        var record = records[position]
        val intent = Intent(this, AudioPlayerActivity::class.java)
        intent.putExtra("filePath", record.filePath)
        intent.putExtra("fileName", record.fileName)
        startActivity(intent)
    }

    override fun onItemLongClicked(position: Int) {
//        Toast.makeText(this, "Item $position long clicked", Toast.LENGTH_SHORT).show()
        val record = records[position]
        val alertDialog = AlertDialog.Builder(this)
            .setMessage("Delete ${record.fileName}?")
            .setPositiveButton("Ok") { dialog, which ->
                deleteRecord(record)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .create()
        alertDialog.show()
    }

    private fun deleteRecord(record: AudioRecord) {
        GlobalScope.launch {
            db.audioRecordDao().delete(record)

            val audioFile = File(record.filePath)
            audioFile.delete()

            val ampsFile = File(record.ampsPath)
            ampsFile.delete()

            fetchAll()
        }
    }
}
