package com.example.travel_companion.presentation.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.travel_companion.presentation.viewmodel.HomeViewModel

class HomeFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    }

    fun getViewModel(): Class<HomeViewModel> {
        return HomeViewModel::class.java
    }
}