import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-tag border px-1.5 py-0.5 font-mono text-[11px] font-semibold leading-none",
  {
    variants: {
      variant: {
        neutral: "border-line bg-surface-3 text-ink-2",
        outline: "border-line bg-surface text-ink-2",
        accent: "border-accent-border bg-accent-soft text-accent",
        get: "border-get-border bg-get-soft text-get",
        post: "border-post-border bg-post-soft text-post",
        put: "border-put-border bg-put-soft text-put",
        delete: "border-del-border bg-del-soft text-del",
      },
    },
    defaultVariants: { variant: "neutral" },
  },
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export function methodVariant(method: string): BadgeProps["variant"] {
  switch (method.toUpperCase()) {
    case "GET":
      return "get";
    case "POST":
      return "post";
    case "PUT":
    case "PATCH":
      return "put";
    case "DELETE":
      return "delete";
    default:
      return "neutral";
  }
}

export function HttpMethodBadge({ method }: { method: string }) {
  return <Badge variant={methodVariant(method)}>{method.toUpperCase()}</Badge>;
}

