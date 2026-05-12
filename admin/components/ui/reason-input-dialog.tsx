"use client";

import React, { useState } from "react";

interface ReasonInputDialogProps {
  open: boolean;
  title: string;
  description?: string;
  placeholder?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
  onConfirm: (reason: string) => void;
  onCancel: () => void;
}

export function ReasonInputDialog({
  open,
  title,
  description,
  placeholder = "사유를 입력해 주세요.",
  confirmLabel = "확인",
  cancelLabel = "취소",
  destructive = false,
  onConfirm,
  onCancel,
}: ReasonInputDialogProps) {
  const [reason, setReason] = useState("");

  if (!open) return null;

  function handleConfirm() {
    if (!reason.trim()) return;
    onConfirm(reason.trim());
    setReason("");
  }

  function handleCancel() {
    setReason("");
    onCancel();
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="reason-dialog-title"
      className="fixed inset-0 z-50 flex items-center justify-center"
    >
      {/* backdrop */}
      <div
        className="absolute inset-0 bg-black/40"
        onClick={handleCancel}
        aria-hidden="true"
      />
      <div className="relative z-10 w-full max-w-md rounded-2xl bg-white p-8 shadow-xl">
        <h2
          id="reason-dialog-title"
          className="mb-2 text-xl font-bold text-gray-900"
        >
          {title}
        </h2>
        {description && (
          <p className="mb-4 text-base text-gray-600">{description}</p>
        )}
        <textarea
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder={placeholder}
          rows={4}
          className="mt-2 w-full rounded-lg border border-gray-300 p-3 text-base text-gray-900 placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
          aria-label="사유 입력"
        />
        <p className="mt-1 text-sm text-gray-400">{reason.length} / 200자</p>
        <div className="mt-6 flex justify-end gap-3">
          <button
            type="button"
            onClick={handleCancel}
            className="rounded-lg border border-gray-300 bg-white px-5 py-2.5 text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-300"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={!reason.trim()}
            className={`rounded-lg px-5 py-2.5 text-base font-medium text-white focus:outline-none focus:ring-2 disabled:cursor-not-allowed disabled:opacity-40 ${
              destructive
                ? "bg-red-600 hover:bg-red-700 focus:ring-red-400"
                : "bg-brand-500 hover:bg-brand-600 focus:ring-brand-400"
            }`}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
