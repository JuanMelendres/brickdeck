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
import type { PriceAlertRuleResponse, PriceAlertType } from "@/lib/types/alerts";

const TYPE_LABELS: Record<PriceAlertType, string> = {
  BELOW_TARGET_PRICE: "Below target",
  PERCENT_BELOW_AVERAGE: "% below average",
  AT_OR_BELOW_LOWEST: "At/below lowest",
};

interface AlertRulesListProps {
  rules: PriceAlertRuleResponse[];
  onDelete: (id: string) => void;
}

export function AlertRulesList({ rules, onDelete }: AlertRulesListProps) {
  if (rules.length === 0) {
    return (
      <Typography color="text.secondary">
        No alert rules yet. Create one for a set on your wishlist.
      </Typography>
    );
  }

  return (
    <TableContainer component={Paper} variant="outlined">
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Set</TableCell>
            <TableCell>Currency</TableCell>
            <TableCell>Type</TableCell>
            <TableCell align="right">Threshold</TableCell>
            <TableCell />
          </TableRow>
        </TableHead>
        <TableBody>
          {rules.map((rule) => (
            <TableRow key={rule.id}>
              <TableCell>{rule.setNumber}</TableCell>
              <TableCell>{rule.currency}</TableCell>
              <TableCell>{TYPE_LABELS[rule.type]}</TableCell>
              <TableCell align="right">{rule.thresholdValue ?? "—"}</TableCell>
              <TableCell align="right">
                <Button size="small" color="error" onClick={() => onDelete(rule.id)}>
                  Delete
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
