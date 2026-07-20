"use client";

import {
  Button,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import type { UserSetResponse } from "@/lib/types/collection";

interface CollectionSetsListProps {
  sets: UserSetResponse[];
  onRemove: (id: string) => void;
  onEdit?: (set: UserSetResponse) => void;
  removingId?: string | null;
}

function formatPrice(price: number | null): string {
  return price == null ? "—" : `$${price.toFixed(2)}`;
}

export function CollectionSetsList({
  sets,
  onRemove,
  onEdit,
  removingId,
}: CollectionSetsListProps) {
  if (sets.length === 0) {
    return (
      <Typography color="text.secondary">
        No sets in your collection yet.
      </Typography>
    );
  }

  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Set</TableCell>
            <TableCell>Number</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="right">Price</TableCell>
            <TableCell>Purchased</TableCell>
            <TableCell align="right">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {sets.map((set) => (
            <TableRow key={set.id}>
              <TableCell>{set.setName ?? "—"}</TableCell>
              <TableCell>{set.setNumber ?? "—"}</TableCell>
              <TableCell>{set.status ?? "—"}</TableCell>
              <TableCell align="right">{formatPrice(set.purchasePrice)}</TableCell>
              <TableCell>{set.purchaseDate ?? "—"}</TableCell>
              <TableCell align="right">
                {onEdit && (
                  <Button size="small" onClick={() => onEdit(set)}>
                    Edit
                  </Button>
                )}
                <Button
                  color="error"
                  size="small"
                  onClick={() => onRemove(set.id)}
                  disabled={removingId === set.id}
                >
                  Remove
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
