package com.urbanguide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.model.LatLng
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.style.layers.generated.rasterLayer
import com.mapbox.maps.extension.style.sources.generated.rasterSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import java.net.URL
@Composable
fun MapBoxComponent(mapData: List<DataBeam>) {
    val context = LocalContext.current
    val modena = convertLatLangToPoint(LatLng(44.646469, 10.925139))

    // Use remember to instantiate and remember the initial state
    var markers by remember { mutableStateOf(listOf<MarkerData>()) }
    var heatmaps by remember { mutableStateOf(listOf<HeatmapData>()) }

    // Update markers and heatmaps based on mapData
    markers = mapData.filterIsInstance<MarkerData>()
    heatmaps = mapData.filterIsInstance<HeatmapData>()

    // Prepare your API key and tile server URL
    val apiKey = "Google_Maps_Api_Key"
    val tileServerUrl = "https://airquality.googleapis.com/v1/mapTypes/UAQI_RED_GREEN/heatmapTiles/{z}/{x}/{y}?key=${apiKey}"

    // MapView initialization
    val mapView = rememberMapViewWithLifecycle()
    val pointAnnotationManager = remember { mapView.annotations.createPointAnnotationManager() }

    // Observe heatmaps list and update the map style accordingly
    LaunchedEffect(heatmaps) {
        mapView.mapboxMap.loadStyle(
            style(Style.STANDARD) {
                if (heatmaps.isNotEmpty()) {
                    +rasterSource("xyz-tile-source") {
                        tiles(listOf(tileServerUrl))
                        tileSize(256)
                        maxzoom(16)
                    }
                    +rasterLayer("xyz-tile-layer", "xyz-tile-source") {
                        rasterOpacity(0.8)
                    }
                }
            }
        ) {
            // Apply camera settings after style is loaded
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(modena)
                    .zoom(15.0)
                    .pitch(30.0)
                    .build()
            )

        }
    }

    LaunchedEffect(markers) {
        addMarkersToMap(context, markers, pointAnnotationManager)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView }
    )
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context, MapInitOptions(context).apply {
            MapboxOptions.accessToken = "Mapbox_Public_Key"
        })
    }

    // Handle lifecycle events
    DisposableEffect(context) {
        onDispose {
            mapView.onDestroy()
        }
    }

    return mapView
}

fun convertLatLangToPoint(latLang : LatLng) : Point {
    return Point.fromLngLat(latLang.longitude,latLang.latitude)
}

fun addMarkersToMap(context: Context, markers: List<MarkerData>, pointAnnotationManager: PointAnnotationManager?) {
    pointAnnotationManager?.deleteAll()

    // Add new markers
    markers.forEach { markerData ->
        val point = convertLatLangToPoint(markerData.position)
        val iconImage = BitmapFactory.decodeResource(context.resources, R.drawable.mapbox_marker_icon_20px_blue)

        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(iconImage)
            .withTextField(markerData.title)

        pointAnnotationManager?.create(pointAnnotationOptions)
    }
}
