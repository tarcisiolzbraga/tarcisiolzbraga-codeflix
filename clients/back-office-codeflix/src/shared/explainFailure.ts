import { AdminApiError } from "@/shared/http";

// Uma mensagem por motivo, porque "falhou" não diz à pessoa o que fazer.
//
// 401 e 403 parecem o mesmo defeito na tela e são coisas opostas: no primeiro a sessão venceu e
// entrar de novo resolve; no segundo a conta não tem o papel e entrar de novo não muda nada. Erro
// sem status nenhum quase sempre é a admin-api fora do ar — e dizer isso poupa a pessoa de procurar
// defeito no catálogo.
//
// Fica no shared porque os quatro agregados vão explicar as mesmas falhas.
export function explainFailure(erro: unknown): string {
  if (erro instanceof AdminApiError) {
    if (erro.status === 401) {
      return "Sua sessão expirou. Entre de novo.";
    }
    if (erro.status === 403) {
      return "Sua conta não tem permissão para esta operação.";
    }
    // O 409 da admin-api é sempre o mesmo caso, e tem remédio: o agregado está referenciado por
    // outro e **não se apaga, se desativa** — é a regra de não haver cascata no banco. A mensagem
    // dela diz isso em inglês (`is linked to at least one Genre; deactivate it instead`), e aqui a
    // frase é traduzida porque o caso é conhecido e a ação é uma só. Mensagem de validação continua
    // vindo como a API escreveu: lá a regra é dela, aqui o remédio é nosso.
    if (erro.status === 409) {
      return "Este registro está em uso por outro do catálogo. Desative-o em vez de remover.";
    }
    return erro.message;
  }
  return "Não foi possível falar com a admin-api. Verifique se ela está no ar.";
}
