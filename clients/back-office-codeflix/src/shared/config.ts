// Confere configuração que o back-office não consegue adivinhar.
//
// É o equivalente do ConfiguredValue dos serviços Java, e existe pelo mesmo motivo: variável que
// chega vazia faria a aplicação subir apontando para lugar nenhum, falhando só na primeira tela que
// buscasse dado. Aqui ela falha onde dá para ler o motivo.
//
// Os valores vêm do `define` do vite.config, que os deriva do `.env` da raiz do monorepo — o mesmo
// que o compose e os serviços usam.

function required(name: string, value: string | undefined): string {
  if (value === undefined || value.trim() === "") {
    throw new Error(`'${name}' é obrigatória e não foi definida`);
  }
  return value;
}

function apiUrl(name: string, value: string | undefined): string {
  const checked = required(name, value);
  if (!URL.canParse(checked)) {
    throw new Error(`'${name}' precisa ser uma URL absoluta; veio '${checked}'`);
  }
  // Sem a barra final: a concatenação com a rota geraria "//", que alguns servidores tratam como
  // caminho diferente.
  return checked.replace(/\/+$/, "");
}

export const ADMIN_API = apiUrl("VITE_ADMIN_API_URL", import.meta.env.VITE_ADMIN_API_URL);

export const KEYCLOAK_ISSUER = apiUrl("VITE_KEYCLOAK_ISSUER", import.meta.env.VITE_KEYCLOAK_ISSUER);

export const KEYCLOAK_CLIENT_ID = required(
  "VITE_KEYCLOAK_CLIENT_ID",
  import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
);

// O portal, para onde quem não administra é levado. O back-office não serve o catálogo, e manter
// alguém numa tela de "sem permissão" é deixá-lo parado num lugar que ele não pediu para abrir.
export const PORTAL_URL = apiUrl("VITE_PORTAL_URL", import.meta.env.VITE_PORTAL_URL);

// O papel que o back-office exige. A admin-api aceita papéis por agregado também
// (CODEFLIX_CATEGORIES e companhia), mas o back-office administra os quatro, então quem entra aqui
// precisa do papel que abre todos.
export const ADMIN_ROLE = "CODEFLIX_ADMIN";

// O teto de cada arquivo de mídia, em bytes. Vem do `MEDIA_MAX_FILE_SIZE` do `.env` da raiz — o
// mesmo valor que o `spring.servlet.multipart.max-file-size` da admin-api usa.
//
// Conferir antes de enviar não é zelo: acima do teto a API responde **413 sem corpo nenhum**
// (medido), então sem esta conferência a pessoa esperaria o upload inteiro para ler "falhou" sem
// motivo. A API continua sendo quem decide — isto só evita a viagem.
export const MEDIA_MAX_FILE_SIZE = Number(
  required("VITE_MEDIA_MAX_FILE_SIZE", import.meta.env.VITE_MEDIA_MAX_FILE_SIZE),
);
