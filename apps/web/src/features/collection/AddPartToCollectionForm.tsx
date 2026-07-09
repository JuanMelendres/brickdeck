"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Alert, Box, Button, Stack, TextField } from "@mui/material";
import { applyApiError } from "@/features/auth/formErrors";
import type { AddUserPartRequest } from "@/lib/types/collection";

const schema = z.object({
  externalPartNumber: z.string().trim().min(1, "Part number is required"),
  colorExternalId: z
    .string()
    .trim()
    .min(1, "Color id is required")
    .refine((v) => Number.isInteger(Number(v)), "Enter a valid color id"),
  quantity: z
    .string()
    .trim()
    .min(1, "Quantity is required")
    .refine(
      (v) => Number.isInteger(Number(v)) && Number(v) >= 1,
      "Enter a quantity of at least 1",
    ),
  storageLocation: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

interface AddPartToCollectionFormProps {
  onSubmit: (values: AddUserPartRequest) => Promise<void>;
}

export function AddPartToCollectionForm({
  onSubmit,
}: AddPartToCollectionFormProps) {
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      externalPartNumber: "",
      colorExternalId: "",
      quantity: "1",
      storageLocation: "",
    },
  });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    const payload: AddUserPartRequest = {
      externalPartNumber: values.externalPartNumber,
      colorExternalId: Number(values.colorExternalId),
      quantity: Number(values.quantity),
      ...(values.storageLocation
        ? { storageLocation: values.storageLocation }
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
            label="Part number"
            placeholder="3001"
            {...register("externalPartNumber")}
            error={Boolean(errors.externalPartNumber)}
            helperText={errors.externalPartNumber?.message ?? " "}
          />
          <TextField
            label="Color id"
            type="number"
            {...register("colorExternalId")}
            error={Boolean(errors.colorExternalId)}
            helperText={errors.colorExternalId?.message ?? " "}
          />
          <TextField
            label="Quantity"
            type="number"
            slotProps={{ htmlInput: { min: 1, step: 1 } }}
            {...register("quantity")}
            error={Boolean(errors.quantity)}
            helperText={errors.quantity?.message ?? " "}
          />
          <TextField
            label="Storage location"
            {...register("storageLocation")}
          />
        </Stack>
        <Button
          type="submit"
          variant="contained"
          disabled={isSubmitting}
          sx={{ alignSelf: "flex-start" }}
        >
          Add part
        </Button>
      </Stack>
    </Box>
  );
}
