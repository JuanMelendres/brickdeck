"use client";

import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Alert, Box, Button, MenuItem, Stack, TextField } from "@mui/material";
import { applyApiError } from "@/features/auth/formErrors";
import type {
  AddPriceSnapshotRequest,
  PriceCondition,
} from "@/lib/types/pricing";

const schema = z.object({
  amount: z
    .string()
    .refine((v) => !Number.isNaN(Number(v)) && Number(v) > 0, {
      message: "Enter a valid price",
    }),
  currency: z
    .string()
    .trim()
    .regex(/^[A-Za-z]{3}$/, "Use a 3-letter code (e.g. USD)"),
  condition: z.enum(["NEW", "USED"]),
  observedAt: z.string().min(1, "Date is required"),
  store: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

const CONDITION_LABELS: Record<PriceCondition, string> = {
  NEW: "New",
  USED: "Used",
};

/** Snapshot payload without the set number — the container injects it. */
export type PriceSnapshotFormValues = Omit<AddPriceSnapshotRequest, "setNumber">;

interface AddPriceSnapshotFormProps {
  onSubmit: (values: PriceSnapshotFormValues) => Promise<void>;
}

export function AddPriceSnapshotForm({ onSubmit }: AddPriceSnapshotFormProps) {
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    control,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      amount: "",
      currency: "USD",
      condition: "NEW",
      observedAt: "",
      store: "",
    },
  });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    const payload: PriceSnapshotFormValues = {
      amount: Number(values.amount),
      currency: values.currency.toUpperCase(),
      condition: values.condition,
      observedAt: values.observedAt,
      ...(values.store ? { store: values.store } : {}),
    };
    try {
      await onSubmit(payload);
      reset();
    } catch (error) {
      setFormError(applyApiError(error, setError));
    }
  });

  return (
    <Box component="form" onSubmit={submit} noValidate>
      <Stack spacing={2}>
        {formError && <Alert severity="error">{formError}</Alert>}
        <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
          <TextField
            label="Amount"
            type="number"
            slotProps={{ htmlInput: { step: "0.01", min: "0" } }}
            {...register("amount")}
            error={Boolean(errors.amount)}
            helperText={errors.amount?.message ?? " "}
          />
          <TextField
            label="Currency"
            {...register("currency")}
            error={Boolean(errors.currency)}
            helperText={errors.currency?.message ?? " "}
            sx={{ maxWidth: 120 }}
          />
          <Controller
            name="condition"
            control={control}
            render={({ field }) => (
              <TextField select label="Condition" sx={{ minWidth: 120 }} {...field}>
                {(Object.keys(CONDITION_LABELS) as PriceCondition[]).map((c) => (
                  <MenuItem key={c} value={c}>
                    {CONDITION_LABELS[c]}
                  </MenuItem>
                ))}
              </TextField>
            )}
          />
        </Stack>
        <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
          <TextField
            label="Observed on"
            type="date"
            slotProps={{ inputLabel: { shrink: true } }}
            {...register("observedAt")}
            error={Boolean(errors.observedAt)}
            helperText={errors.observedAt?.message ?? " "}
          />
          <TextField
            label="Store (optional)"
            {...register("store")}
            helperText=" "
            sx={{ flexGrow: 1 }}
          />
        </Stack>
        <Box>
          <Button type="submit" variant="contained" disabled={isSubmitting}>
            Add price
          </Button>
        </Box>
      </Stack>
    </Box>
  );
}
