import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import {
  createGenre,
  deleteGenre,
  getGenre,
  setGenreActive,
  updateGenre,
  type GenreDraft,
} from "@/genres/genresGateway";

function useToken(): string | undefined {
  return useAuth().user?.access_token;
}

export function useGenre(id: string | undefined) {
  const token = useToken();

  return useQuery({
    queryKey: ["genre", id],
    queryFn: () => getGenre(id!, token!),
    enabled: id !== undefined && token !== undefined,
  });
}

function useInvalidate() {
  const client = useQueryClient();
  return (id?: string) => {
    void client.invalidateQueries({ queryKey: ["genres"] });
    if (id !== undefined) {
      void client.invalidateQueries({ queryKey: ["genre", id] });
    }
  };
}

export function useCreateGenre() {
  const token = useToken();
  const invalidar = useInvalidate();

  return useMutation({
    mutationFn: (draft: GenreDraft) => createGenre(draft, token!),
    onSuccess: () => invalidar(),
  });
}

export function useUpdateGenre(id: string) {
  const token = useToken();
  const invalidar = useInvalidate();

  return useMutation({
    mutationFn: (draft: Pick<GenreDraft, "name" | "categories">) =>
      updateGenre(id, draft, token!),
    onSuccess: () => invalidar(id),
  });
}

export function useSetGenreActive() {
  const token = useToken();
  const invalidar = useInvalidate();

  return useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      setGenreActive(id, active, token!),
    onSuccess: (_, { id }) => invalidar(id),
  });
}

export function useDeleteGenre() {
  const token = useToken();
  const invalidar = useInvalidate();

  return useMutation({
    mutationFn: (id: string) => deleteGenre(id, token!),
    onSuccess: () => invalidar(),
  });
}
