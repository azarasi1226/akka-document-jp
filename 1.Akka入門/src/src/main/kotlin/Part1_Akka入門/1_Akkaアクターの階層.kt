package Part1_Akka入門

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

/**
 * 最後に呼ばれるアクター
 */
class Second private constructor(
    context: ActorContext<String>
): AbstractBehavior<String>(context) {
    override fun createReceive(): Receive<String> {
        return newReceiveBuilder()
            .onMessageEquals("apply2") { apply2() }
            .build()
    }

    private fun apply2(): Behavior<String?> {
        val secondRef = context.spawn(Behaviors.empty<String>(), "second-actor")

        context.log.debug("Second: $secondRef")

        return this
    }

    companion object {
        fun create(): Behavior<String> {
            return Behaviors.setup { context: ActorContext<String> -> Second(context) }
        }
    }
}


/**
 * 最初に呼ばれるアクター
 */
class First private constructor(
    context: ActorContext<String>
): AbstractBehavior<String>(context) {
    override fun createReceive(): Receive<String> {
        return newReceiveBuilder()
            .onMessageEquals("apply1") { apply1() }
            .build()
    }

    private fun apply1(): Behavior<String> {
        val firstRef = context.spawn(Second.create(), "first-actor")

        println("First: $firstRef")
        firstRef.tell("apply2")

        return this
    }

    companion object {
        fun create(): Behavior<String> {
            return Behaviors.setup { context: ActorContext<String> -> First(context) }
        }
    }
}

/**
 * エントリーポイント
 */
fun main() {
    val first = ActorSystem.create(First.create(), "first")
    first.tell("apply1")
}
