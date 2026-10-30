#!/usr/bin/env bash
#
# A tag das imagens: **uma fonte só**, porque três lugares precisam concordar.
#
# Quem constrói, quem empurra e quem aplica têm de usar exatamente a mesma string — e se cada um
# calculasse a sua, bastaria um deles mudar de ideia para o cluster pedir uma imagem que ninguém
# empurrou. O sintoma seria `ImagePullBackOff`, longe da causa.
#
# **Por que não `latest`.** Com tag mutável, duas imagens diferentes têm o mesmo nome: foi
# exatamente o que aconteceu nesta máquina, com seis importações seguidas sob a mesma tag. E o
# estrago maior é no `rollout undo` — ele volta a especificação anterior, que aponta para a **mesma**
# tag, então desfaz no papel e não nos bytes. Rollback que não desfaz é pior que rollback nenhum,
# porque dá a impressão de rede de proteção.
#
# **E por que o sufixo quando a árvore está suja.** O SHA identifica o commit, não o que foi
# construído: com mudança não commitada, a imagem contém código que o SHA não descreve. O sufixo diz
# isso em voz alta, em vez de deixar alguém procurar num commit o código que está rodando.
set -euo pipefail

SHA="$(git rev-parse --short HEAD)"
if [ -n "$(git status --porcelain)" ]; then
  echo "${SHA}-sujo"
else
  echo "$SHA"
fi
