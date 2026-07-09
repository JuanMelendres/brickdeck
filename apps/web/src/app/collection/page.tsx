"use client";

import { useState } from "react";
import {
  Alert,
  Box,
  CircularProgress,
  Container,
  Stack,
  Typography,
} from "@mui/material";
import { RequireAuth } from "@/features/auth/RequireAuth";
import { AddSetToCollectionForm } from "@/features/collection/AddSetToCollectionForm";
import { CollectionSetsList } from "@/features/collection/CollectionSetsList";
import {
  useAddCollectionSet,
  useCollectionSets,
  useRemoveCollectionSet,
} from "@/features/collection/collectionSetsHooks";
import type { AddUserSetRequest } from "@/lib/types/collection";

const PAGE_SIZE = 20;

function CollectionContent() {
  const [page] = useState(0);
  const { data, isLoading, isError, error } = useCollectionSets(page, PAGE_SIZE);
  const addSet = useAddCollectionSet();
  const removeSet = useRemoveCollectionSet();

  const handleAdd = async (values: AddUserSetRequest) => {
    await addSet.mutateAsync(values);
  };

  return (
    <Container maxWidth="lg" sx={{ py: 6 }}>
      <Stack spacing={4}>
        <Typography variant="h4" component="h1">
          My collection
        </Typography>

        <AddSetToCollectionForm onSubmit={handleAdd} />

        {isLoading && (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
            <CircularProgress aria-label="Loading" />
          </Box>
        )}
        {isError && (
          <Alert severity="error">
            {error instanceof Error
              ? error.message
              : "Failed to load your collection."}
          </Alert>
        )}
        {data && (
          <CollectionSetsList
            sets={data.content}
            onRemove={(id) => removeSet.mutate(id)}
            removingId={removeSet.isPending ? removeSet.variables : null}
          />
        )}
      </Stack>
    </Container>
  );
}

export default function CollectionPage() {
  return (
    <RequireAuth>
      <CollectionContent />
    </RequireAuth>
  );
}
