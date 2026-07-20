"use client";

import {
  Alert,
  Box,
  Chip,
  FormControlLabel,
  LinearProgress,
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
import { PaginationControls } from "@/features/collection/PaginationControls";
import type { PageResponse } from "@/lib/types/api";
import type { BuildableSetRecommendation } from "@/lib/types/recommendations";

interface RecommendationsViewProps {
  buildableOnly: boolean;
  onBuildableOnlyChange: (value: boolean) => void;
  onPageChange: (page: number) => void;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  data: PageResponse<BuildableSetRecommendation> | undefined;
}

export function RecommendationsView({
  buildableOnly,
  onBuildableOnlyChange,
  onPageChange,
  isLoading,
  isError,
  error,
  data,
}: RecommendationsViewProps) {
  return (
    <Stack spacing={2}>
      <FormControlLabel
        control={
          <Switch
            checked={buildableOnly}
            onChange={(event) => onBuildableOnlyChange(event.target.checked)}
            slotProps={{ input: { "aria-label": "Buildable only" } }}
          />
        }
        label="Buildable only"
      />
      {renderContent(isLoading, isError, error, data, onPageChange)}
    </Stack>
  );
}

function renderContent(
  isLoading: boolean,
  isError: boolean,
  error: unknown,
  data: PageResponse<BuildableSetRecommendation> | undefined,
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
    const message =
      error instanceof Error ? error.message : "Failed to load recommendations.";
    return <Alert severity="error">{message}</Alert>;
  }

  if (!data) {
    return null;
  }

  if (data.content.length === 0) {
    return (
      <Typography color="text.secondary">
        No wishlist sets to recommend yet. Add sets to your wishlist and import
        their inventories to see what you can build.
      </Typography>
    );
  }

  return (
    <Stack spacing={2}>
      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Set</TableCell>
              <TableCell>Number</TableCell>
              <TableCell>Theme</TableCell>
              <TableCell>Completion</TableCell>
              <TableCell align="right">Owned</TableCell>
              <TableCell align="right">Missing</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data.content.map((rec) => {
              const completion = Math.min(100, Math.max(0, rec.completionPercentage));
              return (
                <TableRow key={rec.setNumber}>
                  <TableCell>{rec.name ?? "—"}</TableCell>
                  <TableCell>{rec.setNumber}</TableCell>
                  <TableCell>{rec.themeName ?? "—"}</TableCell>
                  <TableCell>
                    <Stack spacing={0.5} sx={{ minWidth: 120 }}>
                      <Typography variant="caption" color="text.secondary">
                        {completion}%
                      </Typography>
                      <LinearProgress
                        variant="determinate"
                        value={completion}
                        sx={{ height: 8, borderRadius: 1 }}
                      />
                    </Stack>
                  </TableCell>
                  <TableCell align="right">
                    {rec.totalOwned} / {rec.totalRequired}
                  </TableCell>
                  <TableCell align="right">{rec.totalMissing}</TableCell>
                  <TableCell>
                    {rec.buildable ? (
                      <Chip size="small" color="success" label="Buildable" />
                    ) : (
                      <Chip size="small" variant="outlined" label="Incomplete" />
                    )}
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>

      <PaginationControls
        page={data.page}
        totalPages={data.totalPages}
        first={data.first}
        last={data.last}
        onPageChange={onPageChange}
      />
    </Stack>
  );
}
