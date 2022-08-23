package Part1_Akka入門.IoTシステム

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class Device private constructor(
    context: ActorContext<DeviceProtocol.Command>,
    private val groupId: String,
    private val deviceId: String,
    private var lastTemperature: Double? = null
): AbstractBehavior<DeviceProtocol.Command>(context) {
    init {
        context.log.debug("デバイスStart: {${groupId}-${deviceId}}")
    }

    override fun createReceive(): Receive<DeviceProtocol.Command> {
        return newReceiveBuilder()
            .onMessage(DeviceProtocol.RecordTemperature::class.java){ onRecordTemperature(it) }
            .onMessage(DeviceProtocol.ReadTemperature::class.java) { onReadTemperature(it) }
            .onMessage(DeviceProtocol.Passivate::class.java) { Behaviors.stopped() }
            .onSignal(PostStop::class.java) { _ -> onPostStop() }
            .build()
    }

    /**
     * 温度を記録
     */
    private fun onRecordTemperature(command: DeviceProtocol.RecordTemperature): Behavior<DeviceProtocol.Command> {
        context.log.debug("温度を記録しました ${command.value} with ${command.requestId}")
        lastTemperature = command.value
        command.replyTo.tell(DeviceProtocol.TemperatureRecorded(command.requestId))

        return this
    }

    /**
     * 温度を読込
     */
    private fun onReadTemperature(command: DeviceProtocol.ReadTemperature): Behavior<DeviceProtocol.Command> {
        command.replyTo.tell(DeviceProtocol.RespondTemperature(command.requestId, lastTemperature))

        return this
    }

    /**
     * 停止
     */
    private fun onPostStop(): Device {
        context.log.debug("Device actor ${groupId}-${deviceId}ストップ")

        return this
    }

    companion object {
        fun create(groupId: String, deviceId: String): Behavior<DeviceProtocol.Command> {
            return Behaviors.setup { context -> Device(context, groupId, deviceId) }
        }
    }
}