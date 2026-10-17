import { createTheme } from "@mui/material/styles";

// Escuro por padrão, e vermelho Codeflix como cor primária: é um painel de catálogo de vídeo, usado
// por horas seguidas. A referência do curso usa o mesmo partido.
//
// `colorSchemes` em vez de `palette.mode`: no Material 7 é assim que o tema acompanha a preferência
// do sistema sem um segundo render — a decisão sai por CSS, não por JavaScript.
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
