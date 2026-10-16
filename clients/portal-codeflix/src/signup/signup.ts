import { SUBSCRIPTION_API } from "@/shared/config";

// O documento é conferido pelos **dígitos verificadores** na API, não só pelo comprimento. O portal
// não repete essa conta: seria uma segunda fonte de verdade para a mesma regra, e o dia em que ela
// mudasse no domínio o front passaria a recusar o que a API aceita. Aqui só o tipo é oferecido.
export type DocumentType = "cpf" | "cnpj";

export type SignUpForm = {
  firstname: string;
  lastname: string;
  email: string;
  password: string;
  documentNumber: string;
  documentType: DocumentType;
};

// A senha tem mínimo e máximo no domínio (8 e 72), e esses dois valem repetir: dão para conferir no
// formulário sem duplicar regra nenhuma, e evitam uma ida à API para dizer o óbvio.
export const PASSWORD_MIN = 8;
export const PASSWORD_MAX = 72;

// Erro que carrega a mensagem que a API escreveu. Em 422 ela é para quem está na tela — "o documento
// não é válido", "a senha é curta" — e repetir isso aqui seria inventar outra redação para a mesma
// regra.
export class SignUpRefused extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "SignUpRefused";
  }
}

async function messageOf(response: Response, fallback: string): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string };
    return body.message?.trim() || fallback;
  } catch {
    // Corpo vazio ou que não é JSON: acontece em 500 e em erro de gateway.
    return fallback;
  }
}

// Pede o cadastro. **A API responde 202 com a mesma mensagem em todos os casos** — e-mail novo, que
// já existe no provedor de identidade, ou que já tem conta. É decisão dela, para não contar a um
// estranho se um endereço está cadastrado, e o portal preserva isso: a tela de sucesso é a mesma.
export async function requestSignUp(form: SignUpForm): Promise<void> {
  const response = await fetch(`${SUBSCRIPTION_API}/accounts/sign-up`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify(form),
  });

  if (!response.ok) {
    throw new SignUpRefused(response.status, await messageOf(response, "Não conseguimos pedir o seu cadastro agora."));
  }
}

// Confirma com o token do e-mail. **Vai no corpo, não na query string**: a API o recebe assim de
// propósito, porque em query string ele acabaria no log do servidor — e é credencial. O portal o
// recebe na URL porque ele chega por link de e-mail, e tira de lá assim que lê.
//
// Idempotente por contrato: chamar de novo com o mesmo token devolve a mesma conta, porque clicar no
// link duas vezes é comum.
export async function confirmSignUp(token: string): Promise<string> {
  const response = await fetch(`${SUBSCRIPTION_API}/accounts/sign-up/confirm`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({ token }),
  });

  if (!response.ok) {
    throw new SignUpRefused(
      response.status,
      await messageOf(
        response,
        response.status === 404
          ? "Este link não existe ou não vale mais."
          : "Não conseguimos confirmar o seu cadastro.",
      ),
    );
  }

  const body = (await response.json()) as { accountId: string };
  return body.accountId;
}

// O que dá para conferir sem a API: campo vazio e tamanho de senha. Devolve a lista de problemas, e
// não só o primeiro, para a pessoa corrigir tudo de uma vez.
export function problemsIn(form: SignUpForm): string[] {
  const problems: string[] = [];
  if (!form.firstname.trim()) problems.push("Informe o seu nome.");
  if (!form.lastname.trim()) problems.push("Informe o seu sobrenome.");
  if (!form.email.trim()) problems.push("Informe o seu e-mail.");
  if (!form.documentNumber.trim()) problems.push("Informe o número do documento.");
  if (form.password.length < PASSWORD_MIN) {
    problems.push(`A senha precisa de pelo menos ${PASSWORD_MIN} caracteres.`);
  }
  if (form.password.length > PASSWORD_MAX) {
    problems.push(`A senha não pode passar de ${PASSWORD_MAX} caracteres.`);
  }
  return problems;
}
