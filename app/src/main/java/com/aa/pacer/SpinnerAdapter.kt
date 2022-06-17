package com.aa.pacer

import android.R
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.aa.pacer.databinding.SpinnerRowBinding


class MySpinnerAdapter(
    context: Context?,
    textViewResourceId: Int,
    textViewResourceId1: Int,
    objects: ArrayList<String?>,
    val iName: ArrayList<String?>
) : ArrayAdapter<String> (context!!, textViewResourceId, textViewResourceId1,
    objects!!)
{
    var spnItemDel:TextView? = null


    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        return getCustomView(position, view, parent)
    }

    fun getCustomView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val inflater = (context as Activity).layoutInflater
        val binding = SpinnerRowBinding.inflate(inflater)

        spnItemDel = binding.spnItemDel

        binding.spnItemName!!.text = iName!![position] + ""
        binding.spnItemDel!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                //iName[position] = null;
                iName!!.removeAt(position)
                notifyDataSetChanged()
            }
        })
        return binding.root
    }
}