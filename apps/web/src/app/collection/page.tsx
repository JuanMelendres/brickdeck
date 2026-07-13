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
import { EditSetDialog } from "@/features/collection/EditSetDialog";
import { AddPartToCollectionForm } from "@/features/collection/AddPartToCollectionForm";
import { CollectionPartsList } from "@/features/collection/CollectionPartsList";
import { PaginationControls } from "@/features/collection/PaginationControls";
import {
  useAddCollectionSet,
  useCollectionSets,
  useRemoveCollectionSet,
  useUpdateCollectionSet,
} from "@/features/collection/collectionSetsHooks";
import {
  useAddCollectionPart,
  useCollectionParts,
  useRemoveCollectionPart,
} from "@/features/collection/collectionPartsHooks";
import type {
  AddUserPartRequest,
  AddUserSetRequest,
  UpdateUserSetRequest,
  UserSetResponse,
} from "@/lib/types/collection";

const PAGE_SIZE = 20;

function OwnedSetsSection() {
  const [page, setPage] = useState(0);
  const [editingSet, setEditingSet] = useState<UserSetResponse | null>(null);
  const { data, isLoading, isError, error } = useCollectionSets(page, PAGE_SIZE);
  const addSet = useAddCollectionSet();
  const updateSet = useUpdateCollectionSet();
  const removeSet = useRemoveCollectionSet();

  const handleAdd = async (values: AddUserSetRequest) => {
    await addSet.mutateAsync(values);
  };

  const handleUpdate = async (id: string, values: UpdateUserSetRequest) => {
    await updateSet.mutateAsync({ id, request: values });
  };

  return (
    <Stack spacing={2}>
      <Typography variant="h5" component="h2">
        Owned sets
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
            : "Failed to load your sets."}
        </Alert>
      )}
      {data && (
        <>
          <CollectionSetsList
            sets={data.content}
            onRemove={(id) => removeSet.mutate(id)}
            onEdit={setEditingSet}
            removingId={removeSet.isPending ? removeSet.variables : null}
          />
          <PaginationControls
            page={data.page}
            totalPages={data.totalPages}
            first={data.first}
            last={data.last}
            onPageChange={setPage}
          />
        </>
      )}
      {editingSet && (
        <EditSetDialog
          set={editingSet}
          onSubmit={handleUpdate}
          onClose={() => setEditingSet(null)}
        />
      )}
    </Stack>
  );
}

function LoosePartsSection() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError, error } = useCollectionParts(
    page,
    PAGE_SIZE,
  );
  const addPart = useAddCollectionPart();
  const removePart = useRemoveCollectionPart();

  const handleAdd = async (values: AddUserPartRequest) => {
    await addPart.mutateAsync(values);
  };

  return (
    <Stack spacing={2}>
      <Typography variant="h5" component="h2">
        Loose parts
      </Typography>
      <AddPartToCollectionForm onSubmit={handleAdd} />
      {isLoading && (
        <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
          <CircularProgress aria-label="Loading" />
        </Box>
      )}
      {isError && (
        <Alert severity="error">
          {error instanceof Error
            ? error.message
            : "Failed to load your parts."}
        </Alert>
      )}
      {data && (
        <>
          <CollectionPartsList
            parts={data.content}
            onRemove={(id) => removePart.mutate(id)}
            removingId={removePart.isPending ? removePart.variables : null}
          />
          <PaginationControls
            page={data.page}
            totalPages={data.totalPages}
            first={data.first}
            last={data.last}
            onPageChange={setPage}
          />
        </>
      )}
    </Stack>
  );
}

function CollectionContent() {
  return (
    <Container maxWidth="lg" sx={{ py: 6 }}>
      <Stack spacing={6}>
        <Typography variant="h4" component="h1">
          My collection
        </Typography>
        <OwnedSetsSection />
        <LoosePartsSection />
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
