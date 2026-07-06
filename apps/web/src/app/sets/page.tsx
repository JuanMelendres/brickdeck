"use client";

import { useState } from "react";
import { Box, Button, Container, Stack, Typography } from "@mui/material";
import { SetSearchBar } from "@/features/sets/SetSearchBar";
import { SetResults } from "@/features/sets/SetResults";
import { useSetSearch } from "@/features/sets/useSetSearch";

const PAGE_SIZE = 20;

export default function SetsPage() {
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);

  const { data, isFetching, isError, error } = useSetSearch(
    query,
    page,
    PAGE_SIZE,
  );

  const hasQuery = query.trim().length > 0;

  const handleSearch = (next: string) => {
    setQuery(next);
    setPage(0);
  };

  return (
    <Container maxWidth="lg" sx={{ py: 6 }}>
      <Stack spacing={4}>
        <Typography variant="h4" component="h1">
          Search LEGO sets
        </Typography>

        <SetSearchBar onSearch={handleSearch} defaultQuery={query} />

        <SetResults
          hasQuery={hasQuery}
          isFetching={isFetching}
          isError={isError}
          error={error}
          data={data}
        />

        {data && data.totalPages > 1 ? (
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
            }}
          >
            <Button
              disabled={data.first || isFetching}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </Button>
            <Typography variant="body2" color="text.secondary">
              Page {data.page + 1} of {data.totalPages}
            </Typography>
            <Button
              disabled={data.last || isFetching}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </Box>
        ) : null}
      </Stack>
    </Container>
  );
}
