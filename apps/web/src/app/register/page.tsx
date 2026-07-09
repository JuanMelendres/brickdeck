"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import NextLink from "next/link";
import { Container, Link, Stack, Typography } from "@mui/material";
import { RegisterForm } from "@/features/auth/RegisterForm";
import { useAuth } from "@/features/auth/useAuth";
import type { RegisterRequest } from "@/lib/types/auth";

export default function RegisterPage() {
  const { status, register } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "authenticated") {
      router.replace("/");
    }
  }, [status, router]);

  const handleSubmit = async (values: RegisterRequest) => {
    await register(values);
    router.push("/");
  };

  return (
    <Container maxWidth="sm" sx={{ py: 8 }}>
      <Stack spacing={3}>
        <Typography variant="h4" component="h1">
          Create account
        </Typography>
        <RegisterForm onSubmit={handleSubmit} />
        <Typography variant="body2" color="text.secondary">
          Already have an account?{" "}
          <Link component={NextLink} href="/login">
            Log in
          </Link>
        </Typography>
      </Stack>
    </Container>
  );
}
