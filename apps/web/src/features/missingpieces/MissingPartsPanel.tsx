"use client";

import NextLink from "next/link";
import {
  Alert,
  Box,
  Button,
  FormControlLabel,
  LinearProgress,
  Link,
  Paper,
  Stack,
  Switch,
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
  missingOnly: boolean;
  onMissingOnlyChange: (value: boolean) => void;
  onPageChange: (page: number) => void;
}

export function MissingPartsPanel({
  isAuthenticated,
  isLoading,
  isError,
  error,
  report,
  missingOnly,
  onMissingOnlyChange,
  onPageChange,
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

  return (
    <Stack spacing={2}>
      <FormControlLabel
        control={
          <Switch
            checked={missingOnly}
            onChange={(event) => onMissingOnlyChange(event.target.checked)}
            slotProps={{ input: { "aria-label": "Only missing" } }}
          />
        }
        label="Only missing"
      />
      {renderContent(isLoading, isError, error, report, onPageChange)}
    </Stack>
  );
}

function renderContent(
  isLoading: boolean,
  isError: boolean,
  error: unknown,
  report: MissingPartsReport | undefined,
  onPageChange: (page: number) => void,
) {
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

      {report.lines.length === 0 ? (
        <Typography color="text.secondary">
          No pieces to show for this filter.
        </Typography>
      ) : (
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
      )}

      {report.totalPages > 1 && (
        <Stack
          direction="row"
          spacing={2}
          sx={{ justifyContent: "center", alignItems: "center" }}
        >
          <Button
            size="small"
            disabled={report.first}
            onClick={() => onPageChange(report.page - 1)}
          >
            Previous
          </Button>
          <Typography variant="body2">
            Page {report.page + 1} of {report.totalPages}
          </Typography>
          <Button
            size="small"
            disabled={report.last}
            onClick={() => onPageChange(report.page + 1)}
          >
            Next
          </Button>
        </Stack>
      )}
    </Stack>
  );
}
