import { ADMIN_API } from "@/shared/config";

// O pouco que toda chamada à admin-api repete. Igual ao do portal, e pelo mesmo motivo: errar o
// espaço depois de "Bearer" dá 401 sem explicação, e ler o corpo de erro sem `try` derruba o
// tratamento de erro dentro do próprio tratamento de erro.
export class AdminApiError extends Error {
  // Campo declarado e atribuído à mão, e não propriedade no parâmetro do construtor: o tsconfig do
  // Vite liga `erasableSyntaxOnly`, que proíbe a sintaxe que só o TypeScript tem — o código tem de
  // continuar válido depois de apagados os tipos.
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "AdminApiError";
    this.status = status;
  }
}

async function messageOf(response: Response, fallback: string): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string };
    return body.message?.trim() || fallback;
  } catch {
    return fallback;
  }
}

export async function adminFetch<T>(
  path: string,
  token: string,
  init: { method?: string; body?: unknown; fallback: string } = { fallback: "A requisição falhou." },
): Promise<T> {
  const response = await fetch(`${ADMIN_API}${path}`, {
    method: init.method ?? "GET",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${token}`,
      ...(init.body === undefined ? {} : { "Content-Type": "application/json" }),
    },
    body: init.body === undefined ? undefined : JSON.stringify(init.body),
  });

  if (!response.ok) {
    throw new AdminApiError(response.status, await messageOf(response, init.fallback));
  }

  // 204 não tem corpo, e tentar ler JSON dele estoura.
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}

// O envelope de listagem do monorepo, igual nas três APIs.
export type Page<T> = {
  currentPage: number;
  perPage: number;
  total: number;
  items: T[];
};
