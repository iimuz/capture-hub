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

### Renovate

`.github/workflows/renovate.yml` の動作には `RENOVATE_TOKEN` リポジトリ secret の
登録が必要である。`renovate.json` で `helpers:pinGitHubActionDigests` を使用して
おり、`.github/workflows/` 配下への push が発生するため、トークンには
workflow 書き込み権限が必要である
(classic PAT: `workflow` スコープ、fine-grained PAT: "Workflows: Read and write")。
