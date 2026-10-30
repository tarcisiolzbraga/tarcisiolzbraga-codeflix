#!/usr/bin/env python3
"""Compara duas fotos do /actuator/prometheus e mostra o que a carga mexeu.

O resumo do k6 diz o que o **cliente** viu. Isto diz o que o **serviço** sentiu — e é a diferença
entre "a latência subiu" e "a latência subiu porque o GC começou a pausar".

Só as famílias que respondem a uma pergunta de carga; o scrape inteiro tem centenas de séries, e
despejá-las aqui esconderia o que importa.
"""
import re
import sys
from collections import defaultdict

INTERESSANTES = {
    "http_server_requests_seconds_count": "requisições atendidas",
    "http_server_requests_seconds_sum": "tempo total atendendo (s)",
    "jvm_gc_pause_seconds_count": "pausas de GC",
    "jvm_gc_pause_seconds_sum": "tempo total em GC (s)",
    "jvm_memory_used_bytes": "heap em uso (bytes)",
    "jvm_threads_live_threads": "threads vivas",
    "process_cpu_usage": "uso de CPU do processo",
    "executor_active_threads": "threads de executor ativas",
}


def le(caminho):
    valores = defaultdict(float)
    for linha in open(caminho):
        if linha.startswith("#") or not linha.strip():
            continue
        m = re.match(r"^([a-z_0-9]+)(\{[^}]*\})?\s+([0-9.eE+-]+)$", linha.strip())
        if not m:
            continue
        nome, rotulos, valor = m.group(1), m.group(2) or "", m.group(3)
        if nome not in INTERESSANTES:
            continue
        # Soma as séries da mesma família: o que interessa aqui é o total do processo, não a
        # quebra por rótulo. A quebra fica no arquivo bruto, que é guardado junto.
        if nome == "jvm_memory_used_bytes" and 'area="heap"' not in rotulos:
            continue
        try:
            valores[nome] += float(valor)
        except ValueError:
            pass
    return valores


def main():
    antes, depois = le(sys.argv[1]), le(sys.argv[2])
    for nome, rotulo in INTERESSANTES.items():
        a, d = antes.get(nome), depois.get(nome)
        if a is None and d is None:
            continue
        a, d = a or 0.0, d or 0.0
        delta = d - a
        if nome in ("jvm_memory_used_bytes", "jvm_threads_live_threads", "process_cpu_usage",
                    "executor_active_threads"):
            print(f"  {rotulo:32} antes {a:>14,.2f}   depois {d:>14,.2f}")
        else:
            print(f"  {rotulo:32} {delta:>+14,.2f}")

    # A conta que o k6 não dá: o tempo médio que o **servidor** gastou por requisição, que comparado
    # com a latência do cliente separa "o serviço está lento" de "a fila antes dele está cheia".
    pedidos = depois.get("http_server_requests_seconds_count", 0) - antes.get("http_server_requests_seconds_count", 0)
    tempo = depois.get("http_server_requests_seconds_sum", 0) - antes.get("http_server_requests_seconds_sum", 0)
    if pedidos > 0:
        print(f"\n  média no servidor: {1000 * tempo / pedidos:.1f} ms por requisição ({pedidos:,.0f} medidas)")
    pausas = depois.get("jvm_gc_pause_seconds_count", 0) - antes.get("jvm_gc_pause_seconds_count", 0)
    emgc = depois.get("jvm_gc_pause_seconds_sum", 0) - antes.get("jvm_gc_pause_seconds_sum", 0)
    if pausas > 0:
        print(f"  GC: {pausas:,.0f} pausas somando {1000 * emgc:.0f} ms ({1000 * emgc / pausas:.1f} ms cada)")


if __name__ == "__main__":
    main()
