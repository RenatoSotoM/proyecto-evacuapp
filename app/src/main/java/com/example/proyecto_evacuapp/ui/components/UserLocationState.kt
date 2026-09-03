package com.example.proyecto_evacuapp.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.osmdroid.util.GeoPoint

object UserLocationState {
    // Variable global observable por toda la App (MapScreen y RoutesScreen)
    var currentLocation by mutableStateOf<GeoPoint?>(null)
}