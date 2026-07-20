import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createAlertRule,
  deleteAlertRule,
  dismissTriggeredAlert,
  listAlertRules,
  listTriggeredAlerts,
} from "@/lib/api/alerts";
import { queryKeys } from "@/lib/query/keys";

const PAGE_SIZE = 20;

/** The user's alert rules. Disabled until enabled (authenticated). */
export function useAlertRules(enabled: boolean, page = 0) {
  return useQuery({
    queryKey: queryKeys.alerts.rules(page),
    queryFn: () => listAlertRules({ page, size: PAGE_SIZE }),
    enabled,
  });
}

/** The user's triggered alerts. Disabled until enabled (authenticated). */
export function useTriggeredAlerts(enabled: boolean, page = 0) {
  return useQuery({
    queryKey: queryKeys.alerts.triggered(page),
    queryFn: () => listTriggeredAlerts({ page, size: PAGE_SIZE }),
    enabled,
  });
}

export function useCreateAlertRule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createAlertRule,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.alerts.all }),
  });
}

export function useDeleteAlertRule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteAlertRule,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.alerts.all }),
  });
}

export function useDismissTriggeredAlert() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: dismissTriggeredAlert,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.alerts.all }),
  });
}
