package com.ashwin.android.audiorecorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.ashwin.android.audiorecorder.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {
    private val SUB_TAG = MainActivity::class.java.simpleName
    lateinit var binding: ActivityMainBinding

    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false

    private lateinit var mediaRecorder: MediaRecorder

    private lateinit var dirPath: String
    private var fileName: String = ""

    private var isRecording = false
    private var isPaused = false

    private lateinit var timer: Timer
    private lateinit var vibrator: Vibrator

    private var amplitudes: ArrayList<Float> = ArrayList()

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetLayout.root)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        permissionGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) {
            Log.d(APP_TAG, "$SUB_TAG: RECORD_AUDIO permission NOT granted")
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
        } else {
            Log.d(APP_TAG, "$SUB_TAG: RECORD_AUDIO permission granted")
        }

        dirPath = "${filesDir?.absolutePath}/"

        db = Room.databaseBuilder(this, AppDatabase::class.java, DB_NAME).build()

        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        binding.recordButton.setOnClickListener {
            when {
                isRecording -> pauseRecord()
                isPaused -> resumeRecord()
                else -> startRecord()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }

        binding.listButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, GalleryActivity::class.java))
        }

        binding.deleteButton.setOnClickListener {
            stopRecord()
            File("$dirPath/$fileName.mp3").delete()
            Toast.makeText(this@MainActivity, "Record deleted", Toast.LENGTH_LONG).show()
        }

        binding.doneButton.setOnClickListener {
            pauseRecord()

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.bottomSheetBgView.visibility = View.VISIBLE

            binding.bottomSheetLayout.fileNameEditText.setText(fileName)
        }

        binding.bottomSheetLayout.cancelButton.setOnClickListener {
            dismissBottomSheet()
        }

        binding.bottomSheetLayout.okButton.setOnClickListener {
            stopRecord()
            val newFileName = binding.bottomSheetLayout.fileNameEditText.text.toString()
            saveAudioRecord(newFileName)
            dismissBottomSheet()
            Toast.makeText(this@MainActivity, "Record saved", Toast.LENGTH_LONG).show()
        }

        binding.bottomSheetBgView.setOnClickListener {
            dismissBottomSheet()
        }

        binding.deleteButton.isClickable = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE && grantResults.isNotEmpty()) {
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
        Log.d(APP_TAG, "$SUB_TAG: onRequestPermissionsResult: $permissionGranted")
    }

    private fun dismissBottomSheet() {
        hideKeyboard(binding.bottomSheetLayout.fileNameEditText)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.bottomSheetBgView.visibility = View.GONE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }, 200L)
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun saveAudioRecord(newFileName: String) {
        if (newFileName != fileName) {
            val newFile = File("$dirPath$newFileName.mp3")
            File("$dirPath$fileName.mp3").renameTo(newFile)
        }

        val filePath = "$dirPath$newFileName.mp3"
        val timestamp = Date().time
        val ampsPath = "$dirPath$newFileName"
        val duration = binding.timerTextView.text.toString().dropLast(3)
        try {
            val fos = FileOutputStream(ampsPath)
            val out = ObjectOutputStream(fos)
            out.writeObject(amplitudes)
            fos.close()
            out.close()
        } catch (e: IOException) {
            Log.e(APP_TAG, "$SUB_TAG: Exception while saving amplitudes", e)
        }
        val record = AudioRecord(newFileName, filePath, timestamp, duration, ampsPath)
        GlobalScope.launch {
            db.audioRecordDao().insert(record)
        }
    }

    private fun startRecord() {
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
            return
        }

        val simpleDateFormat = SimpleDateFormat("yyyyMMDD_hhmmss")
        val ts = simpleDateFormat.format(Date())
        fileName = "audio_record_$ts"

        mediaRecorder = MediaRecorder()
        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.mp3")

            try {
                prepare()
                start()
                isRecording = true
                binding.recordButton.setImageResource(R.drawable.ic_pause)
                timer.start()

                binding.deleteButton.isClickable = true
                binding.deleteButton.setImageResource(R.drawable.ic_delete)

                binding.listButton.visibility = View.GONE
                binding.doneButton.visibility = View.VISIBLE
            } catch (e: IOException) {
                Log.e(APP_TAG, "$SUB_TAG: IOException", e)
            } catch (e: Exception) {
                Log.e(APP_TAG, "$SUB_TAG: Exception", e)
            }
        }
    }

    private fun pauseRecord() {
        mediaRecorder.pause()
        isPaused = true
        isRecording = false
        binding.recordButton.setImageResource(R.drawable.ic_record)
        timer.pause()
    }

    private fun resumeRecord() {
        mediaRecorder.resume()
        isRecording = true
        isPaused = false
        binding.recordButton.setImageResource(R.drawable.ic_pause)
        timer.start()
    }

    private fun stopRecord() {
        timer.stop()
        mediaRecorder.apply {
            stop()
            release()
        }
        isRecording = false
        isPaused = false

        binding.listButton.visibility = View.VISIBLE
        binding.doneButton.visibility = View.GONE

        binding.deleteButton.isClickable = false
        binding.deleteButton.setImageResource(R.drawable.ic_delete_disabled)

        binding.recordButton.setImageResource(R.drawable.ic_record)

        binding.timerTextView.text = "00:00:00"
        amplitudes = binding.waveformView.clear()
    }

    override fun onTimerTick(duration: String) {
        Log.d(APP_TAG, "$SUB_TAG: onTimerTick( $duration )")
        binding.timerTextView.text = duration

        val amplitude = mediaRecorder.maxAmplitude.toFloat()
        Log.d(APP_TAG, "$SUB_TAG: onTimerTick: amplitude: $amplitude")
        binding.waveformView.addAmplitude(amplitude)
    }
}
