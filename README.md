# capture-hub

思いつきを最小の操作で記録し、Obsidian Daily Note へ集約する入力専用キャプチャ基盤。

閲覧・編集・検索は Obsidian に任せ、本プロジェクトは記録だけに特化する。
Android を中心に、Wear OS や Even G2 などの複数の入力経路を同じ Daily Note へ
集約することを目指す。

## ステータス

構想・検証段階である。アプリケーションコードは未実装であり、まず Even Hub
Plugin から Android localhost への連携成立性を検証する (Phase 0)。

## ドキュメント

- [初期調査 (2026-08-06)](docs/reports/2026-08-06-capture-hub-research.md):
  既存アプリの調査、Even G2 連携の技術調査、推奨アーキテクチャ、ロードマップ。

## 開発環境

ツール管理には [mise](https://mise.jdx.dev/) を利用する。

```sh
mise install
```
