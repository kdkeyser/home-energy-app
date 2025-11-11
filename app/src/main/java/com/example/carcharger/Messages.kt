package com.example.carcharger

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.random.Random

@Serializable
sealed class ChargingState {
    @Serializable
    class NotCharging() : ChargingState()

    @Serializable
    class ChargingWithExcessPower() : ChargingState()

    @Serializable
    data class ChargingWithMaxPower(val maxPower: UInt) : ChargingState()
}

@Serializable
enum class Devices {
    SOLAR,
    BATTERY,
    CAR_CHARGER,
    HEATPUMP,
    GRID
}

@Serializable
// negative power means producing power, positive means consuming
data class Update(val device: Devices, val power: Int)

@Serializable
sealed class Message {
    data class SetCharging(val chargingState: ChargingState) : Message()
    data class PowerUsageUpdate(val updates: List<Update>) : Message()
}

fun deserializeMessage(json: String): Message {
    return Json.decodeFromString<Message>(json)
}

fun randomPowerUsageUpdate(): Message {
    return Message.PowerUsageUpdate(
        listOf(
            Update(Devices.SOLAR, Random.nextInt(-5000, 0)),
            Update(Devices.BATTERY, Random.nextInt(-5000, 5000)),
            Update(Devices.CAR_CHARGER, Random.nextInt(0, 7500)),
            Update(Devices.HEATPUMP, Random.nextInt(0, 5000)),
            Update(Devices.GRID, Random.nextInt(-5000, 5000))
        ))
}