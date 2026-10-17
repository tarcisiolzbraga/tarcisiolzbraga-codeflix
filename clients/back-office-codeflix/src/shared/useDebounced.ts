import { useEffect, useState } from "react";

// Atrasa o valor da busca para que digitar "documentário" não dispare treze requisições à admin-api.
//
// O atraso é por tempo, e não `useDeferredValue`: o do React adia o **render**, que não é o custo
// aqui — o custo é a chamada de rede, e ela só pode ser evitada esperando a pessoa parar de digitar.
export function useDebounced<T>(value: T, delayMs = 400): T {
  const [atrasado, setAtrasado] = useState(value);

  useEffect(() => {
    const id = setTimeout(() => setAtrasado(value), delayMs);
    return () => clearTimeout(id);
  }, [value, delayMs]);

  return atrasado;
}
