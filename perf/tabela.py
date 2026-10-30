#!/usr/bin/env python3
"""Monta a tabela dos degraus a partir dos resumos que o k6 exportou.

A pergunta que ela responde: **em que nível a latência deixa de ser plana**. Para isso a coluna que
importa não é o p99 sozinho, é a razão entre o p99 de cada degrau e o do primeiro — enquanto o
serviço tem folga, dobrar a carga dobra a vazão e deixa a latência quase igual; passado o joelho, a
vazão para de subir e a latência passa a absorver tudo.
"""
import json
import os
import re
import sys


def numeros(caminho):
    d = json.load(open(caminho))
    m = d.get("metrics", {})
    dur = m.get("http_req_duration", {})
    reqs = m.get("http_reqs", {})
    falhas = m.get("http_req_failed", {})
    checks = m.get("checks", {})
    return {
        "rps": reqs.get("rate", 0),
        "total": reqs.get("count", 0),
        "med": dur.get("med", 0),
        "p95": dur.get("p(95)", 0),
        "p99": dur.get("p(99)", 0),
        "max": dur.get("max", 0),
        "falhou": 100 * falhas.get("value", falhas.get("rate", 0) or 0),
        "checks_ok": 100 * (checks.get("passes", 0) / max(1, checks.get("passes", 0) + checks.get("fails", 0))),
    }


def main():
    pasta = sys.argv[1]
    arquivos = []
    for nome in os.listdir(pasta):
        m = re.match(r"^nivel-(\d+)\.json$", nome)
        if m:
            arquivos.append((int(m.group(1)), os.path.join(pasta, nome)))
    arquivos.sort()
    if not arquivos:
        print("nenhum resumo encontrado em", pasta)
        return

    base = None
    print(f"  {'VUs':>4}  {'req/s':>8}  {'mediana':>9}  {'p95':>9}  {'p99':>9}  {'máx':>9}  {'p99/base':>9}  {'erro':>6}")
    print(f"  {'-'*4}  {'-'*8}  {'-'*9}  {'-'*9}  {'-'*9}  {'-'*9}  {'-'*9}  {'-'*6}")
    for vus, caminho in arquivos:
        n = numeros(caminho)
        if base is None:
            base = n["p99"] or 1
        razao = n["p99"] / base
        print(f"  {vus:>4}  {n['rps']:>8.1f}  {n['med']:>8.1f}ms  {n['p95']:>8.1f}ms  "
              f"{n['p99']:>8.1f}ms  {n['max']:>8.1f}ms  {razao:>8.1f}x  {n['falhou']:>5.1f}%")

    print()
    # O joelho: o primeiro nível em que dobrar a carga deixou de dobrar a vazão.
    anterior = None
    for vus, caminho in arquivos:
        n = numeros(caminho)
        if anterior and anterior[1] > 0:
            ganho = n["rps"] / anterior[1]
            dobrou = vus / anterior[0]
            if ganho < 0.6 * dobrou:
                print(f"  joelho entre {anterior[0]} e {vus} VUs: a carga subiu {dobrou:.1f}x "
                      f"e a vazão só {ganho:.1f}x")
                break
        anterior = (vus, n["rps"])
    else:
        print("  nenhum joelho até o último nível: a vazão ainda acompanhava a carga")


if __name__ == "__main__":
    main()
