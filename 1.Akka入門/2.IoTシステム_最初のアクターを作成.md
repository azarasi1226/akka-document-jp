## イントロダクション
これからIoTシステムをAkkaで再現してみましょうか！

IoT システムのトップレベルのアクターは いつも1つになります。
デバイスやダッシュボードを作成・管理などのアクターはトップレベルのアクターの子コンポーネントとなります。
それじゃぁ下の図みたいなアクター階層を作って、実際にIoTシステムを構築してみましょう！

![](https://doc.akka.io/docs/akka/current/typed/guide/diagrams/arch_tree_diagram.png)

<br>
<br>

## 最初のアクターを作成しよう！
最初のアクターである IotSupervisor(Iotの管理者) は、短いコードで定義できます。

```kotlin
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

/**
 * Iotシステムの一番えらい管理者的なアクター
 */
class IotSupervisor private constructor(context: ActorContext<Void>) : AbstractBehavior<Void>(context) {
    init {
        context.log.info("IoT システムスタート")
    }

    override fun createReceive(): Receive<Void> {
        return newReceiveBuilder()
            .onSignal(PostStop::class.java) { signal -> onPostStop() }
            .build()
    }

    private fun onPostStop(): IotSupervisor {
        context.log.info("IoT　システムストップ")

        return this
    }

    companion object {
        fun create(): Behavior<Void> {
            return Behaviors.setup { context: ActorContext<Void> -> IotSupervisor(context) }
        }
    }
}
```

ではこれを動かすエントリーポイントも作っていきましょう。

```kotlin
import akka.actor.typed.ActorSystem

fun main(){
    ActorSystem.create(IotSupervisor.create(), "iot-system");
}
```