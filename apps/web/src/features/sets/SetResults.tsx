import { Alert, Box, CircularProgress, Typography } from "@mui/material";
import { ApiError } from "@/lib/api/client";
import type { BrickSetResponse, PageResponse } from "@/lib/types/api";
import { SetCard } from "./SetCard";

interface SetResultsProps {
  hasQuery: boolean;
  isFetching: boolean;
  isError: boolean;
  error: unknown;
  data: PageResponse<BrickSetResponse> | undefined;
}

function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return "Something went wrong while searching. Please try again.";
}

export function SetResults({
  hasQuery,
  isFetching,
  isError,
  error,
  data,
}: SetResultsProps) {
  if (!hasQuery) {
    return (
      <Typography color="text.secondary">
        Search for a LEGO set by name or number to get started.
      </Typography>
    );
  }

  if (isFetching) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 6 }}>
        <CircularProgress aria-label="Loading sets" />
      </Box>
    );
  }

  if (isError) {
    return <Alert severity="error">{errorMessage(error)}</Alert>;
  }

  if (!data || data.content.length === 0) {
    return <Typography color="text.secondary">No sets found.</Typography>;
  }

  return (
    <Box
      sx={{
        display: "grid",
        gap: 2,
        gridTemplateColumns: {
          xs: "1fr",
          sm: "repeat(2, 1fr)",
          md: "repeat(3, 1fr)",
        },
      }}
    >
      {data.content.map((set) => (
        <SetCard key={set.externalSetNumber} set={set} />
      ))}
    </Box>
  );
}
