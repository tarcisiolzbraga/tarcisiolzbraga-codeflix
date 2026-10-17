import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import {
  createCategory,
  deleteCategory,
  getCategory,
  setCategoryActive,
  updateCategory,
  type CategoryDraft,
} from "@/categories/categoriesGateway";

// O token sai de um lugar só, porque esquecê-lo numa mutação dá 401 numa ação que a pessoa achou que
// tinha feito.
function useToken(): string | undefined {
  return useAuth().user?.access_token;
}

export function useCategory(id: string | undefined) {
  const token = useToken();

  return useQuery({
    queryKey: ["category", id],
    queryFn: () => getCategory(id!, token!),
    enabled: id !== undefined && token !== undefined,
  });
}

// Toda escrita invalida a **listagem**, e não só a linha mexida: criar muda a contagem e a página,
// e desativar muda a ordem se a ordenação for por situação. Invalidar a linha só deixaria a tabela
// mostrando o estado antigo.
function useInvalidateCategories() {
  const client = useQueryClient();
  return (id?: string) => {
    void client.invalidateQueries({ queryKey: ["categories"] });
    if (id !== undefined) {
      void client.invalidateQueries({ queryKey: ["category", id] });
    }
  };
}

export function useCreateCategory() {
  const token = useToken();
  const invalidar = useInvalidateCategories();

  return useMutation({
    mutationFn: (draft: CategoryDraft) => createCategory(draft, token!),
    onSuccess: () => invalidar(),
  });
}

export function useUpdateCategory(id: string) {
  const token = useToken();
  const invalidar = useInvalidateCategories();

  return useMutation({
    mutationFn: (draft: Pick<CategoryDraft, "name" | "description">) =>
      updateCategory(id, draft, token!),
    onSuccess: () => invalidar(id),
  });
}

export function useSetCategoryActive() {
  const token = useToken();
  const invalidar = useInvalidateCategories();

  return useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      setCategoryActive(id, active, token!),
    onSuccess: (_, { id }) => invalidar(id),
  });
}

export function useDeleteCategory() {
  const token = useToken();
  const invalidar = useInvalidateCategories();

  return useMutation({
    mutationFn: (id: string) => deleteCategory(id, token!),
    onSuccess: () => invalidar(),
  });
}
