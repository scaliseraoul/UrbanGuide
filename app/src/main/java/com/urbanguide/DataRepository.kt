package com.urbanguide

import com.google.android.gms.maps.model.LatLng
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

interface DataBeam {
    val id: String
    val title: String
    val category: Category
    fun getData(){

    }
}

data class MarkerData(
    override val id: String,
    override val title: String,
    override val category: Category,
    val position: LatLng,
) : DataBeam {
    override fun getData() {
        super.getData()
    }
}

data class HeatmapPoint(
    val position: LatLng,
    val intensity: Double // Intensity or weight of the point
)

data class HeatmapData(
    override val id: String,
    override val title: String,
    override val category: Category,
    val area: String
) : DataBeam {
    override fun getData() {
        super.getData()
    }
}

object DataRepository {

    private val db: List<DataBeam> = listOf(
        MarkerData("1", "Monument 1", Category("Monuments",VisualizationType.Pins), getRandomLocation()),
        MarkerData("2", "Monument 2", Category("Monuments",VisualizationType.Pins), getRandomLocation()),
        MarkerData("2", "Monument 3", Category("Monuments",VisualizationType.Pins), getRandomLocation()),
        MarkerData("2", "Monument 4", Category("Monuments",VisualizationType.Pins), getRandomLocation()),
        MarkerData("2", "Monument 5", Category("Monuments",VisualizationType.Pins), getRandomLocation()),
        HeatmapData("1", "Air Pollution Heatmap", Category("Air Pollution",VisualizationType.HeatMap),"Area1"),
    )

    val heatmapPoints = generateHeatmapPoints()

    fun getData(category: Category): List<DataBeam> {
        return db.filter { dataBeam ->
            dataBeam.category == category
        }
    }
}

fun getRandomLocation(): LatLng {
    val startingPoint: LatLng = LatLng(44.646469, 10.925139)
    val radiusInMeters : Double = 500.0
    val radiusInDegrees = radiusInMeters / 111320f

    // Random distance and angle
    val u = Random.nextDouble()
    val v = Random.nextDouble()
    val w = radiusInDegrees * sqrt(u)
    val t = 2 * Math.PI * v
    val x = w * cos(t)
    val y = w * sin(t)

    // Adjust the x-coordinate for the shrinking of the east-west distances
    val new_x = x / cos(Math.toRadians(startingPoint.latitude))

    val foundLongitude = new_x + startingPoint.longitude
    val foundLatitude = y + startingPoint.latitude

    return LatLng(foundLatitude, foundLongitude)
}


fun generateHeatmapPoints(): List<HeatmapPoint> {
    val steps = 10
    val stepRadius = 0.001
    val center: LatLng = LatLng(44.646469, 10.925139)
    val points = mutableListOf<HeatmapPoint>()

    for (i in -steps..steps) {
        for (j in -steps..steps) {
            val lat = center.latitude + i * stepRadius
            val lng = center.longitude + j * stepRadius
            val intensity = Random.nextDouble()
            points.add(HeatmapPoint(position = LatLng(lat, lng),intensity = intensity))
        }
    }

    return points
}

