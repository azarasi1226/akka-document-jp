## 依存関係

```
def versions = [
  ScalaBinary: "2.13"
]
dependencies {
  implementation platform("com.typesafe.akka:akka-bom_${versions.ScalaBinary}:2.6.19")

  implementation "com.typesafe.akka:akka-actor-typed_${versions.ScalaBinary}"
}
```
## イントロダクション
Akka を使用すると、アクターシステムのインフラストラクチャを作成したり、基本的な動作を制御するために必要な低レベルのコードを書いたりする手間が省けます。このことを理解するために、コード内で作成するアクターと、Akka が内部で作成・管理するアクターとの関係、アクターのライフサイクル、および障害処理について見ていきましょう。

## Akkaアクターの階層
Akka のアクターは、常に親に属しています。アクターを作成するには、ActorContext.spawn() を呼び出します。作成したアクターは、新しく作成された子アクターの親になります。では、最初に作成したアクターの親は誰なのか？

下図に示すように、すべてのアクタは共通の親であるユーザー ガーディアンを持っており、これは ActorSystem の起動時に定義され作成されます。クイックスタートガイドで説明したように、アクタの作成は有効な URL である参照を返します。したがって、例えば context.spawn(someBehavior, "someActor") でユーザー ガーディアンから someActor という名前のアクターを作成すると、その参照には /user/someActor というパスが含まれることになります。