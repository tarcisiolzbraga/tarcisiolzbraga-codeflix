"use client";

import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { fetchSignedMediaUrl, type MediaType } from "@/catalog/catalog";

// A arte de um título: pôster no acervo, banner na página.
//
// **O endereço é assinado, pelo mesmo motivo do player:** `<img src>` também não carrega cabeçalho
// de `Authorization`. O browser busca o `src` por conta própria, sem token — então a URL precisa se
// autorizar sozinha, e é o catálogo que a emite depois de conferir quem está pedindo.
//
// Diferente do player, o endereço é pedido **ao montar**, e não no clique: ninguém clica num pôster
// para que ele apareça. A assinatura vale cinco minutos, o que basta: imagem já carregada fica no
// cache do browser, e o que ainda não carregou pede um endereço novo.
//
// **O custo está aqui, dito em vez de escondido:** uma grade de doze títulos faz doze chamadas ao
// `/link`, uma por arte. Não há rota que assine várias de uma vez, e um bucket público resolveria
// isso ao preço de a arte deixar de ser restrita.

export function Artwork({
  videoId,
  type,
  location,
  title,
  className,
}: {
  videoId: string;
  type: MediaType;
  // A localização no armazenamento, como o catálogo a publica. Serve para **uma** coisa: saber se o
  // título tem a arte, para não pedir um endereço que voltaria vazio.
  location: string | null;
  title: string;
  className?: string;
}) {
  const token = useAuth().user?.access_token;
  // **Um estado, e não três.** A primeira versão tinha `idle`, `ready` e `none` — e a conferência ao
  // contrário mostrou que `none` não mudava nada na tela: ele e `idle` desenham o mesmo substituto,
  // de propósito. Estado que nenhum teste consegue distinguir é estado que não existe.
  const [url, setUrl] = useState<string | null>(null);

  useEffect(() => {
    if (location === null || token === undefined) {
      return;
    }
    let atual = true;
    fetchSignedMediaUrl(videoId, type, token)
      .then((assinado) => {
        if (atual && assinado !== null) {
          setUrl(assinado.url);
        }
      })
      // Arte que não carrega não é motivo para a tela falhar: o título continua navegável, e o lugar
      // dela mostra o substituto — que é o que já está desenhado.
      .catch((erro: unknown) => console.error("falha ao pedir o endereço da arte", erro));
    return () => {
      atual = false;
    };
  }, [videoId, type, location, token]);

  // O substituto: a inicial do título sobre um degradê. Vale para três casos que, para quem olha,
  // são o mesmo — título sem arte, endereço ainda a caminho, e falha ao pedi-lo. Um retângulo vazio
  // pareceria defeito nos três.
  if (url === null) {
    return (
      <div
        aria-hidden
        className={`flex items-center justify-center bg-gradient-to-br from-neutral-800 to-neutral-950 ${className ?? ""}`}
      >
        <span className="text-4xl font-bold text-neutral-700">{title.slice(0, 1)}</span>
      </div>
    );
  }

  return (
    // `<img>`, e não o `<Image>` do Next, de propósito: o otimizador dele busca a imagem **no
    // servidor**, e este endereço vale cinco minutos e é emitido para quem está olhando. Guardar em
    // cache um endereço que vence, num host que muda por ambiente, seria trocar um problema simples
    // por dois.
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={url}
      alt={`Arte de ${title}`}
      loading="lazy"
      className={`object-cover ${className ?? ""}`}
    />
  );
}
