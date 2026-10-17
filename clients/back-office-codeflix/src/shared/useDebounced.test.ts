import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useDebounced } from "@/shared/useDebounced";

describe("useDebounced", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("givenValorInicial_whenRenderiza_thenJaDevolveEle", () => {
    const { result } = renderHook(() => useDebounced("documentário"));

    expect(result.current).toBe("documentário");
  });

  it("givenValorNovo_whenOTempoAindaNaoPassou_thenDevolveOAntigo", () => {
    const { result, rerender } = renderHook(({ valor }) => useDebounced(valor, 400), {
      initialProps: { valor: "a" },
    });

    rerender({ valor: "ab" });
    act(() => void vi.advanceTimersByTime(399));

    expect(result.current).toBe("a");
  });

  it("givenValorNovo_whenOTempoPassa_thenDevolveONovo", () => {
    const { result, rerender } = renderHook(({ valor }) => useDebounced(valor, 400), {
      initialProps: { valor: "a" },
    });

    rerender({ valor: "ab" });
    act(() => void vi.advanceTimersByTime(400));

    expect(result.current).toBe("ab");
  });

  // O que o atraso existe para fazer: treze teclas não viram treze valores, viram um.
  it("givenVariasTeclasSeguidas_whenOTempoPassa_thenSoAUltimaVale", () => {
    const { result, rerender } = renderHook(({ valor }) => useDebounced(valor, 400), {
      initialProps: { valor: "" },
    });

    for (const parcial of ["d", "do", "doc", "docu"]) {
      rerender({ valor: parcial });
      act(() => void vi.advanceTimersByTime(100));
    }
    expect(result.current).toBe("");

    act(() => void vi.advanceTimersByTime(400));
    expect(result.current).toBe("docu");
  });
});
