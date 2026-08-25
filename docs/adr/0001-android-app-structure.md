---
id: "0001"
status: Accepted
date: 2026-08-19
supersedes: []
---

# ADR-0001: Android アプリを単一モジュールと公式パターン準拠のパッケージ構成にする

## Context

capture-hub の Android MVP (テキスト入力を Room durable queue へ保存し、
SAF で Obsidian Daily Note へ追記する) の実装にあたり、コード構成の方針が
必要になった。将来は音声入力、文字起こし、LLM 整形、Wear OS 連携、
Even G2 ブリッジ、Queue 一覧画面などの拡張を予定している。

検討した選択肢とトレードオフ:

- 責務別パッケージ (data / settings / writer / work / ui):
  小規模では読みやすいが、画面が増えると ui が肥大化し、
  将来の Gradle モジュール分割と対応しない
- vertical slice (機能ごとの縦割り): このアプリは全機能が durable queue を
  共有する 1 本のパイプラインであり、スライス間の独立性が得られず
  利点が薄い
- 複数 Gradle モジュール: 個人開発かつ十数ファイルの規模では、
  ビルド設定の管理コストが利点を上回る
- 単一 Gradle モジュール + Android 公式モジュール化パターン準拠の
  パッケージ構成: 公式ガイドの依存方向 (feature は data に依存し、
  feature 同士は依存しない) をパッケージレベルで先取りし、
  必要になった時点で 1:1 で Gradle モジュールへ昇格できる

## Decision

単一 Gradle モジュール (app) を維持し、dev.iimuz.capturehub 配下を
次のパッケージで分割する。

- core.common: 複数領域から使う汎用コードのみ (最初は Ulid だけ)
- core.database: Room の entity / DAO / database (durable queue の正本)
- core.datastore: Vault 設定の保存と読み出し
- core.designsystem: Material3 テーマ
- sync: queue から Daily Note への同期一式
  (writer、SAF アダプタ、WorkManager の Worker)
- feature.capture / feature.settings: 画面単位の UI と ViewModel
- ルート: CaptureHubApp / MainActivity が全体を組み立てる
  (DI フレームワークは使わず手動 DI)

依存ルールは feature → core、sync → core のみとする。feature 同士および
feature → sync の依存は作らない。

将来の拡張は既存パッケージを変更せず追加する: 音声入力は core.audio、
文字起こし・LLM 整形は sync と並ぶパイプラインステージ、Even G2 ブリッジは
input.evenhub、Queue 一覧・詳細は feature.queue。Gradle モジュールへの
分割は、Wear OS アプリ追加などで model / DB の共有が必要になった時点、
またはビルド時間が問題になった時点で行う。

## Consequences

- 将来の機能追加が既存パッケージの変更なしに行える
- モジュール分割時にパッケージを 1:1 で昇格でき、移行コストが小さい
- 単一モジュールのため依存ルールはコンパイラで強制されず、
  レビューで守る必要がある
- DI フレームワークがないため、依存が増えると CaptureHubApp の
  組み立てコードが手動で増える
