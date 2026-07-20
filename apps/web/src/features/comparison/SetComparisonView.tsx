"use client";

import {
  Alert,
  Box,
  Chip,
  LinearProgress,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from "@mui/material";
import { ApiError } from "@/lib/api/client";
import { PaginationControls } from "@/features/collection/PaginationControls";
import type {
  ComparisonCategory,
  SetComparisonReport,
} from "@/lib/types/comparison";

interface SetComparisonViewProps {
  category: ComparisonCategory | null;
  onCategoryChange: (category: ComparisonCategory | null) => void;
  onPageChange: (page: number) => void;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  report: SetComparisonReport | undefined;
}

const CATEGORY_LABEL: Record<ComparisonCategory, string> = {
  BOTH: "Both",
  ONLY_A: "Only A",
  ONLY_B: "Only B",
};

export function SetComparisonView({
  category,
  onCategoryChange,
  onPageChange,
  isLoading,
  isError,
  error,
  report,
}: SetComparisonViewProps) {
  return (
    <Stack spacing={2}>
      {report && (
        <Stack
          direction="row"
          spacing={2}
          sx={{ justifyContent: "center", alignItems: "center" }}
        >
          <Typography variant="h6">{report.setNumberA}</Typography>
          <Typography variant="body2" color="text.secondary">
            vs
          </Typography>
          <Typography variant="h6">{report.setNumberB}</Typography>
        </Stack>
      )}

      <ToggleButtonGroup
        exclusive
        size="small"
        value={category ?? "ALL"}
        onChange={(_event, value) => {
          if (value === null) {
            return;
          }
          onCategoryChange(value === "ALL" ? null : (value as ComparisonCategory));
        }}
        aria-label="Filter parts"
      >
        <ToggleButton value="ALL">All</ToggleButton>
        <ToggleButton value="BOTH">{CATEGORY_LABEL.BOTH}</ToggleButton>
        <ToggleButton value="ONLY_A">{CATEGORY_LABEL.ONLY_A}</ToggleButton>
        <ToggleButton value="ONLY_B">{CATEGORY_LABEL.ONLY_B}</ToggleButton>
      </ToggleButtonGroup>

      {renderContent(isLoading, isError, error, report, onPageChange)}
    </Stack>
  );
}

function renderContent(
  isLoading: boolean,
  isError: boolean,
  error: unknown,
  report: SetComparisonReport | undefined,
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
          Import both sets&apos; inventories to compare them.
        </Alert>
      );
    }
    const message =
      error instanceof Error ? error.message : "Failed to compare the sets.";
    return <Alert severity="error">{message}</Alert>;
  }

  if (!report) {
    return null;
  }

  const similarity = Math.round(
    Math.min(1, Math.max(0, report.similarityScore)) * 100,
  );

  return (
    <Stack spacing={2}>
      <Box>
        <Stack
          direction="row"
          sx={{ justifyContent: "space-between", alignItems: "baseline" }}
        >
          <Typography variant="body2" color="text.secondary">
            {similarity}% similar
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {report.sharedLineCount} shared &middot; {report.onlyALineCount} only A
            &middot; {report.onlyBLineCount} only B
          </Typography>
        </Stack>
        <LinearProgress
          variant="determinate"
          value={similarity}
          sx={{ height: 10, borderRadius: 1, mt: 0.5 }}
        />
      </Box>

      {report.lines.length === 0 ? (
        <Typography color="text.secondary">
          No parts to show for this filter.
        </Typography>
      ) : (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Part</TableCell>
                <TableCell>Number</TableCell>
                <TableCell>Color</TableCell>
                <TableCell align="right">Set A</TableCell>
                <TableCell align="right">Set B</TableCell>
                <TableCell align="right">Shared</TableCell>
                <TableCell>In</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {report.lines.map((line, index) => (
                <TableRow
                  key={`${line.partNumber}-${line.colorExternalId}-${index}`}
                >
                  <TableCell>{line.partName ?? "—"}</TableCell>
                  <TableCell>{line.partNumber ?? "—"}</TableCell>
                  <TableCell>{line.colorName ?? "—"}</TableCell>
                  <TableCell align="right">{line.quantityA}</TableCell>
                  <TableCell align="right">{line.quantityB}</TableCell>
                  <TableCell align="right">{line.shared}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={CATEGORY_LABEL[line.category]}
                      variant="outlined"
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <PaginationControls
        page={report.page}
        totalPages={report.totalPages}
        first={report.first}
        last={report.last}
        onPageChange={onPageChange}
      />
    </Stack>
  );
}
