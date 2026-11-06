import type { Metadata } from "next";
import { Toaster } from "sonner";
import "@fontsource-variable/inter";
import "@fontsource-variable/jetbrains-mono";
import "./globals.css";

export const metadata: Metadata = {
  title: "SpringDocAI — Documentação contextual a partir da AST do Spring Boot",
  description: "Gere API-REFERENCE.md, INTEGRATION-GUIDE.md e ARCHITECTURE.md a partir do seu projeto Spring Boot.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <body>
        {children}
        <Toaster position="bottom-right" />
      </body>
    </html>
  );
}

