import "@testing-library/jest-dom/vitest";
import { useEffect, useState } from "react";
import { act, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { authSession } from "../../../shared/auth";
import ConciergeExperience from "./ConciergeExperience";

const lifecycle = vi.hoisted(() => ({ opened: 0, closed: 0 }));
let unmountExperience: (() => void) | undefined;
vi.mock("../hooks/useConciergeVoice", () => ({
  useConciergeVoice: () => {
    const [id] = useState(() => ++lifecycle.opened);
    useEffect(
      () => () => {
        lifecycle.closed++;
      },
      [],
    );
    return { id, active: true, turns: [], confirmNavigation: vi.fn() };
  },
}));
vi.mock("./ConciergeVoicePanel", () => ({
  ConciergeVoiceDock: ({ voice }: { voice: { id: number } }) => (
    <output data-testid="session">{voice.id}</output>
  ),
}));

beforeEach(() => {
  lifecycle.opened = 0;
  lifecycle.closed = 0;
  authSession.completeBootstrap(null);
});
afterEach(() => {
  unmountExperience?.();
  authSession.resetForTests();
});

it("promotes anonymous voice once and releases it on logout or account switch", () => {
  unmountExperience = render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter>
        <ConciergeExperience />
      </MemoryRouter>
    </QueryClientProvider>,
  ).unmount;
  const login = (id: number) =>
    act(() =>
      authSession.setSession({
        id,
        username: `user${id}`,
        email: `user${id}@example.com`,
        roles: ["ROLE_USER"],
      }),
    );
  expect(screen.getByTestId("session")).toHaveTextContent("1");
  login(17);
  expect(screen.getByTestId("session")).toHaveTextContent("1");
  expect(lifecycle.closed).toBe(0);
  login(18);
  expect(screen.getByTestId("session")).toHaveTextContent("2");
  expect(lifecycle.closed).toBe(1);
  act(() => authSession.clear());
  expect(screen.getByTestId("session")).toHaveTextContent("3");
  expect(lifecycle.closed).toBe(2);
  login(19);
  expect(screen.getByTestId("session")).toHaveTextContent("3");
});
