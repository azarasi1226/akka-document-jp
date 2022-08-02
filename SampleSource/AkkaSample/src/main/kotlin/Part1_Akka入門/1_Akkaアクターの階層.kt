package Part1_Akka入門

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class Secound private constructor(context: ActorContext<String>): AbstractBehavior<String>(context) {
    override fun createReceive(): Receive<String> {
        return newReceiveBuilder()
            .onMessageEquals("apply2") { apply2() }
            .build()
    }

    private fun apply2(): Behavior<String?> {
        val secondRef = context.spawn(Behaviors.empty<String>(), "second-actor")

        println("Second: $secondRef")

        return this
    }

    companion object {
        fun create(): Behavior<String> {
            return Behaviors.setup { context: ActorContext<String> -> Secound(context) }
        }
    }
}

class First private constructor(context: ActorContext<String>) : AbstractBehavior<String>(context) {
    override fun createReceive(): Receive<String> {
        return newReceiveBuilder()
            .onMessageEquals("apply1") { this.apply1() }
            .build()
    }

    private fun apply1(): Behavior<String?> {
        val firstRef = context.spawn(Secound.create(), "first-actor")

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

fun main() {
    val testSystem: ActorRef<String> = ActorSystem.create(First.create(), "testSystem")
    testSystem.tell("apply1")
}
