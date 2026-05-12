"use client";

import React, { useEffect, useState } from "react";
import { TopBar } from "@/components/user-ui/top-bar";
import { Button } from "@/components/user-ui/button";

const FONT_SIZES = [
  { label: "작게", value: "14px", cls: "text-sm" },
  { label: "보통", value: "16px", cls: "text-base" },
  { label: "크게", value: "18px", cls: "text-lg" },
  { label: "매우 크게", value: "20px", cls: "text-xl" },
];

const STORAGE_KEY = "user_font_size";

export default function FontSizePage() {
  const [selected, setSelected] = useState("16px");
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) setSelected(stored);
  }, []);

  function handleSave() {
    localStorage.setItem(STORAGE_KEY, selected);
    document.documentElement.style.setProperty("--user-font-size", selected);
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  }

  return (
    <>
      <TopBar title="글자 크기 설정" showBack />

      <div className="px-4 py-6 space-y-6">
        <p className="text-base text-gray-600">화면에 표시되는 글자 크기를 조절할 수 있어요.</p>

        <div className="space-y-3">
          {FONT_SIZES.map((fs) => (
            <button
              key={fs.value}
              onClick={() => setSelected(fs.value)}
              className={`flex w-full items-center justify-between rounded-2xl border px-5 py-4 transition-colors ${
                selected === fs.value
                  ? "border-forest-700 bg-forest-50"
                  : "border-gray-200 bg-white hover:bg-gray-50"
              }`}
            >
              <span className={`font-medium text-gray-900 ${fs.cls}`}>{fs.label}</span>
              <span className={`text-gray-500 ${fs.cls}`}>가나다라 ABC 123</span>
              {selected === fs.value && (
                <span className="ml-2 text-forest-700">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                    <path fillRule="evenodd" d="M19.916 4.626a.75.75 0 01.208 1.04l-9 13.5a.75.75 0 01-1.154.114l-6-6a.75.75 0 011.06-1.06l5.353 5.353 8.493-12.739a.75.75 0 011.04-.208z" clipRule="evenodd" />
                  </svg>
                </span>
              )}
            </button>
          ))}
        </div>

        {saved && (
          <div className="rounded-xl border border-forest-200 bg-forest-50 p-4 text-base text-forest-700" role="status">
            글자 크기가 저장됐어요.
          </div>
        )}

        <Button variant="primary" fullWidth onClick={handleSave}>
          저장하기
        </Button>
      </div>
    </>
  );
}
