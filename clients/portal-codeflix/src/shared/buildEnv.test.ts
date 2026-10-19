import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { keycloakIssuer, publicUrl } from "../../buildEnv";

// As URLs que o pacote embute saem daqui, e são embutidas **em tempo de build**: uma imagem
// construída para um ambiente não serve para outro. Por isso estas regras têm teste próprio — errar
// a precedência faria toda imagem apontar para `localhost`, que era o caso até aqui.

describe("publicUrl", () => {
  // O caminho do desenvolvimento: a porta vem do mesmo `.env` que o compose usa.
  it("givenOnlyAPort_whenResolved_thenPointsAtLocalhostOnThatPort", () => {
    expect(publicUrl(undefined, "8084", "9999")).toBe("http://localhost:8084");
  });

  it("givenNeitherUrlNorPort_whenResolved_thenFallsBackToTheDefaultPort", () => {
    expect(publicUrl(undefined, undefined, "8084")).toBe("http://localhost:8084");
  });

  // **A precedência que torna a imagem de outro ambiente possível:** a URL inteira ganha da porta.
  it("givenAFullUrl_whenResolved_thenItWinsOverThePort", () => {
    expect(publicUrl("https://h.tarcisiolzbraga.codeflix.com", "8084", "9999")).toBe(
      "https://h.tarcisiolzbraga.codeflix.com",
    );
  });

  // Sem a barra final: a concatenação com a rota geraria "//", que alguns servidores tratam como
  // caminho diferente.
  it("givenAFullUrlWithATrailingSlash_whenResolved_thenTheSlashIsDropped", () => {
    expect(publicUrl("https://codeflix.com/", undefined, "8084")).toBe("https://codeflix.com");
  });

  // Variável definida e vazia não é o mesmo que ausente, e aqui as duas caem para a porta: um
  // `build-arg` que chega vazio é o caso comum num compose sem o valor no `.env`.
  it("givenAnEmptyUrl_whenResolved_thenItFallsBackToThePort", () => {
    expect(publicUrl("   ", "8084", "9999")).toBe("http://localhost:8084");
  });
});

describe("keycloakIssuer", () => {
  const original = { ...process.env };

  beforeEach(() => {
    delete process.env.KEYCLOAK_HOST;
    delete process.env.KEYCLOAK_PORT;
    delete process.env.KEYCLOAK_REALM;
  });

  afterEach(() => {
    process.env = { ...original };
  });

  // Montado das mesmas variáveis que o `keycloak.issuer-uri` dos serviços: divergir aqui faria o
  // token ser recusado pelo `iss`.
  it("givenTheKeycloakVariables_whenResolved_thenBuildsTheSameIssuerTheServicesValidate", () => {
    process.env.KEYCLOAK_HOST = "keycloak";
    process.env.KEYCLOAK_PORT = "8081";
    process.env.KEYCLOAK_REALM = "codeflix";

    expect(keycloakIssuer()).toBe("http://keycloak:8081/realms/codeflix");
  });

  it("givenNothingSet_whenResolved_thenUsesTheDevelopmentDefaults", () => {
    expect(keycloakIssuer()).toBe("http://localhost:8081/realms/codeflix");
  });

  it("givenAFullIssuer_whenResolved_thenItWinsOverTheParts", () => {
    process.env.KEYCLOAK_HOST = "keycloak";

    expect(keycloakIssuer("https://auth.codeflix.com/realms/codeflix")).toBe(
      "https://auth.codeflix.com/realms/codeflix",
    );
  });
});
