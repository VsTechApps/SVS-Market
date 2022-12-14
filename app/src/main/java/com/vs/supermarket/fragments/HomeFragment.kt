package com.vs.supermarket.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.smarteist.autoimageslider.IndicatorView.animation.type.IndicatorAnimationType
import com.smarteist.autoimageslider.SliderAnimations
import com.smarteist.autoimageslider.SliderView
import com.vs.supermarket.MainActivity
import com.vs.supermarket.R
import com.vs.supermarket.adapters.HomeRecyclerViewAdapter
import com.vs.supermarket.adapters.SliderAdapter
import com.vs.supermarket.models.HomeItem
import com.vs.supermarket.models.SliderItem

class HomeFragment : Fragment(), HomeRecyclerViewAdapter.OnItemClickListener {

    lateinit var adapter: SliderAdapter
    private val db = FirebaseFirestore.getInstance()
    private val sliderRef = db.collection("slider")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        val sliderView: SliderView = root.findViewById(R.id.imageSlider)

        adapter = SliderAdapter(context!!)

        sliderView.setSliderAdapter(adapter)

        sliderView.setIndicatorAnimation(IndicatorAnimationType.WORM)

        sliderView.setSliderTransformAnimation(SliderAnimations.SIMPLETRANSFORMATION)
        sliderView.autoCycleDirection = SliderView.AUTO_CYCLE_DIRECTION_BACK_AND_FORTH
        sliderView.indicatorSelectedColor = Color.WHITE
        sliderView.indicatorUnselectedColor = Color.GRAY
        sliderView.scrollTimeInSec = 4
        sliderView.startAutoCycle()

        renewItems()

        val view = root.findViewById<RecyclerView>(R.id.viewHome)
        view.adapter = HomeRecyclerViewAdapter(generateHomeList(), this)
        view.layoutManager = GridLayoutManager(context, 2)
        view.hasFixedSize()
        return root
    }

    private fun renewItems() {
        val sliderItemList: MutableList<SliderItem> = ArrayList()
        sliderItemList.add(SliderItem("", R.drawable.logo))
        sliderItemList.add(SliderItem("", R.drawable.ontime))
        sliderItemList.add(SliderItem("", R.drawable.logo))
        adapter.renewItems(sliderItemList)
    }

    private fun generateHomeList(): List<HomeItem> {

        val list = ArrayList<HomeItem>()

        list += HomeItem("Food Items", R.drawable.food)
        list += HomeItem("Grocery", R.drawable.grocery)
        list += HomeItem("Beverages", R.drawable.beverages)
        list += HomeItem("Detergents", R.drawable.detergents)
        list += HomeItem("Kitchen & Cleaning", R.drawable.kitchen)
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
            "Kitchen & Cleaning" -> {
                (activity as MainActivity?)?.navigateToPage(5)
            }
            "Other" -> {
                (activity as MainActivity?)?.navigateToPage(6)
            }
        }
    }
}