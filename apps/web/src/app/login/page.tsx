"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import NextLink from "next/link";
import { Container, Link, Stack, Typography } from "@mui/material";
import { LoginForm } from "@/features/auth/LoginForm";
import { useAuth } from "@/features/auth/useAuth";
import type { LoginRequest } from "@/lib/types/auth";

export default function LoginPage() {
  const { status, login } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "authenticated") {
      router.replace("/");
    }
  }, [status, router]);

  const handleSubmit = async (values: LoginRequest) => {
    await login(values);
    router.push("/");
  };

  return (
    <Container maxWidth="sm" sx={{ py: 8 }}>
      <Stack spacing={3}>
        <Typography variant="h4" component="h1">
          Log in
        </Typography>
        <LoginForm onSubmit={handleSubmit} />
        <Typography variant="body2" color="text.secondary">
          Need an account?{" "}
          <Link component={NextLink} href="/register">
            Create one
          </Link>
        </Typography>
      </Stack>
    </Container>
  );
}
