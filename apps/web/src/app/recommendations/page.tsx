"use client";

import { useState } from "react";
import { Box, Container, Stack, Typography } from "@mui/material";
import { RequireAuth } from "@/features/auth/RequireAuth";
import { RecommendationsView } from "@/features/recommendations/RecommendationsView";
import { useBuildableRecommendations } from "@/features/recommendations/useBuildableRecommendations";

export default function RecommendationsPage() {
  const [buildableOnly, setBuildableOnly] = useState(false);
  const [page, setPage] = useState(0);

  const { data, isLoading, isError, error } = useBuildableRecommendations({
    buildableOnly,
    page,
  });

  const handleBuildableOnlyChange = (value: boolean) => {
    setBuildableOnly(value);
    setPage(0);
  };

  return (
    <RequireAuth>
      <Container maxWidth="lg" sx={{ py: 6 }}>
        <Stack spacing={4}>
          <Box>
            <Typography variant="h4" component="h1">
              Build recommendations
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Wishlist sets you can build from your inventory, most complete first.
            </Typography>
          </Box>
          <RecommendationsView
            buildableOnly={buildableOnly}
            onBuildableOnlyChange={handleBuildableOnlyChange}
            onPageChange={setPage}
            isLoading={isLoading}
            isError={isError}
            error={error}
            data={data}
          />
        </Stack>
      </Container>
    </RequireAuth>
  );
}
