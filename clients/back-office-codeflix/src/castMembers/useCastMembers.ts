import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { listCastMembers } from "@/castMembers/castMembersGateway";
import type { SearchQuery } from "@/shared/search";

export function useCastMembers(query: SearchQuery) {
  const token = useAuth().user?.access_token;

  return useQuery({
    queryKey: ["castMembers", query],
    queryFn: () => listCastMembers(query, token!),
    enabled: token !== undefined,
    placeholderData: keepPreviousData,
  });
}
