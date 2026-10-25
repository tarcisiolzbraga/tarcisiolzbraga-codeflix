// A leitura do catálogo sob carga — o caminho de maior volume num produto de streaming: uma página
// de vídeos com as relações resolvidas, que é o que a tela de quem navega pede.
//
// **Por que esta consulta e não outra.** O tipo `Video` do schema resolve `categories`, `genres` e
// `castMembers` por item. Pedir só `id` e `title` mediria o Elasticsearch e a serialização; pedir as
// relações mede o que de fato acontece quando alguém abre o acervo — e é ali que um N+1 aparece.
//
// O token é pedido **uma vez** no `setup()`, cujo tempo o k6 não conta como requisição. Medir o
// Keycloak junto diluiria o que se quer medir; e um token por VU faria o teste bater mais no
// provedor de identidade que no catálogo.
//
// O client é o `catalog-test-codeflix`, que tem `CODEFLIX_SUBSCRIBER` — o mesmo papel de um
// assinante de verdade, e não o de administrador.
import http from "k6/http";
import { check } from "k6";

const CATALOGO = __ENV.CATALOGO_URL;
const KEYCLOAK = __ENV.KEYCLOAK_URL;
const REALM = __ENV.KEYCLOAK_REALM;
const SEGREDO = __ENV.KEYCLOAK_CLIENT_SECRET;
const POR_PAGINA = Number(__ENV.POR_PAGINA || 10);
const PAGINAS = Number(__ENV.PAGINAS || 10);

// **Um nível por execução, e não uma rampa.** A primeira versão subia em degraus dentro de uma
// execução só, e o resumo do k6 agrega tudo: dava um p99 do conjunto e não respondia a pergunta que
// motivou o teste — *em que degrau* a latência deixa de ser plana. O `degraus.sh` chama este script
// uma vez por nível e monta a tabela; a curva vem da comparação entre execuções.
//
// Rampa curta antes de medir: VU entrando de uma vez faz o primeiro segundo parecer pior do que é,
// e com JIT ainda frio o número do começo não representa o regime.
export const options = {
  stages: [
    { duration: __ENV.AQUECIMENTO || "10s", target: Number(__ENV.VUS || 10) },
    { duration: __ENV.DURACAO || "40s", target: Number(__ENV.VUS || 10) },
  ],
  // **Os limites são de correção, não de latência**, e isso é escolha explicada.
  //
  // Exigir um p99 só faria sentido num nível fixo e numa máquina dedicada: aqui o gerador de carga
  // divide CPU com o cluster que ele mede, então um teto de latência reprovaria por causa do
  // vizinho e não do código. O que **não** depende da máquina é o serviço responder certo sob
  // carga: zero falha de transporte e zero erro de GraphQL, em todos os níveis medidos.
  //
  // A latência fica na tabela do `degraus.sh`, para ser lida e comparada, em vez de virar um portão
  // que reprova por ruído.
  thresholds: {
    http_req_failed: ["rate==0"],
    checks: ["rate==1"],
  },
  summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max"],
};

// Duas formas da **mesma** consulta, e a diferença entre elas é o ponto: `acervo` pede as relações
// resolvidas, como a tela de quem navega; `minimo` pede só os campos do próprio vídeo. Rodar as duas
// no mesmo nível de carga isola quanto custa resolver `categories`, `genres` e `castMembers` — que é
// a resposta acionável, porque é o único pedaço que se pode otimizar sem mexer em infraestrutura.
const FORMAS = {
  acervo: `query Acervo($pagina: Int!, $porPagina: Int!) {
  videos(page: $pagina, perPage: $porPagina, sort: "title", direction: "asc") {
    meta { currentPage perPage total }
    items {
      id title yearLaunched duration rating opened
      categories { id name }
      genres { id name }
      castMembers { id name }
    }
  }
}`,
  minimo: `query Minimo($pagina: Int!, $porPagina: Int!) {
  videos(page: $pagina, perPage: $porPagina, sort: "title", direction: "asc") {
    meta { currentPage perPage total }
    items { id title yearLaunched duration rating opened }
  }
}`,
};

const FORMA = __ENV.FORMA || "acervo";
const CONSULTA = FORMAS[FORMA];
if (!CONSULTA) {
  throw new Error(`FORMA desconhecida: ${FORMA} (use acervo ou minimo)`);
}

export function setup() {
  const resposta = http.post(
    `${KEYCLOAK}/realms/${REALM}/protocol/openid-connect/token`,
    {
      grant_type: "client_credentials",
      client_id: "catalog-test-codeflix",
      client_secret: SEGREDO,
    },
  );
  if (resposta.status !== 200) {
    throw new Error(`não consegui token: HTTP ${resposta.status} ${resposta.body}`);
  }
  return { token: resposta.json("access_token") };
}

export default function (dados) {
  // Página sorteada a cada iteração: com página fixa, o cache do Elasticsearch responde sempre a
  // mesma coisa e o número sai bonito por motivo errado.
  const pagina = Math.floor(Math.random() * PAGINAS);

  const resposta = http.post(
    `${CATALOGO}/api/graphql`,
    JSON.stringify({ query: CONSULTA, variables: { pagina, porPagina: POR_PAGINA } }),
    {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${dados.token}`,
      },
      tags: { name: FORMA },
    },
  );

  // **Conferir o corpo, e não só o status.** GraphQL responde 200 com `errors` dentro: sem esta
  // checagem, um teste de carga sobre um catálogo quebrado passaria com p99 ótimo.
  check(resposta, {
    "status 200": (r) => r.status === 200,
    "sem erros de GraphQL": (r) => {
      try {
        return !r.json("errors");
      } catch {
        return false;
      }
    },
    "veio item": (r) => {
      try {
        return (r.json("data.videos.items") || []).length > 0;
      } catch {
        return false;
      }
    },
  });
}
