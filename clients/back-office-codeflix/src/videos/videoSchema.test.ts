import { describe, expect, it } from "vitest";
import { videoSchema } from "@/videos/videoSchema";

function erros(values: unknown): string[] {
  const resultado = videoSchema.safeParse(values);
  return resultado.success ? [] : resultado.error.issues.map((questao) => questao.message);
}

const VALIDO = {
  title: "Duna",
  description: "Areia, e muita",
  launchedAt: 2021,
  duration: 155,
  rating: "14",
  categories: [],
  genres: [],
  castMembers: [],
};

describe("videoSchema", () => {
  it("givenVideoValido_whenConferido_thenPassa", () => {
    expect(erros(VALIDO)).toEqual([]);
  });

  // O mínimo do título é 1, como o nome do gênero — e não 3, como categoria e elenco. Cada esquema
  // foi lido do validador do seu agregado, não copiado por analogia.
  it("givenTituloComDuasLetras_whenConferido_thenPassa", () => {
    expect(erros({ ...VALIDO, title: "Up" })).toEqual([]);
  });

  it("givenTituloEmBranco_whenConferido_thenRecusa", () => {
    expect(erros({ ...VALIDO, title: "   " })).toEqual(["O título é obrigatório."]);
  });

  // **A sinopse é obrigatória no vídeo**, diferente da descrição da categoria, que é opcional.
  it("givenSinopseEmBranco_whenConferida_thenRecusa", () => {
    expect(erros({ ...VALIDO, description: "" })).toEqual(["A sinopse é obrigatória."]);
  });

  it("givenSinopseCom4001Caracteres_whenConferida_thenRecusa", () => {
    expect(erros({ ...VALIDO, description: "a".repeat(4001) })).toEqual([
      "A sinopse passa de 4000 caracteres.",
    ]);
  });

  // Campo de número vazio chega como NaN pelo `valueAsNumber`, e é daí que sai "é obrigatório".
  it("givenAnoVazio_whenConferido_thenRecusa", () => {
    expect(erros({ ...VALIDO, launchedAt: Number.NaN })).toEqual([
      "O ano de lançamento é obrigatório.",
    ]);
  });

  it("givenAnoComDecimal_whenConferido_thenRecusa", () => {
    expect(erros({ ...VALIDO, launchedAt: 2021.5 })).toEqual(["O ano é um número inteiro."]);
  });

  // Catálogo tem filme de 1902, e estreia marcada para o ano que vem é caso real: não há teto.
  it("givenAnoAntigoEAnoFuturo_whenConferidos_thenPassam", () => {
    expect(erros({ ...VALIDO, launchedAt: 1902 })).toEqual([]);
    expect(erros({ ...VALIDO, launchedAt: new Date().getFullYear() + 2 })).toEqual([]);
  });

  it("givenDuracaoNegativa_whenConferida_thenRecusa", () => {
    expect(erros({ ...VALIDO, duration: -1 })).toEqual(["A duração não pode ser negativa."]);
  });

  // Duração zero é válida no domínio: `duration < 0` é a única regra de lá.
  it("givenDuracaoZero_whenConferida_thenPassa", () => {
    expect(erros({ ...VALIDO, duration: 0 })).toEqual([]);
  });

  it("givenDuracaoComDecimal_whenConferida_thenPassa", () => {
    expect(erros({ ...VALIDO, duration: 155.5 })).toEqual([]);
  });

  // A classificação é o **rótulo** do Rating, e não o nome da constante Java.
  it("givenClassificacaoPeloNomeDaConstante_whenConferida_thenRecusa", () => {
    expect(erros({ ...VALIDO, rating: "AGE_14" })).toEqual(["Escolha a classificação indicativa."]);
  });

  it.each(["ER", "L", "10", "12", "14", "16", "18"])(
    "givenAClassificacao_%s_whenConferida_thenPassa",
    (rating) => {
      expect(erros({ ...VALIDO, rating })).toEqual([]);
    },
  );

  // Os três vínculos são opcionais: o `VideoReferenceIds` troca ausente por conjunto vazio.
  it("givenSemVinculoNenhum_whenConferido_thenPassa", () => {
    expect(erros({ ...VALIDO, categories: [], genres: [], castMembers: [] })).toEqual([]);
  });
});
