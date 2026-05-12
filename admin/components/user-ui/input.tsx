import React from "react";

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export function Input({
  label,
  error,
  id,
  className = "",
  ...props
}: InputProps) {
  const inputId = id ?? label;
  return (
    <div className="flex flex-col gap-1.5">
      {label && (
        <label
          htmlFor={inputId}
          className="text-base font-medium text-gray-700"
        >
          {label}
        </label>
      )}
      <input
        id={inputId}
        className={`
          w-full rounded-xl border px-4 py-3.5 text-lg text-gray-900
          placeholder-gray-400 transition-colors
          focus:outline-none focus:ring-2
          ${
            error
              ? "border-gray-900 focus:border-gray-900 focus:ring-gray-900/20"
              : "border-gray-300 focus:border-forest-700 focus:ring-forest-700/20"
          }
          disabled:bg-gray-50 disabled:text-gray-400
          ${className}
        `}
        {...props}
      />
      {error && (
        <p className="text-sm text-gray-800" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
