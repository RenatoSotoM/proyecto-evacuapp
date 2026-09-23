package com.example.proyecto_evacuapp.ui.components

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Nodo del grafo vial local (intersección o punto de referencia de la red).
 * Se persiste en Room para soportar el modo Offline-First: el grafo se descarga/siembra
 * una vez y luego se consulta y actualiza completamente sin conexión.
 */
@Entity(tableName = "road_nodes")
data class RoadNodeEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Arista del grafo vial (tramo de calle entre dos nodos).
 *
 * riskWeight y accessibilityPenalty están normalizados en [0.0, 1.0] y corresponden a
 * R(e) y A(e) de la función de costo adaptativa:
 *   C(e) = w_d*D(e) + w_t*T(e) + w_r*R(e) + w_a*A(e)
 *
 * El tiempo T(e) no se guarda en la arista: se calcula en tiempo de consulta dividiendo
 * distanceMeters por la velocidad del perfil de movilidad (RouteMobilityProfile), ya que
 * una misma calle toma distinto tiempo a pie, en bicicleta o en vehículo.
 */
@Entity(
    tableName = "road_edges",
    indices = [Index("fromNodeId"), Index("toNodeId")]
)
data class RoadEdgeEntity(
    @PrimaryKey val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val distanceMeters: Double,
    val riskWeight: Double = 0.0,           // R(e): 0 = sin riesgo, 1 = bloqueo total
    val accessibilityPenalty: Double = 0.0, // A(e): 0 = totalmente accesible, 1 = inaccesible
    val isBidirectional: Boolean = true,
    val isBlocked: Boolean = false,
    val blockingIncidentLocalId: String? = null
)

@Dao
interface RoadGraphDao {

    @Query("SELECT * FROM road_nodes")
    suspend fun getAllNodes(): List<RoadNodeEntity>

    @Query("SELECT * FROM road_edges")
    suspend fun getAllEdges(): List<RoadEdgeEntity>

    @Query("SELECT COUNT(*) FROM road_edges")
    suspend fun countEdges(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<RoadNodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdges(edges: List<RoadEdgeEntity>)

    @Query(
        """
        UPDATE road_edges
        SET riskWeight = :riskWeight,
            isBlocked = :isBlocked,
            blockingIncidentLocalId = :incidentLocalId
        WHERE id = :edgeId
        """
    )
    suspend fun updateEdgeRisk(
        edgeId: String,
        riskWeight: Double,
        isBlocked: Boolean,
        incidentLocalId: String?
    )

    @Query(
        """
        UPDATE road_edges
        SET accessibilityPenalty = :penalty
        WHERE id = :edgeId
        """
    )
    suspend fun updateEdgeAccessibility(edgeId: String, penalty: Double)

    @Query(
        """
        UPDATE road_edges
        SET riskWeight = 0.0, isBlocked = 0, blockingIncidentLocalId = NULL
        WHERE blockingIncidentLocalId = :incidentLocalId
        """
    )
    suspend fun clearBlockForIncident(incidentLocalId: String)
}