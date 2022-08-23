package Part1_Akka入門.IoTシステム

import akka.actor.typed.ActorRef

object DeviceGroupProtocol{
    sealed interface Command

    //デバイスを終了する
    data class DeviceTerminated(
        val device: ActorRef<DeviceProtocol.Command>,
        val groupId: String,
        val deviceId: String
    ): Command
}