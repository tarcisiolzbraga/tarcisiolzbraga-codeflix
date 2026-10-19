import { QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router";
import { vi } from "vitest";
import { FeedbackProvider } from "@/shared/FeedbackProvider";
import { newQueryClient } from "@/shared/queryClient";

// Os mesmos provedores do App, menos a autenticação — ela é dublada em cada teste, porque trocar o
// `?code=` por token exige um navegador de verdade.
//
// O cliente de consultas é o **de produção** (`newQueryClient`), e não um com `retry: false`: um
// dublê aqui faria os testes passarem com uma política que ninguém usa. O preço é que falha de rede
// leva as repetições em tempo real, e por isso aquele caso é testado no `queryClient.test.ts`.

// Mostra o caminho atual para o teste poder afirmar que a tela navegou. Fica fora do `Routes` para
// valer em qualquer rota.
function Onde() {
  return <span data-testid="rota">{useLocation().pathname}</span>;
}

// O DataGrid precisa de altura medida para desenhar linha, e o jsdom dá zero a tudo. Sem isto a
// tabela existe mas nenhuma célula é renderizada, e o teste acusaria um defeito que não existe.
export function withMeasuredHeight() {
  for (const propriedade of ["offsetHeight", "offsetWidth"] as const) {
    vi.spyOn(HTMLElement.prototype, propriedade, "get").mockReturnValue(600);
  }
}

export function renderWithProviders(
  element: React.ReactNode,
  { path = "/", route = "/" }: { path?: string; route?: string } = {},
) {
  return render(
    <QueryClientProvider client={newQueryClient()}>
      <FeedbackProvider>
        <MemoryRouter initialEntries={[route]}>
          <Onde />
          <Routes>
            <Route path={path} element={element} />
          </Routes>
        </MemoryRouter>
      </FeedbackProvider>
    </QueryClientProvider>,
  );
}

// Uma `Response` nova por chamada: reaproveitar a mesma instância faz a segunda leitura estourar com
// "body already read", e o teste acusa um defeito que não existe.
export function jsonResponse(body: unknown, init: ResponseInit = {}): Response {
  return body === undefined
    ? new Response(null, init)
    : new Response(JSON.stringify(body), {
        ...init,
        headers: { "Content-Type": "application/json" },
      });
}

// **Um dublê de `XMLHttpRequest`**, porque o envio de mídia usa XHR e não `fetch`: é o único que
// expõe o progresso do envio, e um arquivo de 100 MB sem barra de progresso é uma tela que parece
// travada.
//
// O jsdom tem um `XMLHttpRequest` de verdade, e ele tentaria falar com a rede. Este substitui a
// classe e devolve um controlador: o teste vê o que foi pedido e decide quando a resposta chega.
export type SentRequest = {
  method: string;
  url: string;
  headers: Record<string, string>;
  body: unknown;
};

export type XhrDouble = {
  sent: SentRequest[];
  /** Entrega o progresso do envio, como o browser o entregaria. */
  progress: (index: number, loaded: number, total: number) => void;
  respond: (index: number, status: number, body?: string) => void;
  /** Rede caída: o browser emite `error`, e não um status. */
  fail: (index: number) => void;
};

export function installXhrDouble(): XhrDouble {
  const sent: SentRequest[] = [];
  const instancias: FakeXhr[] = [];

  class FakeXhr {
    private readonly ouvintes = new Map<string, ((event: unknown) => void)[]>();
    private readonly doEnvio = new Map<string, ((event: unknown) => void)[]>();
    private readonly cabecalhos: Record<string, string> = {};
    private metodo = "";
    private endereco = "";

    status = 0;
    responseText = "";

    readonly upload = {
      addEventListener: (nome: string, ouvinte: (event: unknown) => void) => {
        this.doEnvio.set(nome, [...(this.doEnvio.get(nome) ?? []), ouvinte]);
      },
    };

    open(metodo: string, endereco: string) {
      this.metodo = metodo;
      this.endereco = endereco;
    }

    setRequestHeader(nome: string, valor: string) {
      this.cabecalhos[nome] = valor;
    }

    addEventListener(nome: string, ouvinte: (event: unknown) => void) {
      this.ouvintes.set(nome, [...(this.ouvintes.get(nome) ?? []), ouvinte]);
    }

    send(corpo: unknown) {
      sent.push({ method: this.metodo, url: this.endereco, headers: this.cabecalhos, body: corpo });
      instancias.push(this);
    }

    emitirProgresso(loaded: number, total: number) {
      for (const ouvinte of this.doEnvio.get("progress") ?? []) {
        ouvinte({ lengthComputable: true, loaded, total });
      }
    }

    emitir(nome: string) {
      for (const ouvinte of this.ouvintes.get(nome) ?? []) {
        ouvinte({});
      }
    }
  }

  vi.stubGlobal("XMLHttpRequest", FakeXhr);

  return {
    sent,
    progress: (index, loaded, total) => instancias[index]?.emitirProgresso(loaded, total),
    respond: (index, status, body = "{}") => {
      const instancia = instancias[index];
      if (instancia === undefined) {
        return;
      }
      instancia.status = status;
      instancia.responseText = body;
      instancia.emitir("load");
    },
    fail: (index) => instancias[index]?.emitir("error"),
  };
}
