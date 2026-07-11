# fortnite-news

Fortniteのゲームアップデートを検知してSlackに通知するツール。

## 仕組み

[fortnite-api.com](https://fortnite-api.com) の `/v2/aes` エンドポイントからゲームのビルドバージョンを取得し、前回と差分があれば Slack に通知する。GitHub Actions で2時間おきに自動実行される。

## セットアップ

### 1. シークレットの設定

リポジトリの **Settings → Secrets and variables → Actions** で以下のシークレットを追加する。

| Name | Value |
|------|-------|
| `FORTNITE_API_KEY` | [Fortnite-API ダッシュボード](https://dash.fortnite-api.com/account) で発行した API キー |
| `SLACK_WEBHOOK_URL` | Slack の Incoming Webhook URL |

### 2. 通知例

```
🎮 Fortniteアップデート！

40.00 → 41.00
ビルド: CL-54618515
```

## 開発

```bash
# ビルド
mvn compile

# 実行（FORTNITE_API_KEY と SLACK_WEBHOOK_URL が必要）
FORTNITE_API_KEY=your-api-key SLACK_WEBHOOK_URL=https://hooks.slack.com/... mvn exec:java -Dexec.mainClass=org.example.Main
```
