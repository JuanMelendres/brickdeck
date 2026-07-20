"use client";

import { useState } from "react";
import NextLink from "next/link";
import { Link, Stack, Typography } from "@mui/material";
import { AddPriceSnapshotForm, type PriceSnapshotFormValues } from "./AddPriceSnapshotForm";
import { PriceAnalysisPanel } from "./PriceAnalysisPanel";
import { useAddPriceSnapshot, usePriceAnalysis } from "./pricingHooks";

interface PriceTrackingSectionProps {
  setNumber: string;
  isAuthenticated: boolean;
}

export function PriceTrackingSection({
  setNumber,
  isAuthenticated,
}: PriceTrackingSectionProps) {
  const [currency, setCurrency] = useState("USD");
  const [candidatePrice, setCandidatePrice] = useState("");

  const candidate = Number(candidatePrice);
  const candidateForQuery =
    candidatePrice !== "" && !Number.isNaN(candidate) && candidate > 0
      ? candidate
      : undefined;

  const analysis = usePriceAnalysis(
    setNumber,
    { currency: currency.toUpperCase(), candidatePrice: candidateForQuery },
    isAuthenticated,
  );
  const addSnapshot = useAddPriceSnapshot();

  if (!isAuthenticated) {
    return (
      <Typography color="text.secondary">
        <Link component={NextLink} href="/login">
          Log in
        </Link>{" "}
        to track prices and spot deals for this set.
      </Typography>
    );
  }

  const handleAdd = async (values: PriceSnapshotFormValues) => {
    await addSnapshot.mutateAsync({ ...values, setNumber });
  };

  return (
    <Stack spacing={3}>
      <AddPriceSnapshotForm onSubmit={handleAdd} />
      <PriceAnalysisPanel
        currency={currency}
        candidatePrice={candidatePrice}
        onCurrencyChange={setCurrency}
        onCandidatePriceChange={setCandidatePrice}
        isLoading={analysis.isLoading}
        isError={analysis.isError}
        error={analysis.error}
        data={analysis.data}
      />
    </Stack>
  );
}
