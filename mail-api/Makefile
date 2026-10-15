# As tarefas do dia a dia. O equivalente ao gradlew dos serviços Java: o build acontece no host, e
# os containers ficam para a infraestrutura.

.PHONY: run test cover vet fmt check tidy

run:
	go run ./cmd/mail

# -race porque o serviço é concorrente: vários operários consomem a fila ao mesmo tempo.
test:
	go test -race ./...

cover:
	go test -race -coverprofile=coverage.out ./...
	go tool cover -func=coverage.out | tail -1

vet:
	go vet ./...

fmt:
	gofmt -l -w .

tidy:
	go mod tidy

# A porta de entrada de cada commit, como o ./gradlew build é nos serviços Java.
check: vet test
