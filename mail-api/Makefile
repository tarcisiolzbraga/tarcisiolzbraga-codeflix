# As tarefas do dia a dia. O equivalente ao gradlew dos serviços Java: o build acontece no host, e
# os containers ficam para a infraestrutura.

.PHONY: run test cover vet fmt check tidy

run:
	go run ./cmd/mail

# -race porque o serviço é concorrente: vários operários consomem a fila ao mesmo tempo.
#
# -count=1 desliga o cache: o copied_sources_test.go lê o ecs.go do encoder-api, que vive fora deste
# módulo, e o cache do `go test` não vigia arquivo de fora. Sem isto ele responde "(cached)" mesmo
# com a cópia já divergida — verificado divergindo a cópia de propósito. A suíte roda em ~1s, então
# o cache não comprava nada mesmo.
test:
	go test -race -count=1 ./...

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
