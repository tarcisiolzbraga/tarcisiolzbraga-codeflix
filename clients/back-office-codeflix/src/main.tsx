import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { App } from "@/App";

const raiz = document.getElementById("root");
if (raiz === null) {
  throw new Error("o index.html precisa ter a <div id=\"root\">");
}

createRoot(raiz).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
