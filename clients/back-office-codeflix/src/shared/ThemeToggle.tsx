import DarkIcon from "@mui/icons-material/DarkModeOutlined";
import LightIcon from "@mui/icons-material/LightModeOutlined";
import SystemIcon from "@mui/icons-material/SettingsBrightnessOutlined";
import { IconButton, Tooltip } from "@mui/material";
import { useColorScheme } from "@mui/material/styles";

// Três estados, e não dois: escuro, claro e **o do sistema**.
//
// O padrão é **escuro**, e isso é escolha do painel, não omissão: é um catálogo de vídeo, usado por
// horas seguidas. O terceiro estado existe para quem quer a preferência do sistema — sem ele, quem
// já configurou o sistema teria de escolher de novo aqui, e não haveria como voltar atrás.
//
// O ciclo é escuro → claro → sistema, nessa ordem: começa onde o painel abre, e o "sistema" fecha a
// volta em vez de ser o primeiro clique de quem só quer clarear a tela.
//
// O `useColorScheme` do Material guarda a escolha em `localStorage` por conta própria, então não há
// estado nosso aqui.
const PROXIMO = { dark: "light", light: "system", system: "dark" } as const;

const ROTULO = {
  system: "Tema: o do sistema",
  light: "Tema: claro",
  dark: "Tema: escuro",
} as const;

export function ThemeToggle() {
  const { mode, setMode } = useColorScheme();

  // Antes de o Material ler o `localStorage`, `mode` é indefinido. Desenhar o escuro aqui evita o
  // piscar de um ícone que muda logo depois, porque escuro é onde o painel abre.
  const atual = mode ?? "dark";

  return (
    <Tooltip title={ROTULO[atual]}>
      <IconButton size="small" aria-label={ROTULO[atual]} onClick={() => setMode(PROXIMO[atual])}>
        {atual === "light" && <LightIcon fontSize="small" />}
        {atual === "dark" && <DarkIcon fontSize="small" />}
        {atual === "system" && <SystemIcon fontSize="small" />}
      </IconButton>
    </Tooltip>
  );
}
