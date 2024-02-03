package com.urbanguide

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import java.io.ByteArrayOutputStream
import java.net.URL
import kotlin.random.Random


@Composable
fun GoogleMapComponent(mapData: List<DataBeam>) {
    val modena = LatLng(44.646469, 10.925139)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(modena, 15f)
    }

    var maxZoom by remember { mutableFloatStateOf(100.0f) }
    var markers by remember { mutableStateOf(listOf<MarkerData>()) }
    var heatmaps by remember { mutableStateOf(listOf<HeatmapData>()) }

    markers = mapData.filterIsInstance<MarkerData>()
    heatmaps = mapData.filterIsInstance<HeatmapData>()

    val adjustZoom : (float: Float) -> Unit =  {float: Float ->
        maxZoom = float
    }

    Log.d("Raoul",DataRepository.heatmapPoints.toString())
    val properties = MapProperties(
        isBuildingEnabled = false,
        isIndoorEnabled = false,
        isMyLocationEnabled = false,
        isTrafficEnabled = false,
        latLngBoundsForCameraTarget = null,
        mapStyleOptions = null,
        mapType = MapType.NORMAL,
        maxZoomPreference = maxZoom,
        minZoomPreference = 10.0f
    )

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
    }
}

@Composable
fun GetMarkers(markers: List<MarkerData>) {
    markers.forEach {markerData: MarkerData ->
        GoogleMapMarker(markerData)
    }
}

@Composable
fun GetOverlays(overlays: List<HeatmapData>, maxZoom: (float: Float) -> Unit) {

    if(overlays.isNotEmpty()){
        maxZoom(16.0f)
    } else {
        maxZoom(45.0f)
    }

    overlays.forEach { _: HeatmapData ->
        TileOverlay(
            tileProvider = HeatmapTileProvider("UAQI_RED_GREEN"),
            transparency = 0.2f
        )
    }
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
class CustomTileProvider : TileProvider {
    override fun getTile(x: Int, y: Int, zoom: Int): Tile {
        Log.d("Raoul", "$x $y $zoom")
        val bitmap = createMockBitmap()
        val byteArray = bitmapToByteArray(bitmap)
        return Tile(bitmap.width, bitmap.height, byteArray)
    }
}

fun createMockBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val intensity = Random.nextDouble(0.0, 1.0)
    val color = Color.argb(255, (intensity * 255).toInt(), 0, 0)
    canvas.drawColor(color)
    return bitmap
}

fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}