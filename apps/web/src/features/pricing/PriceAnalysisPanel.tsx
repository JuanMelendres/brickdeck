"use client";

import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { ApiError } from "@/lib/api/client";
import type { DealVerdict, PriceAnalysisResponse } from "@/lib/types/pricing";

interface PriceAnalysisPanelProps {
  currency: string;
  candidatePrice: string;
  onCurrencyChange: (value: string) => void;
  onCandidatePriceChange: (value: string) => void;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  data: PriceAnalysisResponse | undefined;
}

const VERDICT: Record<DealVerdict, { label: string; color: "success" | "warning" | "default" | "error" }> = {
  GREAT_DEAL: { label: "Great deal", color: "success" },
  GOOD_DEAL: { label: "Good deal", color: "success" },
  FAIR: { label: "Fair", color: "warning" },
  POOR: { label: "Poor", color: "error" },
};

export function PriceAnalysisPanel({
  currency,
  candidatePrice,
  onCurrencyChange,
  onCandidatePriceChange,
  isLoading,
  isError,
  error,
  data,
}: PriceAnalysisPanelProps) {
  return (
    <Stack spacing={2}>
      <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
        <TextField
          label="Currency"
          value={currency}
          onChange={(e) => onCurrencyChange(e.target.value)}
          sx={{ maxWidth: 120 }}
        />
        <TextField
          label="Candidate price"
          type="number"
          value={candidatePrice}
          onChange={(e) => onCandidatePriceChange(e.target.value)}
          slotProps={{ htmlInput: { step: "0.01", min: "0" } }}
          helperText="Optional — check if a price is a good deal"
        />
      </Stack>
      {renderContent(isLoading, isError, error, data)}
    </Stack>
  );
}

function renderContent(
  isLoading: boolean,
  isError: boolean,
  error: unknown,
  data: PriceAnalysisResponse | undefined,
) {
  if (isLoading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 3 }}>
        <CircularProgress aria-label="Loading" />
      </Box>
    );
  }

  if (isError) {
    if (error instanceof ApiError && error.status === 404) {
      return (
        <Alert severity="info">
          No price data yet for this currency. Add a snapshot to start tracking.
        </Alert>
      );
    }
    const message =
      error instanceof Error ? error.message : "Failed to load price analysis.";
    return <Alert severity="error">{message}</Alert>;
  }

  if (!data) {
    return null;
  }

  return (
    <Stack spacing={1.5}>
      <Stack direction="row" spacing={3} sx={{ flexWrap: "wrap", rowGap: 1 }}>
        <Metric label="Lowest" value={`${data.minAmount} ${data.currency}`} />
        <Metric label="Average" value={`${data.averageAmount} ${data.currency}`} />
        <Metric label="Highest" value={`${data.maxAmount} ${data.currency}`} />
        <Metric label="Latest" value={`${data.latestAmount} ${data.currency}`} />
        {data.pricePerPiece != null && (
          <Metric label="Per piece" value={`${data.pricePerPiece} ${data.currency}`} />
        )}
        <Metric label="Snapshots" value={String(data.snapshotCount)} />
      </Stack>

      {data.candidate && (
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          <Chip
            color={VERDICT[data.candidate.verdict].color}
            label={VERDICT[data.candidate.verdict].label}
          />
          <Typography variant="body2" color="text.secondary">
            {data.candidate.amount} {data.currency} ·{" "}
            {data.candidate.percentBelowAverage >= 0
              ? `${data.candidate.percentBelowAverage}% below average`
              : `${Math.abs(data.candidate.percentBelowAverage)}% above average`}
            {data.candidate.atOrBelowLowest ? " · at/below your lowest" : ""}
          </Typography>
        </Stack>
      )}
    </Stack>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
        {label}
      </Typography>
      <Typography variant="body1">{value}</Typography>
    </Box>
  );
}
