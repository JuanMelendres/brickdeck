"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import {
  Alert,
  Box,
  Container,
  Link as MuiLink,
  Skeleton,
  Stack,
  Typography,
} from "@mui/material";
import { ApiError } from "@/lib/api/client";
import { SetDetail } from "@/features/sets/SetDetail";
import { PartsInventory } from "@/features/sets/PartsInventory";
import { useSetDetail } from "@/features/sets/useSetDetail";
import { useSetParts } from "@/features/sets/useSetParts";
import { useImportInventory } from "@/features/sets/useImportInventory";
import { MissingPartsPanel } from "@/features/missingpieces/MissingPartsPanel";
import { useMissingParts } from "@/features/missingpieces/useMissingParts";
import { PriceTrackingSection } from "@/features/pricing/PriceTrackingSection";
import { useAuth } from "@/features/auth/useAuth";

const PARTS_PAGE_SIZE = 50;

function detailError(error: unknown): string {
  if (error instanceof ApiError || error instanceof Error) return error.message;
  return "Something went wrong while loading this set.";
}

export default function SetDetailPage() {
  const params = useParams<{ setNumber: string }>();
  const setNumber = params.setNumber;

  const [partsPage, setPartsPage] = useState(0);
  const [missingOnly, setMissingOnly] = useState(false);
  const [missingPage, setMissingPage] = useState(0);

  const { status } = useAuth();
  const isAuthenticated = status === "authenticated";

  const detail = useSetDetail(setNumber);
  const parts = useSetParts(setNumber, partsPage, PARTS_PAGE_SIZE);
  const importInventory = useImportInventory(setNumber);
  const missingParts = useMissingParts(setNumber, isAuthenticated, {
    missingOnly,
    page: missingPage,
  });

  const handleMissingOnlyChange = (value: boolean) => {
    setMissingOnly(value);
    setMissingPage(0);
  };

  return (
    <Container maxWidth="lg" sx={{ py: 6 }}>
      <Stack spacing={4}>
        <MuiLink component={Link} href="/sets" variant="body2">
          ← Back to search
        </MuiLink>

        {detail.isFetching ? (
          <Stack spacing={1}>
            <Skeleton variant="text" width={320} height={48} />
            <Skeleton variant="text" width={200} />
            <Skeleton variant="text" width={160} />
          </Stack>
        ) : detail.isError ? (
          <Alert severity="error">{detailError(detail.error)}</Alert>
        ) : detail.data ? (
          <SetDetail set={detail.data} />
        ) : null}

        <Box>
          <Typography variant="h6" component="h2" gutterBottom>
            Parts inventory
          </Typography>
          <PartsInventory
            isFetching={parts.isFetching}
            isError={parts.isError}
            error={parts.error}
            data={parts.data}
            page={partsPage}
            onPageChange={setPartsPage}
            onImport={() => importInventory.mutate()}
            isImporting={importInventory.isPending}
            importError={importInventory.error}
          />
        </Box>

        <Box>
          <Typography variant="h6" component="h2" gutterBottom>
            Missing pieces
          </Typography>
          <MissingPartsPanel
            isAuthenticated={isAuthenticated}
            isLoading={missingParts.isLoading}
            isError={missingParts.isError}
            error={missingParts.error}
            report={missingParts.data}
            missingOnly={missingOnly}
            onMissingOnlyChange={handleMissingOnlyChange}
            onPageChange={setMissingPage}
          />
        </Box>

        <Box>
          <Typography variant="h6" component="h2" gutterBottom>
            Price tracking
          </Typography>
          <PriceTrackingSection
            setNumber={setNumber}
            isAuthenticated={isAuthenticated}
          />
        </Box>
      </Stack>
    </Container>
  );
}
