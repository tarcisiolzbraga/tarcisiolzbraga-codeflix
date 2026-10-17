import { createContext, useContext } from "react";

export type Feedback = { kind: "success" | "error"; message: string };

// O contexto fica em arquivo próprio, separado do provedor, porque um módulo que exporta componente
// e função perde o fast refresh do Vite.
export const FeedbackContext = createContext<((aviso: Feedback) => void) | null>(null);

export function useFeedback(): (aviso: Feedback) => void {
  const avisar = useContext(FeedbackContext);
  if (avisar === null) {
    throw new Error("useFeedback precisa do <FeedbackProvider> acima dele");
  }
  return avisar;
}
