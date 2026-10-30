#!/usr/bin/env bash
#
# Constrói as sete imagens **para o cluster**.
#
# Isto não é o mesmo que `docker compose build`, e a diferença é invisível até alguém abrir o front:
# as URLs das APIs são **embutidas no JavaScript em tempo de build**. Um build sem estes argumentos
# produz fronts apontando para `localhost:8080` e companhia — que é o certo para o compose e o errado
# para o cluster, onde as APIs atendem pelos nomes do gateway.
#
# Os dois fronts são os únicos que mudam de imagem entre os dois arranjos; as três Java e as duas Go
# leem configuração em tempo de execução, mas são construídas aqui do mesmo jeito para a lista ficar
# num lugar só.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
set -a; . ./.env; set +a

# Os fronts falam com as APIs **pelo browser**, então estes são os nomes que o gateway atende.
export PUBLIC_ADMIN_API_URL="${PUBLIC_ADMIN_API_URL:-http://admin-api.localtest.me}"
export PUBLIC_CATALOG_API_URL="${PUBLIC_CATALOG_API_URL:-http://catalogo.localtest.me}"
export PUBLIC_SUBSCRIPTION_API_URL="${PUBLIC_SUBSCRIPTION_API_URL:-http://assinatura.localtest.me}"
export PUBLIC_KEYCLOAK_ISSUER="${PUBLIC_KEYCLOAK_ISSUER:-http://keycloak.localtest.me/realms/codeflix}"
# E o portal é servido em localhost, por NodePort, porque o login precisa de contexto seguro.
export PUBLIC_PORTAL_URL="${PUBLIC_PORTAL_URL:-http://localhost:3000}"

# As três Java empacotam o jar no host; as outras compilam dentro da própria imagem.
for servico in admin-api videos-api subscription-api; do
  echo "==> jar de $servico"
  (cd "services/$servico" && ./gradlew bootJar -q --console=plain)
done

echo "==> imagens"
docker compose build admin-api videos-api subscription-api encoder-api mail-api portal-codeflix back-office-codeflix

echo
echo "pronto. Agora: bash deploy/up.sh (ou deploy/cluster/import-images.sh, se o cluster já existe)"
