"use client";

import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Alert, Box, Button, MenuItem, Stack, TextField } from "@mui/material";
import { applyApiError } from "@/features/auth/formErrors";
import {
  COLLECTION_STATUSES,
  type AddUserSetRequest,
  type CollectionStatus,
} from "@/lib/types/collection";

const schema = z.object({
  setNumber: z.string().trim().min(1, "Set number is required"),
  status: z.enum(["OWNED", "WISHLIST", "BUILT", "IN_PROGRESS"]),
  purchasePrice: z
    .string()
    .optional()
    .refine((v) => !v || (!Number.isNaN(Number(v)) && Number(v) >= 0), {
      message: "Enter a valid price",
    }),
  purchaseDate: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

const STATUS_LABELS: Record<CollectionStatus, string> = {
  OWNED: "Owned",
  WISHLIST: "Wishlist",
  BUILT: "Built",
  IN_PROGRESS: "In progress",
};

interface AddSetToCollectionFormProps {
  onSubmit: (values: AddUserSetRequest) => Promise<void>;
}

export function AddSetToCollectionForm({
  onSubmit,
}: AddSetToCollectionFormProps) {
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
      status: "OWNED",
      purchasePrice: "",
      purchaseDate: "",
    },
  });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    const payload: AddUserSetRequest = {
      setNumber: values.setNumber,
      status: values.status,
      ...(values.purchasePrice
        ? { purchasePrice: Number(values.purchasePrice) }
        : {}),
      ...(values.purchaseDate ? { purchaseDate: values.purchaseDate } : {}),
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
          <Controller
            name="status"
            control={control}
            render={({ field }) => (
              <TextField select label="Status" sx={{ minWidth: 160 }} {...field}>
                {COLLECTION_STATUSES.map((status) => (
                  <MenuItem key={status} value={status}>
                    {STATUS_LABELS[status]}
                  </MenuItem>
                ))}
              </TextField>
            )}
          />
          <TextField
            label="Price"
            type="number"
            slotProps={{ htmlInput: { min: 0, step: "0.01" } }}
            {...register("purchasePrice")}
            error={Boolean(errors.purchasePrice)}
            helperText={errors.purchasePrice?.message ?? " "}
          />
          <TextField
            label="Purchase date"
            type="date"
            slotProps={{ inputLabel: { shrink: true } }}
            {...register("purchaseDate")}
          />
        </Stack>
        <Button
          type="submit"
          variant="contained"
          disabled={isSubmitting}
          sx={{ alignSelf: "flex-start" }}
        >
          Add set
        </Button>
      </Stack>
    </Box>
  );
}
