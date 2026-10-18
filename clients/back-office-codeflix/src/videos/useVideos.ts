import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { listVideos, type VideoFilters } from "@/videos/videosGateway";
import type { SearchQuery } from "@/shared/search";

export function useVideos(query: SearchQuery, filters: VideoFilters) {
  const token = useAuth().user?.access_token;

  return useQuery({
    queryKey: ["videos", query, filters],
    queryFn: () => listVideos(query, filters, token!),
    enabled: token !== undefined,
    placeholderData: keepPreviousData,
  });
}
