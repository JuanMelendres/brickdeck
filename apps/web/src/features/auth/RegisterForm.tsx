"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Alert, Box, Button, Stack, TextField } from "@mui/material";
import type { RegisterRequest } from "@/lib/types/auth";
import { applyApiError } from "./formErrors";

const schema = z.object({
  email: z.string().trim().email("Enter a valid email"),
  password: z.string().min(8, "Password must be at least 8 characters"),
  displayName: z.string().trim().max(255).optional(),
});

type RegisterFormValues = z.infer<typeof schema>;

interface RegisterFormProps {
  onSubmit: (values: RegisterRequest) => Promise<void>;
}

export function RegisterForm({ onSubmit }: RegisterFormProps) {
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "", displayName: "" },
  });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    const payload: RegisterRequest = {
      email: values.email,
      password: values.password,
      ...(values.displayName ? { displayName: values.displayName } : {}),
    };
    try {
      await onSubmit(payload);
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
          autoComplete="new-password"
          fullWidth
          {...register("password")}
          error={Boolean(errors.password)}
          helperText={errors.password?.message ?? " "}
        />
        <TextField
          label="Display name"
          autoComplete="nickname"
          fullWidth
          {...register("displayName")}
          error={Boolean(errors.displayName)}
          helperText={errors.displayName?.message ?? " "}
        />
        <Button
          type="submit"
          variant="contained"
          size="large"
          disabled={isSubmitting}
        >
          Create account
        </Button>
      </Stack>
    </Box>
  );
}
