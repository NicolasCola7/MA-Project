package com.example.travel_companion.presentation.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException

class ViewModelFactory(): ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            //TODO declare all ViewModel like the following example:
            //modelClass.isAssignableFrom(ExampleViewModel::class.java) -> ExampleViewModel(repository as ExampleRepository) as T
            else -> throw IllegalArgumentException("Class Not found")
        }

    }
}