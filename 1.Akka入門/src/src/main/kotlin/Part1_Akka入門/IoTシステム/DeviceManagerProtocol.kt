package Part1_Akka入門.IoTシステム

import akka.actor.typed.ActorRef

object DeviceManagerProtocol {
    sealed interface Command

    // デバイスを登録する
    data class RequestTrackDevice(
        val groupId: String,
        val deviceId: String,
        val replyTo: ActorRef<DeviceRegistered>
    ) : DeviceManagerProtocol.Command, DeviceGroupProtocol.Command

    data class DeviceRegistered(val device: ActorRef<DeviceProtocol.Command>)

    //　デバイス一覧を取得する
    data class RequestDeviceList(
        val requestId: Long,
        val groupId: String,
        val replyTo: ActorRef<ReplyDeviceList>

    ): DeviceManagerProtocol.Command, DeviceGroupProtocol.Command

    data class ReplyDeviceList(val requestId: Long, val ids: Set<String>)

    data class DeviceGroupTerminated(
        val groupId: String
    ): DeviceManagerProtocol.Command
}