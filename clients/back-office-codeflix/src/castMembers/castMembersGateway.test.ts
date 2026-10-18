import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  castMembersPath,
  createCastMember,
  deleteCastMember,
  getCastMember,
  listCastMembers,
  setCastMemberActive,
  updateCastMember,
} from "@/castMembers/castMembersGateway";
import { ADMIN_API } from "@/shared/config";
import { SEARCH_DEFAULTS } from "@/shared/search";
import { jsonResponse } from "@/shared/testing";

describe("castMembersPath", () => {
  // `cast-members` com hífen, como a rota da admin-api. Errar aqui dá 404 em tudo.
  it("givenConsultaPadrao_whenMontada_thenApontaParaOsCastMembers", () => {
    expect(castMembersPath(SEARCH_DEFAULTS)).toBe(
      "/cast-members?page=0&perPage=10&sort=name&dir=asc",
    );
  });
});

describe("as chamadas", () => {
  beforeEach(() => {
    vi.spyOn(globalThis, "fetch").mockImplementation(() =>
      Promise.resolve(jsonResponse({ id: "cm-1" })),
    );
  });

  function chamada() {
    const [url, init] = vi.mocked(globalThis.fetch).mock.calls[0]!;
    return { url, init: init as RequestInit };
  }

  it("givenUmaConsulta_whenLista_thenVaiComOToken", async () => {
    await listCastMembers(SEARCH_DEFAULTS, "abc.123");

    expect(chamada().init.headers).toMatchObject({ Authorization: "Bearer abc.123" });
  });

  it("givenUmMembroNovo_whenCriado_thenVaiDePostNaColecao", async () => {
    await createCastMember({ name: "Vin Diesel", type: "ACTOR", active: true }, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/cast-members`);
    expect(chamada().init.method).toBe("POST");
    expect(chamada().init.body).toBe(
      JSON.stringify({ name: "Vin Diesel", type: "ACTOR", active: true }),
    );
  });

  // `active` **não** entra no corpo da atualização: no `UpdateCastMemberRequest` o campo não existe.
  it("givenUmaEdicao_whenSalva_thenVaiDePutSemOCampoActive", async () => {
    await updateCastMember("cm-1", { name: "Vin Diesel", type: "DIRECTOR" }, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/cast-members/cm-1`);
    expect(chamada().init.method).toBe("PUT");
    expect(chamada().init.body).toBe(JSON.stringify({ name: "Vin Diesel", type: "DIRECTOR" }));
  });

  it("givenAtivar_whenChamado_thenVaiNaRotaActivate", async () => {
    await setCastMemberActive("cm-1", true, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/cast-members/cm-1/activate`);
    expect(chamada().init.method).toBe("PUT");
  });

  it("givenDesativar_whenChamado_thenVaiNaRotaDeactivate", async () => {
    await setCastMemberActive("cm-1", false, "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/cast-members/cm-1/deactivate`);
  });

  it("givenUmaRemocao_whenChamada_thenVaiDeDeleteNoRecurso", async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(jsonResponse(undefined, { status: 204 })),
    );

    await deleteCastMember("cm-1", "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/cast-members/cm-1`);
    expect(chamada().init.method).toBe("DELETE");
  });

  it("givenIdComCaractereEspecial_whenUsado_thenVaiEscapado", async () => {
    await getCastMember("cm/1 2", "abc.123");

    expect(chamada().url).toBe(`${ADMIN_API}/cast-members/cm%2F1%202`);
  });
});
