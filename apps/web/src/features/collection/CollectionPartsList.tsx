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
import type { UserPartResponse } from "@/lib/types/collection";

interface CollectionPartsListProps {
  parts: UserPartResponse[];
  onRemove: (id: string) => void;
  removingId?: string | null;
}

export function CollectionPartsList({
  parts,
  onRemove,
  removingId,
}: CollectionPartsListProps) {
  if (parts.length === 0) {
    return (
      <Typography color="text.secondary">No loose parts yet.</Typography>
    );
  }

  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Part</TableCell>
            <TableCell>Number</TableCell>
            <TableCell>Color</TableCell>
            <TableCell align="right">Qty</TableCell>
            <TableCell>Storage</TableCell>
            <TableCell align="right">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {parts.map((part) => (
            <TableRow key={part.id}>
              <TableCell>{part.partName ?? "—"}</TableCell>
              <TableCell>{part.partNumber ?? "—"}</TableCell>
              <TableCell>{part.colorName ?? "—"}</TableCell>
              <TableCell align="right">{part.quantity ?? "—"}</TableCell>
              <TableCell>{part.storageLocation ?? "—"}</TableCell>
              <TableCell align="right">
                <Button
                  color="error"
                  size="small"
                  onClick={() => onRemove(part.id)}
                  disabled={removingId === part.id}
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
