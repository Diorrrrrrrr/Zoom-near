import React from "react";

interface CardProps {
  children: React.ReactNode;
  className?: string;
  onClick?: () => void;
}

export function Card({ children, className = "", onClick }: CardProps) {
  const base =
    "rounded-2xl border border-gray-100 bg-white p-5 shadow-soft-sm";
  if (onClick) {
    return (
      <button
        type="button"
        onClick={onClick}
        className={`${base} w-full text-left active:bg-gray-50 ${className}`}
      >
        {children}
      </button>
    );
  }
  return <div className={`${base} ${className}`}>{children}</div>;
}
