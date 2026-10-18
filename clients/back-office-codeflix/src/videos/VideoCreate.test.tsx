import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ADMIN_API } from "@/shared/config";
import { jsonResponse, renderWithProviders } from "@/shared/testing";

const sessao = vi.hoisted(() => ({ atual: {} as Record<string, unknown> }));
vi.mock("react-oidc-context", () => ({ useAuth: () => sessao.atual }));

const { VideoCreate } = await import("@/videos/VideoCreate");

function respondeCom(criacao: { body: unknown; status: number } = { body: { id: "vid-9" }, status: 201 }) {
  vi.spyOn(globalThis, "fetch").mockImplementation((_url, init) => {
    if (((init as RequestInit | undefined)?.method ?? "GET") === "POST") {
      return Promise.resolve(jsonResponse(criacao.body, { status: criacao.status }));
    }
    return Promise.resolve(jsonResponse({ currentPage: 0, perPage: 20, total: 0, items: [] }));
  });
}

function corpoDoPost(): Record<string, unknown> | undefined {
  const achada = vi
    .mocked(globalThis.fetch)
    .mock.calls.find(([, init]) => (init as RequestInit).method === "POST");
  return achada === undefined
    ? undefined
    : (JSON.parse((achada[1] as RequestInit).body as string) as Record<string, unknown>);
}

async function preenche() {
  await userEvent.type(screen.getByLabelText(/^Título/), "Duna");
  await userEvent.type(screen.getByLabelText(/^Sinopse/), "Areia, e muita");
  const duracao = screen.getByLabelText(/Duração/);
  await userEvent.clear(duracao);
  await userEvent.type(duracao, "155");
}

describe("VideoCreate", () => {
  beforeEach(() => {
    sessao.atual = { user: { access_token: "abc.123" } };
    respondeCom();
  });

  // Os arquivos **não** aparecem aqui: a rota de envio é `/videos/{id}/medias/{type}`, e sem o
  // vídeo criado não há onde pôr o arquivo. A tela diz isso em vez de mostrar um campo que falharia.
  it("givenATelaDeCriacao_whenAbre_thenNaoOfereceEnvioDeArquivo", async () => {
    renderWithProviders(<VideoCreate />, { path: "/videos/novo", route: "/videos/novo" });

    expect(screen.queryByLabelText("Arquivo de Vídeo")).not.toBeInTheDocument();
    expect(await screen.findByText(/Os arquivos entram depois de criar/)).toBeInTheDocument();
  });

  // Os três estados também não: o vídeo nasce ativo, não publicado e fechado, e mudar isso é ação
  // da tela de edição.
  it("givenATelaDeCriacao_whenAbre_thenNaoOfereceOsEstados", () => {
    renderWithProviders(<VideoCreate />);

    expect(screen.queryByLabelText(/Publicado/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Aberto/)).not.toBeInTheDocument();
  });

  it("givenOsCamposPreenchidos_whenCria_thenMandaOPostComOsVinculosVazios", async () => {
    renderWithProviders(<VideoCreate />, { path: "/videos/novo", route: "/videos/novo" });

    await preenche();
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    await waitFor(() => expect(corpoDoPost()).toBeDefined());
    expect(vi.mocked(globalThis.fetch).mock.calls.find(([, i]) => (i as RequestInit).method === "POST")![0]).toBe(
      `${ADMIN_API}/videos`,
    );
    expect(corpoDoPost()).toMatchObject({
      title: "Duna",
      description: "Areia, e muita",
      duration: 155,
      categories: [],
      genres: [],
      castMembers: [],
    });
  });

  // Vai para a **edição**, e não para a lista: é lá que os arquivos entram, e criar um vídeo sem
  // mídia é metade do trabalho.
  it("givenACriacaoBemSucedida_whenTermina_thenVaiParaAEdicaoDoVideoCriado", async () => {
    renderWithProviders(<VideoCreate />, { path: "/videos/novo", route: "/videos/novo" });

    await preenche();
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    expect(await screen.findByText("Vídeo criado. Agora envie os arquivos.")).toBeInTheDocument();
    // `waitFor`, e não asserção direta: o aviso de sucesso e a navegação saem do mesmo
    // `onSuccess`, mas `navegate` do react-router é assíncrono — em máquina lenta os dois
    // renders se separam, e a asserção direta lia a rota antiga. Foi o que o CI pegou.
    await waitFor(() => expect(screen.getByTestId("rota")).toHaveTextContent("/videos/vid-9"));
  });

  it("givenSinopseVazia_whenTentaCriar_thenNaoChamaOPost", async () => {
    renderWithProviders(<VideoCreate />);

    await userEvent.type(screen.getByLabelText(/^Título/), "Duna");
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    expect(await screen.findByText("A sinopse é obrigatória.")).toBeInTheDocument();
    expect(corpoDoPost()).toBeUndefined();
  });

  it("given422DeVinculoInexistente_whenCria_thenMostraAMensagemDaApi", async () => {
    respondeCom({
      body: {
        message: "Some genres could not be found: gen-9",
        errors: ["Some genres could not be found: gen-9"],
      },
      status: 422,
    });

    renderWithProviders(<VideoCreate />, { path: "/videos/novo", route: "/videos/novo" });

    await preenche();
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    expect(await screen.findByText("Some genres could not be found: gen-9")).toBeInTheDocument();
    expect(screen.getByTestId("rota")).toHaveTextContent("/videos/novo");
  });
});
