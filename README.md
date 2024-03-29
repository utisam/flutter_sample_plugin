# sample_plugin

Android 向け Flutter Plugin のサンプル。
実装するにあたって気になったことを試す。

## DI どうする問題

一旦 Dagger を利用した。

Hilt は `@HiltAndroidApp` を `Application` につけて `EntryPoints` を利用すればできそう。
Timber と一緒に Application クラスのカスタマイズ方法をプラグインのドキュメントで指示すべき。

Flutter Plugin に限っては Hilt より Dagger を使ったほうがセットアップも少なくて済むので楽そう。

## Coroutine どうする問題

### Engine のスコープ

`onAttachedToEngine` から `onDetachedFromEngine` の `CoroutineScope` を `SupervisorJob` で作る。
`EventChannel` と `Stream` は `Flow` というより `SharedFlow` と似ているので、
このスコープで `collect` した結果を流す。
もしくは Kotlin の `Channel` を使う。

### Activity のスコープ

`lifecycle.coroutineScope` を使う。
`MethodChannel` で `suspend` な関数を呼び出したければこのスコープで動かす。

デバッグとかしてるとどうやら `onAttachedToActivity` より先に `onMethodCall` が呼ばれることがあるみたいなので、
その場合は Engine のスコープで動かすか Attach まではエラーを返す仕様にするあたりが安定。

## Activity の状態保存どうする問題

Plugin の状態は `addOnSaveStateListener` で保存する。
Plugin のインスタンスに保存しても丸ごと再作成されるので無駄。
`Activity#onRetainNonConfigurationInstance` に相当するものは無さそう。

Flutter 側は `RestorationMixin` を使う。

## 権限どうする問題

Plugin 側では特になにもせずに
[permission_handler](https://pub.dev/packages/permission_handler)
で許可もらってから使うようにプラグインのドキュメントで指示する。

## CI どうする問題

CI は公式の Example も
[subosito/flutter-action](https://github.com/subosito/flutter-action)
を使ってるのでこれで大丈夫なのでは。

## 詰め替えめんどくさい問題

リフレクションで書くなら ProGuard をうまく設定する必要がある。
ただしそもそも他のライブラリが ProGuard を設定できてなかったりするので Optimize 切りがち。
