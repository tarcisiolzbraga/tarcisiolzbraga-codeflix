"use client";

import Link from "next/link";
import { Artwork } from "@/catalog/Artwork";
import { formatDuration, ratingLabel, type Video } from "@/catalog/catalog";

// O cartão de um título: pôster em pé, 2:3, que é a proporção de cartaz de cinema e a que o
// `THUMBNAIL` do admin recebe.
//
// Em arquivo próprio porque a grade de busca e as trilhas por gênero mostram **o mesmo** cartão — e
// na segunda cópia já seria duas telas para manter iguais.
//
// A sinopse não está aqui de propósito: numa grade de pôsteres ela é ruído, e a página do título já
// a mostra inteira.
export function Poster({ video }: { video: Video }) {
  return (
    <Link
      href={`/acervo/${video.id}`}
      className="group flex h-full flex-col gap-2 rounded-lg border border-neutral-800 bg-neutral-900 transition hover:border-neutral-600"
    >
      <Artwork
        videoId={video.id}
        type="THUMBNAIL"
        location={video.thumbnail}
        title={video.title}
        className="aspect-2/3 w-full rounded-t-lg"
      />
      <div className="flex flex-col gap-1 p-3 pt-0">
        <h3 className="font-semibold text-neutral-100 group-hover:text-neutral-50">{video.title}</h3>
        <p className="text-xs text-neutral-500">
          {video.yearLaunched} · {formatDuration(video.duration)} · {ratingLabel(video.rating)}
        </p>
      </div>
    </Link>
  );
}
