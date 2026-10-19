import { createTheme } from "@mui/material/styles";

// Escuro por padrão, e vermelho Codeflix como cor primária: é um painel de catálogo de vídeo, usado
// por horas seguidas.
//
// `colorSchemes` com `cssVariables`, e não `palette.mode`: é assim que o Material 7 troca de tema
// **por CSS**, sem um segundo render da árvore. O `colorSchemeSelector: "class"` é o que permite o
// `useColorScheme` alternar escrevendo uma classe na raiz — e é o que o botão de alternância usa.
export const theme = createTheme({
  colorSchemes: { dark: true, light: true },
  defaultColorScheme: "dark",
  cssVariables: { colorSchemeSelector: "class" },
  palette: {
    primary: { main: "#e50914" },
  },
  shape: { borderRadius: 8 },
  typography: {
    fontFamily: ["system-ui", "Segoe UI", "Roboto", "Helvetica", "Arial", "sans-serif"].join(","),
  },
});
