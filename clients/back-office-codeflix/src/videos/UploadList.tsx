import CloseIcon from "@mui/icons-material/Close";
import DoneIcon from "@mui/icons-material/CheckCircleOutline";
import ErrorIcon from "@mui/icons-material/ErrorOutline";
import {
  Button,
  IconButton,
  LinearProgress,
  Paper,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import { megabytes } from "@/videos/mediaGateway";
import { useUploadQueue, type QueuedUpload } from "@/videos/uploadQueue";
import { MEDIA_LABELS } from "@/videos/videosGateway";

// A fila de envios, visível de qualquer tela.
//
// Fica flutuando no canto porque é isso que ela precisa ser: enviar cinco arquivos de 100 MB leva
// minutos, e quem administra o catálogo continua trabalhando. Um painel dentro da tela do vídeo
// desapareceria ao navegar, e aí a fila não serviria para nada.
//
// Só aparece quando há algo nela.
function Item({ upload, onDismiss }: { upload: QueuedUpload; onDismiss: () => void }) {
  return (
    <Stack spacing={0.5} sx={{ minWidth: 280 }}>
      <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
        {upload.status === "done" && <DoneIcon fontSize="small" color="success" />}
        {upload.status === "failed" && <ErrorIcon fontSize="small" color="error" />}
        <Typography variant="body2" sx={{ flexGrow: 1 }} noWrap>
          {MEDIA_LABELS[upload.type]} · {upload.videoTitle}
        </Typography>
        {/* Descartar só o que terminou: cancelar um envio em curso exigiria abortar o pedido, e
            abortar no meio deixa o arquivo pela metade no armazenamento. */}
        {(upload.status === "done" || upload.status === "failed") && (
          <Tooltip title="Dispensar">
            <IconButton size="small" onClick={onDismiss} aria-label={`Dispensar ${upload.filename}`}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        )}
      </Stack>

      <Typography variant="caption" sx={{ color: "text.secondary" }} noWrap>
        {upload.filename} · {megabytes(upload.size)}
      </Typography>

      {upload.status === "failed" ? (
        <Typography variant="caption" color="error">
          {upload.failure}
        </Typography>
      ) : (
        <LinearProgress
          // Sempre `determinate`, com a fração que o `xhr.upload.onprogress` deu. Quem espera fica
          // em zero, e não indeterminada: indeterminada diria "está acontecendo algo", e não está.
          variant="determinate"
          value={Math.round(upload.progress * 100)}
          color={upload.status === "done" ? "success" : "primary"}
          aria-label={`Progresso de ${upload.filename}`}
        />
      )}
    </Stack>
  );
}

export function UploadList() {
  const fila = useUploadQueue();

  if (fila.uploads.length === 0) {
    return null;
  }

  const terminados = fila.uploads.filter(
    (upload) => upload.status === "done" || upload.status === "failed",
  ).length;

  return (
    <Paper
      elevation={8}
      sx={{
        position: "fixed",
        right: 16,
        bottom: 16,
        p: 2,
        maxWidth: 360,
        maxHeight: "50vh",
        overflowY: "auto",
        zIndex: (theme) => theme.zIndex.snackbar - 1,
      }}
    >
      <Stack spacing={2}>
        <Stack direction="row" sx={{ alignItems: "center", justifyContent: "space-between" }}>
          <Typography variant="subtitle2">Envios ({fila.uploads.length})</Typography>
          {terminados > 0 && (
            <Button size="small" onClick={fila.clearFinished}>
              Limpar
            </Button>
          )}
        </Stack>

        {fila.uploads.map((upload) => (
          <Item key={upload.id} upload={upload} onDismiss={() => fila.dismiss(upload.id)} />
        ))}
      </Stack>
    </Paper>
  );
}
