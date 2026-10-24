# Duas etapas: a primeira compila, a segunda só carrega o binário e o ffmpeg.
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
RUN CGO_ENABLED=0 go build -o /encoder ./cmd/encoder

FROM alpine:3.21

# ffmpeg é a única dependência de sistema: é ele que converte. O curso instala o Bento4 compilado
# à mão aqui; uma linha do gerenciador de pacotes resolve o mesmo.
# O `upgrade` antes do `add` pega as correções publicadas depois de a base ser construída. No
# encoder isto achou o CVE-2026-84782 no libssl3/libcrypto3 3.3.7-r1, com correção no -r2 já
# disponível no repositório do Alpine.
RUN apk -U upgrade --no-cache && apk add --no-cache ffmpeg ca-certificates

# Usuário sem privilégio: o encoder não tem motivo para rodar como root.
RUN addgroup -S encoder && adduser -S encoder -G encoder

COPY --from=build --chown=encoder:encoder /encoder /opt/app/encoder

USER encoder:encoder

ENTRYPOINT ["/opt/app/encoder"]
