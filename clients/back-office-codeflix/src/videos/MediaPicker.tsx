import ClearIcon from "@mui/icons-material/Close";
import FileIcon from "@mui/icons-material/AttachFileOutlined";
import { Button, IconButton, Stack, Typography } from "@mui/material";
import { useRef } from "react";
import { MEDIA_MAX_FILE_SIZE } from "@/shared/config";
import { megabytes } from "@/videos/mediaGateway";
import { MEDIA_LABELS, MEDIA_TYPES, type MediaType } from "@/videos/videosGateway";
import type { ChosenMedia } from "@/videos/chosenMedia";

// Escolher os arquivos **junto com os campos do vídeo**, na tela de criação.
//
// Eles não podem ser enviados aqui: a rota de envio é `/videos/{id}/medias/{type}`, e o id só existe
// depois de o vídeo ser criado. Então esta peça **só escolhe** — quem envia é a fila, logo depois da
// criação, com o id em mãos.
//
// O arquivo acima do teto é recusado aqui mesmo, antes de a pessoa preencher o resto do formulário e
// descobrir no fim.
export function MediaPicker({
  chosen,
  onChange,
}: {
  chosen: ChosenMedia;
  onChange: (chosen: ChosenMedia) => void;
}) {
  const campos = useRef(new Map<MediaType, HTMLInputElement | null>());

  return (
    <Stack spacing={1}>
      <Typography variant="subtitle2">Arquivos (opcional)</Typography>
      {/* Só o teto aqui: o aviso no topo da tela já explica que os arquivos entram na fila depois
          da criação, e dizer a mesma coisa duas vezes na mesma tela é ruído. */}
      <Typography variant="caption" sx={{ color: "text.secondary" }}>
        Até {megabytes(MEDIA_MAX_FILE_SIZE)} cada.
      </Typography>

      <Stack spacing={1}>
        {MEDIA_TYPES.map((type) => {
          const arquivo = chosen[type];
          const grande = arquivo !== undefined && arquivo.size > MEDIA_MAX_FILE_SIZE;
          return (
            <Stack key={type} direction="row" spacing={1} sx={{ alignItems: "center" }}>
              <input
                ref={(elemento) => {
                  campos.current.set(type, elemento);
                }}
                type="file"
                hidden
                aria-label={`Arquivo de ${MEDIA_LABELS[type]}`}
                onChange={(evento) => {
                  const escolhido = evento.target.files?.[0];
                  onChange({ ...chosen, [type]: escolhido });
                  evento.target.value = "";
                }}
              />
              <Button
                size="small"
                variant="outlined"
                startIcon={<FileIcon />}
                onClick={() => campos.current.get(type)?.click()}
                sx={{ minWidth: 170, justifyContent: "flex-start" }}
              >
                {MEDIA_LABELS[type]}
              </Button>

              {arquivo === undefined ? (
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  nenhum
                </Typography>
              ) : (
                <>
                  <Typography
                    variant="caption"
                    color={grande ? "error" : "text.secondary"}
                    sx={{ flexGrow: 1 }}
                    noWrap
                  >
                    {arquivo.name} · {megabytes(arquivo.size)}
                    {grande && " — passa do limite"}
                  </Typography>
                  <IconButton
                    size="small"
                    aria-label={`Remover o arquivo de ${MEDIA_LABELS[type]}`}
                    onClick={() => {
                      const restante = { ...chosen };
                      delete restante[type];
                      onChange(restante);
                    }}
                  >
                    <ClearIcon fontSize="small" />
                  </IconButton>
                </>
              )}
            </Stack>
          );
        })}
      </Stack>
    </Stack>
  );
}

