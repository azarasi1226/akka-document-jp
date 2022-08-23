package Part1_Akka入門.IoTシステム

import Part1_Akka入門.IoTシステム.DeviceGroupProtocol.DeviceTerminated
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive


class DeviceGroup private constructor(
    context: ActorContext<DeviceGroupProtocol.Command>,
    private val groupId: String,
    private val deviceIdToActor: HashMap<String, ActorRef<DeviceProtocol.Command>> = HashMap()
) : AbstractBehavior<DeviceGroupProtocol.Command>(context){

    init {
        context.log.debug("デバイスグループ${groupId}:Start")
    }

    companion object {
        fun create(groupId: String): Behavior<DeviceGroupProtocol.Command> {
            return Behaviors.setup{ context ->
                DeviceGroup(context, groupId)
            }
        }
    }

    override fun createReceive(): Receive<DeviceGroupProtocol.Command> {
        return newReceiveBuilder()
            .onMessage(DeviceManagerProtocol.RequestTrackDevice::class.java){ command -> onTrackDevice(command)}
            .onMessage(DeviceGroupProtocol.DeviceTerminated::class.java){command -> onTerminated(command)}
            .onMessage(DeviceManagerProtocol.RequestDeviceList::class.java, {command -> command.groupId == groupId}){command -> onDeviceList(command)}
            .onSignal(PostStop::class.java){onPostStop()}
            .build()
    }

    //デバイスリスト一覧取得
    private fun onDeviceList(command: DeviceManagerProtocol.RequestDeviceList): DeviceGroup {
        command.replyTo.tell(DeviceManagerProtocol.ReplyDeviceList(command.requestId, deviceIdToActor.keys))

        return this
    }

    //デバイスを追跡する
    private fun onTrackDevice(trackMsg: DeviceManagerProtocol.RequestTrackDevice): DeviceGroup {
        //自分のグループか？
        if (groupId == trackMsg.groupId) {
            var deviceActor: ActorRef<DeviceProtocol.Command>? = deviceIdToActor[trackMsg.deviceId]

            if (deviceActor != null) {
                //すでに登録されてるデバイスとして通知
                trackMsg.replyTo.tell(DeviceManagerProtocol.DeviceRegistered(deviceActor))
            } else {
                //新しくデバイスを作成して通知
                context.log.info("デバイスが作成されました:${trackMsg.deviceId}")
                deviceActor = context.spawn(Device.create(groupId, trackMsg.deviceId), "device-" + trackMsg.deviceId)
                deviceIdToActor[trackMsg.deviceId] = deviceActor
                trackMsg.replyTo.tell(DeviceManagerProtocol.DeviceRegistered(deviceActor))
            }
        } else {
            context.log.warn("Ignoring TrackDevice request for {}. This actor is responsible for {}.", trackMsg.groupId, groupId)
        }

        return this
    }

    //デバイス削除
    private fun onTerminated(command: DeviceTerminated): DeviceGroup {
        context.log.info("デバイスが削除されました:${command.deviceId}")
        deviceIdToActor.remove(command.deviceId)

        return this
    }

    //ストップハンドラ
    private fun onPostStop(): DeviceGroup {
        context.log.info("デバイスグループ${groupId}: stopped")
        return this
    }
}