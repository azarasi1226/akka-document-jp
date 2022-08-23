package Part1_Akka入門.IoTシステム

import Part1_Akka入門.IoTシステム.DeviceManagerProtocol.DeviceGroupTerminated
import Part1_Akka入門.IoTシステム.DeviceManagerProtocol.ReplyDeviceList
import Part1_Akka入門.IoTシステム.DeviceManagerProtocol.RequestDeviceList
import Part1_Akka入門.IoTシステム.DeviceManagerProtocol.RequestTrackDevice
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive


class DeviceManager private constructor(
    context: ActorContext<DeviceManagerProtocol.Command>,
    var groupIdToActor: MutableMap<String, ActorRef<DeviceGroupProtocol.Command>> = mutableMapOf()
) : AbstractBehavior<DeviceManagerProtocol.Command>(context){
    companion object{
        fun create(): Behavior<DeviceManagerProtocol.Command>{
            return Behaviors.setup{ context ->
                DeviceManager(context)
            }
        }
    }

    override fun createReceive(): Receive<DeviceManagerProtocol.Command> {
        return newReceiveBuilder()
            .onMessage(DeviceManagerProtocol.RequestDeviceList::class.java){ command -> onRequestDeviceList(command)}
            .onMessage(DeviceManagerProtocol.RequestTrackDevice::class.java){command -> onTrackDevice(command)}
            .onMessage(DeviceManagerProtocol.DeviceGroupTerminated::class.java) {command -> onTerminated(command)}
            .onSignal(PostStop::class.java){onPostStop()}
            .build();
    }

    private fun onPostStop(): DeviceManager? {
        context.log.info("DeviceManager stopped")
        return this
    }

    //デバイスグループの削除
    private fun onTerminated(t: DeviceGroupTerminated): DeviceManager? {
        context.log.info("デバイスグループ停止 ${t.groupId}")
        groupIdToActor.remove(t.groupId)

        return this
    }

    //デバイスリストの返却
    private fun onRequestDeviceList(request: RequestDeviceList): DeviceManager{
        val ref = groupIdToActor[request.groupId]
        if (ref != null) {

            ref.tell(request)
        }
        else {
            request.replyTo.tell(ReplyDeviceList(request.requestId, setOf()))
        }

        return this
    }

    //デバイスグループを追加する
    private fun onTrackDevice(trackMsg: RequestTrackDevice): DeviceManager? {
        val groupId = trackMsg.groupId
        val ref = groupIdToActor[groupId]
        if (ref != null) {
            ref.tell(trackMsg)
        }
        else {
            context.log.info("デバイスグループ作成: ${groupId}")
            val groupActor = context.spawn(DeviceGroup.create(groupId), "group-$groupId")
            context.watchWith(groupActor, DeviceManagerProtocol.DeviceGroupTerminated(groupId))
            groupActor.tell(trackMsg)
            groupIdToActor[groupId] = groupActor
        }

        return this
    }

}