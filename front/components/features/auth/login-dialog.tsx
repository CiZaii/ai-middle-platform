"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { useLogin } from "@/lib/hooks/use-auth";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface LoginDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function LoginDialog({ open, onOpenChange }: LoginDialogProps) {
  const router = useRouter();
  const loginMutation = useLogin();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);

    try {
      await loginMutation.mutateAsync({ email, password });
      onOpenChange(false);
      router.push("/dashboard");
    } catch (submitError) {
      const message =
        submitError instanceof Error
          ? submitError.message
          : "登录失败，请稍后重试";
      setError(message);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md bg-[#0A0A14]/95 backdrop-blur-xl border border-white/20 text-white shadow-2xl shadow-indigo-500/20">
        <DialogHeader>
          <DialogTitle
            className="text-2xl font-light tracking-wider text-center"
            style={{
              textShadow:
                "0 0 10px rgba(94, 23, 235, 0.7), 0 0 20px rgba(94, 23, 235, 0.5)",
            }}
          >
            <span className="text-white">欢迎来到</span>{" "}
            <span className="bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 bg-clip-text text-transparent font-bold">
              AI NEXUS
            </span>
          </DialogTitle>
          <DialogDescription className="text-gray-300 text-center">
            登录以体验下一代人工智能平台
          </DialogDescription>
        </DialogHeader>

        <form className="space-y-6 mt-4" onSubmit={handleSubmit}>
          <div className="space-y-2">
            <Label htmlFor="email" className="text-gray-200 text-sm font-light">
              邮箱地址
            </Label>
            <Input
              id="email"
              type="email"
              placeholder="name@example.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
              className="bg-white/5 border-white/20 text-white placeholder:text-gray-500 focus:border-indigo-500 focus:ring-indigo-500/30 transition-all"
            />
          </div>

          <div className="space-y-2">
            <Label
              htmlFor="password"
              className="text-gray-200 text-sm font-light"
            >
              密码
            </Label>
            <Input
              id="password"
              type="password"
              placeholder="请输入密码"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              className="bg-white/5 border-white/20 text-white placeholder:text-gray-500 focus:border-indigo-500 focus:ring-indigo-500/30 transition-all"
            />
          </div>

          {error && (
            <div className="backdrop-blur-md bg-red-500/10 border border-red-500/30 rounded-lg p-3">
              <p className="text-sm text-red-300 text-center" role="alert">
                {error}
              </p>
            </div>
          )}

          <Button
            type="submit"
            className="w-full bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 text-white shadow-lg shadow-indigo-500/30 transition-all hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed"
            disabled={loginMutation.isPending}
          >
            {loginMutation.isPending ? (
              <span className="flex items-center">
                <svg
                  className="animate-spin -ml-1 mr-3 h-5 w-5 text-white"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                  ></circle>
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  ></path>
                </svg>
                登录中...
              </span>
            ) : (
              <span className="flex items-center justify-center">
                <svg
                  className="w-5 h-5 mr-2"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path
                    fillRule="evenodd"
                    d="M12.395 2.553a1 1 0 00-1.45-.385c-.345.23-.614.558-.822.88-.214.33-.403.713-.57 1.116-.334.804-.614 1.768-.84 2.734a31.365 31.365 0 00-.613 3.58 2.64 2.64 0 01-.945-1.067c-.328-.68-.398-1.534-.398-2.654A1 1 0 005.05 6.05 6.981 6.981 0 003 11a7 7 0 1011.95-4.95c-.592-.591-.98-.985-1.348-1.467-.363-.476-.724-1.063-1.207-2.03zM12.12 15.12A3 3 0 017 13s.879.5 2.5.5c0-1 .5-4 1.25-4.5.5 1 .786 1.293 1.371 1.879A2.99 2.99 0 0113 13a2.99 2.99 0 01-.879 2.121z"
                    clipRule="evenodd"
                  />
                </svg>
                开始体验
              </span>
            )}
          </Button>

          <div className="text-center">
            <p className="text-xs text-gray-400">
              还没有账号？{" "}
              <button
                type="button"
                className="text-indigo-400 hover:text-indigo-300 transition-colors underline"
                onClick={() => {
                  // 这里可以添加注册功能
                  console.log("跳转到注册");
                }}
              >
                立即注册
              </button>
            </p>
          </div>
        </form>

        <div className="mt-6 pt-6 border-t border-white/10">
          <p className="text-xs text-gray-500 text-center">
            登录即表示您同意我们的{" "}
            <a href="#" className="text-indigo-400 hover:text-indigo-300">
              服务条款
            </a>{" "}
            和{" "}
            <a href="#" className="text-indigo-400 hover:text-indigo-300">
              隐私政策
            </a>
          </p>
        </div>
      </DialogContent>
    </Dialog>
  );
}
