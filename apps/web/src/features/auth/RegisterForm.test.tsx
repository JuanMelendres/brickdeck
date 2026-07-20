import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { RegisterForm } from "./RegisterForm";
import { ApiError } from "@/lib/api/client";

describe("RegisterForm", () => {
  it("renders email, password, display name, and a submit button", () => {
    render(<RegisterForm onSubmit={vi.fn()} />);
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/display name/i)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /create account/i }),
    ).toBeInTheDocument();
  });

  it("requires a password of at least 8 characters", async () => {
    const onSubmit = vi.fn();
    render(<RegisterForm onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/email/i), "me@example.com");
    await user.type(screen.getByLabelText(/password/i), "short");
    await user.click(screen.getByRole("button", { name: /create account/i }));

    expect(await screen.findByText(/at least 8 characters/i)).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("submits with an optional display name", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<RegisterForm onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/email/i), "me@example.com");
    await user.type(screen.getByLabelText(/password/i), "password123");
    await user.type(screen.getByLabelText(/display name/i), "Me");
    await user.click(screen.getByRole("button", { name: /create account/i }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({
        email: "me@example.com",
        password: "password123",
        displayName: "Me",
      }),
    );
  });

  it("maps a server field error onto the email field", async () => {
    const onSubmit = vi
      .fn()
      .mockRejectedValue(
        new ApiError(409, "Email already registered", {
          email: "Email already registered",
        }),
      );
    render(<RegisterForm onSubmit={onSubmit} />);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/email/i), "taken@example.com");
    await user.type(screen.getByLabelText(/password/i), "password123");
    await user.click(screen.getByRole("button", { name: /create account/i }));

    expect(
      await screen.findByText(/email already registered/i),
    ).toBeInTheDocument();
  });
});
