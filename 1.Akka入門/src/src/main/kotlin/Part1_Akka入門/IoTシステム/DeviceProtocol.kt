package Part1_Akka入門.IoTシステム

import akka.actor.typed.ActorRef

object DeviceProtocol {
    sealed interface Command

    //温度を記録
    class RecordTemperature(
        val requestId: Integer,
        val value: Double,
        val replyTo: ActorRef<TemperatureRecorded>
    ): Command

    class TemperatureRecorded(
        val requestId: Integer
    )

    //温度を読込
    class ReadTemperature(
        val requestId: Integer,
        val replyTo: ActorRef<RespondTemperature>
    ): Command

    data class RespondTemperature(
        val requestId: Integer,
        val value: Double?
    )

    enum class Passivate : Command{
        INSTANCE
    }
}