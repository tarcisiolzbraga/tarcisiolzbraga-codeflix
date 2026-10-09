# As tarefas do dia a dia. O equivalente ao gradlew dos serviços Java: o build acontece no host, e
# os containers ficam para a infraestrutura.
#
# cover separado de test de propósito: o relatório custa tempo e nem todo ciclo precisa dele.

.PHONY: run test test-all cover vet fmt check tidy

run:
	go run ./cmd/encoder

# O ciclo de escrita: -short pula o que sobe container, como o unitTests dos serviços Java.
# -race porque o encoder é concorrente por natureza: ele converte vários trabalhos ao mesmo tempo.
test:
	go test -race -short ./...

# Tudo, inclusive o que sobe Garage e RabbitMQ em container. Exige Docker.
test-all:
	go test -race ./...

cover:
	go test -race -coverprofile=coverage.out ./...
	go tool cover -func=coverage.out | tail -1

vet:
	go vet ./...

fmt:
	gofmt -l -w .

# A porta de entrada de cada commit, como o ./gradlew build é nos outros dois: roda tudo.
check: vet test-all
