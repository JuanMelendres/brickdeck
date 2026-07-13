"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { applyApiError } from "@/features/auth/formErrors";
import type {
  UpdateUserPartRequest,
  UserPartResponse,
} from "@/lib/types/collection";

const schema = z.object({
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

interface EditPartDialogProps {
  part: UserPartResponse;
  onSubmit: (id: string, values: UpdateUserPartRequest) => Promise<void>;
  onClose: () => void;
}

export function EditPartDialog({
  part,
  onSubmit,
  onClose,
}: EditPartDialogProps) {
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      quantity: part.quantity != null ? String(part.quantity) : "1",
      storageLocation: part.storageLocation ?? "",
    },
  });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    // PATCH is partial and cannot clear values to null, so cleared storage is
    // omitted rather than sent as null.
    const payload: UpdateUserPartRequest = {
      quantity: Number(values.quantity),
      ...(values.storageLocation
        ? { storageLocation: values.storageLocation }
        : {}),
    };
    try {
      await onSubmit(part.id, payload);
      onClose();
    } catch (error) {
      setFormError(applyApiError(error, setError));
    }
  });

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>Edit loose part</DialogTitle>
      <form onSubmit={submit} noValidate>
        <DialogContent>
          <Stack spacing={2}>
            {formError && <Alert severity="error">{formError}</Alert>}
            <Typography variant="subtitle1">
              {part.partName ?? part.partNumber ?? "Part"}
              {part.colorName ? ` — ${part.colorName}` : ""}
            </Typography>
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
              helperText="Leave blank to keep unchanged"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={isSubmitting}>
            Save
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
