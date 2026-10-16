// Confere configuração que o portal não consegue adivinhar.
//
// É o equivalente do ConfiguredValue dos serviços Java, e existe pelo mesmo motivo: variável que
// chega vazia ou com o texto do placeholder não resolvido faria o portal subir apontando para lugar
// nenhum, falhando só na primeira tela que buscasse dado. Aqui ela falha onde dá para ler o motivo.

const UNRESOLVED_MARKER = "${";

export function apiUrl(name: string, value: string | undefined): string {
  if (value === undefined || value.trim() === "") {
    throw new Error(`'${name}' é obrigatória e não foi definida`);
  }
  if (value.includes(UNRESOLVED_MARKER)) {
    throw new Error(`'${name}' não foi resolvida; veio '${value}'. Defina a variável de ambiente.`);
  }
  if (!URL.canParse(value)) {
    throw new Error(`'${name}' precisa ser uma URL absoluta; veio '${value}'`);
  }
  return value.replace(/\/+$/, "");
}

// Referenciada com o nome literal de propósito: é assim que o Next embute o valor no pacote. Com
// process.env[variavel] o valor viraria undefined no browser.
export const SUBSCRIPTION_API = apiUrl(
  "NEXT_PUBLIC_SUBSCRIPTION_API_URL",
  process.env.NEXT_PUBLIC_SUBSCRIPTION_API_URL,
);
