package com.urbanguide

import android.gesture.Gesture
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.TileOverlay
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.generated.LocationComponentSettings


@Composable
fun MapBoxComponent(mapData: List<DataBeam>) {

    MapboxOptions.accessToken = "Mapbox_Public_Key"

    val modena = convertLatLangToPoint(LatLng(44.646469, 10.925139))

    var markers by remember { mutableStateOf(listOf<MarkerData>()) }
    var heatmaps by remember { mutableStateOf(listOf<HeatmapData>()) }

    markers = mapData.filterIsInstance<MarkerData>()
    heatmaps = mapData.filterIsInstance<HeatmapData>()

    MapboxMap(
        Modifier.fillMaxSize(),
        mapViewportState = MapViewportState().apply {
            setCameraOptions {
                zoom(15.0)
                center(modena)
                pitch(30.0)
                bearing(0.0)
            }
        }
    ) {
        GetMapBoxMarkers(markers)
        GetMapBoxOverlays(heatmaps)
    }
}

fun convertLatLangToPoint(latLang : LatLng) : Point {
    return Point.fromLngLat(latLang.longitude,latLang.latitude)
}

@Composable
fun GetMapBoxMarkers(markers: List<MarkerData>) {
    val iconImage : Bitmap = BitmapFactory.decodeResource(LocalContext.current.resources, R.drawable.mapbox_marker_icon_20px_red)

    var selectedMarker by remember { mutableStateOf<MarkerData?>(null) }

    markers.forEach {markerData: MarkerData ->
        PointAnnotation(
            point = convertLatLangToPoint(markerData.position),
            iconImageBitmap = iconImage,
            textField = markerData.title,
            onClick = { _: PointAnnotation ->
                selectedMarker = markerData
                true
            },

        )
    }
}

@Composable
fun GetMapBoxOverlays(markers: List<HeatmapData>) {
    markers.forEach {_: HeatmapData ->
        
    }
}
