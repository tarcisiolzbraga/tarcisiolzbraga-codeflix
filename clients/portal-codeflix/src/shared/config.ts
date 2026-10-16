// Confere configuração que o portal não consegue adivinhar.
//
// É o equivalente do ConfiguredValue dos serviços Java, e existe pelo mesmo motivo: variável que
// chega vazia ou com o texto do placeholder não resolvido faria o portal subir apontando para lugar
// nenhum, falhando só na primeira tela que buscasse dado. Aqui ela falha onde dá para ler o motivo.

const UNRESOLVED_MARKER = "${";

export function required(name: string, value: string | undefined): string {
  if (value === undefined || value.trim() === "") {
    throw new Error(`'${name}' é obrigatória e não foi definida`);
  }
  if (value.includes(UNRESOLVED_MARKER)) {
    throw new Error(`'${name}' não foi resolvida; veio '${value}'. Defina a variável de ambiente.`);
  }
  return value;
}

export function apiUrl(name: string, value: string | undefined): string {
  const checked = required(name, value);
  if (!URL.canParse(checked)) {
    throw new Error(`'${name}' precisa ser uma URL absoluta; veio '${checked}'`);
  }
  // Sem a barra final: a concatenação com a rota geraria "//", que alguns servidores tratam como
  // caminho diferente.
  return checked.replace(/\/+$/, "");
}

// Referenciada com o nome literal de propósito: é assim que o Next embute o valor no pacote. Com
// process.env[variavel] o valor viraria undefined no browser.
export const SUBSCRIPTION_API = apiUrl(
  "NEXT_PUBLIC_SUBSCRIPTION_API_URL",
  process.env.NEXT_PUBLIC_SUBSCRIPTION_API_URL,
);

export const KEYCLOAK_ISSUER = apiUrl(
  "NEXT_PUBLIC_KEYCLOAK_ISSUER",
  process.env.NEXT_PUBLIC_KEYCLOAK_ISSUER,
);

// Não é URL, então não passa pelo apiUrl: é só um nome que precisa existir.
export const KEYCLOAK_CLIENT_ID = required(
  "NEXT_PUBLIC_KEYCLOAK_CLIENT_ID",
  process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID,
);
