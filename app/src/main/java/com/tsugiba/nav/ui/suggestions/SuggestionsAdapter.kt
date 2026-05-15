package com.tsugiba.nav.ui.suggestions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tsugiba.nav.data.model.SuggestionType
import com.tsugiba.nav.data.model.TrafficSuggestion
import com.tsugiba.nav.databinding.ItemSuggestionBinding
import com.tsugiba.nav.R

class SuggestionsAdapter(
    private val onSuggestionClick: (TrafficSuggestion) -> Unit
) : ListAdapter<TrafficSuggestion, SuggestionsAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemSuggestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TrafficSuggestion) {
            binding.tvTitle.text = item.title
            binding.tvDescription.text = item.description
            binding.ivIcon.setImageResource(
                when (item.type) {
                    SuggestionType.FASTER_PATH, SuggestionType.ALTERNATE_ROUTE -> R.drawable.ic_route_alt
                    SuggestionType.CONGESTION_AHEAD, SuggestionType.INCIDENT_AHEAD -> R.drawable.ic_traffic_warning
                    SuggestionType.QUIETER_STREET -> R.drawable.ic_quiet_street
                }
            )
            binding.root.setOnClickListener {
                if (item.altRoute != null) onSuggestionClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemSuggestionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<TrafficSuggestion>() {
        override fun areItemsTheSame(a: TrafficSuggestion, b: TrafficSuggestion) = a.id == b.id
        override fun areContentsTheSame(a: TrafficSuggestion, b: TrafficSuggestion) = a == b
    }
}
