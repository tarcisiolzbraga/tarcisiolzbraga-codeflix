#!/usr/bin/env python3
"""Gera as mídias da semeadura: dois pôsteres, um banner, um vídeo e um trailer.

Gerar em vez de versionar: binário em repositório de código é peso que nunca sai do histórico, e
estes arquivos existem só para a tela não ficar vazia. O vídeo sai do ffmpeg, que também é o que o
codificador usa — então o arquivo é de um formato que ele sabe converter de verdade.
"""
import os
import subprocess
import sys
from PIL import Image, ImageDraw, ImageFont

DESTINO = sys.argv[1] if len(sys.argv) > 1 else "/tmp/codeflix-midia"
NEGRITO = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
NORMAL = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
os.makedirs(DESTINO, exist_ok=True)


def duna(largura, altura, nome, tamanho_titulo, subtitulo):
    img = Image.new("RGB", (largura, altura))
    d = ImageDraw.Draw(img)
    for y in range(altura):  # um degradê de areia para a noite
        t = y / (altura - 1)
        d.line([(0, y), (largura, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip((208, 150, 78), (38, 24, 16))))
    for i, (topo, cor) in enumerate([(0.62, (120, 82, 44)), (0.74, (82, 55, 30)), (0.86, (48, 32, 18))]):
        d.ellipse([-largura * 0.3 + i * largura * 0.2, altura * topo, largura * 1.1 + i * largura * 0.1, altura * 1.8], fill=cor)
    d.ellipse([largura * 0.62, altura * 0.10, largura * 0.62 + largura * 0.17, altura * 0.10 + largura * 0.17], fill=(246, 222, 170))
    titulo = ImageFont.truetype(NEGRITO, tamanho_titulo)
    sub = ImageFont.truetype(NORMAL, int(tamanho_titulo * 0.22))
    caixa = d.textbbox((0, 0), "DUNA", font=titulo)
    x, y = (largura - (caixa[2] - caixa[0])) / 2, altura * 0.70
    d.text((x + 4, y + 4), "DUNA", font=titulo, fill=(20, 12, 6))
    d.text((x, y), "DUNA", font=titulo, fill=(250, 240, 220))
    cx = d.textbbox((0, 0), subtitulo, font=sub)
    d.text(((largura - (cx[2] - cx[0])) / 2, y + (caixa[3] - caixa[1]) + tamanho_titulo * 0.35), subtitulo, font=sub, fill=(226, 198, 150))
    img.save(f"{DESTINO}/{nome}", quality=92)
    print(f"  {nome:24} {img.size} {os.path.getsize(f'{DESTINO}/{nome}') // 1024} kB")


def video(nome, segundos, cor, texto, tom):
    saida = f"{DESTINO}/{nome}"
    subprocess.run([
        "ffmpeg", "-y", "-loglevel", "error",
        "-f", "lavfi", "-i", f"color=c={cor}:s=1280x720:d={segundos},format=yuv420p",
        "-f", "lavfi", "-i", f"sine=frequency={tom}:duration={segundos}",
        "-vf", f"drawtext=fontfile={NEGRITO}:text='{texto}':fontcolor=0xfaf0dc:fontsize=120:x=(w-text_w)/2:y=(h-text_h)/2",
        "-c:v", "libx264", "-preset", "veryfast", "-crf", "26",
        "-c:a", "aac", "-b:a", "64k", "-movflags", "+faststart", "-shortest", saida,
    ], check=True)
    print(f"  {nome:24} {segundos}s {os.path.getsize(saida) // 1024} kB")


duna(800, 1200, "duna-poster.jpg", 120, "O deserto não perdoa o desatento")
duna(1600, 600, "duna-banner.jpg", 150, "O deserto não perdoa o desatento")
duna(400, 600, "duna-poster-meio.jpg", 60, "2021")
video("duna.mp4", 12, "0x2a1a10", "DUNA", 220)
video("duna-trailer.mp4", 6, "0x101a2a", "DUNA — trailer", 330)
