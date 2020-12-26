package com.vs.supermarket.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vs.supermarket.MainActivity
import com.vs.supermarket.R
import com.vs.supermarket.adapters.HomeRecyclerViewAdapter
import com.vs.supermarket.models.HomeItem

class HomeFragment : Fragment(), HomeRecyclerViewAdapter.OnItemClickListener {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val view = root.findViewById<RecyclerView>(R.id.viewHome)
        view.adapter = HomeRecyclerViewAdapter(generateHomeList(), this)
        view.layoutManager = GridLayoutManager(context, 2)
        view.hasFixedSize()
        return root
    }

    private fun generateHomeList(): List<HomeItem> {

        val list = ArrayList<HomeItem>()

        list += HomeItem("Food Items", R.drawable.food)
        list += HomeItem("Grocery", R.drawable.grocery)
        list += HomeItem("Beverages", R.drawable.beverages)
        list += HomeItem("Detergents", R.drawable.detergents)
        list += HomeItem("Other", R.drawable.other)

        return list
    }

    override fun onItemClick(title: String) {
        when (title) {
            "Food Items" -> {
                (activity as MainActivity?)?.navigateToPage(1)
            }
            "Grocery" -> {
                (activity as MainActivity?)?.navigateToPage(2)
            }
            "Beverages" -> {
                (activity as MainActivity?)?.navigateToPage(3)
            }
            "Detergents" -> {
                (activity as MainActivity?)?.navigateToPage(4)
            }
            "Other" -> {
                (activity as MainActivity?)?.navigateToPage(5)
            }
        }
    }
}