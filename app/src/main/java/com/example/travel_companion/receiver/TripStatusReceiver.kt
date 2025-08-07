package com.example.travel_companion.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.travel_companion.data.repository.TripRepository
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.util.TripScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
/*
    CLASSE CHE GESTISCE LA RICEZIONE E AGGIORNAMENTO DELLO STATO
    La classe riceve il broadcast dal sistema. Legge, da intent, le informazioni relative all'id (del viaggio a cui
    bisogna aggiornare lo stato) e il nuovo stato.
    Infine richiama il metodo "updateTripStatus" della classe TripRepository per aggiornare lo stato sul db.
    Inoltre tramite "goAsync()" e coroutine non viene bloccato il thread del sistema.

    Risultato: L’aggiornamento avviene in background e in modo sicuro anche se
    il ricevitore viene chiamato mentre l’app è chiusa.
 */
@AndroidEntryPoint
class TripStatusReceiver : BroadcastReceiver() {

    @Inject
    lateinit var tripRepository: TripRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TripScheduler.ACTION_UPDATE_TRIP_STATUS) {
            val tripId = intent.getLongExtra(TripScheduler.EXTRA_TRIP_ID, -1)
            val statusName = intent.getStringExtra(TripScheduler.EXTRA_NEW_STATUS)

            if (tripId != -1L && statusName != null) {
                val status = TripStatus.valueOf(statusName)

                // Usa goAsync() per operazioni asincrone
                val pendingResult = goAsync()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        tripRepository.updateTripStatus(tripId, status)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}