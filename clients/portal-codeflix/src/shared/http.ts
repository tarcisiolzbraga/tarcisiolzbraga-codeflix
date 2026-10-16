// O pouco que toda conversa com a API repete. Extraído na terceira cópia, não antes.

// O cabeçalho de quem está logado. Existe para o `Bearer ` não ser escrito à mão em cada chamada —
// errar o espaço depois do Bearer dá 401 sem explicação.
export function bearer(token: string): Record<string, string> {
  return { Authorization: `Bearer ${token}` };
}

// A mensagem que a API escreveu, ou a de reserva. Em 422 a mensagem dela é para quem está na tela,
// e inventar outra redação aqui seria uma segunda versão da mesma regra.
//
// O corpo pode não ser JSON — 502 de gateway, 504 de tempo esgotado —, e aí ler sem try derrubaria
// o tratamento de erro dentro do tratamento de erro.
export async function messageOf(response: Response, fallback: string): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string };
    return body.message?.trim() || fallback;
  } catch {
    return fallback;
  }
}
