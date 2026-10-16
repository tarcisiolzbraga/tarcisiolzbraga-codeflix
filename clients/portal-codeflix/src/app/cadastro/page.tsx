"use client";

import Link from "next/link";
import { useState } from "react";
import {
  PASSWORD_MAX,
  PASSWORD_MIN,
  problemsIn,
  requestSignUp,
  SignUpRefused,
  type DocumentType,
  type SignUpForm,
} from "@/signup/signup";

const EMPTY: SignUpForm = {
  firstname: "",
  lastname: "",
  email: "",
  password: "",
  documentNumber: "",
  documentType: "cpf",
};

export default function SignUp() {
  const [form, setForm] = useState<SignUpForm>(EMPTY);
  const [problems, setProblems] = useState<string[]>([]);
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);

  const change = (field: keyof SignUpForm) => (value: string) =>
    setForm((it) => ({ ...it, [field]: value }));

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    const found = problemsIn(form);
    if (found.length > 0) {
      setProblems(found);
      return;
    }
    setProblems([]);
    setSending(true);
    try {
      await requestSignUp(form);
      setSent(true);
    } catch (error: unknown) {
      console.error("falha ao pedir o cadastro", error);
      setProblems([
        error instanceof SignUpRefused ? error.message : "Não conseguimos pedir o seu cadastro agora.",
      ]);
    } finally {
      setSending(false);
    }
  }

  // A tela de sucesso **não diz se o e-mail já existia**: a API responde igual nos três casos, de
  // propósito, para não contar a um estranho se um endereço está cadastrado. Dizer aqui jogaria essa
  // decisão fora.
  if (sent) {
    return (
      <section className="flex flex-col items-start gap-4">
        <h1 className="text-3xl font-bold tracking-tight">Confira o seu e-mail</h1>
        <p className="max-w-xl text-neutral-400">
          Enviamos um link de validação para <strong className="text-neutral-200">{form.email}</strong>. O
          link vale por 24 horas, e a sua conta é criada quando você clicar nele.
        </p>
        <p className="text-sm text-neutral-500">
          Não chegou? Peça o cadastro de novo — só o link mais novo continua valendo.
        </p>
      </section>
    );
  }

  return (
    <section className="flex max-w-xl flex-col gap-6">
      <h1 className="text-3xl font-bold tracking-tight">Criar conta</h1>

      {problems.length > 0 && (
        <ul className="flex flex-col gap-1 rounded-lg border border-red-900 bg-red-950/40 p-4 text-sm text-red-300">
          {problems.map((problem) => (
            <li key={problem}>{problem}</li>
          ))}
        </ul>
      )}

      <form className="flex flex-col gap-4" onSubmit={(event) => void submit(event)} noValidate>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Nome" value={form.firstname} onChange={change("firstname")} autoComplete="given-name" />
          <Field label="Sobrenome" value={form.lastname} onChange={change("lastname")} autoComplete="family-name" />
        </div>
        <Field label="E-mail" type="email" value={form.email} onChange={change("email")} autoComplete="email" />
        <Field
          label={`Senha (de ${PASSWORD_MIN} a ${PASSWORD_MAX} caracteres)`}
          type="password"
          value={form.password}
          onChange={change("password")}
          autoComplete="new-password"
        />
        <div className="grid gap-4 sm:grid-cols-[1fr_auto]">
          <Field
            label="Documento"
            value={form.documentNumber}
            onChange={change("documentNumber")}
            inputMode="numeric"
          />
          <label className="flex flex-col gap-1">
            <span className="text-xs uppercase tracking-wide text-neutral-500">Tipo</span>
            <select
              value={form.documentType}
              onChange={(event) => change("documentType")(event.target.value as DocumentType)}
              className="rounded border border-neutral-700 bg-neutral-900 px-3 py-2 text-neutral-100"
            >
              <option value="cpf">CPF</option>
              <option value="cnpj">CNPJ</option>
            </select>
          </label>
        </div>

        <button
          type="submit"
          disabled={sending}
          className="self-start rounded bg-red-600 px-6 py-3 font-semibold text-neutral-50 hover:bg-red-500 disabled:opacity-60"
        >
          {sending ? "Enviando…" : "Criar conta"}
        </button>
      </form>

      <p className="text-sm text-neutral-500">
        Já tem conta? <Link href="/conta" className="text-neutral-300 hover:text-neutral-50">Entrar</Link>.
      </p>
    </section>
  );
}

function Field({
  label,
  value,
  onChange,
  type = "text",
  autoComplete,
  inputMode,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  autoComplete?: string;
  inputMode?: "numeric";
}) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-xs uppercase tracking-wide text-neutral-500">{label}</span>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        autoComplete={autoComplete}
        inputMode={inputMode}
        className="rounded border border-neutral-700 bg-neutral-900 px-3 py-2 text-neutral-100"
      />
    </label>
  );
}
