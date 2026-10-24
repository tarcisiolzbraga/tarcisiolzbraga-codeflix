#!/usr/bin/env bash
#
# O resolvedor **dos nós** do cluster.
#
# O k3d escreve `nameserver <gateway da rede docker>` no `/etc/resolv.conf` de cada nó, e isso só
# funciona se o host atender DNS naquele endereço. Nesta máquina o `systemd-resolved` escuta apenas
# em 127.0.0.53, que de dentro do container é o próprio container — então o nó não resolve nada.
#
# O sintoma não fala de DNS: o pod fica em `Init:0/2` para sempre, e o evento diz
# "failed to get sandbox image rancher/mirrored-pause:3.6 ... lookup registry-1.docker.io: Try again".
# Levei um tempo lendo logs de aplicação por causa disso.
#
# Vai no nó, e não na configuração do CoreDNS, porque são dois problemas: o CoreDNS encaminha o que
# não conhece para o `/etc/resolv.conf` **do nó**, e quem baixa a imagem do sandbox é o containerd,
# que nem passa pelo CoreDNS.
set -euo pipefail

CLUSTER="${CLUSTER:-codeflix}"
# Resolvedores públicos de propósito: o upstream do host muda de rede para rede (Wi-Fi, VPN), e um
# cluster que para de subir quando se troca de rede é pior que um que não usa o resolvedor de casa.
RESOLVEDORES="${RESOLVEDORES:-1.1.1.1 8.8.8.8}"

# Pelo rótulo do Docker, e não pelo `k3d node list`: a saída dele muda de formato entre versões, e
# um nó perdido no filtro é um nó que continua sem resolver — foi o que aconteceu na primeira versão
# deste script, e o cluster ficou meio consertado.
for no in $(docker ps --format '{{.Names}}' --filter "label=k3d.cluster=${CLUSTER}" --filter "label=k3d.role=server" --filter "label=k3d.role=agent"); do
  case "$no" in *serverlb*) continue;; esac
  docker exec "$no" sh -c "for r in $RESOLVEDORES; do echo \"nameserver \$r\"; done > /etc/resolv.conf"
  echo "  $no: $(docker exec "$no" sh -c 'tr "\n" " " < /etc/resolv.conf')"
done

# O CoreDNS leu o resolv.conf na subida; sem reiniciar, ele segue encaminhando para o endereço morto.
kubectl -n kube-system rollout restart deployment coredns >/dev/null
kubectl -n kube-system rollout status deployment coredns --timeout=120s
