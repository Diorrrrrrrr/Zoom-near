"use client";

import React from "react";

export interface Column<T> {
  key: string;
  header: string;
  render: (row: T) => React.ReactNode;
  className?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  keyExtractor: (row: T) => string | number;
  onRowClick?: (row: T) => void;
  isLoading?: boolean;
  emptyMessage?: string;
}

export function DataTable<T>({
  columns,
  data,
  keyExtractor,
  onRowClick,
  isLoading = false,
  emptyMessage = "데이터가 없습니다.",
}: DataTableProps<T>) {
  return (
    <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white shadow-sm">
      <table className="min-w-full text-base">
        <thead>
          <tr className="border-b border-gray-200 bg-gray-50">
            {columns.map((col) => (
              <th
                key={col.key}
                scope="col"
                className={`px-4 py-3 text-left text-sm font-semibold text-gray-600 ${col.className ?? ""}`}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {isLoading ? (
            <tr>
              <td
                colSpan={columns.length}
                className="py-16 text-center text-base text-gray-400"
              >
                <span className="inline-block animate-pulse">불러오는 중…</span>
              </td>
            </tr>
          ) : data.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length}
                className="py-16 text-center text-base text-gray-400"
              >
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((row) => (
              <tr
                key={keyExtractor(row)}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
                className={`transition-colors ${
                  onRowClick
                    ? "cursor-pointer hover:bg-blue-50"
                    : "hover:bg-gray-50"
                }`}
              >
                {columns.map((col) => (
                  <td
                    key={col.key}
                    className={`px-4 py-3 text-gray-800 ${col.className ?? ""}`}
                  >
                    {col.render(row)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

interface PaginationProps {
  page: number; // 0-based
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  const pages = Array.from({ length: totalPages }, (_, i) => i);
  const visible = pages.filter(
    (p) => p === 0 || p === totalPages - 1 || Math.abs(p - page) <= 2
  );

  return (
    <nav
      aria-label="페이지 이동"
      className="mt-4 flex items-center justify-center gap-1"
    >
      <button
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
        aria-label="이전 페이지"
        className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
      >
        ‹
      </button>
      {visible.map((p, idx) => {
        const prev = visible[idx - 1];
        const showEllipsis = prev !== undefined && p - prev > 1;
        return (
          <React.Fragment key={p}>
            {showEllipsis && (
              <span className="px-2 text-gray-400">…</span>
            )}
            <button
              onClick={() => onPageChange(p)}
              aria-current={p === page ? "page" : undefined}
              aria-label={`${p + 1}페이지`}
              className={`rounded-lg border px-3 py-2 text-sm font-medium ${
                p === page
                  ? "border-brand-500 bg-brand-500 text-white"
                  : "border-gray-300 bg-white text-gray-600 hover:bg-gray-50"
              }`}
            >
              {p + 1}
            </button>
          </React.Fragment>
        );
      })}
      <button
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
        aria-label="다음 페이지"
        className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
      >
        ›
      </button>
    </nav>
  );
}
