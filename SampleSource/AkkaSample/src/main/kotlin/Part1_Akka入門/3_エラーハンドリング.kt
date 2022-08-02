package Part1_Akka入門

import akka.actor.typed.*
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

/**
 * 監督される側
 */
class SupervisedActor private constructor(context: ActorContext<String>): AbstractBehavior<String>(context) {
    init {
        println("監督されたアクターを起動します")
    }

    override fun createReceive(): Receive<String> {
        return newReceiveBuilder()
            .onMessageEquals("fail") { fail() }
            .onSignal(PreRestart::class.java) { _ -> preRestart() }
            .onSignal(PostStop::class.java) { _ -> postStop() }
            .build()
    }

    private fun fail(): Behavior<String?> {
        println("異常停止！！")

        throw RuntimeException()
    }

    private fun preRestart(): Behavior<String?> {
        println("リスタート")

        return this
    }

    private fun postStop(): Behavior<String?> {
        println("ストップ")

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
class SupervisingActor private constructor(context: ActorContext<String>): AbstractBehavior<String>(context) {
    private val child: ActorRef<String>

    init {
        child = context.spawn(
            // 監視してActorを作成するちなみに、これは落ちたら再起動するように設定してある
            Behaviors.supervise(SupervisedActor.create())
                .onFailure(SupervisorStrategy.restart()),
            "supervised-actor"
        )
    }

    override fun createReceive(): Receive<String> {
        return newReceiveBuilder()
            .onMessageEquals("failChild") { onFailChild() }
            .onMessageEquals("stop"){ Behaviors.stopped() }
            .build()
    }

    private fun onFailChild(): Behavior<String?> {
        child.tell("fail")

        return this
    }

    companion object {
        fun create(): Behavior<String> {
            return Behaviors.setup { context: ActorContext<String> -> SupervisingActor(context) }
        }
    }
}

fun main(){
    val testSystem: ActorRef<String> = ActorSystem.create(SupervisingActor.create(), "first")
    testSystem.tell("failChild")
    testSystem.tell("stop")
}