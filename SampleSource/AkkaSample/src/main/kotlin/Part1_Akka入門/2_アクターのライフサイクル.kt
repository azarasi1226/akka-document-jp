package Part1_Akka入門

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class StartStopActor2 private constructor(context: ActorContext<String>): AbstractBehavior<String>(context) {
    init {
        println("Second Start")
    }

    override fun createReceive(): Receive<String> {
        return newReceiveBuilder()
            // PostStopシグナルが送信されたらonPostStop()実行
            .onSignal(PostStop::class.java) { _: PostStop? -> onPostStop() }
            .build()
    }

    private fun onPostStop(): Behavior<String?> {
        println("Secound Stop!!")

        return this
    }

    companion object {
        fun create(): Behavior<String> {
            return Behaviors.setup {
                    context: ActorContext<String> -> StartStopActor2(context)
            }
        }
    }
}

class StartStopActor1 private constructor(context: ActorContext<String>): AbstractBehavior<String>(context) {
    init {
        println("First Start")

        //どこの参照にも入れないけどとりあえずアクター作成
        context.spawn(StartStopActor2.create(), "second")
    }

    override fun createReceive(): Receive<String> {
        return newReceiveBuilder()
            // stopメッセージが来たらアクター停止
            .onMessageEquals("stop") { Behaviors.stopped() }
            .onSignal(PostStop::class.java) { _: PostStop? -> onPostStop() }
            .build()
    }


    private fun onPostStop(): Behavior<String?> {
        println("first Stop!!!")

        return this
    }

    companion object {
        fun create(): Behavior<String> {
            return Behaviors.setup { context: ActorContext<String> -> StartStopActor1(context)
            }
        }
    }
}

fun main(){
    val testSystem: ActorRef<String> = ActorSystem.create(StartStopActor1.create(), "first")
    testSystem.tell("stop")
}