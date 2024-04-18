package com.example.motolog.Fragments.RepairsLog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.motolog.Models.Motorcycle
import com.example.motolog.Models.RepairsLog
import com.example.motolog.R
import com.example.motolog.longToDateString
import com.example.motolog.repairColors

class RepairsLogAdapter: RecyclerView.Adapter<RepairsLogAdapter.MyViewHolder>() {
    private var repairsLogList = emptyList<RepairsLog>()
    private lateinit var currentBike: Motorcycle
    class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.repairslog_row, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return repairsLogList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = repairsLogList[position]

        holder.itemView.findViewById<TextView>(R.id.tw_repair_type).text = currentItem.typeText
        holder.itemView.findViewById<TextView>(R.id.tw_repair_notes).text = currentItem.notes
        holder.itemView.findViewById<TextView>(R.id.tw_repair_date).text = longToDateString(currentItem.date)

        val id = if(currentItem.typeIndex == -1) repairColors.size-1 else currentItem.typeIndex
        holder.itemView.findViewById<ImageView>(R.id.repair_image).setColorFilter(holder.itemView.resources.getColor(repairColors[id], null));

        val price = String.format("${holder.itemView.resources.getString(R.string.price)}: %.2f€", currentItem.price)
        holder.itemView.findViewById<TextView>(R.id.tw_repair_price).text = price

        holder.itemView.findViewById<CardView>(R.id.cv_repairs_row).setOnClickListener {
            val action = RepairsLogFragmentDirections.repairslistToRepairsadd(currentBike, position)
            holder.itemView.findNavController().navigate(action)
        }
    }

    fun bindBike(bike: Motorcycle)
    {
        repairsLogList = bike.maintenance_logs
        currentBike = bike
        notifyDataSetChanged()
    }
}