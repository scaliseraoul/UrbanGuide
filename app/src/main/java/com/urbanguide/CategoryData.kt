package com.urbanguide

enum class VisualizationType {
    HeatMap,
    Pins
}

data class Category(val label: String, val type: VisualizationType)

data class Section(val title: String, val buttons: List<Category>)

object CategoryDataProvider {
    fun getCategories(): List<Section> {
        return listOf(
            Section("City Health", listOf(Category("Air Pollution",VisualizationType.HeatMap))),
            Section("Attractions", listOf(Category("Monuments",VisualizationType.Pins))),
        )
    }
}