"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Alert, Box, Button, Stack, TextField } from "@mui/material";
import type { LoginRequest } from "@/lib/types/auth";
import { applyApiError } from "./formErrors";

const schema = z.object({
  email: z.string().trim().email("Enter a valid email"),
  password: z.string().min(1, "Password is required"),
});

type LoginFormValues = z.infer<typeof schema>;

interface LoginFormProps {
  onSubmit: (values: LoginRequest) => Promise<void>;
}

export function LoginForm({ onSubmit }: LoginFormProps) {
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "" },
  });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await onSubmit(values);
    } catch (error) {
      setFormError(applyApiError(error, setError));
    }
  });

  return (
    <Box component="form" onSubmit={submit} noValidate>
      <Stack spacing={2}>
        {formError && <Alert severity="error">{formError}</Alert>}
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          fullWidth
          {...register("email")}
          error={Boolean(errors.email)}
          helperText={errors.email?.message ?? " "}
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="current-password"
          fullWidth
          {...register("password")}
          error={Boolean(errors.password)}
          helperText={errors.password?.message ?? " "}
        />
        <Button
          type="submit"
          variant="contained"
          size="large"
          disabled={isSubmitting}
        >
          Log in
        </Button>
      </Stack>
    </Box>
  );
}
