import { Alert, Snackbar } from "@mui/material";
import { useCallback, useState } from "react";
import { FeedbackContext, type Feedback } from "@/shared/feedback";

// O aviso de "salvo" e de "não deu" numa peça só, porque toda ação de escrita precisa dos dois.
//
// **`Snackbar` do Material, e não `notistack`.** A referência do curso usa o notistack, e a escolha
// aqui foi medida: a última publicação dele é de janeiro de 2025, e o que ele oferece além disto é
// empilhar vários avisos ao mesmo tempo — num painel de administração a pessoa faz uma ação por vez.
// Trinta linhas aqui custam menos que uma dependência parada que ainda estiliza por conta própria,
// fora do tema.
export function FeedbackProvider({ children }: { children: React.ReactNode }) {
  const [aviso, setAviso] = useState<Feedback | null>(null);

  // `useCallback` porque o valor vai para um contexto: sem ele, cada render do provedor daria uma
  // função nova e redesenharia toda tela que a consome.
  const avisar = useCallback((proximo: Feedback) => setAviso(proximo), []);

  return (
    <FeedbackContext value={avisar}>
      {children}
      <Snackbar
        open={aviso !== null}
        autoHideDuration={aviso?.kind === "error" ? 10_000 : 4_000}
        onClose={() => setAviso(null)}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        {/* O aviso de erro fica o dobro do tempo: ele costuma ser uma frase para ler, e o de
            sucesso é uma palavra para conferir de canto de olho. */}
        <Alert severity={aviso?.kind ?? "success"} onClose={() => setAviso(null)} variant="filled">
          {aviso?.message ?? ""}
        </Alert>
      </Snackbar>
    </FeedbackContext>
  );
}
