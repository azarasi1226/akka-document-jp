package Part1_Akka入門

import akka.actor.typed.*
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.event.Logging
import org.slf4j.Logger

/**
 * 監督される側
 */
class SupervisedActor private constructor(
    context: ActorContext<String>
): AbstractBehavior<String>(context) {
    private val log: Logger
    init {
        log = context.log;
        println("子：起動するよ")
    }

    override fun createReceive(): Receive<String> {
        return newReceiveBuilder()
            .onMessageEquals("fail") { fail() }
            .onSignal(PreRestart::class.java) { _ -> preRestart() }
            .onSignal(PostStop::class.java) { _ -> postStop() }
            .build()
    }

    private fun fail(): Behavior<String?> {
        log.debug("子：今から例外で異状停止を再現するよ！")

        throw RuntimeException()
    }

    private fun preRestart(): Behavior<String> {
        println("子：リスタート")

        return this
    }

    private fun postStop(): Behavior<String> {
        println("子：ストップ")

        return this
    }

    companion object {
        fun create(): Behavior<String> {
            return Behaviors.setup { context: ActorContext<String> -> SupervisedActor(context) }
        }
    }
}

/**
 * 監督する側
 */
class SupervisingActor private constructor(
    context: ActorContext<String>
): AbstractBehavior<String>(context) {
    private val child: ActorRef<String>

    init {
        child = context.spawn(
            // 監視してActorを作成する。ちなみにこれは子が落ちたらその子を再起動させるという意味
            Behaviors.supervise(SupervisedActor.create())
                .onFailure(SupervisorStrategy.restart()),
            "supervised-actor"
        )
    }

    override fun createReceive(): Receive<String> {
        return newReceiveBuilder()
            .onMessageEquals("failChild") { onFailChild() }
            .onMessageEquals("stop"){ Behaviors.stopped() }
            .onSignal(PostStop::class.java) { _ -> postStop() }
            .build()
    }

    private fun onFailChild(): Behavior<String> {
        child.tell("fail")

        return this
    }

    private fun postStop(): Behavior<String> {
        println("親:停止")

        return this
    }

    companion object {
        fun create(): Behavior<String> {
            return Behaviors.setup { context: ActorContext<String> -> SupervisingActor(context) }
        }
    }
}

/**
 * エントリーポイント
 */
fun main(){
    val testSystem: ActorRef<String> = ActorSystem.create(SupervisingActor.create(), "first")
    testSystem.tell("failChild")
    testSystem.tell("stop")
}