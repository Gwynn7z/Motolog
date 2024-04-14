package com.example.motolog.Fragments.Garage

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.motolog.Models.DistanceLog
import com.example.motolog.Models.Motorcycle
import com.example.motolog.Models.getUpdateBikeDistance
import com.example.motolog.Path
import com.example.motolog.R
import com.example.motolog.ViewModel.MotorcycleViewModel
import com.example.motolog.showToast
import com.example.motolog.yearFromLong
import java.util.Calendar


class MotorcycleAddFragment : Fragment() {
    private lateinit var mMotorcycleViewModel: MotorcycleViewModel
    private val args by navArgs<MotorcycleAddFragmentArgs>()
    private var currentPath: Path = Path.Add
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.motorcycle_add, container, false)
        mMotorcycleViewModel = ViewModelProvider(this)[MotorcycleViewModel::class.java]

        if(args.currentMotorcycle != null) currentPath = Path.Edit
        val buttonText = if(currentPath == Path.Add) "Add Motorcycle" else "Edit Motorcycle"

        if(currentPath == Path.Edit) {
            val bike = args.currentMotorcycle!!
            view.findViewById<EditText>(R.id.et_bike_manufacturer).setText(bike.manufacturer)
            view.findViewById<EditText>(R.id.et_bike_model).setText(bike.model)
            view.findViewById<EditText>(R.id.et_bike_alias).setText(bike.alias)
            view.findViewById<EditText>(R.id.et_bike_year).setText(bike.year.toString())
            view.findViewById<EditText>(R.id.et_bike_startkm).setText(bike.start_km.toString())
        }

        val button = view.findViewById<Button>(R.id.bt_addMotorcycle)
        button.text = buttonText
        button.setOnClickListener {
            insertDataToDatabase(view)
        }

        setHasOptionsMenu(currentPath == Path.Edit)
        return view
    }

    private fun insertDataToDatabase(view: View) {
        val manufacturer = view.findViewById<EditText>(R.id.et_bike_manufacturer).text.toString()
        val model = view.findViewById<EditText>(R.id.et_bike_model).text.toString()
        val name = view.findViewById<EditText>(R.id.et_bike_alias).text.toString()
        val year = view.findViewById<EditText>(R.id.et_bike_year).text.toString()
        val startKm = view.findViewById<EditText>(R.id.et_bike_startkm).text.toString()

        if(inputCheck(manufacturer, model, year))
        {
            val km = if(startKm.isEmpty()) 0 else startKm.toInt()
            val yearInt = year.toInt()

            if(yearInt > Calendar.getInstance().get(Calendar.YEAR)) {
                showToast(requireContext(), "Bike from the future not allowed")
                return
            }
            if(yearInt < 1900) {
                showToast(requireContext(), "Bikes didn't exist before 1900")
                return
            }
            if(currentPath == Path.Edit)
            {
                val motorcycle = args.currentMotorcycle!!.copy(manufacturer = manufacturer, model = model, alias = name, year = yearInt, start_km = km, personal_km = 0)

                if(motorcycle.km_logs.any { log -> distanceLogsCheck(log, motorcycle.start_km, motorcycle.year) })
                {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setPositiveButton("Delete logs"){ _,_ ->
                        motorcycle.km_logs = motorcycle.km_logs.filter { log -> !distanceLogsCheck(log, motorcycle.start_km, motorcycle.year) }
                        motorcycle.personal_km = getUpdateBikeDistance(motorcycle)
                        mMotorcycleViewModel.updateMotorcycle(motorcycle)
                        showToast(requireContext(), "Motorcycle saved!")
                        findNavController().navigateUp()
                    }
                    builder.setNegativeButton("Back"){ _,_ -> }
                    builder.setTitle("Distance logs mismatch found")
                    builder.setMessage("Some logs happened before the new date or with less kilometers will be eliminated")
                    builder.create().show()
                    return
                }
                else
                {
                    motorcycle.personal_km = getUpdateBikeDistance(motorcycle)
                    mMotorcycleViewModel.updateMotorcycle(motorcycle)
                }
            }
            else
            {
                val motorcycle = Motorcycle(0, manufacturer, model, name, yearInt, km)
                mMotorcycleViewModel.addMotorcycle(motorcycle)
            }

            showToast(requireContext(), "Motorcycle saved!")
            findNavController().navigateUp()
        }
        else showToast(requireContext(), "Please fill every field (Alias is optional)")
    }

    private fun inputCheck(manufacturer: String, model: String, year: String): Boolean
    {
        return manufacturer.isNotEmpty() && model.isNotEmpty() && year.isNotEmpty()
    }

    private fun distanceLogsCheck(log: DistanceLog, startKm: Int, bikeYear: Int): Boolean
    {
        return yearFromLong(log.date) < bikeYear || log.distance < startKm
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.delete_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.menu_delete) deleteMotorcycle()
        return super.onContextItemSelected(item)
    }

    private fun deleteMotorcycle()
    {
        val currentBike = args.currentMotorcycle!!
        val builder = AlertDialog.Builder(requireContext())
        builder.setPositiveButton("Yes"){ _,_ ->
            mMotorcycleViewModel.deleteMotorcycle(currentBike)
            showToast(requireContext(),"Motorcycle deleted!")
            findNavController().navigateUp()
        }
        builder.setNegativeButton("No"){ _,_ -> }
        builder.setTitle("Delete ${currentBike.manufacturer} ${currentBike.model}?")
        builder.setMessage("Are you sure you want to delete this motorcycle?")
        builder.create().show()
    }
}