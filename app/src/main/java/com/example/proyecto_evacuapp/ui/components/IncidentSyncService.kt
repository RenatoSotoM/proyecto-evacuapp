package com.example.proyecto_evacuapp.ui.components

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.proyecto_evacuapp.data.remote.IncidentSyncWorker

object IncidentSyncService {

    fun scheduleSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<IncidentSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    suspend fun syncPendingActions(): Result<Unit> {
        return Result.success(Unit)
    }
}