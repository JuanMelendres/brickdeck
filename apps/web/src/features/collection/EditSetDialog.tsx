"use client";

import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { applyApiError } from "@/features/auth/formErrors";
import {
  COLLECTION_STATUSES,
  type CollectionStatus,
  type UpdateUserSetRequest,
  type UserSetResponse,
} from "@/lib/types/collection";

const schema = z.object({
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

interface EditSetDialogProps {
  set: UserSetResponse;
  onSubmit: (id: string, values: UpdateUserSetRequest) => Promise<void>;
  onClose: () => void;
}

export function EditSetDialog({ set, onSubmit, onClose }: EditSetDialogProps) {
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    control,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      status: set.status ?? "OWNED",
      purchasePrice: set.purchasePrice != null ? String(set.purchasePrice) : "",
      purchaseDate: set.purchaseDate ?? "",
    },
  });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    // PATCH is partial and cannot clear values to null, so only send filled
    // fields. Cleared inputs are omitted rather than sent as null.
    const payload: UpdateUserSetRequest = {
      status: values.status,
      ...(values.purchasePrice
        ? { purchasePrice: Number(values.purchasePrice) }
        : {}),
      ...(values.purchaseDate ? { purchaseDate: values.purchaseDate } : {}),
    };
    try {
      await onSubmit(set.id, payload);
      onClose();
    } catch (error) {
      setFormError(applyApiError(error, setError));
    }
  });

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>Edit set</DialogTitle>
      <form onSubmit={submit} noValidate>
        <DialogContent>
          <Stack spacing={2}>
            {formError && <Alert severity="error">{formError}</Alert>}
            <Typography variant="subtitle1">
              {set.setName ?? set.setNumber ?? "Set"}
            </Typography>
            <Controller
              name="status"
              control={control}
              render={({ field }) => (
                <TextField select label="Status" {...field}>
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
              helperText={
                errors.purchasePrice?.message ?? "Leave blank to keep unchanged"
              }
            />
            <TextField
              label="Purchase date"
              type="date"
              slotProps={{ inputLabel: { shrink: true } }}
              {...register("purchaseDate")}
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
