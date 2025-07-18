package com.example.travel_companion.presentation

import android.app.Activity
import android.content.Intent
import android.view.View

/**
 * Start new [Activity] using the provided [Class]
 *
 * @param A Activity class
 * @param activity Provide [Activity] to be started
 */
fun <A : Activity> Activity.startNewActivity(activity: Class<A>) {
    val intent = Intent(this, activity).also {
        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(it)
    }
}

/**
 * Enable/disable visibility of View objects
 *
 * @param isVisible
 */
fun View.visible(isVisible: Boolean) {
    visibility = if(isVisible) View.VISIBLE else View.GONE
}

fun View.enable(enabled: Boolean) {
    isEnabled = enabled
    alpha = if(enabled) 1f else 0.5f
}


