import { describe, expect, it } from "vitest";
import { apiUrl } from "@/shared/config";

describe("apiUrl", () => {
  it("givenAnAbsoluteUrl_whenChecked_thenReturnsItWithoutTheTrailingSlash", () => {
    // Given uma URL absoluta, com barra no fim
    const value = "http://localhost:8084/";

    // When é conferida
    const actual = apiUrl("NEXT_PUBLIC_SUBSCRIPTION_API_URL", value);

    // Then volta sem a barra, para a concatenação com a rota não gerar "//"
    expect(actual).toBe("http://localhost:8084");
  });

  it("givenAnUndefinedValue_whenChecked_thenFailsSayingWhichVariable", () => {
    // Given variável que não foi definida
    const value = undefined;

    // When é conferida / Then o nome dela aparece no erro
    expect(() => apiUrl("NEXT_PUBLIC_SUBSCRIPTION_API_URL", value)).toThrow(
      /'NEXT_PUBLIC_SUBSCRIPTION_API_URL' é obrigatória/,
    );
  });

  it("givenAnEmptyValue_whenChecked_thenFailsToo", () => {
    // Given variável definida e vazia, que não é o mesmo que ausente
    const value = "   ";

    // When é conferida / Then falha igual
    expect(() => apiUrl("QUALQUER", value)).toThrow(/é obrigatória/);
  });

  it("givenAnUnresolvedPlaceholder_whenChecked_thenFailsSayingWhatArrived", () => {
    // Given o texto do placeholder, que é o que chega quando ninguém resolveu a variável
    const value = "${SUBSCRIPTION_APP_PORT}";

    // When é conferida / Then o erro mostra o valor, senão ninguém entende o que aconteceu
    expect(() => apiUrl("QUALQUER", value)).toThrow(/não foi resolvida; veio/);
  });

  it("givenSomethingThatIsNotAUrl_whenChecked_thenFailsSayingItNeedsToBeAbsolute", () => {
    // Given um caminho relativo, que não serve para o browser chamar
    const value = "/api";

    // When é conferida / Then falha dizendo que precisa ser absoluta
    expect(() => apiUrl("QUALQUER", value)).toThrow(/URL absoluta/);
  });
});
