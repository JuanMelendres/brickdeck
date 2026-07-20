"use client";

import {
  Button,
  List,
  ListItem,
  ListItemText,
  Typography,
} from "@mui/material";
import type { TriggeredAlertResponse } from "@/lib/types/alerts";

interface TriggeredAlertsListProps {
  alerts: TriggeredAlertResponse[];
  onDismiss: (id: string) => void;
}

export function TriggeredAlertsList({ alerts, onDismiss }: TriggeredAlertsListProps) {
  if (alerts.length === 0) {
    return (
      <Typography color="text.secondary">
        No triggered alerts. When a recorded price meets one of your rules, it
        shows up here.
      </Typography>
    );
  }

  return (
    <List disablePadding>
      {alerts.map((alert) => (
        <ListItem
          key={alert.id}
          divider
          secondaryAction={
            <Button size="small" onClick={() => onDismiss(alert.id)}>
              Dismiss
            </Button>
          }
        >
          <ListItemText
            primary={alert.message}
            secondary={`${alert.setNumber} · ${alert.amount} ${alert.currency} · ${alert.triggeredAt.slice(0, 10)}`}
          />
        </ListItem>
      ))}
    </List>
  );
}
