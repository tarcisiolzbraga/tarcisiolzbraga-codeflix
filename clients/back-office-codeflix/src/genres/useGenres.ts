import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { listGenres } from "@/genres/genresGateway";
import type { SearchQuery } from "@/shared/search";

export function useGenres(query: SearchQuery) {
  const token = useAuth().user?.access_token;

  return useQuery({
    queryKey: ["genres", query],
    queryFn: () => listGenres(query, token!),
    enabled: token !== undefined,
    placeholderData: keepPreviousData,
  });
}
