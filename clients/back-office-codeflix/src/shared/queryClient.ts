import { QueryClient } from "@tanstack/react-query";
import { AdminApiError } from "@/shared/http";

// Em arquivo próprio, e não junto do provedor: um módulo que exporta componente e função perde o
// fast refresh do Vite — editar a função recarregaria a página inteira em vez do componente.
//
// A decisão que importa aqui é **não repetir 4xx**. O padrão do TanStack é três tentativas, e com
// isso um 403 vira três chamadas e segundos de espera antes de a pessoa ler "sem permissão" — quando
// a primeira resposta já disse tudo. Repetir só faz sentido para 5xx e para rede caída.
export function newQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: (tentativas, erro) => {
          if (erro instanceof AdminApiError && erro.status < 500) {
            return false;
          }
          return tentativas < 2;
        },
        staleTime: 30_000,
        refetchOnWindowFocus: false,
      },
    },
  });
}
