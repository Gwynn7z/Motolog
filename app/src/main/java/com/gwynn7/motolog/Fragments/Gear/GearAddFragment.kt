package com.gwynn7.motolog.Fragments.Gear

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
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gwynn7.motolog.Models.Gear
import com.gwynn7.motolog.Path
import com.gwynn7.motolog.R
import com.gwynn7.motolog.ViewModel.GearViewModel
import com.gwynn7.motolog.dateFromLong
import com.gwynn7.motolog.longFromDate
import com.gwynn7.motolog.showToast
import java.util.Calendar
import java.util.Date

class GearAddFragment : Fragment() {
    private lateinit var mGearViewModel: GearViewModel
    private val args by navArgs<GearAddFragmentArgs>()
    private var savedDate: Long = Date().time
    private var currentPath: Path = Path.Add
    private var tempBitmap: Bitmap? = null
    private var bShouldRemoveImage = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.gear_add, container, false)
        mGearViewModel = ViewModelProvider(this)[GearViewModel::class.java]

        if(args.currentGear != null) currentPath = Path.Edit

        val imageAdd = view.findViewById<ImageButton>(R.id.ib_gear_image)
        if(currentPath == Path.Edit)
        {
            val currentGear = args.currentGear!!
            savedDate = currentGear.date

            view.findViewById<EditText>(R.id.et_gear_manufacturer).setText(currentGear.manufacturer)
            view.findViewById<EditText>(R.id.et_gear_model).setText(currentGear.model)
            view.findViewById<EditText>(R.id.et_gear_price).setText(currentGear.price.toString())

            if(currentGear.image != null) imageAdd.setImageURI(currentGear.image)
            else imageAdd.setImageResource(R.drawable.add_photo)
        }

        val date = view.findViewById<DatePicker>(R.id.dp_gear_date)
        date.maxDate = Date().time
        date.init(dateFromLong(savedDate, Calendar.YEAR), dateFromLong(savedDate, Calendar.MONTH), dateFromLong(savedDate, Calendar.DAY_OF_MONTH))
        { _, year, month, dayOfMonth ->
            savedDate = longFromDate(year, month, dayOfMonth)
        }

        val button = view.findViewById<Button>(R.id.bt_deleteGear)
        button.visibility = if(currentPath == Path.Edit) View.VISIBLE else View.GONE
        button.setOnClickListener{
            deleteGear()
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
        val manufacturer = view.findViewById<EditText>(R.id.et_gear_manufacturer).text.toString().trim()
        val model = view.findViewById<EditText>(R.id.et_gear_model).text.toString().trim()
        val price = view.findViewById<EditText>(R.id.et_gear_price).text.toString()

        if(inputCheck(manufacturer, model, price))
        {
            if(currentPath == Path.Add) {
                mGearViewModel.addGear(Gear(0, manufacturer, model, price.toDouble(), savedDate), tempBitmap)
                showToast(requireContext(), getString(R.string.gear_added), Toast.LENGTH_LONG)
            }
            else
            {
                val updatedGear = args.currentGear!!.copy(manufacturer = manufacturer, model = model, price = price.toDouble(), date = savedDate)
                mGearViewModel.updateGear(updatedGear, tempBitmap, bShouldRemoveImage)
                showToast(requireContext(), getString(R.string.gear_save))
            }
            findNavController().navigateUp()
        }
        else showToast(requireContext(), getString(R.string.fill_fields))
    }

    private fun inputCheck(manufacturer: String, model: String, price: String): Boolean
    {
        return manufacturer.isNotEmpty() && model.isNotEmpty() && price.isNotEmpty()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.save_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.save_menu) insertDataToDatabase(requireView())
        return super.onContextItemSelected(item)
    }

    private fun deleteGear()
    {
        val currentGear = args.currentGear!!
        MaterialAlertDialogBuilder(requireContext())
            .setPositiveButton(getString(R.string.yes)){ _, _ ->
            mGearViewModel.deleteGear(currentGear)
            showToast(requireContext(), getString(R.string.gear_delete))
            findNavController().navigateUp()
        }
            .setNegativeButton(getString(R.string.no), null)
            .setTitle("${getString(R.string.delete)} ${currentGear.manufacturer} ${currentGear.model}?")
            .setMessage(getString(R.string.delete_gear_question))
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
            requireView().findViewById<ImageButton>(R.id.ib_gear_image).setImageBitmap(bitmap)
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