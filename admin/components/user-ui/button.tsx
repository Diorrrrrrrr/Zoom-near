import React from "react";

type Variant = "primary" | "secondary" | "danger" | "ghost";

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  loading?: boolean;
  fullWidth?: boolean;
}

const VARIANT_STYLES: Record<Variant, string> = {
  primary:
    "bg-forest-700 text-white hover:bg-forest-800 active:bg-forest-800 focus:ring-forest-700/30 disabled:opacity-60",
  secondary:
    "bg-white text-gray-800 border border-gray-300 hover:bg-gray-50 active:bg-gray-100 focus:ring-gray-300/50 disabled:opacity-60",
  danger:
    "bg-red-600 text-white hover:bg-red-700 active:bg-red-700 focus:ring-red-600/30 disabled:opacity-60",
  ghost:
    "bg-transparent text-forest-700 hover:bg-forest-50 active:bg-forest-100 focus:ring-forest-700/20 disabled:opacity-60",
};

export function Button({
  variant = "primary",
  loading = false,
  fullWidth = false,
  children,
  disabled,
  className = "",
  ...props
}: ButtonProps) {
  return (
    <button
      disabled={disabled || loading}
      className={`
        inline-flex h-[3.25rem] items-center justify-center gap-2 rounded-xl px-6
        text-base font-semibold tracking-tight-1 transition-colors
        shadow-soft-sm
        focus:outline-none focus:ring-2
        disabled:cursor-not-allowed disabled:shadow-none
        ${VARIANT_STYLES[variant]}
        ${fullWidth ? "w-full" : ""}
        ${className}
      `}
      {...props}
    >
      {loading ? (
        <span className="h-5 w-5 animate-spin rounded-full border-2 border-current border-t-transparent" />
      ) : null}
      {children}
    </button>
  );
}
