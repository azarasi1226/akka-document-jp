package Part1_Akka入門

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

/**
 * 最初に停止されるアクター
 */
class StartStopActor2 private constructor(
    context: ActorContext<String>
): AbstractBehavior<String>(context) {
    init {
        println("Second Start")
    }

    override fun createReceive(): Receive<String> {
        return newReceiveBuilder()
            // PostStopシグナルが送信されたらonPostStop()実行
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()
    }

    private fun onPostStop(): Behavior<String> {
        println("Second Stop!")

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

/**
 * ２番目に停止するアクター
 */
class StartStopActor1 private constructor(
    context: ActorContext<String>
): AbstractBehavior<String>(context) {
    init {
        println("First Start")

        //どこの参照にも入れないけどとりあえずアクター作成
        //これだけで小アクターとして登録されるよ！
        context.spawn(StartStopActor2.create(), "second")
    }

    override fun createReceive(): Receive<String> {
        return newReceiveBuilder()
            // stopメッセージが来たらアクターをBehaveirs.stopped()を返して停止させる
            .onMessageEquals("stop") { Behaviors.stopped() }
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()
    }


    private fun onPostStop(): Behavior<String> {
        println("First Stop!")

        return this
    }

    companion object {
        fun create(): Behavior<String> {
            return Behaviors.setup { context: ActorContext<String> -> StartStopActor1(context)
            }
        }
    }
}

/**
 * エントリーポイント
 */
fun main(){
    val first = ActorSystem.create(StartStopActor1.create(), "first")
    first.tell("stop")
}