#!/usr/bin/env python3
"""Semeia o catálogo com volume, para o teste de carga ter o que ler.

O `deploy/seed/seed.py` cria **um** filme: serve para conferir uma tela, não para medir leitura
paginada. Aqui o alvo é outro — dezenas ou centenas de vídeos, cada um com categorias, gêneros e
elenco, porque é a resolução dessas relações que custa no GraphQL do catálogo.

Grava na admin-api e espera o CDC replicar: é o caminho de verdade do dado, e escrever direto no
Elasticsearch mediria um catálogo que não existe.

    ADMIN_URL=http://admin-api.localtest.me CATALOGO_URL=http://catalogo.localtest.me \\
    python3 perf/semear.py 200
"""
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
env = {}
for linha in open(os.path.join(RAIZ, ".env")):
    linha = linha.strip()
    if linha and not linha.startswith("#") and "=" in linha:
        chave, valor = linha.split("=", 1)
        env[chave] = valor

ADMIN = os.environ.get("ADMIN_URL") or f"http://localhost:{env['APP_PORT']}"
CATALOGO = os.environ.get("CATALOGO_URL") or f"http://localhost:{env['CATALOG_APP_PORT']}"
KC_BASE = os.environ.get("KEYCLOAK_URL") or f"http://localhost:{env['KEYCLOAK_PORT']}"
KC = f"{KC_BASE}/realms/{env['KEYCLOAK_REALM']}/protocol/openid-connect"
QUANTOS = int(sys.argv[1]) if len(sys.argv) > 1 else 200

RATINGS = ["ER", "L", "10", "12", "14", "16", "18"]


def chama(metodo, url, corpo=None, token=None, timeout=60):
    dados = json.dumps(corpo).encode() if corpo is not None else None
    pedido = urllib.request.Request(url, data=dados, method=metodo)
    if dados:
        pedido.add_header("Content-Type", "application/json")
    if token:
        pedido.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(pedido, timeout=timeout) as r:
            texto = r.read().decode()
            return r.status, (json.loads(texto) if texto else None)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


def token_de(client_id):
    dados = urllib.parse.urlencode({
        "grant_type": "client_credentials",
        "client_id": client_id,
        "client_secret": env["KEYCLOAK_CLIENT_SECRET"],
    }).encode()
    with urllib.request.urlopen(urllib.request.Request(f"{KC}/token", data=dados), timeout=30) as r:
        return json.load(r)["access_token"]


def cria(rota, corpo, token):
    status, corpo_resposta = chama("POST", f"{ADMIN}/{rota}", corpo, token)
    if status not in (200, 201):
        raise SystemExit(f"falhou criar em /{rota}: HTTP {status} {corpo_resposta}")
    return corpo_resposta["id"]


def referencias(token):
    """As relações que cada vídeo aponta. Poucas e reusadas de propósito: o custo que se quer medir
    é o de **resolver** a relação na leitura, não o de ter muitas distintas."""
    categorias = [cria("categories", {"name": f"Carga {i}", "description": "para medir"}, token)
                  for i in range(3)]
    generos = [cria("genres", {"name": f"Gênero de carga {i}", "categories": categorias}, token)
               for i in range(3)]
    # `/cast-members` com hífen: a rota não segue o snake_case do resto do mundo FC3, e errar isso
    # dá 404 depois de as categorias e os gêneros já terem sido criados.
    elenco = [cria("cast-members", {"name": f"Elenco de carga {i}", "type": "ACTOR"}, token)
              for i in range(3)]
    return categorias, generos, elenco


def conta_no_catalogo(token):
    consulta = {"query": "{ videos(perPage: 1) { meta { total } } }"}
    status, corpo = chama("POST", f"{CATALOGO}/api/graphql", consulta, token)
    if status != 200 or not corpo or corpo.get("errors"):
        return -1
    return corpo["data"]["videos"]["meta"]["total"]


def main():
    admin = token_de("admin-codeflix")
    leitor = token_de("catalog-test-codeflix")

    antes = conta_no_catalogo(leitor)
    print(f"catálogo antes: {antes} vídeo(s)")

    print("criando as referências")
    categorias, generos, elenco = referencias(admin)

    # **Dois pedidos por vídeo, e não um.** O catálogo só serve o que está `active` E `published`
    # (ver o VideoElasticsearchGateway), e `published` não é campo da criação: é a rota
    # `PUT /videos/{id}/publish`. Semear sem publicar enche o admin e deixa o catálogo vazio — o
    # tipo de erro que só aparece quando o teste de carga mede zero item por página.
    print(f"criando e publicando {QUANTOS} vídeos")
    for i in range(QUANTOS):
        video = cria("videos", {
            "title": f"Filme de carga {i:04d} {uuid.uuid4().hex[:6]}",
            "description": "Registro criado para o teste de carga do catálogo.",
            "launchedAt": 1990 + (i % 36),
            "duration": 90.0 + (i % 60),
            "rating": RATINGS[i % len(RATINGS)],
            "categories": categorias,
            "genres": generos,
            "castMembers": elenco,
        }, admin)
        status, corpo = chama("PUT", f"{ADMIN}/videos/{video}/publish", None, admin)
        if status != 200:
            raise SystemExit(f"falhou publicar {video}: HTTP {status} {corpo}")
        if (i + 1) % 25 == 0:
            print(f"  {i + 1}/{QUANTOS}")

    # O CDC é assíncrono: esperar pela contagem é mais honesto que um sleep fixo.
    alvo = antes + QUANTOS if antes >= 0 else QUANTOS
    print("esperando o CDC replicar")
    limite = time.time() + 300
    while time.time() < limite:
        agora = conta_no_catalogo(leitor)
        if agora >= alvo:
            print(f"catálogo depois: {agora} vídeo(s)")
            return
        print(f"  {agora}/{alvo}")
        time.sleep(5)
    raise SystemExit(f"o CDC não replicou tudo em 5 min (está em {conta_no_catalogo(leitor)}/{alvo})")


if __name__ == "__main__":
    main()
