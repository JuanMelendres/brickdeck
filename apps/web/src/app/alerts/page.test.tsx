import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import AlertsPage from "./page";
import type { PageResponse } from "@/lib/types/api";
import type {
  PriceAlertRuleResponse,
  TriggeredAlertResponse,
} from "@/lib/types/alerts";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
}));

vi.mock("@/features/auth/useAuth", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: null,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
  }),
}));

const {
  useAlertRulesMock,
  useTriggeredAlertsMock,
  useCreateAlertRuleMock,
  useDeleteAlertRuleMock,
  useDismissTriggeredAlertMock,
} = vi.hoisted(() => ({
  useAlertRulesMock: vi.fn(),
  useTriggeredAlertsMock: vi.fn(),
  useCreateAlertRuleMock: vi.fn(),
  useDeleteAlertRuleMock: vi.fn(),
  useDismissTriggeredAlertMock: vi.fn(),
}));

vi.mock("@/features/alerts/alertsHooks", () => ({
  useAlertRules: useAlertRulesMock,
  useTriggeredAlerts: useTriggeredAlertsMock,
  useCreateAlertRule: useCreateAlertRuleMock,
  useDeleteAlertRule: useDeleteAlertRuleMock,
  useDismissTriggeredAlert: useDismissTriggeredAlertMock,
}));

function rulesPage(content: PriceAlertRuleResponse[]): PageResponse<PriceAlertRuleResponse> {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1, first: true, last: true };
}
function triggeredPage(content: TriggeredAlertResponse[]): PageResponse<TriggeredAlertResponse> {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1, first: true, last: true };
}

describe("AlertsPage", () => {
  afterEach(() => vi.clearAllMocks());

  it("renders triggered alerts and alert rules", () => {
    useTriggeredAlertsMock.mockReturnValue({
      data: triggeredPage([
        {
          id: "t1",
          ruleId: "r1",
          setNumber: "75257-1",
          amount: 80,
          currency: "USD",
          message: "80 is below your target 100",
          triggeredAt: "2026-01-11T00:00:00",
        },
      ]),
      isLoading: false,
    });
    useAlertRulesMock.mockReturnValue({
      data: rulesPage([
        {
          id: "r1",
          setNumber: "75257-1",
          currency: "USD",
          type: "BELOW_TARGET_PRICE",
          thresholdValue: 100,
          active: true,
          createdAt: "2026-01-10T00:00:00",
        },
      ]),
      isLoading: false,
    });
    useCreateAlertRuleMock.mockReturnValue({ mutateAsync: vi.fn() });
    useDeleteAlertRuleMock.mockReturnValue({ mutate: vi.fn() });
    useDismissTriggeredAlertMock.mockReturnValue({ mutate: vi.fn() });

    render(<AlertsPage />);

    expect(screen.getByRole("heading", { name: /price alerts/i })).toBeInTheDocument();
    expect(screen.getByText(/below your target/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /create alert/i })).toBeInTheDocument();
    expect(screen.getByText("75257-1")).toBeInTheDocument();
  });
});
