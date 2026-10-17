import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { listCategories, type CategoryQuery } from "@/categories/categoriesGateway";

// A chave carrega a consulta inteira: mudar página, ordenação ou busca é outra pergunta, e o cache
// do TanStack trata como tal. Trocar de página e voltar não refaz a chamada.
//
// `keepPreviousData`: sem ele a tabela esvazia a cada página e o DataGrid perde a altura, o que faz
// a tela pular. Com ele as linhas antigas ficam enquanto as novas vêm.
export function useCategories(query: CategoryQuery) {
  const auth = useAuth();
  const token = auth.user?.access_token;

  return useQuery({
    queryKey: ["categories", query],
    queryFn: () => listCategories(query, token!),
    // Sem token não há o que perguntar — e `enabled` evita a chamada que voltaria 401.
    enabled: token !== undefined,
    placeholderData: keepPreviousData,
  });
}
