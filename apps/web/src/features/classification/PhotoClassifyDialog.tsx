"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Alert,
  Avatar,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Radio,
  RadioGroup,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { applyApiError } from "@/features/auth/formErrors";
import { downscaleImage } from "./imageDownscale";
import type { AddUserPartRequest } from "@/lib/types/collection";
import type { PartClassificationResponse } from "@/lib/types/classification";

const MAX_DIMENSION = 1024;

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

interface PhotoClassifyDialogProps {
  onClassify: (image: File) => Promise<PartClassificationResponse>;
  onConfirm: (values: AddUserPartRequest) => Promise<void>;
  onClose: () => void;
}

export function PhotoClassifyDialog({
  onClassify,
  onConfirm,
  onClose,
}: PhotoClassifyDialogProps) {
  const [classifying, setClassifying] = useState(false);
  const [classifyError, setClassifyError] = useState<string | null>(null);
  const [classification, setClassification] =
    useState<PartClassificationResponse | null>(null);
  const [selectedPartNumber, setSelectedPartNumber] = useState<string | null>(
    null,
  );
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { quantity: "1", storageLocation: "" },
  });

  const handleFileChange = async (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    setClassifying(true);
    setClassifyError(null);
    setClassification(null);
    setSelectedPartNumber(null);

    try {
      const downscaled = await downscaleImage(file, MAX_DIMENSION);
      const result = await onClassify(downscaled);
      setClassification(result);
      setSelectedPartNumber(result.partSuggestions[0]?.partNumber ?? null);
    } catch (error) {
      setClassifyError(
        error instanceof Error ? error.message : "Failed to classify photo",
      );
    } finally {
      setClassifying(false);
    }
  };

  const colorId = classification?.colorSuggestion.colorId ?? null;
  const canConfirm = Boolean(classification && selectedPartNumber && colorId !== null);

  const submit = handleSubmit(async (values) => {
    if (!classification || !selectedPartNumber || colorId === null) {
      return;
    }
    setFormError(null);
    const payload: AddUserPartRequest = {
      externalPartNumber: selectedPartNumber,
      colorExternalId: colorId,
      quantity: Number(values.quantity),
      ...(values.storageLocation
        ? { storageLocation: values.storageLocation }
        : {}),
    };
    try {
      await onConfirm(payload);
      onClose();
    } catch (error) {
      setFormError(applyApiError(error, setError));
    }
  });

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Scan a part</DialogTitle>
      <form onSubmit={submit} noValidate>
        <DialogContent>
          <Stack spacing={2}>
            <Button variant="outlined" component="label" sx={{ alignSelf: "flex-start" }}>
              Choose photo
              <input
                type="file"
                accept="image/*"
                capture="environment"
                data-testid="photo-file-input"
                style={{
                  position: "absolute",
                  width: 1,
                  height: 1,
                  opacity: 0,
                  overflow: "hidden",
                }}
                onChange={handleFileChange}
              />
            </Button>

            {classifying && (
              <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                <CircularProgress size={20} aria-label="Analyzing photo" />
                <Typography variant="body2">Analyzing photo...</Typography>
              </Stack>
            )}

            {classifyError && <Alert severity="error">{classifyError}</Alert>}
            {formError && <Alert severity="error">{formError}</Alert>}

            {classification && (
              <>
                <Typography variant="body2">
                  Color: {classification.colorSuggestion.colorName}
                  {classification.colorSuggestion.confidence != null
                    ? ` (${Math.round(classification.colorSuggestion.confidence * 100)}%)`
                    : ""}
                </Typography>
                {colorId === null && (
                  <Alert severity="warning">
                    This color is not in your catalog yet. Add the part
                    manually with the color id instead.
                  </Alert>
                )}

                <RadioGroup
                  value={selectedPartNumber ?? ""}
                  onChange={(event) => setSelectedPartNumber(event.target.value)}
                >
                  {classification.partSuggestions.map((suggestion) => (
                    <FormControlLabel
                      key={suggestion.partNumber}
                      value={suggestion.partNumber}
                      control={<Radio />}
                      label={
                        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                          {suggestion.referenceImageUrl && (
                            <Avatar
                              src={suggestion.referenceImageUrl}
                              variant="rounded"
                              sx={{ width: 32, height: 32 }}
                            />
                          )}
                          <Box>
                            <Typography variant="body2">
                              {suggestion.partNumber}
                              {suggestion.partNameGuess
                                ? ` — ${suggestion.partNameGuess}`
                                : ""}
                            </Typography>
                          </Box>
                          <Chip
                            size="small"
                            label={
                              suggestion.resolutionStatus === "RESOLVED"
                                ? "In catalog"
                                : "Will be imported"
                            }
                            color={
                              suggestion.resolutionStatus === "RESOLVED"
                                ? "success"
                                : "default"
                            }
                          />
                        </Stack>
                      }
                    />
                  ))}
                </RadioGroup>

                <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
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
              </>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={!canConfirm || isSubmitting}
          >
            Add to collection
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
