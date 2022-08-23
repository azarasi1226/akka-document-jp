package Part1_Akka入門.IoTシステム

import akka.actor.typed.ActorSystem

fun main(){
    ActorSystem.create(IotSupervisor.create(), "iot-system");
}