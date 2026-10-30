#!/usr/bin/env bash
#
# O DNS que faz o `iss` do token fechar.
#
# O problema, em uma frase: o `iss` de um token é **a URL que o cliente usou** para pedi-lo, e quem
# valida precisa buscar as chaves públicas na mesma URL. Browser e pod estão em redes diferentes, e
# `localhost` significa coisas diferentes nas duas — foi exatamente isso que derrubou a tentativa de
# rodar as aplicações em container, no compose.
#
# **`*.localtest.me`, e não `*.localhost` — e isto foi aprendido errando.**
#
# `.localhost` parecia melhor: além de resolver para loopback sem configuração, o navegador trata
# toda origem nele como **contexto seguro**, que é o que o PKCE do login precisa (`crypto.subtle` não
# existe fora de contexto seguro). Mas é exatamente por ser reservado para loopback que ele não serve
# aqui: o resolvedor do container responde `127.0.0.1` para `keycloak.localhost` **sem consultar o
# DNS**, e o pod tenta falar consigo mesmo. Medido: `connect to 127.0.0.1 port 80 failed`.
#
# Então os dois lados usam `*.localtest.me`, e o contexto seguro do navegador vem de outro lugar: os
# dois fronts são publicados em `localhost:3000` e `localhost:3001` (veja deploy/apps/fronts-nodeport.yaml).
# As APIs e o Keycloak, que o browser também alcança, não precisam de contexto seguro.
#
# O caminho definitivo é TLS no gateway. Aí o certificado autoassinado precisa entrar no truststore
# das três JVMs, senão elas recusam o JWKS — fica para quando houver HTTPS de propósito.
#
# A saída é um nome que vale nos dois lados:
#
#   de fora   keycloak.localtest.me -> 127.0.0.1 (resolvido pelo próprio sistema, sem /etc/hosts)
#                                   -> porta 80 do host -> balanceador do k3d -> gateway do Istio
#   de dentro keycloak.localtest.me -> este bloco do CoreDNS -> ClusterIP do gateway do Istio
#
# O mesmo nome, a mesma porta, os dois lados. Nenhuma linha de código dos serviços muda — e é por
# isso que este arquivo existe em vez de um segundo endereço de JWKS na configuração deles.
set -euo pipefail

IP="$(kubectl -n istio-system get svc istio-ingressgateway -o jsonpath='{.spec.clusterIP}')"
[ -n "$IP" ] || { echo "o gateway do Istio não foi encontrado; rode deploy/mesh/install-istio.sh primeiro" >&2; exit 1; }

# `coredns-custom` é o gancho do k3s: o Corefile dele termina em `import /etc/coredns/custom/*.server`,
# então um bloco novo entra por aqui sem editar o Corefile — que o k3s reescreveria de volta.
#
# `template`, e não `hosts`: uma regra responde por **qualquer** nome em localhost, então
# acrescentar um serviço ao gateway não exige voltar aqui.
kubectl -n kube-system apply -f - <<YAML
apiVersion: v1
kind: ConfigMap
metadata:
  name: coredns-custom
  namespace: kube-system
data:
  localtest.server: |
    localtest.me:53 {
        errors
        template IN A localtest.me {
            match .*\.localtest\.me\.\$
            answer "{{ .Name }} 60 IN A ${IP}"
        }
        # O AAAA responde **vazio**, e não nada: sem esta regra a consulta IPv6 volta SERVFAIL, e
        # cliente que pergunta os dois (o Java pergunta) trata o erro como falha de resolução em vez
        # de cair no IPv4. NOERROR sem resposta é o "não existe endereço v6" que o resolvedor espera.
        template IN AAAA localtest.me {
            match .*\.localtest\.me\.\$
            rcode NOERROR
        }
    }
YAML

kubectl -n kube-system rollout restart deployment coredns
kubectl -n kube-system rollout status deployment coredns --timeout=120s
echo "localtest.me resolve para ${IP} dentro do cluster"
