package com.urbanguide

import androidx.compose.runtime.Composable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

@Composable
fun GoogleMapMarker(markerData: MarkerData) {
    Marker(
        state = MarkerState(position = markerData.position),
        title = markerData.title
    )
}

