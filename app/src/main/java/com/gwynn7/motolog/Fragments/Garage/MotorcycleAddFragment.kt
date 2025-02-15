package com.gwynn7.motolog.Fragments.Garage

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gwynn7.motolog.Models.DistanceLog
import com.gwynn7.motolog.Models.Motorcycle
import com.gwynn7.motolog.Models.getUpdatedBikeDistance
import com.gwynn7.motolog.Path
import com.gwynn7.motolog.R
import com.gwynn7.motolog.UnitHelper
import com.gwynn7.motolog.ViewModel.MotorcycleViewModel
import com.gwynn7.motolog.capitalize
import com.gwynn7.motolog.dateFromLong
import com.gwynn7.motolog.showToast
import java.util.Calendar

class MotorcycleAddFragment : Fragment() {
    private lateinit var mMotorcycleViewModel: MotorcycleViewModel
    private val args by navArgs<MotorcycleAddFragmentArgs>()
    private var currentPath: Path = Path.Add
    private var tempBitmap: Bitmap? = null
    private var bShouldRemoveImage: Boolean = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.motorcycle_add, container, false)
        mMotorcycleViewModel = ViewModelProvider(this)[MotorcycleViewModel::class.java]

        if(args.currentMotorcycle != null) currentPath = Path.Edit

        view.findViewById<TextView>(R.id.textView_bike_distance_pre).text = capitalize(getString(R.string.distance_when_bought, UnitHelper.getDistanceText(requireContext())))
        val imageAdd = view.findViewById<ImageButton>(R.id.ib_bike_image)

        if(currentPath == Path.Edit) {
            val bike = args.currentMotorcycle!!
            view.findViewById<EditText>(R.id.et_bike_manufacturer).setText(bike.manufacturer)
            view.findViewById<EditText>(R.id.et_bike_model).setText(bike.model)
            view.findViewById<EditText>(R.id.et_bike_alias).setText(bike.alias)
            view.findViewById<EditText>(R.id.et_bike_year).setText(bike.year.toString())
            view.findViewById<EditText>(R.id.et_bike_startkm).setText(bike.start_km.toString())

            if(bike.image != null) imageAdd.setImageURI(bike.image)
            else imageAdd.setImageResource(R.drawable.add_photo)
        }

        val button = view.findViewById<Button>(R.id.bt_deleteMotorcycle)
        button.visibility = if(currentPath == Path.Edit) View.VISIBLE else View.INVISIBLE
        button.setOnClickListener {
            deleteMotorcycle()
        }


        imageAdd.setOnClickListener{
            uploadImage()
        }

        imageAdd.setOnLongClickListener{
            tempBitmap = null
            bShouldRemoveImage = true
            imageAdd.setImageResource(R.drawable.add_photo)
            true
        }

        setHasOptionsMenu(true)
        return view
    }

    private fun insertDataToDatabase(view: View) {
        val manufacturer = view.findViewById<EditText>(R.id.et_bike_manufacturer).text.toString().trim()
        val model = view.findViewById<EditText>(R.id.et_bike_model).text.toString().trim()
        val name = view.findViewById<EditText>(R.id.et_bike_alias).text.toString().trim()
        val year = view.findViewById<EditText>(R.id.et_bike_year).text.toString()
        val startKm = view.findViewById<EditText>(R.id.et_bike_startkm).text.toString()

        if(inputCheck(manufacturer, model, year))
        {
            val km = if(startKm.isEmpty()) 0 else startKm.toInt()
            val yearInt = year.toInt()

            if(yearInt > Calendar.getInstance().get(Calendar.YEAR)) {
                showToast(requireContext(), getString(R.string.bike_future))
                return
            }
            if(yearInt < 1900) {
                showToast(requireContext(), getString(R.string.bike_past))
                return
            }
            if(currentPath == Path.Edit)
            {
                val motorcycle = args.currentMotorcycle!!.copy(manufacturer = manufacturer, model = model, alias = name, year = yearInt, start_km = km, personal_km = 0)

                if(motorcycle.logs.distance.any { log -> distanceLogsCheck(log, motorcycle.start_km, motorcycle.year) })
                {
                    MaterialAlertDialogBuilder(requireContext())
                        .setPositiveButton(getString(R.string.delete_logs)){ _,_ ->
                            motorcycle.logs.distance = motorcycle.logs.distance.filter { log -> !distanceLogsCheck(log, motorcycle.start_km, motorcycle.year) }
                            motorcycle.personal_km = getUpdatedBikeDistance(motorcycle)
                            mMotorcycleViewModel.updateMotorcycle(motorcycle, tempBitmap, bShouldRemoveImage)
                            showToast(requireContext(), getString(R.string.bike_saved))
                            findNavController().navigateUp()
                        }
                        .setNegativeButton(getString(R.string.back), null)
                        .setTitle(getString(R.string.log_mismatch))
                        .setMessage(getString(R.string.log_mismatch_action))
                        .show()
                    return
                }
                else
                {
                    motorcycle.personal_km = getUpdatedBikeDistance(motorcycle)
                    mMotorcycleViewModel.updateMotorcycle(motorcycle, tempBitmap, bShouldRemoveImage)
                }
                showToast(requireContext(), getString(R.string.bike_saved))
            }
            else
            {
                val motorcycle = Motorcycle(0, manufacturer, model, name, yearInt, km)
                mMotorcycleViewModel.addMotorcycle(motorcycle, tempBitmap)
                showToast(requireContext(), getString(R.string.bike_add), Toast.LENGTH_LONG)
            }

            findNavController().navigateUp()
        }
        else showToast(requireContext(), getString(R.string.fill_fields))
    }

    private fun inputCheck(manufacturer: String, model: String, year: String): Boolean
    {
        return manufacturer.isNotEmpty() && model.isNotEmpty() && year.isNotEmpty()
    }

    private fun distanceLogsCheck(log: DistanceLog, startKm: Int, bikeYear: Int): Boolean
    {
        return dateFromLong(log.date, Calendar.YEAR) < bikeYear || log.distance < startKm
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.save_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.save_menu) insertDataToDatabase(requireView())
        return super.onContextItemSelected(item)
    }

    private fun deleteMotorcycle()
    {
        val currentBike = args.currentMotorcycle!!
        MaterialAlertDialogBuilder(requireContext())
            .setPositiveButton(getString(R.string.yes)){ _, _ ->
            mMotorcycleViewModel.deleteMotorcycle(currentBike)
            showToast(requireContext(),getString(R.string.bike_delete))
            findNavController().navigateUp()
        }
            .setNegativeButton(getString(R.string.no), null)
            .setTitle("${getString(R.string.delete)} ${currentBike.manufacturer} ${currentBike.model}?")
            .setMessage(getString(R.string.delete_bike_question))
            .show()
    }

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful && result.uriContent != null) {
            val uriContent = result.uriContent!!

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, uriContent))
            } else {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uriContent)
            }
            requireView().findViewById<ImageButton>(R.id.ib_bike_image).setImageBitmap(bitmap)
            tempBitmap = bitmap

            MaterialAlertDialogBuilder(requireContext())
                .setPositiveButton(getString(R.string.ok), null)
                .setTitle(getString(R.string.image_saved))
                .setMessage(getString(R.string.image_saved_text))
                .show()
        }
    }

    private fun uploadImage() {
        val options = CropImageContractOptions(null, CropImageOptions(imageSourceIncludeGallery = true, imageSourceIncludeCamera = false))
        cropImage.launch(options)
    }
}