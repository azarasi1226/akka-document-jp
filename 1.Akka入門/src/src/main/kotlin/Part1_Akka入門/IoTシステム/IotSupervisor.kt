package Part1_Akka入門.IoTシステム

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

/**
 * Iotシステムの一番えらい管理者的なアクター
 */
class IotSupervisor private constructor(
    context: ActorContext<Void>
): AbstractBehavior<Void>(context) {
    init {
        context.log.debug("IoT システムスタート")
    }

    override fun createReceive(): Receive<Void> {
        return newReceiveBuilder()
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()
    }

    private fun onPostStop(): IotSupervisor {
        println("IoT　システムストップ")

        return this
    }

    companion object {
        fun create(): Behavior<Void> {
            return Behaviors.setup { context: ActorContext<Void> -> IotSupervisor(context) }
        }
    }
}