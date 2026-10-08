# 💎 Projeto Diamante 02 — Delivery com microsserviços

Monorepo **delivery-system** (Gradle) — Java Advanced, 2º semestre.

## Integrantes

| Nome | RM |
|------|----|
| _preencher_ | _preencher_ |
| _preencher_ | _preencher_ |
| _preencher_ | _preencher_ |

## Stack

Java 25 · Spring Boot 4.0.8 · Spring Cloud 2025.1.2 (Eureka, LoadBalancer) · Spring Framework 7 (`@Retryable`) · Spring AMQP + RabbitMQ 4 · Spring Data JPA + H2 · Spring AI 2.0.1

## Arquitetura

```
App React Native ──REST──▶ order-service :8080 ──registro──▶ eureka-server :8761
 (usa :8080 e :8083)         │  cardápio · pedidos com lock    ▲   ▲
                             │  publica avaliações · IA        │   │ (todos se registram)
                             ├─ LB + retry ─▶ payment-service :8081 / :8082  (falha em ~50%)
                             ├─ AMQP ─▶ RabbitMQ: delivery.exchange ─(reviews.new)─▶ reviews.queue
                             │                                                    │
                             └─ ChatClient ─▶ OpenAI / LM Studio / Ollama         ▼
                                                              review-service :8083
                                                              buffer + flush a cada 5 s · ranking
```

| Serviço | Porta | Responsabilidade |
|---|---|---|
| `eureka-server` | 8761 | Service discovery |
| `order-service` | 8080 | Cardápio, pedidos (lock pessimista), publicação de avaliações, assistente de IA |
| `payment-service` | 8081 / 8082 | Pagamento simulado, instável (500 em ~50% das chamadas) |
| `review-service` | 8083 | Consome avaliações, acumula em memória, grava a cada 5 s, ranking |

## Como rodar

Pré-requisitos: **JDK 25** e **Docker**.

```bash
# 1. RabbitMQ (painel em http://localhost:15672, guest/guest)
docker compose up -d

# 2. Cada serviço em um terminal, nesta ordem
./gradlew :eureka-server:bootRun
./gradlew :payment-service:bootRun
./gradlew :payment-service:bootRun --args='--server.port=8082'   # segunda instância
./gradlew :review-service:bootRun
./gradlew :order-service:bootRun
```

Eureka: <http://localhost:8761> — devem aparecer `ORDER-SERVICE`, `PAYMENT-SERVICE` (2 instâncias) e `REVIEW-SERVICE`.

### Assistente de IA (chave nunca vai para o repositório)

```bash
cp .env.example .env     # .env está no .gitignore
# edite OPENAI_API_KEY no .env  (o order-service lê o arquivo ao subir)
```

Ou exporte as variáveis de ambiente: `export OPENAI_API_KEY=sk-...`

**Modelo local (LM Studio / Ollama):** suba o servidor local, defina `OPENAI_BASE_URL` (ex.: LM Studio `…:1234/v1`, Ollama `…:11434/v1`) e `OPENAI_MODEL`, e rode o order-service com `SPRING_PROFILES_ACTIVE=local`. Veja `.env.example`.

> Sem chave, tudo funciona normalmente; apenas `POST /assistant` responde `503 {"error": "..."}`.

### Variáveis opcionais

| Variável | Para quê |
|---|---|
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Endereço do Eureka, se não estiver na mesma máquina |
| `SPRING_RABBITMQ_HOST` | Endereço do RabbitMQ |
| `DELIVERY_RATE_LIMIT_ORDERS_PER_SECOND` | Limite do bônus (padrão 20; `0` desliga) |

Nenhum serviço chama outro por `localhost`: o pagamento é chamado por `http://PAYMENT-SERVICE/payments`, resolvido pelo Eureka.

## Contrato da API

Todos os erros têm o formato `{"error": "mensagem"}`.

| Método | Rota | Corpo | Respostas |
|---|---|---|---|
| GET | `/dishes` | — | 200 lista de Dish |
| GET | `/dishes/{id}` | — | 200 Dish · 404 |
| POST | `/orders` | `{"dishId":1,"quantity":2}` | 201 Order · 400 · 404 · 409 · 502 · 429 (bônus) |
| GET | `/orders/{id}` | — | 200 Order · 404 |
| POST | `/reviews` | `{"dishId":1,"rating":5,"comment":"Great"}` | 202 sem corpo · 400 · 404 |
| POST | `/assistant` | `{"question":"..."}` | 200 `{"answer":"..."}` |
| GET | `:8083/reviews/ranking` | — | 200 `[{dishId,dishName,average,count}]` |
| POST | `:8081/:8082/payments` (interno) | `{"amount":79.80}` | 200 `{"status":"APPROVED","instance":8081}` · 500 |

Exemplos prontos em [`requests.http`](requests.http).

## Como cada requisito foi atendido

| Tema | Onde / como |
|---|---|
| **Eureka** | `eureka-server`; os três serviços usam `spring.application.name` e `eureka-client`. Instâncias do pagamento têm `instance-id` com a porta |
| **Load balance** | `RestClientConfig`: `RestTemplate` com `@LoadBalanced`; `PaymentClient` chama `http://PAYMENT-SERVICE/payments`. Os logs mostram `[payment-service:8081]` / `[payment-service:8082]` alternando |
| **Retry** | `PaymentClient` (bean separado) com `@Retryable` (`maxRetries=4`, `delay=200`, `multiplier=2`, `jitter=100`, `maxDelay=2000`) e `@EnableResilientMethods`. Esgotadas as tentativas: rollback, estoque intacto, **502** |
| **Race condition** | `DishRepository.findByIdForUpdate` com `@Lock(PESSIMISTIC_WRITE)` + `@Transactional` em `OrderService.createOrder`. 50 requisições simultâneas → exatamente 10 confirmados, stock 0 |
| **Mensageria** | `RabbitConfig`: `TopicExchange delivery.exchange`, fila durável `reviews.queue`, routing key `reviews.new` e `Binding` explícitos; JSON via `JacksonJsonMessageConverter`. `POST /reviews` valida (1–5), publica e responde **202** sem gravar no banco |
| **Backpressure** | `review-service`: `@RabbitListener` → `ConcurrentHashMap` (soma + quantidade por prato) → `@Scheduled` a cada 5 s grava em `ReviewSummary` (H2) e limpa o buffer. `GET /reviews/ranking` lê do banco |
| **Spring AI** | `ChatService`: `ChatClient` criado do `ChatClient.Builder`, system message (atendente, respostas curtas em PT-BR, recusa fora do tema) e cardápio (nome, preço, estoque) lido do banco a cada pergunta |
| **Bônus rate limit** | `TokenBucket` + `RateLimitInterceptor`: 20 req/s em `POST /orders`, excedente recebe **429** `{"error": ...}` |
| **CORS** | Liberado em 8080 e 8083 (o app roda no navegador via Expo web) |

### Decisão de projeto: pagamento dentro da transação

O estoque é reservado, o pagamento é cobrado e o pedido é confirmado numa **única transação**, com a linha do prato bloqueada. Isso garante a regra "pagamento falhou ⇒ estoque intacto" por simples rollback. O custo: pedidos do mesmo prato ficam em fila enquanto o pagamento (com retries) responde. Para este cenário (10 unidades) é o comportamento desejado; em produção real, o usual seria reservar → cobrar fora do lock → compensar em caso de falha.

## Testando

```bash
# 50 pedidos simultâneos no prato da promoção (id 1, stock 10)
./scripts/concurrency-test.sh

# Teste automatizado (pagamento mockado): 50 threads => 10 pedidos, stock 0; falha de pagamento => rollback
./gradlew :order-service:test
```

Para ver apenas o par 201/409 sem o rate limit: `./gradlew :order-service:bootRun --args='--delivery.rate-limit.orders-per-second=0'`.

## App React Native

Use o app do professor ([joaocarloslima/diamante-delivery](https://github.com/joaocarloslima/diamante-delivery)) apontando para o IP da máquina que roda os serviços. Ele usa `:8080` (order-service) e `:8083` (review-service).
