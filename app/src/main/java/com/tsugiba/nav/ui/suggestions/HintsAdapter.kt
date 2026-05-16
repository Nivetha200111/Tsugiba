package com.tsugiba.nav.ui.suggestions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tsugiba.nav.data.model.HintType
import com.tsugiba.nav.data.model.RouteHint
import com.tsugiba.nav.databinding.ItemHintBinding

class HintsAdapter : ListAdapter<RouteHint, HintsAdapter.VH>(Diff) {

    inner class VH(private val b: ItemHintBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: RouteHint) {
            b.tvInstruction.text = item.instruction
            b.tvDistance.text = item.distanceLabel
            b.tvIcon.text = when (item.type) {
                HintType.TURN_LEFT -> "⬅"
                HintType.TURN_RIGHT -> "➡"
                HintType.ROUNDABOUT -> "↻"
                HintType.ARRIVE -> "📍"
                HintType.STRAIGHT -> "⬆"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemHintBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object Diff : DiffUtil.ItemCallback<RouteHint>() {
        override fun areItemsTheSame(a: RouteHint, b: RouteHint) = a.instruction == b.instruction
        override fun areContentsTheSame(a: RouteHint, b: RouteHint) = a == b
    }
}
