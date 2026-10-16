import type { Metadata } from "next";
import { Geist } from "next/font/google";
import Link from "next/link";
import "./globals.css";

const geist = Geist({ variable: "--font-geist-sans", subsets: ["latin"] });

export const metadata: Metadata = {
  title: "Codeflix",
  description: "O portal de quem assina o Codeflix: escolha o plano, assine e assista.",
};

// lang="pt-BR" e não "en": as rotas, os textos e o e-mail de validação que a subscription-api manda
// são em português, e é o atributo que o leitor de tela usa para escolher a pronúncia.
export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="pt-BR" className={`${geist.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col bg-neutral-950 text-neutral-100">
        <header className="border-b border-neutral-800">
          <nav className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4">
            <Link href="/" className="text-xl font-bold tracking-tight text-red-600">
              CODEFLIX
            </Link>
            <Link href="/planos" className="text-sm text-neutral-300 hover:text-neutral-50">
              Planos
            </Link>
          </nav>
        </header>
        <main className="mx-auto w-full max-w-5xl grow px-4 py-10">{children}</main>
      </body>
    </html>
  );
}
