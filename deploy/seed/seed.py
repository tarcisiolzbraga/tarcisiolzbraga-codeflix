#!/usr/bin/env python3
"""Semeia um catálogo mínimo: uma categoria, um gênero, dois do elenco, um filme com as cinco
mídias, publicado — e uma pessoa cadastrada pelo fluxo de verdade, com confirmação por e-mail.

O cluster nasce vazio a cada recriação, e conferir uma tela de catálogo sem catálogo não prova nada.

Os endereços vêm do ambiente, porque este script serve aos dois arranjos:

    # no compose, com tudo publicado no host
    python3 deploy/seed/seed.py

    # no cluster, pelos nomes do gateway
    ADMIN_URL=http://admin-api.localtest.me \\
    ASSINATURA_URL=http://assinatura.localtest.me \\
    KEYCLOAK_URL=http://keycloak.localtest.me \\
    MAILPIT_URL=http://mailpit.localtest.me \\
    python3 deploy/seed/seed.py
"""
import json
import mimetypes
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
env = {}
for linha in open(os.path.join(RAIZ, ".env")):
    linha = linha.strip()
    if linha and not linha.startswith("#") and "=" in linha:
        chave, valor = linha.split("=", 1)
        env[chave] = valor

ADMIN = os.environ.get("ADMIN_URL") or f"http://localhost:{env['APP_PORT']}"
ASSINATURA = os.environ.get("ASSINATURA_URL") or f"http://localhost:{env['SUBSCRIPTION_APP_PORT']}"
KC_BASE = os.environ.get("KEYCLOAK_URL") or f"http://localhost:{env['KEYCLOAK_PORT']}"
MAILPIT = os.environ.get("MAILPIT_URL") or f"http://localhost:{env['MAILPIT_UI_PORT']}"
KC = f"{KC_BASE}/realms/{env['KEYCLOAK_REALM']}/protocol/openid-connect"
MIDIA = os.environ.get("MIDIA_DIR") or "/tmp/codeflix-midia"
EMAIL, SENHA = os.environ.get("EMAIL_SEMEADURA", "ana.ribeiro@codeflix.com.br"), "uma-senha-boa"


def chama(metodo, url, corpo=None, token=None):
    dados = json.dumps(corpo).encode() if corpo is not None else None
    req = urllib.request.Request(url, data=dados, method=metodo)
    if dados:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            texto = r.read().decode()
            return r.status, (json.loads(texto) if texto.strip() else None)
    except urllib.error.HTTPError as e:
        bruto = e.read().decode()
        try:
            return e.code, json.loads(bruto)
        except ValueError:
            return e.code, bruto[:200]


def token_de_servico():
    dados = urllib.parse.urlencode({
        "grant_type": "client_credentials",
        "client_id": env["KEYCLOAK_CLIENT_ID"],
        "client_secret": env["KEYCLOAK_CLIENT_SECRET"],
    }).encode()
    with urllib.request.urlopen(urllib.request.Request(f"{KC}/token", data=dados), timeout=30) as r:
        return json.load(r)["access_token"]


def envia_arquivo(url, caminho, token):
    """Multipart à mão: o `urllib` não o monta, e trazer uma dependência para cinco arquivos não paga."""
    limite = "----" + uuid.uuid4().hex
    nome = os.path.basename(caminho)
    tipo = mimetypes.guess_type(nome)[0] or "application/octet-stream"
    with open(caminho, "rb") as f:
        conteudo = f.read()
    corpo = b"".join([
        f"--{limite}\r\n".encode(),
        f'Content-Disposition: form-data; name="file"; filename="{nome}"\r\n'.encode(),
        f"Content-Type: {tipo}\r\n\r\n".encode(),
        conteudo,
        f"\r\n--{limite}--\r\n".encode(),
    ])
    req = urllib.request.Request(url, data=corpo, method="POST")
    req.add_header("Content-Type", f"multipart/form-data; boundary={limite}")
    req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()[:200]


def acha_ou_cria(rota, nome, corpo, token, campo="name"):
    situacao, resposta = chama("GET", f"{ADMIN}/{rota}?search={urllib.parse.quote(nome)}&perPage=10", token=token)
    if situacao == 200:
        for item in resposta.get("items", []):
            if item.get(campo) == nome:
                print(f"  {rota}: '{nome}' já existia")
                return item["id"]
    situacao, resposta = chama("POST", f"{ADMIN}/{rota}", corpo, token=token)
    if situacao not in (200, 201):
        sys.exit(f"  {rota}: falhou com {situacao} {resposta}")
    print(f"  {rota}: '{nome}' criado")
    return resposta["id"]


def catalogo():
    if not os.path.isdir(MIDIA):
        print(f"gerando as mídias em {MIDIA}")
        subprocess.run([sys.executable, os.path.join(RAIZ, "deploy/seed/make-media.py"), MIDIA], check=True)

    token = token_de_servico()
    print("catálogo")
    filmes = acha_ou_cria("categories", "Filmes", {"name": "Filmes", "description": "Longas-metragens do acervo", "isActive": True}, token)
    ficcao = acha_ou_cria("genres", "Ficção científica", {"name": "Ficção científica", "isActive": True, "categoriesId": [filmes]}, token)
    diretor = acha_ou_cria("cast-members", "Denis Villeneuve", {"name": "Denis Villeneuve", "type": "DIRECTOR"}, token)
    ator = acha_ou_cria("cast-members", "Timothée Chalamet", {"name": "Timothée Chalamet", "type": "ACTOR"}, token)

    situacao, resposta = chama("GET", f"{ADMIN}/videos?search=Duna&perPage=10", token=token)
    existente = next((v for v in (resposta.get("items") or []) if v.get("title") == "Duna"), None) if situacao == 200 else None
    if existente:
        print("  videos: 'Duna' já existia")
        return existente["id"]

    situacao, resposta = chama("POST", f"{ADMIN}/videos", {
        "title": "Duna",
        "description": (
            "Paul Atreides chega a Arrakis, o único lugar do universo onde se colhe a especiaria — e "
            "descobre que a casa que lhe deram é uma armadilha. O deserto não perdoa o desatento."
        ),
        "launchedAt": 2021,
        "duration": 155.0,
        "opened": True,
        "published": False,
        "rating": "12",
        "categories": [filmes],
        "genres": [ficcao],
        "castMembers": [diretor, ator],
    }, token=token)
    if situacao not in (200, 201):
        sys.exit(f"  videos: falhou com {situacao} {resposta}")
    video = resposta["id"]
    print(f"  videos: 'Duna' criado ({video})")

    for tipo, arquivo in [("BANNER", "duna-banner.jpg"), ("THUMBNAIL", "duna-poster.jpg"),
                          ("THUMBNAIL_HALF", "duna-poster-meio.jpg"), ("VIDEO", "duna.mp4"),
                          ("TRAILER", "duna-trailer.mp4")]:
        situacao, resposta = envia_arquivo(f"{ADMIN}/videos/{video}/medias/{tipo}", f"{MIDIA}/{arquivo}", token)
        print(f"    {tipo:15} {arquivo:22} {situacao}")

    print("  publicando:", chama("PUT", f"{ADMIN}/videos/{video}/publish", token=token)[0])
    return video


def pessoa():
    print("pessoa")
    situacao, resposta = chama("POST", f"{ASSINATURA}/accounts/sign-up", {
        "firstname": "Ana", "lastname": "Ribeiro", "email": EMAIL,
        "password": SENHA, "documentNumber": "11144477735", "documentType": "cpf",
    })
    print(f"  cadastro: {situacao} {resposta}")
    if situacao not in (200, 201, 202):
        return

    # O token de confirmação chega por e-mail, e o coletor local tem API para lê-lo. É o único jeito
    # honesto de fechar o fluxo: inventar o token puláriam o mail-api, que é parte do que se quer ver.
    for _ in range(20):
        try:
            with urllib.request.urlopen(f"{MAILPIT}/api/v1/messages?limit=30", timeout=20) as r:
                mensagens = json.load(r).get("messages", [])
        except Exception as erro:
            print("  mailpit indisponível:", erro)
            return
        for m in mensagens:
            if EMAIL in json.dumps(m.get("To", [])):
                with urllib.request.urlopen(f"{MAILPIT}/api/v1/message/{m['ID']}", timeout=20) as r:
                    detalhe = json.load(r)
                achado = re.search(r"token=([A-Za-z0-9._\-]+)", (detalhe.get("Text") or "") + (detalhe.get("HTML") or ""))
                if achado:
                    print(f"  e-mail recebido: {m.get('Subject')!r}")
                    print("  confirmação:", chama("POST", f"{ASSINATURA}/accounts/sign-up/confirm", {"token": achado.group(1)})[0])
                    return
        time.sleep(3)
    print("  o e-mail não chegou a tempo")


if __name__ == "__main__":
    catalogo()
    pessoa()
    print(f"\npronto. A pessoa entra com {EMAIL} / {SENHA}; o papel do catálogo chega depois de assinar e pagar.")
