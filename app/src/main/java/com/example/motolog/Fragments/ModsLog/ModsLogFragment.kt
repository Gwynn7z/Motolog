package com.example.motolog.Fragments.ModsLog

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.motolog.Models.Motorcycle
import com.example.motolog.R
import com.example.motolog.ViewModel.MotorcycleViewModel
import com.example.motolog.showToast
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ModsLogFragment : Fragment() {
    private lateinit var mMotorcycleViewModel: MotorcycleViewModel
    private lateinit var currentbike: Motorcycle
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.modslog_list, container, false)

        val adapter = ModsLogAdapter()
        val recyclerView = view.findViewById<RecyclerView>(R.id.rw_modslogs)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        mMotorcycleViewModel = ViewModelProvider(this)[MotorcycleViewModel::class.java]

        val bikeId = MotorcycleViewModel.currentBikeId
        if(bikeId == null)
        {
            returnToList()
            return view
        }

        val bikeData = mMotorcycleViewModel.getMotorcycle(bikeId)
        bikeData.observe(viewLifecycleOwner, Observer {
            bikes -> run {
            if(bikes.isNotEmpty()) {
                currentbike = bikes.first()
                adapter.bindBike(currentbike)
            }
            else returnToList()
        }
        })

        view.findViewById<FloatingActionButton>(R.id.fab_addModsLog).setOnClickListener{
            val action = ModsLogFragmentDirections.modslogToModsadd(currentbike)
            findNavController().navigate(action)
        }
        setHasOptionsMenu(true)
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.money_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.money_menu){
            var totalMoney = 0.0
            val modsList = currentbike.mods_logs
            for (mod in modsList) totalMoney += mod.price
            showToast(requireContext(), "${getString(R.string.total_spent)}: ${String.format("%.2f€", totalMoney)}")
        }
        return super.onContextItemSelected(item)
    }

    private fun returnToList()
    {
        activity?.finish()
    }
}