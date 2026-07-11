package com.avtar.cabbilling.data.model

data class Car(val name: String, val plate: String = "") {
    val display: String get() = if (plate.isBlank()) name else "$name · $plate"
    fun toStorage(): String = if (plate.isBlank()) name else "$name|$plate"

    companion object {
        fun fromStorage(s: String): Car {
            val i = s.indexOf('|')
            return if (i < 0) Car(s) else Car(s.substring(0, i), s.substring(i + 1))
        }
    }
}

val DEFAULT_CARS = listOf(
    "Maruti Suzuki Ertiga",
    "Maruti Suzuki Dzire",
    "Maruti Suzuki Swift",
    "Maruti Suzuki WagonR",
    "Maruti Suzuki Ciaz",
    "Maruti Suzuki Alto",
    "Hyundai Aura",
    "Hyundai Verna",
    "Hyundai i20",
    "Hyundai Creta",
    "Toyota Innova Crysta",
    "Toyota Fortuner",
    "Mahindra XUV700",
    "Mahindra Scorpio",
    "Kia Seltos"
)
