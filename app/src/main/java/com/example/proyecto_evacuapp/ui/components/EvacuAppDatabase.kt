package com.example.proyecto_evacuapp.ui.components

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [IncidentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EvacuAppDatabase : RoomDatabase() {

    abstract fun incidentDao(): IncidentDao

    companion object {
        @Volatile
        private var instance: EvacuAppDatabase? = null

        fun getInstance(context: Context): EvacuAppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EvacuAppDatabase::class.java,
                    "evacuapp_database"
                ).build().also { database ->
                    instance = database
                }
            }
        }
    }
}