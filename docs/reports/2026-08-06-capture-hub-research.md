## 1. エグゼクティブサマリー

本構想の目的は、思いつきをできるだけ少ない操作で記録し、閲覧・編集はObsidianなど既存アプリへ任せる「入力専用キャプチャ基盤」を実現することである。

現在は、ObsidianのMobile First Daily Interfaceを開き、Gboardの音声入力で書き起こし、Copilot Pluginで文章を整形する運用が成立している。したがって、音声認識やLLM整形そのものを再検証することが主目的ではない。解決すべき中心課題は、Obsidianを開く手間をなくし、Android、Wear OS、Even G2など複数の入力経路を同じDaily Noteへ集約することである。

調査結果は以下のとおりである。

- 要件全体を製品品質で満たす既存アプリは確認できなかった。
- DriftVoiceはAndroidからObsidian Daily Noteへ直接追記する点で近いが、利用可能性とWear OS・Even G2対応に課題がある。
- VoiceNote Captureと G2sidian は技術的な参考になるが、現時点では初期段階であり、日常利用の基盤として採用するにはリスクが高い。
- Voice MD、ReWriteなどは音声認識、LLM整形、中間データ保存の参考になるが、Obsidianを起動する必要があり、今回の出発点となった入力摩擦を解消しない。
- Even G2では、公式SDKを介してG2の4マイクアレイから16 kHz・16-bit・monoのPCM音声を取得できる。WebView内で書き起こし処理を行うことも可能である。
- 一方、Even Hub Pluginから任意の自作Androidアプリへデータを直接渡す公式ネイティブブリッジは確認できない。Even Hub PluginはEven Realities App内のWebViewで動作するためである。
- サーバーを極力使わずにAndroid側へデータを渡す場合、Androidアプリ内にlocalhost HTTPエンドポイントを設け、Even Hub Pluginから送信する方式が最も目的に合う。ただし、Even Hubの本番private buildでlocalhost通信とHTTP許可が成立するか、最初に実機PoCが必要である。

推奨方針は、ノートアプリを新規開発するのではなく、Androidを中心とする書き込み専用の「Capture Hub」を構築することである。最初の技術検証は、**Even G2音声取得 → Even Hub WebView → Android localhostへのPOST**に限定する。

---

## 2. 背景と目的

### 2.1 現在の運用

現在、以下の流れで音声メモを記録している。

1. Obsidianを起動する。
2. Mobile First Daily Interfaceを開く。
3. Gboardの音声入力で発話を文字列にする。
4. Copilot Pluginで文字列を整形する。
5. Daily Noteへ時刻やセクション単位で記録する。

この方法により、音声入力、書き起こし、LLM整形、Daily Noteへの記録という基本的な処理が有効であることは確認済みである。

### 2.2 解決したい問題

主な問題は、記録のたびにObsidianを開き、入力画面へ移動し、整形処理を実行する必要があることである。思いつきを瞬時に保存する用途では、この数段階の操作が記録の妨げになる。

目標は以下である。

- 記録に特化し、閲覧や編集機能を持たない。
- Androidではテキスト入力と音声入力を提供する。
- Wear OSとEven G2からも音声入力できるようにする。
- Android端末のマイク、Bluetoothマイク、Wear OS、Even G2のマイクを入力元として扱う。
- 音声を文字起こしし、LLMで読みやすい文章へ整形する。
- 最終結果を指定されたObsidian Daily Noteへ追記する。
- 原音、未整形の書き起こし、整形済みテキストなどの中間データを保持する。
- 可能な限り外部サーバーを必要とせず、Android端末内で完結させる。

---

## 3. 既存アプリ・OSSの調査結果

## 3.1 DriftVoice

DriftVoiceはAndroid上でワンタップ録音を開始し、音声を書き起こしてObsidian Daily Noteへ直接追記する点で、Android単体の要件に最も近い。ロック画面、Quick Settings、Google Assistantから起動でき、Daily Noteのファイル名形式と追記テンプレートを設定できると説明されている。

ただし、現時点で一般ユーザーが容易に試せる状態か不明瞭であり、クローズドテスト中である可能性がある。また、公開情報上ではWear OS、Even G2、原音・raw transcript・整形結果を統一して保存する仕組みは確認できない。

したがって、設計思想は近いが、今回の構想を置き換える既存製品とは判断できない。

## 3.2 VoiceNote Capture

VoiceNote Captureは、Wear OSのコンプリケーションから録音し、音声をWear Data Layer経由でAndroidへ転送し、自前の文字起こし環境を介してMarkdownをObsidian Vaultへ保存するOSSである。Wear OSからAndroidへの転送、クラウンによる停止、ハプティクス、実機上のバッテリー挙動などが確認されている。

一方で、README上でもPhase 1 prototypeとされており、日常的なデータ記録の基盤として無条件に採用できる成熟度ではない。また、ライセンスはPolyForm Noncommercial 1.0.0であり、将来公開・商用化する場合には直接流用しにくい。

利用対象というより、以下の参考実装として位置付けるのが適切である。

- Wear OSコンプリケーションからの録音開始
- Wear Data Layerによる音声転送
- Wear OS側の録音状態管理
- Android側での再試行と後続処理

## 3.3 G2sidian

G2sidian は、Even G2から音声でObsidianのDaily NoteまたはInboxへ追記するOSSである。閲覧中ノートへの追記、Markdownの直接読み書き、atomic append、競合確認などを実装している。

今回のEven G2要件に最も近いものの、公開時点でコミット数が少なく、非常に初期のプロジェクトである。また、AndroidローカルのVaultへ直接書く構成ではなく、自分のコンピューター上にPythonバックエンドを立て、Tailscale経由でアクセスする。

したがって、完成品として導入するのではなく、次の観点でコードを参照する価値がある。

- Even Hub SDKによる音声取得
- G2上の操作フロー
- Daily Noteへのappend設計
- append-only・atomic write・競合検知

## 3.4 Voice MD、ReWrite、Whisper Plugin

Voice MDは、録音停止後に音声をローカル保存し、文字起こし結果をDaily Noteへ時刻見出し付きで追記できる。Mobile First Daily Interfaceに近い出力形式を実現しており、通信失敗後の再試行にも配慮されている。

ReWriteは、録音、文字起こし、LLMによる文章の整理、テンプレート適用、音声ファイル保存をまとめて扱う。クラウドAPIだけでなく、whisper.cppとローカルのOpenAI互換LLMを組み合わせた端末内処理も可能である。

Whisper Pluginも、録音・音声ファイル保存・文字起こし・LLM後処理を提供し、複数のWhisper互換APIやOpenAI互換エンドポイントを利用できる。

ただし、これらはいずれもObsidian Pluginとして動作し、記録時にObsidianを起動する必要がある。今回の問題は音声認識や整形の品質ではなく、Obsidianを開く操作そのものであるため、完成形としては適合しない。

## 3.5 Transcribable

TranscribableはAndroidとWear OSに対応する音声文字起こしアプリである。Wear OS側で入力した結果をAndroidへ転送し、Storage Access Frameworkを介して任意の保存先を扱える。

Wear OSとAndroidの連携体験を比較する対象としては有用だが、Obsidian Daily Noteへの定型追記、LLM整形、Even G2連携を一体化したものではない。

## 3.6 Fleeting Notes、Voicenotes、Obsidian Voice

Fleeting NotesはAndroid、Web、ブラウザ拡張などから入力し、Obsidianへ同期できる。ローカルMarkdown同期、オフライン利用、E2EE、ウィジェットを提供する。

VoicenotesはAndroidを含む複数プラットフォームとWatchに対応し、文字起こし、AI要約、アクション抽出、Obsidian Pluginによる同期、オプションの音声ファイル保存を提供する。

Obsidian VoiceはAndroidから録音し、Whisperによる文字起こしとAI整形を行い、Obsidian Vaultへ同期する。既存ノートを読まない書き込み専用のアクセスモデルを掲げている。

これらは入力摩擦を下げる既存例として参考になるが、独自クラウドや独自データベースが中心であること、Daily Noteへの直接append、複数デバイスの統一キュー、Even G2対応という点で今回の構想と異なる。

---

## 4. 既存候補の総合評価

| 候補              |    Android |   Wear OS | Even G2 | Daily Note追記 | LLM整形 | 中間データ | 評価                                        |
| ----------------- | ---------: | --------: | ------: | -------------: | ------: | ---------: | ------------------------------------------- |
| DriftVoice        |       対応 |    非対応 |  非対応 |           対応 |    不明 |       不明 | Android部分は近いが利用可能性に懸念         |
| VoiceNote Capture |       対応 |      対応 |  非対応 |       部分対応 |  非対応 |   音声あり | Wear OS実装の参考、成熟度とライセンスに注意 |
| G2sidian          |   部分対応 |    非対応 |    対応 |           対応 |    不明 |       不明 | G2実装の参考、自前サーバー前提で初期段階    |
| Transcribable     |       対応 |      対応 |  非対応 |         非対応 |  非対応 |   部分対応 | Wear OS連携の比較対象                       |
| Voice MD          | Obsidian内 |    非対応 |  非対応 |           対応 |    対応 |       対応 | 出力処理は近いがObsidian起動が必要          |
| ReWrite           | Obsidian内 |    非対応 |  非対応 |       部分対応 |    対応 |       対応 | 処理パイプラインの参考                      |
| Fleeting Notes    |       対応 |    非対応 |  非対応 |       部分対応 |  非対応 |       対応 | クイックキャプチャとして参考                |
| Voicenotes        |       対応 | Watch対応 |  非対応 |       部分対応 |    対応 |       対応 | クラウド中心で思想が異なる                  |

結論として、既存アプリを組み合わせても要件全体を自然には満たせない。ただし、各部分の実装例は存在するため、車輪の再発明を避けるには、それぞれを参照しながら統合部分だけを新規開発するべきである。

---

## 5. Even G2で音声入力と書き起こしはできるか

## 5.1 音声入力

Even G2の公式SDKは、G2の4マイクアレイまたはスマートフォンのマイクを録音開始時に選択できる。G2マイクを選択する場合は`AudioInputSource.Glasses`、スマートフォンを選択する場合は`AudioInputSource.Phone`を指定する。

音声はEven Hub Pluginのイベントコールバックに`audioEvent`として届く。データ形式は次のとおりである。

- PCM
- 16 kHz
- signed 16-bit little-endian
- mono
- JavaScript上では`Uint8Array`

概念的な処理は以下になる。

```typescript
await bridge.audioControl(true, AudioInputSource.Glasses);

bridge.onEvenHubEvent((event) => {
  const audio = event.audioEvent;
  if (!audio) return;

  const pcmChunk = audio.audioPcm;
  // PCMチャンクを永続化または後段へ送る
});
```

このPCMをWebView内で蓄積してWAV化する、あるいは音声認識APIへストリーミング送信することができる。

## 5.2 書き起こし

音声データは標準的なPCM形式で得られるため、以下のいずれかで書き起こせる。

1. WebViewから文字起こしAPIへ直接送る。
2. 録音終了後にWAV化して文字起こしAPIへ送る。
3. Androidアプリへ転送し、Android側でオンデバイスまたはクラウドの文字起こしを実行する。

Even Hub Pluginは通常のWebアプリと同様に`fetch()`、XMLHttpRequest、WebSocketを利用できる。ただし、宛先originを`app.json`のnetwork whitelistへ登録し、API側もCORSに対応する必要がある。

技術的には書き起こし可能である。ただし、WebViewへ直接クラウドAPIキーを埋め込むと抽出される可能性がある。個人用private buildでユーザーがキーを入力する運用は可能だが、強い秘密管理ではない。公開アプリで安全に運用するなら、API proxyまたは端末内処理が必要になる。

---

## 6. Even Hub WebViewとAndroidアプリの関係

Even Hub Pluginは、Even Realities AppがホストするWebView内で動作する。AndroidではChromium WebViewが使われ、Even Realities App自体はFlutterで構成されている。G2は表示と入力を担当し、アプリロジックはスマートフォン上のWebViewで動作する。

```text
Even G2
   │ Bluetooth LE
   ▼
Even Realities App
   │ Flutter + Chromium WebView
   ▼
Even Hub Plugin
```

Even Hub SDKのJavaScript Bridgeは、WebView、Even Realities App、G2間の通信に使われる。公開ドキュメント上、自作Androidアプリへ任意データを渡すためのBinder、Intent、ContentProvider、Android Service向けの汎用ブリッジは確認できない。

このため、次の直接接続は公式SDKの標準経路としては利用できない。

```text
Even Hub WebView
   │ 公式ネイティブBridge
   ▼
自作Android Capture App
```

また、G2を通常のBluetoothマイクとして自作Androidアプリから直接選択できるとは限らない。公式モデルでは、G2とのBluetooth通信をEven Realities Appが仲介し、取得したPCMをWebViewへ渡す。

---

## 7. サーバーを使わない連携方式

## 7.1 WebView内で完結する方式

最も単純なのは、Even Hub Plugin内で次の処理を完結させる方法である。

```text
G2 microphone
  ↓ PCM
Even Hub Plugin
  ├─ IndexedDBへ原音・状態保存
  ├─ 文字起こし
  ├─ LLM整形
  └─ 最終結果を外部へ出力
```

ただし、Even Hub Pluginはサンドボックス化されたWebViewであり、Android上の任意のObsidian Vaultへ直接ファイル書き込みできない。このため、最終的にDaily Noteへappendする段階で別の経路が必要になる。

## 7.2 localhost経由でAndroidアプリへ渡す方式

今回の要件に最も合うのは、自作Android Capture Appが端末内でHTTPエンドポイントを公開し、Even Hub Pluginから`127.0.0.1`へPOSTする方式である。

```text
Even G2
  ↓ BLE
Even Realities App
  ↓ audioEvent / PCM
Even Hub Plugin
  ↓ HTTP POST to localhost
Android Capture App
  ├─ Roomへジョブ・状態保存
  ├─ 原音ファイル保存
  ├─ 文字起こし
  ├─ LLM整形
  └─ SAF経由でDaily Noteへappend
```

この方式ではインターネット上の自前サーバーは不要で、処理をAndroid端末内へ閉じられる。

一方、以下の制約を検証する必要がある。

- Even Hubのnetwork whitelistに`http://127.0.0.1:<port>`を登録できるか。
- production/private buildでcleartext HTTPが許可されるか。
- WebViewからlocalhostへのリクエストがEven側の権限チェックを通るか。
- Android側が適切なCORSヘッダーとOPTIONS応答を返せるか。
- Androidのバックグラウンド制限やDoze下でlocalhostサービスを維持できるか。
- Foreground Serviceを使用する場合、常駐通知を許容できるか。

公式ドキュメントでは、ネットワークアクセスに宛先originのwhitelist登録が必要であり、本番ではHTTPSが基本、plain HTTPは主にローカル開発向けと説明されている。そのため、localhost方式がprivate buildで許可されるかは、文書だけで断定せず実機PoCで確認すべきである。

## 7.3 Custom SchemeまたはAndroid App Link

`mycapture://append?...`のようなCustom SchemeやHTTPS App Linkで自作Androidアプリを起動する方法も考えられる。

ただし、主経路には向かない。

- 音声データのような大きなデータを渡せない。
- アプリ切り替えが発生する。
- Even Realities Appがバックグラウンドとなり、WebViewや音声取得が停止する可能性がある。
- 処理結果をEven Hub Pluginへ返しにくい。
- URL長やエンコードに制限される。

短い整形済みテキストを送るフォールバックには使えるが、原音や中間データを含む処理基盤には適さない。

## 7.4 外部Relay API

WebViewからHTTPSのRelay APIへ送信し、Androidアプリがpushまたはpollingで取得する方式は最も一般的で安定する。ただし、サーバーの運用、認証、データ保持、費用、プライバシー管理が必要となるため、今回の方針では最後の手段とする。

---

## 8. Android上でObsidian Daily Noteへ追記する方法

Android Capture Appは、Storage Access Frameworkを使用してユーザーからVaultまたはDaily Noteフォルダへのアクセス権を取得する。権限を永続化し、日付に基づいて対象Markdownファイルを決定してappendする。

Obsidianを起動せずMarkdownファイルへ直接追記する運用は、MacroDroidやAutomateを使った既存事例でも実現されている。

推奨する出力形式は、Mobile First Daily Interfaceと互換になるよう設定可能にする。

```markdown
## 15:42

整形済みのメモ本文

<!-- capture-id: 01K1EXAMPLE -->
```

`capture-id`を埋め込むことで、次を実現できる。

- 同じ入力の二重追記防止
- 書き起こし・整形の再実行
- 障害後の復旧
- 元の音声・raw transcriptとの対応付け

Daily Noteへの書き込みは、単純な追記だけでなく、以下も考慮する。

- ファイルが存在しない場合の作成
- 同時書き込み時の排他制御
- UTF-8と改行コードの維持
- 追記前後のバックアップまたはatomic replace
- 日付変更境界での対象ファイル決定
- タイムゾーンの固定

---

## 9. 中間データの保存設計

Android Capture Appを正本とし、すべての入力をdurable queueへ保存してから処理する。

```text
captures/
  2026-08-04/
    <capture-id>/
      metadata.json
      original.txt
      audio.wav
      transcript.txt
      structured.md
```

`metadata.json`には次の情報を保持する。

```json
{
  "captureId": "01K1EXAMPLE",
  "source": "even-g2",
  "inputType": "audio",
  "audioSource": "glasses",
  "createdAt": "2026-08-04T15:42:12+09:00",
  "status": "written",
  "transcriptionProvider": "configured-provider",
  "formattingProfile": "daily-memo",
  "targetNote": "Daily/2026-08-04.md"
}
```

処理状態は少なくとも以下に分ける。

```text
RECEIVED
  ↓
AUDIO_STORED
  ↓
TRANSCRIBED
  ↓
FORMATTED
  ↓
WRITTEN
```

失敗時は`FAILED_TRANSCRIPTION`、`FAILED_FORMATTING`、`FAILED_WRITE`などを記録し、AndroidのWorkManagerで再試行する。

Even Hub WebView側でも、Androidへ転送が完了するまではPCMチャンクとcapture IDをIndexedDBへ保存する。AndroidではWebViewがバックグラウンド化またはメモリ不足で停止される可能性があり、音声取得もWebView停止時に終了するため、メモリだけに状態を置かない設計が必要である。

---

## 10. 推奨アーキテクチャ

全体をノートアプリとして作るのではなく、入力を統一するCapture Hubとして構築する。

```text
Android text input ─────────────┐
Android microphone ─────────────┤
Bluetooth microphone ───────────┤
Wear OS microphone ─────────────┼──> Android Capture Hub
Even G2 Plugin via localhost ────┘             │
                                               ▼
                                         Durable Queue
                                      Room + local files
                                               │
                          ┌────────────────────┼───────────────────┐
                          ▼                    ▼                   ▼
                    Transcription        LLM formatting      Retry control
                          └────────────────────┬───────────────────┘
                                               ▼
                                        Daily Note Writer
                                          SAF direct append
```

Even G2のみ、入力経路が特殊になる。

```text
G2 microphone
  ↓ Bluetooth LE
Even Realities App
  ↓ audioEvent / PCM
Even Hub Plugin
  ↓ localhost HTTP
Android Capture Hub
```

### 責務の分離

#### Even Hub Plugin

- G2上の録音開始・停止UI
- G2またはスマートフォンマイクの選択
- PCMチャンクの受信
- Androidへの転送までの一時保存
- 転送状態とエラーのG2表示

#### Android Capture Hub

- Androidテキスト・音声入力UI
- Bluetoothマイク入力
- Wear OSとのData Layer連携
- Even Hub Pluginからのlocalhost受信
- durable queue
- 音声ファイル・raw transcript・整形結果保存
- 文字起こしとLLM整形
- SAFによるDaily Noteへのappend
- 再試行、重複排除、障害復旧

#### Obsidian

- 閲覧
- 編集
- 検索
- リンク・タグ・タスク管理
- Mobile First Daily Interfaceなど既存UIの活用

この分離により、新規アプリがObsidianの代替にならず、入力摩擦の削減に集中できる。

---

## 11. Even G2利用時の制約

### 11.1 バックグラウンド動作

AndroidではEven Realities App内のChromium WebViewがメモリ圧迫などで停止・回収される可能性がある。WebViewが停止すると`audioControl(true, ...)`による音声取得も停止する。公式ドキュメントは、重要な状態を早期に永続化し、再起動時に復元することを求めている。

したがって、完全な常時待機・完全バックグラウンド録音を前提にしてはならない。G2上でプラグインを開いて録音を開始し、録音中はEven Realities AppのWebViewが動作している状態を維持する必要がある。

### 11.2 ネットワークとCORS

Even Hub Pluginからの通信には、以下の両方が必要である。

1. `app.json`のnetwork whitelistによるEven側の許可
2. 接続先が返す適切なCORSヘッダー

whitelistはCORSを回避しない。localhostのAndroidサービスも、必要なCORSレスポンスとプリフライト処理を実装する必要がある。

### 11.3 APIキー管理

Even Hub PluginはWebアプリであるため、ビルド成果物へAPIキーを固定で埋め込むべきではない。個人用private buildで設定画面から入力する構成でも、WebViewストレージ上の秘密を完全には保護できない。

推奨順序は次のとおりである。

1. Android Capture Hubへ転送し、Android Keystoreで認証情報を管理する。
2. 可能ならオンデバイス処理を使用する。
3. 公開サービス化する場合のみ、最小限の認証付きAPI proxyを設ける。

---

## 12. 実装・検証ロードマップ

## Phase 0: localhost成立性の確認

最初に最大の不確実性を潰す。

1. Androidアプリで`127.0.0.1:<port>`をlistenする。
2. `GET /health`のみ実装する。
3. Even Hub Pluginから`fetch()`する。
4. QR sideloadだけでなくprivate buildでも検証する。
5. Android画面ロック、Even Realities Appのバックグラウンド化、再起動後も確認する。
6. localhostが不可の場合、Custom Schemeまたは外部Relay APIへ設計を切り替える。

**この検証を他機能より先に行う。** localhost通信が成立しなければ、Even G2とAndroid Capture Hubをサーバーなしで統合する中心案が崩れるためである。

## Phase 1: Even G2音声取得

- G2マイクからPCMチャンクを受信する。
- 録音開始・停止をG2のtapまたはring操作へ割り当てる。
- PCMをWAVへ変換し、内容を確認する。
- 受信中のチャンクをIndexedDBへ退避する。

## Phase 2: テキスト転送

- WebViewからAndroid localhostへ短いテキストをPOSTする。
- Android側でRoomへ保存する。
- 固有のcapture IDで重複を排除する。
- ACKをWebViewへ返し、転送済みデータを削除する。

## Phase 3: Daily Note追記

- AndroidでSAFの永続権限を取得する。
- 日付と設定から対象Daily Noteを決定する。
- Mobile First Daily Interface互換のテンプレートでappendする。
- 同時書き込み、ファイル未作成、再試行を検証する。

## Phase 4: 音声処理

- Androidの音声ファイル保存
- 既に実用性を確認済みの文字起こし処理
- LLM整形
- raw transcriptとstructured Markdownの保存
- WorkManagerによる再試行

## Phase 5: Androidネイティブ入力

- テキスト用の小さな入力画面
- Quick Settings Tile
- ホーム画面ウィジェット
- Androidマイク・Bluetoothマイク選択
- 共有Intentからのテキスト受信

## Phase 6: Wear OS

- コンプリケーションから録音開始
- Wear OSまたは電話側マイクの選択
- Wear Data Layerによる音声またはイベント転送
- オフライン時のキューイング

## Phase 7: 品質向上

- end-to-endの暗号化・秘密管理
- データ保持期間と自動削除
- バックアップと復旧
- バッテリー消費測定
- 音声処理プロバイダーの切替
- 公開アプリ化する場合のプライバシーポリシーと権限説明

---

## 13. 最小PoCの合格条件

最初のPoCは次を満たせば合格とする。

1. Even G2の操作から録音を開始・停止できる。
2. G2マイク由来のPCMを正常なWAVとして復元できる。
3. Even Hub PluginからAndroid localhostへ接続できる。
4. Androidがcapture IDとテキストを保存できる。
5. Obsidianを起動せずDaily Noteへ追記できる。
6. 同じcapture IDを再送しても重複追記されない。
7. 一時的な通信失敗後に再送できる。
8. rawデータと最終結果を対応付けて保持できる。

PoC段階ではLLM整形やWear OSまで実装しない。既に実用性が確認できている処理よりも、Even HubからAndroidへのサーバーレス連携という最大の不確実性を優先する。

---

## 14. リスクと対策

| リスク                                             | 影響                           | 対策                                                                          |
| -------------------------------------------------- | ------------------------------ | ----------------------------------------------------------------------------- |
| Even Hub private buildでlocalhost HTTPが拒否される | 中心アーキテクチャが成立しない | Phase 0で最優先検証。失敗時はApp Linkまたは最小Relayへ切替                    |
| Androidがバックグラウンドサービスを停止する        | G2から転送できない             | Foreground Service、再接続、WebView側キューを実装                             |
| Even Realities AppのWebViewが停止する              | 録音中断・状態消失             | PCMを逐次IndexedDBへ保存し、再開可能な状態機械を採用                          |
| SAF経由の追記で競合する                            | Daily Note破損・記録消失       | atomic write、capture ID、書き込み直前の再読込、バックアップ                  |
| WebViewにAPIキーを置く                             | キー漏洩                       | 処理をAndroid側へ移し、Android Keystoreを利用                                 |
| Wear OS・G2・Androidで実装が分散する               | 保守負荷増加                   | Android Capture Hubを唯一の処理基盤とし、周辺端末を薄い入力クライアントにする |
| 音声・書き起こしの蓄積                             | プライバシー・容量問題         | 保持期間、暗号化、自動削除、ユーザー指定フォルダを実装                        |
| 初期OSSを直接採用する                              | 品質・継続性・ライセンス問題   | 実装パターンだけ参照し、依存または流用前にコード・ライセンスを精査            |

---

## 15. 最終結論

この構想には新規開発の価値がある。ただし、価値の中心は音声認識、LLM整形、Obsidian連携を個別に再実装することではない。それらは既存アプリや現在の運用で成立している。

独自価値は次の統合にある。

- Obsidianを開かずに記録できる。
- Android、Bluetoothマイク、Wear OS、Even G2を同じ入力基盤へ統合する。
- 原音、raw transcript、整形結果を失わないdurable queueを持つ。
- Mobile First Daily Interfaceと互換性のある形式でDaily Noteへ直接appendする。
- 記録専用に徹し、閲覧・編集・知識管理はObsidianへ任せる。

Even G2については、音声取得とWebView内での書き起こしは実現可能である。一方、Even Hub Pluginから自作Androidアプリへの公式な直接ブリッジは確認できないため、サーバーを避けるにはlocalhost連携の成立性が鍵となる。

したがって、次の一手は明確である。

> **Even G2からPCMを取得し、Even Hubのprivate buildからAndroidのlocalhostへ送信できるかを、最小PoCで確認する。**

ここが成立すれば、Android Capture Hubを中心に、既に検証済みの音声認識・LLM整形と、SAFによるObsidian Daily Note追記を統合する構成が妥当である。

---

## 参考情報

- Even Hub Device APIs: G2およびPhoneマイクの選択、PCM音声イベント形式。
- Even Hub Architecture: Even Realities App、WebView、G2間の実行・通信モデル。
- Even Hub Networking: network whitelist、CORS、HTTPS要件。
- Even Hub Background & Lifecycle: Android WebViewの停止と音声取得への影響。
- G2sidian: Even G2とObsidianの音声キャプチャ実装例。
- VoiceNote Capture: Wear OSとAndroidの音声転送実装例。
- DriftVoice: AndroidからObsidian Daily Noteへの音声追記の製品例。
- Voice MD / ReWrite: 音声保存、文字起こし、LLM整形、Daily Note出力の実装例。
