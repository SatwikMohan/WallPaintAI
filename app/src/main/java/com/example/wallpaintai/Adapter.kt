package com.example.wallpaintai

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class Adapter(val list:List<AISuggestionObject>,val clickInterface: ClickInterface): RecyclerView.Adapter<Adapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorName = itemView.findViewById<TextView>(R.id.colorName)
        val colorView: View = itemView.findViewById(R.id.color_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Adapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_color, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: Adapter.ViewHolder, position: Int) {
        val color = Color.parseColor(list[position].HexCode)
        holder.colorView.setBackgroundColor(color)
        holder.colorName.text = list[position].color
        holder.colorView.setOnClickListener {
            clickInterface.onClick(color)
        }
        holder.colorView.setOnLongClickListener {
            clickInterface.onLongClick(list[position].reason)
            true
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}