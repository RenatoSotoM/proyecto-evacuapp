package com.example.proyecto_evacuapp

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.proyecto_evacuapp.ui.components.EvacuAppDatabase
import com.example.proyecto_evacuapp.ui.components.RoadEdgeEntity
import com.example.proyecto_evacuapp.ui.components.RoadGraphDao
import com.example.proyecto_evacuapp.ui.components.RoadNodeEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba instrumentada para verificar el rendimiento y tiempo de respuesta
 * de las consultas espaciales por Bounding Box (BBox) sobre Room DB (< 500 ms).
 */
@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {

    private lateinit var database: EvacuAppDatabase
    private lateinit var roadGraphDao: RoadGraphDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, EvacuAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        roadGraphDao = database.roadGraphDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun testBBoxQueryPerformanceUnder500ms() = runBlocking {
        // 1. Sembrar nodos de prueba en la zona de San Bernardo / Calera de Tango
        val nodesList = listOf(
            RoadNodeEntity("node_sb_1", -33.5925, -70.7045),
            RoadNodeEntity("node_sb_2", -33.5940, -70.7060),
            RoadNodeEntity("node_ct_1", -33.6200, -70.7800)
        )
        roadGraphDao.insertNodes(nodesList)

        val edgesList = listOf(
            RoadEdgeEntity("edge_sb_1", "node_sb_1", "node_sb_2", 250.0)
        )
        roadGraphDao.insertEdges(edgesList)

        // 2. Medir tiempo de consulta BBox para San Bernardo
        val startTime = System.currentTimeMillis()
        val minLat = -33.6000
        val maxLat = -33.5800
        val minLon = -70.7100
        val maxLon = -70.6900

        val nodesInBBox = roadGraphDao.getNodesInBBox(minLat, maxLat, minLon, maxLon)
        val edgesInBBox = roadGraphDao.getEdgesInBBox(minLat, maxLat, minLon, maxLon)
        val queryDurationMs = System.currentTimeMillis() - startTime

        // 3. Afirmaciones
        assertTrue("La consulta espacial BBox debe responder en menos de 500 ms", queryDurationMs < 500)
        assertEquals("Debe retornar los nodos dentro del rango geográfico", 2, nodesInBBox.size)
        assertEquals("Debe retornar las aristas conectadas", 1, edgesInBBox.size)
    }
}
