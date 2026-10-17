import { QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";
import { newQueryClient } from "@/shared/queryClient";

// O cliente nasce em estado, e não em constante de módulo: em teste cada render precisa de um cache
// limpo, senão o resultado de um teste aparece no seguinte.
export function QueryProvider({ children }: { children: React.ReactNode }) {
  const [client] = useState(newQueryClient);
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}
