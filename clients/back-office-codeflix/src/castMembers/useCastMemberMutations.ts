import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import {
  createCastMember,
  deleteCastMember,
  getCastMember,
  setCastMemberActive,
  updateCastMember,
  type CastMemberDraft,
} from "@/castMembers/castMembersGateway";

function useToken(): string | undefined {
  return useAuth().user?.access_token;
}

export function useCastMember(id: string | undefined) {
  const token = useToken();

  return useQuery({
    queryKey: ["castMember", id],
    queryFn: () => getCastMember(id!, token!),
    enabled: id !== undefined && token !== undefined,
  });
}

// Invalida a **listagem**, e não só a linha mexida: criar muda a contagem e a página, e trocar o
// papel muda a ordem se a ordenação for por papel.
function useInvalidate() {
  const client = useQueryClient();
  return (id?: string) => {
    void client.invalidateQueries({ queryKey: ["castMembers"] });
    if (id !== undefined) {
      void client.invalidateQueries({ queryKey: ["castMember", id] });
    }
  };
}

export function useCreateCastMember() {
  const token = useToken();
  const invalidar = useInvalidate();

  return useMutation({
    mutationFn: (draft: CastMemberDraft) => createCastMember(draft, token!),
    onSuccess: () => invalidar(),
  });
}

export function useUpdateCastMember(id: string) {
  const token = useToken();
  const invalidar = useInvalidate();

  return useMutation({
    mutationFn: (draft: Pick<CastMemberDraft, "name" | "type">) =>
      updateCastMember(id, draft, token!),
    onSuccess: () => invalidar(id),
  });
}

export function useSetCastMemberActive() {
  const token = useToken();
  const invalidar = useInvalidate();

  return useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      setCastMemberActive(id, active, token!),
    onSuccess: (_, { id }) => invalidar(id),
  });
}

export function useDeleteCastMember() {
  const token = useToken();
  const invalidar = useInvalidate();

  return useMutation({
    mutationFn: (id: string) => deleteCastMember(id, token!),
    onSuccess: () => invalidar(),
  });
}
