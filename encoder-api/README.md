# encoder-api

Converte as mídias que a `admin-api` recebe. Em Go, diferente dos outros dois serviços: o trabalho
aqui é chamar o `ffmpeg` e mexer em arquivo, e não servir API.

## O que ele faz

O admin publica um aviso quando um arquivo `VIDEO` ou `TRAILER` é enviado. O encoder consome,
converte e responde:

```
video.created.queue  →  baixa do Garage  →  ffmpeg: mp4 → MPEG-DASH  →  sobe o pacote
                                                                      ↓
                                               video.encoded.queue  ←  responde
```

São **duas respostas** por conversão: `PROCESSING` assim que o trabalho começa, que tira a mídia de
`PENDING` no admin, e depois `COMPLETED` com o caminho da saída, ou `ERROR` com o motivo.

O formato das mensagens está no [README da admin-api](../admin-api/README.md), que é o dono do
contrato. Dois pontos dele valem repetir:

- **o `checksum` volta igual** em todas as respostas. É por ele que o admin descarta resposta
  atrasada: o caminho do arquivo não distingue um envio do outro, porque reenviar sobrescreve no
  mesmo lugar;
- **`ERROR` é só registrado** pelo admin. O domínio dele ainda não tem um estado de falha para a
  mídia, então ela fica como está.

## Rodando

```bash
make test      # ciclo rápido: pula o que sobe container
make check     # tudo, inclusive Garage, RabbitMQ e Postgres em container. Porta de entrada do commit
make run       # precisa das variáveis; o compose da raiz as fornece
```

Pelo compose, da raiz do monorepo:

```bash
docker compose up -d encoder-api
```

## Decisões

A referência é o [microsservico-encoder](https://github.com/codeedu/microsservico-encoder) do curso,
lido no repositório. Ele **informa, não decide**, e as divergências deliberadas são:

| | Curso | Aqui |
|---|---|---|
| contrato | `{resource_id, file_path}` | o do admin, com `checksum` e os três status |
| conversão | Bento4: `mp4fragment` e `mp4dash` | `ffmpeg`, que faz os dois passos num só |
| armazenamento | Google Cloud Storage, credencial baixada à mão | Garage, o mesmo que o admin usa |
| esquema do banco | `AutoMigrate` do gorm | migration em SQL, aplicada na subida |
| banco nos testes | SQLite em memória | Postgres de verdade, em container |
| acesso a dados | gorm | SQL explícito com pgx |
| validação | `govalidator` com efeito global num `init()` | explícita, com erro declarado por motivo |
| anotações no domínio | `json:` e `gorm:` nas entidades | nenhuma; traduz o `messaging` |
| biblioteca de fila | `streadway/amqp`, descontinuada | `rabbitmq/amqp091-go`, o sucessor oficial |
| topologia do RabbitMQ | criada à mão pelo painel | provisionada por arquivo versionado |

### Por que duas respostas e não uma

O curso responde uma vez, no fim. Aqui o admin tem `PROCESSING` no domínio dele, e é essa resposta
que tira a mídia de `PENDING`. Sem ela, quem olha o vídeo não distingue "ninguém pegou" de "está
convertendo", e uma conversão longa pareceria trabalho perdido.

### Por que o `checksum` entra no domínio

Ele não é dado de transporte: é a identidade do envio. O par `(videoId, type)` diz qual mídia é,
mas não **qual envio dela** — e reenviar sobrescreve o arquivo no mesmo caminho. O índice único em
`(video_id, type, checksum)` é o que evita converter duas vezes o mesmo arquivo quando a fila
entrega a mesma mensagem mais de uma vez.
