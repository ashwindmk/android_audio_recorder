package com.ashwin.android.audiorecorder

import android.media.MediaPlayer
import android.media.PlaybackParams
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.SeekBar
import androidx.core.content.res.ResourcesCompat
import com.ashwin.android.audiorecorder.databinding.ActivityAudioPlayerBinding
import java.text.DecimalFormat
import java.text.NumberFormat

class AudioPlayerActivity : AppCompatActivity() {
    private val SUB_TAG = AudioPlayerActivity::class.java.simpleName
    private lateinit var binding: ActivityAudioPlayerBinding
    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private val delay = 1000L
    private val jump = 5000

    private var playSpeed = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filePath = intent.getStringExtra("filePath")
        val fileName = intent.getStringExtra("fileName")
        Log.d(APP_TAG, "$SUB_TAG: filePath: $filePath, fileName: $fileName")

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolBar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.fileNameTextView.text = fileName

        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filePath)
            prepare()
        }

        mediaPlayer.setOnCompletionListener {
            binding.playButton.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_play_circle, theme)
            handler.removeCallbacks(runnable)
            binding.seekBar.progress = 0
        }

        binding.seekBar.max = mediaPlayer.duration

        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            binding.seekBar.progress = mediaPlayer.currentPosition
            handler.postDelayed(runnable, delay)
        }

        binding.durationTextView.text = format(mediaPlayer.duration)

        binding.playButton.setOnClickListener {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
                binding.playButton.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_pause_circle, theme)
                handler.post(runnable)
            } else {
                mediaPlayer.pause()
                binding.playButton.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_play_circle, theme)
                handler.removeCallbacks(runnable)
            }
        }

        binding.forwardButton.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition + jump)
            binding.seekBar.progress += jump
        }

        binding.backwardButton.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition - jump)
            binding.seekBar.progress -= jump
        }

        binding.speedChip.setOnClickListener {
            if (playSpeed < 2.0f) {
                playSpeed += 0.5f
            } else {
                playSpeed = 0.5f
            }

            mediaPlayer.playbackParams = PlaybackParams().setSpeed(playSpeed)
            binding.speedChip.text = "x $playSpeed"
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.d(APP_TAG, "$SUB_TAG: onProgressChanged(progress: $progress, fromUser: $fromUser)")
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                }
                binding.progressTextView.text = format(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                Log.d(APP_TAG, "$SUB_TAG: onStartTrackingTouch")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Log.d(APP_TAG, "$SUB_TAG: onStopTrackingTouch")
            }
        })
    }

    private fun format(duration: Int): String {
        val millis = (duration % 1000) / 10
        val secs = (duration / 1000) % 60
        val mins = (duration / (1000 * 60)) % 60
        val hrs = (duration / (1000 * 60 * 60))
        val numberFloat: NumberFormat = DecimalFormat("00")
        var str = "$mins:${numberFloat.format(secs)}"
        if (hrs > 0) {
            str = "$hrs:$str"
        }
        return str
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
        handler.removeCallbacks(runnable)
    }
}
