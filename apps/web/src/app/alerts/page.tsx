"use client";

import { Box, Container, Divider, Stack, Typography } from "@mui/material";
import { RequireAuth } from "@/features/auth/RequireAuth";
import { useAuth } from "@/features/auth/useAuth";
import { CreateAlertRuleForm } from "@/features/alerts/CreateAlertRuleForm";
import { AlertRulesList } from "@/features/alerts/AlertRulesList";
import { TriggeredAlertsList } from "@/features/alerts/TriggeredAlertsList";
import {
  useAlertRules,
  useCreateAlertRule,
  useDeleteAlertRule,
  useDismissTriggeredAlert,
  useTriggeredAlerts,
} from "@/features/alerts/alertsHooks";
import type { AddPriceAlertRuleRequest } from "@/lib/types/alerts";

export default function AlertsPage() {
  const { status } = useAuth();
  const isAuthenticated = status === "authenticated";

  const triggered = useTriggeredAlerts(isAuthenticated);
  const rules = useAlertRules(isAuthenticated);
  const createRule = useCreateAlertRule();
  const deleteRule = useDeleteAlertRule();
  const dismiss = useDismissTriggeredAlert();

  const handleCreate = async (values: AddPriceAlertRuleRequest) => {
    await createRule.mutateAsync(values);
  };

  return (
    <RequireAuth>
      <Container maxWidth="lg" sx={{ py: 6 }}>
        <Stack spacing={4}>
          <Typography variant="h4" component="h1">
            Price alerts
          </Typography>

          <Box>
            <Typography variant="h6" component="h2" gutterBottom>
              Triggered alerts
            </Typography>
            <TriggeredAlertsList
              alerts={triggered.data?.content ?? []}
              onDismiss={(id) => dismiss.mutate(id)}
            />
          </Box>

          <Divider />

          <Box>
            <Typography variant="h6" component="h2" gutterBottom>
              Create alert rule
            </Typography>
            <CreateAlertRuleForm onSubmit={handleCreate} />
          </Box>

          <Box>
            <Typography variant="h6" component="h2" gutterBottom>
              Your alert rules
            </Typography>
            <AlertRulesList
              rules={rules.data?.content ?? []}
              onDelete={(id) => deleteRule.mutate(id)}
            />
          </Box>
        </Stack>
      </Container>
    </RequireAuth>
  );
}
