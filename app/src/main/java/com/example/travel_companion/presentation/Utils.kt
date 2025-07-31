package com.example.travel_companion.presentation

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Build
import com.example.travel_companion.service.Polyline
import com.google.android.gms.maps.model.LatLng
import pub.devrel.easypermissions.EasyPermissions
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object Utils {

    val DEFAULT_POSITION =  LatLng(44.4949, 11.3426)

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
    val dateTimeFormat: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)


    fun hasLocationPermissions(context: Context) =
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }

    fun calculatePolylineLength(polyline: Polyline): Float {
        var distance = 0f
        for(i in 0..polyline.size - 2) {
            val pos1 = polyline[i]
            val pos2 = polyline[i + 1]

            val result = FloatArray(1)
            Location.distanceBetween(
                pos1.latitude,
                pos1.longitude,
                pos2.latitude,
                pos2.longitude,
                result
            )
            distance += result[0]
        }
        return distance
    }

    /**
     * Converte un Bitmap in ByteArray per il salvataggio nel database
     * @param bitmap Il bitmap da convertire
     * @param quality Qualità di compressione (0-100), default 80
     * @return ByteArray dell'immagine compressa
     */
    fun bitmapToByteArray(bitmap: Bitmap, quality: Int = 80): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Converte un ByteArray in Bitmap per la visualizzazione
     * @param byteArray Il ByteArray da convertire
     * @return Bitmap dell'immagine o null se la conversione fallisce
     */
    fun byteArrayToBitmap(byteArray: ByteArray?): Bitmap? {
        return if (byteArray != null && byteArray.isNotEmpty()) {
            try {
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Ridimensiona un bitmap mantenendo le proporzioni
     * @param bitmap Il bitmap originale
     * @param maxWidth Larghezza massima
     * @param maxHeight Altezza massima
     * @return Bitmap ridimensionato
     */
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // Calcola il fattore di scala
        val scaleWidth = maxWidth.toFloat() / originalWidth
        val scaleHeight = maxHeight.toFloat() / originalHeight
        val scaleFactor = minOf(scaleWidth, scaleHeight)

        // Calcola le nuove dimensioni
        val newWidth = (originalWidth * scaleFactor).toInt()
        val newHeight = (originalHeight * scaleFactor).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}



