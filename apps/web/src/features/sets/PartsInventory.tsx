import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
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
import type { PageResponse, SetPartResponse } from "@/lib/types/api";

interface PartsInventoryProps {
  isFetching: boolean;
  isError: boolean;
  error: unknown;
  data: PageResponse<SetPartResponse> | undefined;
  page: number;
  onPageChange: (page: number) => void;
  onImport: () => void;
  isImporting: boolean;
  importError: unknown;
}

function errorMessage(error: unknown): string {
  if (error instanceof ApiError || error instanceof Error) return error.message;
  return "Something went wrong while loading the inventory.";
}

export function PartsInventory({
  isFetching,
  isError,
  error,
  data,
  page,
  onPageChange,
  onImport,
  isImporting,
  importError,
}: PartsInventoryProps) {
  if (isFetching) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 6 }}>
        <CircularProgress aria-label="Loading inventory" />
      </Box>
    );
  }

  if (isError) {
    return <Alert severity="error">{errorMessage(error)}</Alert>;
  }

  if (!data || data.content.length === 0) {
    return (
      <Stack spacing={2} sx={{ alignItems: "flex-start" }}>
        <Typography color="text.secondary">
          No inventory imported yet for this set.
        </Typography>
        {importError ? (
          <Alert severity="error">{errorMessage(importError)}</Alert>
        ) : null}
        <Button
          variant="contained"
          onClick={onImport}
          disabled={isImporting}
        >
          {isImporting ? "Importing…" : "Import inventory"}
        </Button>
      </Stack>
    );
  }

  return (
    <Stack spacing={2}>
      <TableContainer component={Paper} variant="outlined">
        <Table size="small" aria-label="Set parts inventory">
          <TableHead>
            <TableRow>
              <TableCell>Part</TableCell>
              <TableCell>Number</TableCell>
              <TableCell>Color</TableCell>
              <TableCell align="right">Qty</TableCell>
              <TableCell>Spare</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data.content.map((part) => (
              <TableRow key={part.id}>
                <TableCell>{part.partName}</TableCell>
                <TableCell>{part.partNumber}</TableCell>
                <TableCell>{part.colorName ?? "—"}</TableCell>
                <TableCell align="right">{part.quantity}</TableCell>
                <TableCell>
                  {part.spare ? <Chip label="Spare" size="small" /> : ""}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {data.totalPages > 1 ? (
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <Button disabled={data.first} onClick={() => onPageChange(page - 1)}>
            Previous
          </Button>
          <Typography variant="body2" color="text.secondary">
            Page {data.page + 1} of {data.totalPages}
          </Typography>
          <Button disabled={data.last} onClick={() => onPageChange(page + 1)}>
            Next
          </Button>
        </Box>
      ) : null}
    </Stack>
  );
}
