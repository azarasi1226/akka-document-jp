## 依存関係
```gradle
def versions = [
  ScalaBinary: "2.13"
]
dependencies {
  implementation platform("com.typesafe.akka:akka-bom_${versions.ScalaBinary}:2.6.19")

  implementation "com.typesafe.akka:akka-persistence-typed_${versions.ScalaBinary}"
  testImplementation "com.typesafe.akka:akka-persistence-testkit_${versions.ScalaBinary}"
}
```
<br>
<br>

## イントロダクション
Akka Persistence はアクターの状態を保持し、JVMクラッシュ・スーパーバイザによる再起動・手動による停止・あるいはクラスタ内でのマイグレーション時にアクタを回復できるようにするためのものです。  
Akka Persistence の規定となる考え方にイベントソーシングがあります。
これはアクターによって永続化されたイベントのみが保存され、アクターの実際の状態は保存されないということです。(後者は普通にCRUDで保存するシステムのことで、ステートソーシングって言うね)

基本的にイベントは追加だけなのでトランザクションのロック等が発生せずスケラーベリティを確保しやすいという利点がある。状態を復元する際はイベントを一から再生して元のアクターに戻すやり方を取る。
これは非常に無駄な作業に思えるが、スナップショットというやり方を採用することで再生にかかる時間を短縮することができる。

AkkaPersistenceを理解するにはイベントソーシングを理解しないといけないため。わからない人は勉強してね(´・ω・｀)

<br>
<br>

## サンプル
説明だけ聞いても意味不明だと思うので実際にコードで見ていきましょう。  
これはEventSourcedBehaviorを継承させた最小構成のアクターです。

```
import akka.actor.typed.Behavior
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.javadsl.CommandHandler
import akka.persistence.typed.javadsl.EventHandler
import akka.persistence.typed.javadsl.EventSourcedBehavior

class MyPersistentBehavior private constructor(
    persistenceId: PersistenceId
) : EventSourcedBehavior<MyPersistentBehavior.Command, MyPersistentBehavior.Event, MyPersistentBehavior.State>(persistenceId) {
    interface Command
    interface Event
    class State

    override fun emptyState(): State {
        return State()
    }

    override fun commandHandler(): CommandHandler<Command, Event, State> {
        return CommandHandler<Command, Event, State> { state, command ->
            throw RuntimeException(
                "TODO: process the command & return an Effect"
            )
        }
    }

    override fun eventHandler(): EventHandler<State, Event> {
        return EventHandler<State, Event> { state, event ->
            throw RuntimeException(
                "TODO: process the event return the next state"
            )
        }
    }

    companion object {
        fun create(persistenceId: PersistenceId): Behavior<Command> {
            return MyPersistentBehavior(persistenceId)
        }
    }
}
```

<br>

### EventSourcedBehaviorを構成するメンバ

|  TH  |  TH  |
| ---- | ---- |
|  persistenceId  |  永続したアクターを示すユニークな識別子のことです。  |
|  emptyState  | エンティティが最初に作成されたときの状態を定義します。<br>(例えばCounterというアクターだったら0にする的な)  |
|  commandHandler  |  Effectsを作成しコマンドを処理する方法を定義します。  |
|  eventHandler  |  イベントが永続化されたときに、現在の状態に新しい状態を加えた状態を返します。 |

ここで注意していただきたいのが、EventSourceBehaviorを継承する具象クラスには状態を含めてはいけないということです。
すべての状態はEventSourceBehaviorのジェネリクスで指定したStateに持っていなくてはいけません。  
さもなければアクターが停止、または再起動したら状態が消えちゃいますので...
あとStateを直接更新するといったこともやめてください、Stateへの更新は常に```EventHandlerで実行```されるようにしなくてはいけません。

じゃぁ次は更に詳しくこの構成要素を見ていきましょうか。

<br>

### PersistenceId
PersistenceIdはイベントジャーナルおよびスナップショットストアにある永続的なアクターのユニークな識別子のことです。

Cluster Shardingは通常、PersistenceIdに対してアクティブなエンティティが1つだけ存在することを保証するために使用されます。  
```PersistenceId.ofUniqueId```でカスタムなIDを作成することが可能です

<br>

### CommandHandler
コマンドハンドラは現在の状態と入力されるコマンドを持ったメソッドです。

コマンドハンドラはコマンドによる結果(Effect)を返します。
例えばコマンドが成功してEventが発行されたとか、逆に失敗して発行されなかったとかことです。
これは```Effect()``` ファクトリメソッドにより返します。

一般的最も使用されるEffectは以下の２種類です

* ```persist()```　イベントをイベントジャーナルに保存する
* ```none()``` 永続化するイベントを指定しない(コマンドが失敗した時 or 読み取り専用のコマンドだったなど...)

<br>

### EventHandler
コマンドハンドラーでイベントが発行が無事成功すると今度はこいつに飛んできます。  
永続化されたイベントを再生し、状態を更新するためのメソッドです。
再生される順番は```Effect().persist()```された順番(昔→今)に再生され、最新の状態を復元します

<br>

### 例の残りのコード
これまでの解説でEventSourcedBehaviorの概要は理解できたと思いますので実際にコードベースで更に理解を深めていきましょう。


```kotlin
object Commands {
    sealed interface Command

    //追加コマンド
    data class Add(
        val data: String
    ) : Command

    //クリアコマンド
    enum class Clear : Command {
        INSTANCE
    }
}

object Events {
    sealed interface Event

    //追加されたイベント
    class Added(
        val data: String
    ) : Event

    //クリアされたイベント
    enum class Cleared : Event {
        INSTANCE
    }
}
```


```kotlin
data class State(
    private val items: List<String> = listOf()
) {
    fun addItem(data: String): State {
        var newItems = ArrayList(items)
        newItems.add(0, data)

        //直近５件のデータのみ常に保持し続ける的なビジネスロジックだった場合
        var latest = items.subList(0, Math.min(5,newItems.size))

        return copy(items = latest)
    }
}
```

```kotlin
class MyPersistentBehavior private constructor(
    persistenceId: PersistenceId
) : EventSourcedBehavior<Commands.Command, Events.Event, State>(persistenceId) {
    companion object {
        fun create(persistenceId: PersistenceId): Behavior<Commands.Command> {
            return MyPersistentBehavior(persistenceId)
        }
    }

    override fun emptyState(): State {
        return State()
    }

    override fun commandHandler(): CommandHandler<Commands.Command, Events.Event, State> {
        return newCommandHandlerBuilder()
            .forAnyState()
            .onCommand(Commands.Add::class.java) { command ->
                Effect().persist(Events.Added(command.data))
            }
            .onCommand(Commands.Clear::class.java) { _ ->
                Effect().persist(Events.Cleared.INSTANCE)
            }
            .build()
    }

    override fun eventHandler(): EventHandler<State, Events.Event> {
        return newEventHandlerBuilder()
            .forAnyState()
            .onEvent(Events.Added::class.java){ state, event ->
                state.addItem(event.data)
            }
            .onEvent(Events.Cleared::class.java){ _, _ ->
                State()
            }
            .build()
    }
}
```

## Effect(結果)とSubEffect(副作用)
コマンドハンドラは受け取ったコマンドの結果を示す結果を```Effect()```ファクトリメソッドで返すと以前説明しましたが
ファクトリーを使用して制作されるのは以下のいずれかになります

<br>

### コマンドの結果
* ```persist```一つのイベントもしくは複数のイベントを保存します
* ```none```読み取り専用のコマンドなどで保存するイベントがない場合に使用します
* ```unhandled```現在の状態ではコマンドは処理されないことを表します
* ```stop```このアクターは停止します
* ```stash```現在のコマンドをstashします
* ```unstashAll```stashされたコマンドをすべて処理します
* ```reply```ActorRefに返信メッセージを送信します

<br>

### コマンドの副作用
コマンドが成功した後に継続して実行できる副作用(追加処理的な)を定義することができます。  
副作用の定義方法は簡単で↑のコマンドの結果(persistとかの事)にメソッドチェーンするだけです。  
noneとかunhandledとかは具体的な処理が無いので即座に副作用が実行されます。

例：
```kotlin
Effect().persist().thenRun()
```

* ```thenRun``` コマンドの結果のあとの副作用を表す
* ```thenStop``` コマンドの結果のあとにアクターを停止します
* ```thenUnstashAll``` stashされたコマンドを一斉に処理します
* ```thenReply``` 与えられたActorRefに返信メッセージを送信する



<br>
<br>

## ClusterShardingとEventSourcedBehaviorの関係性
クラスタシャーディングは永続的なアクター(EventSourcedBehavior継承させた奴のことね)をクラスタ上に分散させ、ユニークなIDを元に各ノードにアクターを配置します。  
このおかげで1つのノードのメモリに収まるよりも多くのアクターをメモリ上に展開することが可能になります。  
クラスタシャーディングと永続的なアクターが組み合わさることにより障害耐性を向上することができます。
例えばノードがもしクラッシュしてアクターが全部消えたとしても別のノードで速やかに消えた永続的なアクターを再開することが可能です。

EventSourcedBehaviorを継承した永続的なアクターは通常のアクターと同様に実行することができますが基本的にはクラスターシャーディングとセットで使われることが前提で
設計されていることを頭の隅で覚えておいてください。
基本的に永続的なアクターはクラスター上に２つ存在してはならず、もしダブってしまっいイベントが同時に発行された際は正しく再生できなくなってしまう可能性があるからです。

上記を踏まえて基本的にEventSourcedBehaviorはクラスタ上に一つしか存在していないことを保証するクラスターシャーディングとセットで使われるのです。

<br>
<br>

## ActorContextへのアクセス
あまりないことですが、EventSourcedBehaviorでActorContextを使用したい場合があります。  
その場合は、通常のアクター通り```Behaviors.setup```を用いてインスタンスを作成します。  
(子アクターを更に作成したい時とかね。子アクターの状態までは永続化されないからぶっちゃけアンチパターンな気がするが)

```kotlin
class ContextBehavior private constructor(
    persistenceId: PersistenceId,
    context: ActorContext<Commands.Command>
) : EventSourcedBehavior<Commands.Command, Events.Event, State>(persistenceId) {
    companion object {
        fun create(persistenceId: PersistenceId): Behavior<Commands.Command> {
            return Behaviors.setup{ context ->
                ContextBehavior(persistenceId, context)
            }
        }
    }

    //ここから下はめんどくさいから省略！↑と一緒です
}
```

<br>
<br>

## 永続的なアクターからの応答
Request-Responceパターンは永続的アクターでは非常に一般的です。  
なぜならコマンドが正常に受け付けたのか、または失敗して拒否されたのかをコマンドを依頼した側は知りたい場合がほとんどだからです。  
(akkaはメッセージ駆動だから例外で応答するとかはしないよ！というかクラスター上で実行してたら例外ハンドリングとかもできないしね)


```kotlin
object Protocol {
    sealed interface Command
    sealed interface CommandResponse

    //何かを追加するイベント
    data class Add(
        val content: String,
        // なにか追加したあとに結果を返すためのActor参照
        val replyTo: ActorRef<CommandResponse>
    ) : Command
}
```

```
.onCommand(Protocol.Add::class.java){command, state -> 
    Effect()
        .persist(Event())
        .thenRun{command.replyto.tell(Protocol.CommandResponce())}
}

```
こんな感じで.thenRunを使うとスッキリ書けるよ。

<br>
<br>

## シリアライゼーション
基本的にイベント(イベントジャーナル)と状態(スナップショット)はシリアライズされて保存または取得されます
公式では```Jackson```を使用したシリアライズを推奨しています

<br>
<br>

## リカバリー
イベントソースされたアクタは、ジャーナルされたイベントを再生することにより、起動時および再起動時に自動的にリカバリされます。  
回復中にアクタに送信された新しいメッセージは、再生されたイベントに干渉しません。これらのメッセージは格納され、回復フェーズが完了した後に EventSourcedBehavior によって受信されます。

同時に進行可能なリカバリーの数は、システムとバックエンドのデータストアに過負荷をかけないように制限されています。  
制限を超えると、アクターは他のリカバリーが完了するまで待機します。これは、以下の方法で設定します。

```
akka.persistence.max-concurrent-recoveries = 50
```