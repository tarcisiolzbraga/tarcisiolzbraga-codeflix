import { beforeEach, describe, expect, it, vi } from "vitest";
import { fetchGenres, fetchRails, type Genre } from "@/catalog/catalog";

function respondWith(body: unknown, status = 200) {
  return vi.spyOn(globalThis, "fetch").mockImplementation(() =>
    Promise.resolve(
      new Response(JSON.stringify(body), {
        status,
        headers: { "Content-Type": "application/json" },
      }),
    ),
  );
}

function sent() {
  const [, init] = vi.mocked(globalThis.fetch).mock.calls[0]!;
  return JSON.parse(String((init as RequestInit).body)) as {
    query: string;
    variables: Record<string, unknown>;
  };
}

const video = {
  id: "v1",
  title: "Duna",
  description: "Areia",
  yearLaunched: 2021,
  duration: 155,
  rating: "14",
  video: null,
  trailer: null,
  banner: null,
  thumbnail: null,
  thumbnailHalf: null,
  genres: [],
  categories: [],
  castMembers: [],
};

const ficcao: Genre = { id: "g1", name: "Ficção" };
const terror: Genre = { id: "g2", name: "Terror" };

describe("fetchGenres", () => {
  beforeEach(() => {
    respondWith({ data: { genres: { items: [ficcao, terror] } } });
  });

  // Poucos de propósito: cada trilha é uma linha na tela, e vinte linhas não são navegação, são
  // rolagem.
  it("givenTheDefault_whenFetches_thenAsksForSixGenres", async () => {
    await fetchGenres("token");

    expect(sent().variables.perPage).toBe(6);
  });

  it("givenTheGenresOfTheCatalog_whenFetches_thenReturnsThem", async () => {
    await expect(fetchGenres("token")).resolves.toEqual([ficcao, terror]);
  });
});

describe("fetchRails", () => {
  beforeEach(() => {
    respondWith({ data: { t0: { items: [video] }, t1: { items: [video] } } });
  });

  // **Duas trilhas, uma requisição.** Em REST seriam duas chamadas; em GraphQL são dois campos
  // apelidados no mesmo documento — e é um dos poucos lugares onde a escolha pelo GraphQL se paga.
  it("givenTwoGenres_whenFetches_thenOneRequestCarriesBothFields", async () => {
    await fetchRails([ficcao, terror], "token");

    expect(globalThis.fetch).toHaveBeenCalledOnce();
    expect(sent().query).toContain("t0: videos");
    expect(sent().query).toContain("t1: videos");
  });

  // Os ids vão como **variáveis**, nunca interpolados no texto da consulta: id vem de fora, e
  // consulta montada por concatenação de valor é como se escreve injeção.
  it("givenTheGenreIds_whenFetches_thenTheyTravelAsVariables", async () => {
    await fetchRails([ficcao, terror], "token");

    expect(sent().variables).toMatchObject({ g0: "g1", g1: "g2" });
    expect(sent().query).not.toContain("g1\"");
    expect(sent().query).toContain("$g0: ID!");
  });

  it("givenTheAnswer_whenItArrives_thenEachRailKeepsItsGenre", async () => {
    const rails = await fetchRails([ficcao, terror], "token");

    expect(rails.map((rail) => rail.genre.name)).toEqual(["Ficção", "Terror"]);
    expect(rails[0]!.items).toHaveLength(1);
  });

  // Gênero sem título publicado existe no acervo, e uma linha com cabeçalho e nada embaixo parece
  // defeito. O descarte é aqui, e não na tela.
  it("givenAGenreWithNoTitles_whenFetches_thenItsRailIsDropped", async () => {
    respondWith({ data: { t0: { items: [video] }, t1: { items: [] } } });

    const rails = await fetchRails([ficcao, terror], "token");

    expect(rails.map((rail) => rail.genre.name)).toEqual(["Ficção"]);
  });

  // Acervo sem gênero nenhum: nada a pedir, e pedir um documento vazio seria erro de GraphQL.
  it("givenNoGenres_whenFetches_thenAsksNothing", async () => {
    await expect(fetchRails([], "token")).resolves.toEqual([]);

    expect(globalThis.fetch).not.toHaveBeenCalled();
  });

  it("givenAnAliasMissingInTheAnswer_whenItArrives_thenTheRailIsDroppedInsteadOfBreaking", async () => {
    respondWith({ data: { t0: { items: [video] } } });

    const rails = await fetchRails([ficcao, terror], "token");

    expect(rails.map((rail) => rail.genre.name)).toEqual(["Ficção"]);
  });
});
