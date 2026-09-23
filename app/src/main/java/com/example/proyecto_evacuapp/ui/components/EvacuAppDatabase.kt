package com.example.proyecto_evacuapp.ui.components

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [IncidentEntity::class, RoadNodeEntity::class, RoadEdgeEntity::class, SafeZoneEntity::class],
    version = 3,
    exportSchema = false
)
abstract class EvacuAppDatabase : RoomDatabase() {

    abstract fun incidentDao(): IncidentDao
    abstract fun roadGraphDao(): RoadGraphDao
    abstract fun safeZoneDao(): SafeZoneDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `road_nodes` (
                        `id` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `road_edges` (
                        `id` TEXT NOT NULL, `fromNodeId` TEXT NOT NULL, `toNodeId` TEXT NOT NULL, 
                        `distanceMeters` REAL NOT NULL, `riskWeight` REAL NOT NULL DEFAULT 0.0, 
                        `accessibilityPenalty` REAL NOT NULL DEFAULT 0.0, `isBidirectional` INTEGER NOT NULL DEFAULT 1, 
                        `isBlocked` INTEGER NOT NULL DEFAULT 0, `blockingIncidentLocalId` TEXT, PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_road_edges_fromNodeId` ON `road_edges` (`fromNodeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_road_edges_toNodeId` ON `road_edges` (`toNodeId`)")
            }
        }

        /**
         * Migración v2 -> v3: Agrega la tabla de zonas seguras.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `safe_zones` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `capacity` INTEGER NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL
                    )
                """.trimIndent())
            }
        }

        @Volatile
        private var instance: EvacuAppDatabase? = null

        fun getInstance(context: Context): EvacuAppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EvacuAppDatabase::class.java,
                    "evacuapp_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            prePopulateSafeZones(context)
                        }
                    })
                    .build().also { database ->
                        instance = database
                    }
            }
        }

        private fun prePopulateSafeZones(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = getInstance(context)
                db.safeZoneDao().insertAll(
                    listOf(
                        SafeZoneEntity(name = "Hospital El Pino", description = "Complejo hospitalario de alta complejidad.", capacity = 290, latitude = -33.584536860549434, longitude = -70.67672496849106),
                        SafeZoneEntity(name = "Ilustre Municipalidad de San Bernardo", description = "Centro administrativo comunal.", capacity = 350, latitude = -33.591916722304575, longitude = -70.70606947804932),
                        SafeZoneEntity(name = "Estadio Municipal Luis Navarro Avilés", description = "Espacio abierto oficial para soporte.", capacity = 3500, latitude = -33.59437521938217, longitude = -70.69030789789637),
                        SafeZoneEntity(name = "Hospital Parroquial de San Bernardo", description = "Complejo medico de San Bernardo", capacity = 168, latitude = -33.59230302643862, longitude = -70.69714243689805),
                        SafeZoneEntity(name = "Estadio Municipal de La Cisterna", description = "Recinto deportivo masivo sobre Av. El Parrón.", capacity = 8000, latitude = -33.52047712789239, longitude = -70.67301068771287),
                        SafeZoneEntity(name = "Parque O'Higgins (Zona de Despliegue Masivo)", description = "Gran explanada verde comunal.", capacity = 25000, latitude = -33.46323771542285, longitude = -70.6597903933963),
                        SafeZoneEntity(name = "Palacio de La Moneda", description = "Sede de gobierno nacional.", capacity = 1200, latitude = -33.44111740037891, longitude = -70.65373967510075)
                    )
                )
            }
        }
    }
}