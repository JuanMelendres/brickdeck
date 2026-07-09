"use client";

import NextLink from "next/link";
import {
  Alert,
  Box,
  LinearProgress,
  Link,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import { ApiError } from "@/lib/api/client";
import type { MissingPartsReport } from "@/lib/types/missingParts";

interface MissingPartsPanelProps {
  isAuthenticated: boolean;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  report: MissingPartsReport | undefined;
}

export function MissingPartsPanel({
  isAuthenticated,
  isLoading,
  isError,
  error,
  report,
}: MissingPartsPanelProps) {
  if (!isAuthenticated) {
    return (
      <Typography color="text.secondary">
        <Link component={NextLink} href="/login">
          Log in
        </Link>{" "}
        to see how close you are to completing this set.
      </Typography>
    );
  }

  if (isLoading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
        <LinearProgress sx={{ width: "100%" }} aria-label="Loading" />
      </Box>
    );
  }

  if (isError) {
    if (error instanceof ApiError && error.status === 404) {
      return (
        <Alert severity="info">
          Import this set&apos;s inventory to see which pieces you are missing.
        </Alert>
      );
    }
    const message =
      error instanceof Error
        ? error.message
        : "Failed to load the missing-pieces report.";
    return <Alert severity="error">{message}</Alert>;
  }

  if (!report) {
    return null;
  }

  const completion = Math.min(100, Math.max(0, report.completionPercentage));

  return (
    <Stack spacing={2}>
      <Box>
        <Stack
          direction="row"
          sx={{ justifyContent: "space-between", alignItems: "baseline" }}
        >
          <Typography variant="body2" color="text.secondary">
            {completion}% complete
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {report.totalMissing} of {report.totalRequired} pieces still needed
          </Typography>
        </Stack>
        <LinearProgress
          variant="determinate"
          value={completion}
          sx={{ height: 10, borderRadius: 1, mt: 0.5 }}
        />
      </Box>

      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Part</TableCell>
              <TableCell>Number</TableCell>
              <TableCell>Color</TableCell>
              <TableCell align="right">Required</TableCell>
              <TableCell align="right">Owned</TableCell>
              <TableCell align="right">Missing</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {report.lines.map((line, index) => (
              <TableRow
                key={`${line.partNumber}-${line.colorExternalId}-${index}`}
                sx={
                  line.missing > 0
                    ? { backgroundColor: "action.hover" }
                    : undefined
                }
              >
                <TableCell>{line.partName ?? "—"}</TableCell>
                <TableCell>{line.partNumber ?? "—"}</TableCell>
                <TableCell>{line.colorName ?? "—"}</TableCell>
                <TableCell align="right">{line.required}</TableCell>
                <TableCell align="right">{line.owned}</TableCell>
                <TableCell align="right">
                  {line.missing > 0 ? `${line.missing} missing` : "0"}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Stack>
  );
}
