"use client";

import { Poster } from "@/catalog/Poster";
import type { Rail as RailData } from "@/catalog/catalog";

// Uma trilha: o nome do gênero, e os títulos dele em linha.
//
// Rolagem **horizontal**, e não grade que quebra: a trilha existe para dizer "tem mais aqui" sem
// empurrar as outras para baixo. `snap` para a rolagem parar no cartão, e não no meio dele.
export function Rail({ rail }: { rail: RailData }) {
  return (
    <section className="flex flex-col gap-3">
      <h2 className="text-lg font-semibold text-neutral-200">{rail.genre.name}</h2>
      <ul className="flex snap-x gap-4 overflow-x-auto pb-2">
        {rail.items.map((video) => (
          <li key={video.id} className="w-36 shrink-0 snap-start sm:w-44">
            <Poster video={video} />
          </li>
        ))}
      </ul>
    </section>
  );
}
