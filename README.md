# capture-hub

思いつきを最小の操作で記録し、Obsidian Daily Note へ集約する入力専用キャプチャ基盤。

閲覧・編集・検索は Obsidian に任せ、本プロジェクトは記録だけに特化する。
Android を中心に、Wear OS や Even G2 などの複数の入力経路を同じ Daily Note へ
集約することを目指す。

## ステータス

構想・検証段階である。Android アプリの雛形が存在し、まず Even Hub
Plugin から Android localhost への連携成立性を検証する (Phase 0)。

## ドキュメント

- [初期調査 (2026-08-06)](docs/reports/2026-08-06-capture-hub-research.md):
  既存アプリの調査、Even G2 連携の技術調査、推奨アーキテクチャ、ロードマップ。

## 開発環境

ツール管理には [mise](https://mise.jdx.dev/) を利用する。

### セットアップ

```sh
mise install
mise run setup
```

### lint / format

- `mise run format` で全ファイルを整形する
- `mise run lint` で整形崩れと Markdown の記法、Kotlin のコードスタイルを検査する
- pre-commit フックが staged ファイルを自動整形し、Markdown の記法、Kotlin のコードスタイルを検査する
- 除外対象: docs/superpowers/、docs/tmp/、.superpowers/、docs/reports/

### Android アプリ

`android/` 配下に Android アプリ (Kotlin + Jetpack Compose) を置く。
Android 固有のタスクは `android/mise.toml` に定義し、root の
`mise run build` / `mise run test` / `mise run clean` から呼び出す。

JDK と ktlint は `android/mise.toml` の `[tools]` で管理する。Android SDK は mise の管理外であり、
Android Studio などで導入した SDK を `android/local.properties` から
参照する。`local.properties` は `mise run setup` が生成する
(`ANDROID_HOME` があればそれを優先し、無ければ既定の
`~/Library/Android/sdk` を使う)。SDK の場所が異なる場合は
`sdk.dir` を手動で変更する。

- ビルド (test / Android Lint を含む): `mise run build`
- テスト: `mise run test`
- 生成物の削除: `mise run clean`

`mise run build` / `mise run test` は `local.properties` が無ければ内部で
自動生成するため、事前に `mise run setup` を実行する必要はない。

### リリースビルド

サイドロード用の署名済み APK を作成する。Android は署名の無い APK の
インストールを拒否するため、署名は必須である。

署名鍵とパスワードは Bitwarden のセキュアメモ 1 件に保管し、ビルド時に
Bitwarden CLI から取り出す。鍵ファイルはビルド中の一時ディレクトリにのみ
展開され、ビルド終了時に削除される。jq と Bitwarden CLI は
`android/mise.toml` の `release` タスクにのみ紐づくツールとして宣言する
(`lint` / `build` タスクはこれらを必要としないため)。

#### 初回のみ: 鍵の作成と Bitwarden への登録

鍵はこのアプリ専用に 1 つ作り、他のアプリと共有しない。同じ鍵で署名した
アプリ同士は signature レベルの権限を共有できてしまい、鍵の漏えい時の
影響範囲も広がるためである。

以降、このセクションのコマンドはすべて `android/` ディレクトリで実行する。

鍵を作成する。パスワードは対話的に 2 回入力する。

```sh
cd android
mise exec -- keytool -genkeypair -v -keystore release.jks \
  -alias capture-hub -keyalg RSA -keysize 2048 -validity 10000
```

base64 に変換する。この文字列を Bitwarden のメモ欄に貼り付ける。

```sh
base64 < release.jks | tr -d '\n' | pbcopy
```

Bitwarden への登録は、アプリ (またはブラウザ拡張) で手動作成する方法と、
`bw` CLI で作成する方法のどちらでもよい。

アプリで登録する場合、次の内容でセキュアメモを作成する。

- 名前: `capture-hub-release-keystore`
- メモ: 上でコピーした base64 文字列
- カスタムフィールド `storePassword` (非表示): keystore のパスワード
- カスタムフィールド `keyPassword` (非表示): 鍵のパスワード
- カスタムフィールド `keyAlias` (テキスト): `capture-hub`

`bw` CLI で登録する場合、事前に
`mise exec npm:@bitwarden/cli@2026.8.0 -- bw login` でログインしておく
(`bw get template item` はログインしていないと実行できない)。

```sh
mise exec npm:@bitwarden/cli@2026.8.0 -- bw get template item | mise exec jq@1.8.2 -- jq \
  --arg b "$(base64 < release.jks | tr -d '\n')" \
  '.type = 2 | .secureNote.type = 0 | .name = "capture-hub-release-keystore" | .notes = $b | .fields = [
    {name: "storePassword", value: "REPLACE_ME", type: 1},
    {name: "keyAlias", value: "capture-hub", type: 0},
    {name: "keyPassword", value: "REPLACE_ME", type: 1}
  ]' | mise exec npm:@bitwarden/cli@2026.8.0 -- bw encode \
  | mise exec npm:@bitwarden/cli@2026.8.0 -- bw create item
```

`storePassword` と `keyPassword` の `REPLACE_ME` は実際のパスワードに
置き換える。`fields` の `type` は `0` がテキスト、`1` が非表示を表す。

登録後、ローカルの `release.jks` を削除し、リポジトリのルートに戻る。
鍵の正本は Bitwarden にある。

```sh
rm release.jks
cd ..
```

#### ビルド

以降のコマンドはリポジトリのルートで実行する。

Bitwarden CLI に一度ログインしておく。

```sh
mise exec npm:@bitwarden/cli@2026.8.0 -- bw login
```

ビルドする。Vault がロックされていればマスターパスワードの入力を求められる。

```sh
mise run release
```

`android/app/build/outputs/apk/release/app-release.apk` が生成される。
端末へは次でインストールする。

```sh
adb install -r android/app/build/outputs/apk/release/app-release.apk
```

同じシェルで複数回ビルドする場合は、
`export BW_SESSION=$(mise exec npm:@bitwarden/cli@2026.8.0 -- bw unlock --raw)`
を先に実行しておくとパスワードの再入力を省ける。

アイテムが見つからないというエラーが出る場合は
`mise exec npm:@bitwarden/cli@2026.8.0 -- bw sync` を実行してから再試行する。

環境変数 `CAPTURE_HUB_KEYSTORE_FILE` が設定されていない場合、
`./gradlew assembleRelease` は未署名の APK を生成する。CI はこの経路を通る。

### Renovate

`.github/workflows/renovate.yml` の動作には `RENOVATE_TOKEN` リポジトリ secret の
登録が必要である。`renovate.json` で `helpers:pinGitHubActionDigests` を使用して
おり、`.github/workflows/` 配下への push が発生するため、トークンには
workflow 書き込み権限が必要である
(classic PAT: `workflow` スコープ、fine-grained PAT: "Workflows: Read and write")。

Gradle 本体の更新 PR では `distributionUrl` のみ更新され、
`distributionSha256Sum` は更新されないため、チェックサム不一致で CI が失敗する。
その場合は新しいバージョンの SHA256 値を
`https://services.gradle.org/distributions/gradle-<version>-bin.zip.sha256`
から取得して `distributionSha256Sum` を手動で更新し、あわせて
`cd android && ./gradlew wrapper --gradle-version <version> --gradle-distribution-sha256-sum <SHA256 値>`
を実行して wrapper 一式を追従させる。
