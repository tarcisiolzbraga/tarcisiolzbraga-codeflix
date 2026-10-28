# apresentação

Os onze slides de uma visão geral do projeto, para quem é técnico e não o conhece.

```
deck.json      o índice: título, ordem dos slides, seções e as duas tipografias
slides/*.html  um arquivo por slide, cada um com um <section> só
```

## O que estes arquivos são, e o que não são

**Não são uma página que abre no navegador.** Cada `.html` é um fragmento — um `<section>` numa
tela fixa de 1920×1080, com todo o estilo embutido. O que os monta num deck navegável é o
visualizador em que a apresentação foi publicada; abrir um deles direto mostra o conteúdo sem
layout.

A cópia aqui serve para três coisas: **versionar o texto** junto do código que ele descreve,
**revisar em diff** quando um número mudar, e **recuperar** se a publicação se perder.

## Os números vieram medidos

Nenhum dado de slide foi escrito de memória: a tabela de peças saiu do `README.md` da raiz, a de
carga do [`perf/README.md`](../perf/), e os três números do slide "Medir antes de afirmar" dos
commits que os mediram. Se um deles mudar no projeto, o slide fica desatualizado — e é por isso que
o texto mora aqui, onde o diff aparece.

Um já está nesse caso e vale saber: a tabela de carga usa os números **depois** do conserto do GC
(790 / 869 / 899 req/s), ao lado dos de antes. O `README.md` da raiz ainda registra só os antigos.

## As notas do apresentador

Cada slide termina num `<aside>`: é o que se fala e não se projeta — o contexto, o preço da decisão
e o que foi medido para chegar nela. Quem for apresentar lê essas notas antes.
