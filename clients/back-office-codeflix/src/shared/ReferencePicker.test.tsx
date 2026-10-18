import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import { jsonResponse, renderWithProviders } from "@/shared/testing";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { ReferencePicker } = await import("@/shared/ReferencePicker");

const DOCUMENTARIO = { id: "cat-1", name: "Documentário", description: null, active: true, createdAt: "2026-02-03T10:15:00Z" };
const TERROR = { id: "cat-2", name: "Terror", description: null, active: true, createdAt: "2026-02-03T10:15:00Z" };

// A mesma rota serve a busca e a resolução de id, e o que distingue é o caminho: `/categories?` é
// listagem, `/categories/cat-1` é um recurso.
function respondeCom(acervo: { id: string; name: string }[]) {
  vi.spyOn(globalThis, "fetch").mockImplementation((url) => {
    const texto = String(url);
    const porId = /\/(?:categories|genres|cast-members)\/([^?]+)$/.exec(texto);
    if (porId !== null) {
      const achada = acervo.find((item) => item.id === decodeURIComponent(porId[1]!));
      return Promise.resolve(
        achada === undefined
          ? jsonResponse({ message: "Category was not found" }, { status: 404 })
          : jsonResponse(achada),
      );
    }
    const busca = new URL(texto).searchParams.get("search") ?? "";
    const itens = acervo.filter((item) =>
      item.name.toLowerCase().includes(busca.toLowerCase()),
    );
    return Promise.resolve({ currentPage: 0, perPage: 20, total: itens.length, items: itens }).then(
      jsonResponse,
    );
  });
}

describe("ReferencePicker", () => {
  beforeEach(() => {
    sessao.atual = { user: { access_token: "abc.123" } };
    respondeCom([DOCUMENTARIO, TERROR]);
  });

  // A busca é no servidor, como a da tabela: o catálogo não cabe num `<select>`.
  it("givenUmTrechoDigitado_whenBusca_thenPedeAoServidorComOSearch", async () => {
    renderWithProviders(<ReferencePicker resource="categories" value={[]} onChange={() => {}} />);

    await userEvent.type(screen.getByLabelText("Categorias"), "terr");

    await waitFor(() => {
      const chamadas = vi.mocked(globalThis.fetch).mock.calls.map(([url]) => String(url));
      expect(chamadas.some((url) => url.includes("search=terr"))).toBe(true);
    });
  });

  it("givenAsOpcoesDoServidor_whenAbre_thenMostraOsNomes", async () => {
    renderWithProviders(<ReferencePicker resource="categories" value={[]} onChange={() => {}} />);

    await userEvent.click(screen.getByLabelText("Categorias"));

    expect(await screen.findByRole("option", { name: "Documentário" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Terror" })).toBeInTheDocument();
  });

  it("givenUmaOpcaoEscolhida_whenClicada_thenAvisaComOId", async () => {
    const onChange = vi.fn();
    renderWithProviders(<ReferencePicker resource="categories" value={[]} onChange={onChange} />);

    await userEvent.click(screen.getByLabelText("Categorias"));
    await userEvent.click(await screen.findByRole("option", { name: "Terror" }));

    expect(onChange).toHaveBeenCalledWith(["cat-2"]);
  });

  // Ids já escolhidos chegam da API do gênero; o nome vem de uma consulta por id. Sem isso a
  // etiqueta mostraria o id.
  it("givenIdsJaEscolhidos_whenAbre_thenMostraOsNomesENaoOsIds", async () => {
    renderWithProviders(<ReferencePicker resource="categories" value={["cat-1"]} onChange={() => {}} />);

    expect(await screen.findByText("Documentário")).toBeInTheDocument();
    expect(screen.queryByText("cat-1")).not.toBeInTheDocument();
    expect(vi.mocked(globalThis.fetch).mock.calls.map(([url]) => String(url))).toContain(
      `${ADMIN_API}/categories/cat-1`,
    );
  });

  // Categoria removida fora daqui: o id continua aparecendo, porque esconder a etiqueta faria o
  // gênero parecer ter menos categorias do que tem.
  it("givenIdQueNaoExisteMais_whenAbre_thenMostraOIdEmVezDeEsconder", async () => {
    renderWithProviders(<ReferencePicker resource="categories" value={["cat-apagada"]} onChange={() => {}} />);

    expect(await screen.findByText("cat-apagada")).toBeInTheDocument();
  });

  // O mesmo campo serve os três recursos, e o que muda é a rota consultada. Errar isso buscaria
  // categoria onde a tela pede elenco.
  it("givenOutroRecurso_whenBusca_thenConsultaARotaDele", async () => {
    renderWithProviders(
      <ReferencePicker resource="cast-members" value={[]} onChange={() => {}} />,
    );

    await waitFor(() => {
      const chamadas = vi.mocked(globalThis.fetch).mock.calls.map(([url]) => String(url));
      expect(chamadas.some((url) => url.includes("/cast-members?"))).toBe(true);
    });
  });

  it("givenORecurso_whenSemRotuloProprio_thenUsaONomeDoRecurso", async () => {
    renderWithProviders(<ReferencePicker resource="genres" value={[]} onChange={() => {}} />);

    expect(screen.getByLabelText("Gêneros")).toBeInTheDocument();
  });

  it("givenUmaEscolhidaEOutraNaBusca_whenAbre_thenAEscolhidaNaoApareceDuasVezes", async () => {
    renderWithProviders(<ReferencePicker resource="categories" value={["cat-1"]} onChange={() => {}} />);

    await screen.findByText("Documentário");
    await userEvent.click(screen.getByLabelText("Categorias"));

    await waitFor(() =>
      expect(screen.getAllByRole("option", { name: "Documentário" })).toHaveLength(1),
    );
  });
});
