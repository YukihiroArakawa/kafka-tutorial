# README.md

SpringアプリケーションとDocker上で起動したKafkaを連携させて動かすサンプルアプリケーションです。

## アーキテクチャ概要
アプリがRESTで受けたリクエストをKafkaに発行し、Consumerが購読して処理します。

```mermaid
flowchart LR
  subgraph Client
    U[User / curl]
  end

  subgraph App[Spring Boot App]
    P["ProducerController<br/>/produce"]
    T[KafkaTemplate]
    C["ConsumerController<br/>@KafkaListener"]
    R["REST: /consume/records"]
    P --> T
    C --> R
  end

  K["Kafka Broker<br/>Topic: sample-topic"]

  U -->|HTTP| P
  T -->|send a value| K
  K -->|poll| C
  U -->|HTTP| R
```

## 起動手順
- 依存: Docker, Docker Compose, JDK 21
- Kafka起動: `docker compose up -d`
- アプリ起動: `./gradlew bootRun`

主要設定（`src/main/resources/application.properties`）
- `spring.kafka.bootstrap-servers=localhost:29092`
- `app.kafka.topic=sample-topic`
- `app.kafka.consumer-group=sample-consumer`

## 動作確認
- 送信（GET）: `curl "http://localhost:8080/produce?message=hello"`
- 送信（POST）: `curl -X POST http://localhost:8080/produce -H "Content-Type: application/json" -d '{"value":"hello"}'`
- 受信ログ: アプリ標準出力に `Consumed message: topic=... value=...` がINFOで出力されます。
- 受信一覧（REST）: `curl http://localhost:8080/consume/records`
- CLIで直接確認（コンテナ内）:
  - `docker compose exec -it kafka bash -lc "kafka-console-consumer --bootstrap-server localhost:9092 --topic sample-topic --from-beginning"`

## トラブルシュート
- Kafkaが未起動: `docker compose ps` と `docker compose logs -f kafka` を確認。
- トピック未作成時: `docker compose exec -it kafka bash -lc "kafka-topics --bootstrap-server localhost:9092 --create --topic sample-topic --partitions 1 --replication-factor 1"`
- ポート競合: アプリは `8080`、Kafkaホスト向けは `29092` を使用しています。

## 補足: Consumer Group ID のイメージ
同じ group.id のコンシューマはトピックのパーティションを分担して処理し、異なる group.id のコンシューマはそれぞれ独立に全メッセージを消費します（ブロードキャスト的）。

```mermaid
flowchart LR
  P[Producer] --> T["Topic: sample-topic"]
  T --> T0[Partition 0]
  T --> T1[Partition 1]
  T --> T2[Partition 2]

  subgraph GA[Consumer Group: sample-consumer]
    A1[Consumer A1]
    A2[Consumer A2]
    A3[Consumer A3]
  end

  subgraph GB[Consumer Group: analytics-consumer]
    B1[Consumer B1]
  end

  %% 同一グループ内は分担消費（1パーティション=1コンシューマ）
  T0 --> A1
  T1 --> A2
  T2 --> A3

  %% 別グループは独立に全パーティションを消費（ブロードキャスト）
  T0 -.-> B1
  T1 -.-> B1
  T2 -.-> B1
```

- 同一グループ: 1パーティションにつき同時に処理するコンシューマは1つ。台数を増やすと負荷分散（パーティション数まで有効）。
- 別グループ: それぞれが独立の読み取り位置（オフセット）を持ち、全メッセージを受け取れる。
- オフセットはグループ単位で管理され、再起動やスケール時も処理位置が維持されます。
