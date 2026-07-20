"use client";

import { useState } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Alert, Box, Button, MenuItem, Stack, TextField } from "@mui/material";
import { applyApiError } from "@/features/auth/formErrors";
import type {
  AddPriceAlertRuleRequest,
  PriceAlertType,
} from "@/lib/types/alerts";

const TYPE_LABELS: Record<PriceAlertType, string> = {
  BELOW_TARGET_PRICE: "Below target price",
  PERCENT_BELOW_AVERAGE: "Percent below average",
  AT_OR_BELOW_LOWEST: "At or below lowest",
};

const NEEDS_THRESHOLD: PriceAlertType[] = [
  "BELOW_TARGET_PRICE",
  "PERCENT_BELOW_AVERAGE",
];

const schema = z
  .object({
    setNumber: z.string().trim().min(1, "Set number is required"),
    currency: z.string().trim().regex(/^[A-Za-z]{3}$/, "Use a 3-letter code"),
    type: z.enum([
      "BELOW_TARGET_PRICE",
      "PERCENT_BELOW_AVERAGE",
      "AT_OR_BELOW_LOWEST",
    ]),
    thresholdValue: z.string().optional(),
  })
  .superRefine((values, ctx) => {
    if (!NEEDS_THRESHOLD.includes(values.type)) {
      return;
    }
    const n = Number(values.thresholdValue);
    if (!values.thresholdValue || Number.isNaN(n) || n <= 0) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["thresholdValue"],
        message: "Threshold is required and must be greater than 0",
      });
    } else if (values.type === "PERCENT_BELOW_AVERAGE" && n > 100) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["thresholdValue"],
        message: "Percent must be 100 or less",
      });
    }
  });

type FormValues = z.infer<typeof schema>;

interface CreateAlertRuleFormProps {
  onSubmit: (values: AddPriceAlertRuleRequest) => Promise<void>;
}

export function CreateAlertRuleForm({ onSubmit }: CreateAlertRuleFormProps) {
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
      setNumber: "",
      currency: "USD",
      type: "BELOW_TARGET_PRICE",
      thresholdValue: "",
    },
  });

  const type = useWatch({ control, name: "type" });
  const needsThreshold = NEEDS_THRESHOLD.includes(type);

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    const payload: AddPriceAlertRuleRequest = {
      setNumber: values.setNumber,
      currency: values.currency.toUpperCase(),
      type: values.type,
      ...(NEEDS_THRESHOLD.includes(values.type)
        ? { thresholdValue: Number(values.thresholdValue) }
        : {}),
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
            label="Set number"
            placeholder="75257-1"
            {...register("setNumber")}
            error={Boolean(errors.setNumber)}
            helperText={errors.setNumber?.message ?? " "}
          />
          <TextField
            label="Currency"
            {...register("currency")}
            error={Boolean(errors.currency)}
            helperText={errors.currency?.message ?? " "}
            sx={{ maxWidth: 120 }}
          />
        </Stack>
        <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
          <Controller
            name="type"
            control={control}
            render={({ field }) => (
              <TextField select label="Alert type" sx={{ minWidth: 220 }} {...field}>
                {(Object.keys(TYPE_LABELS) as PriceAlertType[]).map((t) => (
                  <MenuItem key={t} value={t}>
                    {TYPE_LABELS[t]}
                  </MenuItem>
                ))}
              </TextField>
            )}
          />
          <TextField
            label="Threshold"
            type="number"
            disabled={!needsThreshold}
            slotProps={{ htmlInput: { step: "0.01", min: "0" } }}
            {...register("thresholdValue")}
            error={Boolean(errors.thresholdValue)}
            helperText={
              errors.thresholdValue?.message ??
              (needsThreshold
                ? type === "PERCENT_BELOW_AVERAGE"
                  ? "Percent (0-100)"
                  : "Target price"
                : "Not needed for this type")
            }
          />
        </Stack>
        <Box>
          <Button type="submit" variant="contained" disabled={isSubmitting}>
            Create alert
          </Button>
        </Box>
      </Stack>
    </Box>
  );
}
