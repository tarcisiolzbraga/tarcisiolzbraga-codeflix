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
RUN apk add --no-cache ca-certificates

# Usuário sem privilégio: mandar e-mail não tem motivo para rodar como root.
RUN addgroup -S mail && adduser -S mail -G mail

COPY --from=build --chown=mail:mail /mail /opt/app/mail

USER mail:mail

ENTRYPOINT ["/opt/app/mail"]
