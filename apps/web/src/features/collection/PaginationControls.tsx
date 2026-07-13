"use client";

import { Button, Stack, Typography } from "@mui/material";

interface PaginationControlsProps {
  /** Current 0-indexed page. */
  page: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  onPageChange: (page: number) => void;
}

/**
 * Prev/Next pager for paginated collection lists. Renders nothing while the
 * result fits on a single page. Mirrors the missing-pieces panel idiom.
 */
export function PaginationControls({
  page,
  totalPages,
  first,
  last,
  onPageChange,
}: PaginationControlsProps) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <Stack
      direction="row"
      spacing={2}
      sx={{ justifyContent: "center", alignItems: "center" }}
    >
      <Button
        size="small"
        disabled={first}
        onClick={() => onPageChange(page - 1)}
      >
        Previous
      </Button>
      <Typography variant="body2">
        Page {page + 1} of {totalPages}
      </Typography>
      <Button size="small" disabled={last} onClick={() => onPageChange(page + 1)}>
        Next
      </Button>
    </Stack>
  );
}
