package com.vs.supermarket.adapters

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.vs.supermarket.R
import com.vs.supermarket.fragments.*

private val TAB_TITLES = arrayOf(
    R.string.tab_text_1,
    R.string.tab_text_2,
    R.string.tab_text_3,
    R.string.tab_text_4,
    R.string.tab_text_5,
    R.string.tab_text_6,
)

class SectionsPagerAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> FoodFragment()
            2 -> GroceryFragment()
            3 -> BeverageFragment()
            4 -> DetergentsFragment()
            5 -> OtherFragment()
            else -> HomeFragment()
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        // Show 6 total pages.
        return 6
    }
}