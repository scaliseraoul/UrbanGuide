package com.urbanguide

import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.UrlTileProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.net.URL

/*
    GoogleMap(
        modifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomGesturesEnabled = true,
            zoomControlsEnabled = false,
            rotationGesturesEnabled = true,
            mapToolbarEnabled = true
        ),
        properties = properties
    ) {
        GetMarkers(markers)
        GetOverlays(heatmaps,adjustZoom)
    } */

@Composable
fun GoogleMapComponent(mapData: List<DataBeam>, mqttEventChannel: Channel<MqttEvent>, mqttManager: MQTTManager) {
    val TAG = "GoogleMapComponent"
    val BaseTopic = "AndroidKotlinGoogleMaps"
    val modena = LatLng(44.646469, 10.925139)

    var position by remember { mutableStateOf(modena) }
    var markers by remember { mutableStateOf(listOf<MarkerData>()) }
    var heatmaps by remember { mutableStateOf(listOf<HeatmapData>()) }

    markers = mapData.filterIsInstance<MarkerData>()
    heatmaps = mapData.filterIsInstance<HeatmapData>()

    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    val mapView = rememberGoogleMapViewWithLifecycle()

    val mqttEvents = mqttEventChannel.receiveAsFlow()

    mapView.getMapAsync { map ->
        googleMap = map
        map.isBuildingsEnabled = false
        map.isIndoorEnabled = false
        map.isTrafficEnabled = false
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.setMinZoomPreference(10.0f)
        val startTime = System.nanoTime()
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
        map.setOnCameraIdleListener {
            Log.d("Performance Google", "Move Camera Idle Elapsed ${System.nanoTime() - startTime}")
        }
        Log.d("Performance Google", "Move Camera Out Elapsed ${System.nanoTime() - startTime}")
    }

    mqttManager.subscribe("$BaseTopic${Topics.DrawPoint}Receive")
    mqttManager.subscribe("$BaseTopic${Topics.MoveMap}Receive")

    LaunchedEffect(key1 = mqttEvents) {
        mqttEvents.collect { mqttEvent ->
            when (mqttEvent) {
                is MqttEvent.DrawPointEvent -> {
                    val startTime = System.nanoTime()
                    googleMap?.addMarker(MarkerOptions().position(mqttEvent.position).title(mqttEvent.title))
                    val elapsedTime = System.nanoTime() - startTime
                    val mqttPayload = "${mqttEvent.timestamp_sent},Android,Kotlin,GoogleMap,${Topics.DrawPoint},0,0,$elapsedTime"
                    mqttManager.publish("$BaseTopic${Topics.DrawPoint}Complete",mqttPayload)
                    Log.d("Performance", "payload: $mqttPayload")
                }
                is MqttEvent.MoveMapEvent -> {
                    var startTime : Long = 0
                    var elapsedTime : Long = 0
                    googleMap?.setOnCameraIdleListener {
                        elapsedTime = System.nanoTime() - startTime
                        val mqttPayload = "${mqttEvent.timestamp_sent},Android,Kotlin,GoogleMap,${Topics.MoveMap},0,0,$elapsedTime"
                        mqttManager.publish("$BaseTopic${Topics.MoveMap}Complete",mqttPayload)
                        Log.d("Performance", "payload: $mqttPayload")
                    }
                    startTime = System.nanoTime()
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(mqttEvent.position, 15f))
                }
                else -> {}
            }
        }
    }

    AndroidView(
        factory = { mapView },
        update = { _ ->
            googleMap?.let { map ->
                map.clear()
                markers.forEach { markerData ->
                    map.addMarker(MarkerOptions().position(markerData.position).title(markerData.title))
                }
                if(heatmaps.isNotEmpty()){
                    map.setMaxZoomPreference(16.0f)
                } else {
                    map.setMaxZoomPreference(45.0f)
                }
                heatmaps.forEach {heatmapData ->
                    val tileOverlayOption = TileOverlayOptions()
                    tileOverlayOption.tileProvider(HeatmapTileProvider("UAQI_RED_GREEN"))
                    tileOverlayOption.transparency(0.2f)
                    map.addTileOverlay(tileOverlayOption)
                }
            }
        }
    )
}

@Composable
fun rememberGoogleMapViewWithLifecycle(): MapView {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    // Handle lifecycle events
    DisposableEffect(context) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    return mapView
}

class HeatmapTileProvider(private val heatmapType: String) : UrlTileProvider(TILE_WIDTH, TILE_HEIGHT) {

    companion object {
        private const val TILE_WIDTH = 256
        private const val TILE_HEIGHT = 256
        private const val apiKey = "Google_Maps_Api_Key"
    }

    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
        Log.d("Raoul", "$x $y $zoom")

        return try {
            URL("https://airquality.googleapis.com/v1/mapTypes/$heatmapType/heatmapTiles/$zoom/$x/$y?key=$apiKey")
        } catch (e: Exception) {
            Log.d("Raoul", "${e.message}")
            null
        }
    }
}
