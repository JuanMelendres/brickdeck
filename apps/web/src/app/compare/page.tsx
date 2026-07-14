"use client";

import { Suspense, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  Box,
  Button,
  Container,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { SetComparisonView } from "@/features/comparison/SetComparisonView";
import { useSetComparison } from "@/features/comparison/useSetComparison";
import type { ComparisonCategory } from "@/lib/types/comparison";

function CompareContent() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  // Committed set numbers drive the query; the inputs are editable drafts.
  const committedA = searchParams.get("a") ?? "";
  const committedB = searchParams.get("b") ?? "";

  const [inputA, setInputA] = useState(committedA);
  const [inputB, setInputB] = useState(committedB);
  const [category, setCategory] = useState<ComparisonCategory | null>(null);
  const [page, setPage] = useState(0);

  const { data, isLoading, isError, error } = useSetComparison(
    committedA,
    committedB,
    { category: category ?? undefined, page },
  );

  const submit = () => {
    const params = new URLSearchParams();
    if (inputA.trim()) params.set("a", inputA.trim());
    if (inputB.trim()) params.set("b", inputB.trim());
    router.replace(`${pathname}?${params.toString()}`);
    setPage(0);
  };

  const handleCategoryChange = (value: ComparisonCategory | null) => {
    setCategory(value);
    setPage(0);
  };

  return (
    <Container maxWidth="lg" sx={{ py: 6 }}>
      <Stack spacing={4}>
        <Typography variant="h4" component="h1">
          Compare sets
        </Typography>

        <Box
          component="form"
          onSubmit={(event) => {
            event.preventDefault();
            submit();
          }}
        >
          <Stack
            direction={{ xs: "column", sm: "row" }}
            spacing={2}
            sx={{ alignItems: { sm: "center" } }}
          >
            <TextField
              label="Set A"
              value={inputA}
              onChange={(event) => setInputA(event.target.value)}
              size="small"
            />
            <TextField
              label="Set B"
              value={inputB}
              onChange={(event) => setInputB(event.target.value)}
              size="small"
            />
            <Button type="submit" variant="contained">
              Compare
            </Button>
          </Stack>
        </Box>

        {committedA && committedB ? (
          <SetComparisonView
            category={category}
            onCategoryChange={handleCategoryChange}
            onPageChange={setPage}
            isLoading={isLoading}
            isError={isError}
            error={error}
            report={data}
          />
        ) : (
          <Typography color="text.secondary">
            Enter two set numbers to compare their parts.
          </Typography>
        )}
      </Stack>
    </Container>
  );
}

export default function ComparePage() {
  return (
    <Suspense>
      <CompareContent />
    </Suspense>
  );
}
