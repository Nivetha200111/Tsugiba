package com.tsugiba.nav.ui.suggestions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tsugiba.nav.data.model.SuggestionType
import com.tsugiba.nav.data.model.TrafficSuggestion
import com.tsugiba.nav.databinding.ItemSuggestionBinding

class SuggestionsAdapter(
    private val onSuggestionClick: (TrafficSuggestion) -> Unit
) : ListAdapter<TrafficSuggestion, SuggestionsAdapter.VH>(Diff) {

    inner class VH(private val b: ItemSuggestionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: TrafficSuggestion) {
            b.tvEmoji.text = when (item.type) {
                SuggestionType.FASTER_PATH, SuggestionType.ALTERNATE_ROUTE -> "⚡"
                SuggestionType.CONGESTION_AHEAD, SuggestionType.INCIDENT_AHEAD -> "🚧"
                SuggestionType.QUIETER_STREET -> "🌿"
            }
            b.tvDescription.text = item.description
            if (item.altRoute != null) {
                b.btnTakeRoute.visibility = View.VISIBLE
                b.btnTakeRoute.setOnClickListener { onSuggestionClick(item) }
            } else {
                b.btnTakeRoute.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemSuggestionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object Diff : DiffUtil.ItemCallback<TrafficSuggestion>() {
        override fun areItemsTheSame(a: TrafficSuggestion, b: TrafficSuggestion) = a.id == b.id
        override fun areContentsTheSame(a: TrafficSuggestion, b: TrafficSuggestion) = a == b
    }
}
