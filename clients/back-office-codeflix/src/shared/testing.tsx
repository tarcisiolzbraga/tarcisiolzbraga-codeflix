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
