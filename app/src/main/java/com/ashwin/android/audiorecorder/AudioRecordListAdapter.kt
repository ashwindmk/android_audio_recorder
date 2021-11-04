package com.ashwin.android.audiorecorder

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.annotation.MainThread
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.ashwin.android.audiorecorder.databinding.LayoutItemBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AudioRecordListAdapter(records: ArrayList<AudioRecord>, @NonNull val itemClickListener: OnItemClickListener) : RecyclerView.Adapter<AudioRecordListAdapter.ViewHolder>(), Filterable {
    private val SUB_TAG = AudioRecordListAdapter::class.java.simpleName

    inner class ViewHolder(val binding: LayoutItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {
        init {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
        }

        fun bind(audioRecord: AudioRecord) {
            binding.fileNameTextView.text = audioRecord.fileName
            binding.metaTextView.text = "${audioRecord.duration} ${SimpleDateFormat("dd/MM/yyyy").format(Date(audioRecord.timestamp))}"
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                itemClickListener.onItemClicked(position)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                itemClickListener.onItemLongClicked(position)
            }
            return true
        }
    }

    interface OnItemClickListener {
//        fun onItemClicked(record: AudioRecord)
//        fun onItemLongClicked(record: AudioRecord)
        fun onItemClicked(position: Int)
        fun onItemLongClicked(position: Int)
    }

    private val fullRecords: ArrayList<AudioRecord>
    private val filteredRecords: ArrayList<AudioRecord>

    init {
        fullRecords = ArrayList(records)
        filteredRecords = ArrayList(fullRecords)
    }

    private val filter: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            // Bg thread
            Log.d(APP_TAG, "$SUB_TAG: performFiltering( $constraint )")
            val filteredList = ArrayList<AudioRecord>()
            if (constraint == null || constraint.isEmpty()) {
                filteredList.addAll(fullRecords)
            } else {
                val filterPattern = constraint.toString().lowercase().trim()
                for (record in fullRecords) {
                    if (record.fileName.lowercase().contains(filterPattern)) {
                        filteredList.add(record)
                    }
                }
            }

            val filterResults = FilterResults()
            filterResults.values = filteredList
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            // UI thread
            filteredRecords.clear()
            filteredRecords.addAll(results?.values as Collection<AudioRecord>)
            notifyDataSetChanged()
        }
    }

    @MainThread
    fun update(newRecords: List<AudioRecord>) {
        fullRecords.clear()
        fullRecords.addAll(newRecords)
        filteredRecords.clear()
        filteredRecords.addAll(fullRecords)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            val record = filteredRecords[position]
            holder.bind(record)
        }
    }

    override fun getItemCount(): Int {
        return filteredRecords.size
    }

    override fun getFilter(): Filter {
        return filter
    }
}