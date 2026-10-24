# Duas etapas: a primeira compila, a segunda só carrega o binário.
#
# Diferente dos serviços Java, onde o jar é construído no host e a imagem só o empacota: aqui a
# compilação é barata e cabe na imagem, o que dispensa ter Go instalado para gerar a imagem.
FROM golang:1.27-alpine AS build

WORKDIR /src

# go.mod e go.sum antes do resto: enquanto as dependências não mudarem, esta camada é reaproveitada
# e o download não se repete a cada alteração de código.
COPY go.mod go.sum ./
RUN go mod download

COPY . .

# CGO desligado para o binário não depender da libc da imagem de build: assim ele roda no alpine
# final sem precisar das mesmas bibliotecas.
RUN CGO_ENABLED=0 go build -o /mail ./cmd/mail

FROM alpine:3.21

# Nenhuma dependência de sistema: o SMTP vem da biblioteca padrão do Go. Só os certificados, para o
# dia em que o destino for um provedor com TLS.
# O `upgrade` antes do `add` pega as correções publicadas depois de a base ser construída. No
# encoder isto achou o CVE-2026-84782 no libssl3/libcrypto3 3.3.7-r1, com correção no -r2 já
# disponível no repositório do Alpine.
RUN apk -U upgrade --no-cache && apk add --no-cache ca-certificates

# Usuário sem privilégio: mandar e-mail não tem motivo para rodar como root.
#
# `mailer`, e não `mail`: o alpine **já traz** um grupo `mail` (gid 12) e um usuário `mail` (uid 8),
# então `addgroup -S mail` falha com "group 'mail' in use" e a imagem não constrói. O encoder não
# tropeça nisso porque `encoder` não existe na imagem base.
RUN addgroup -S mailer && adduser -S mailer -G mailer

COPY --from=build --chown=mailer:mailer /mail /opt/app/mail

USER mailer:mailer

ENTRYPOINT ["/opt/app/mail"]
