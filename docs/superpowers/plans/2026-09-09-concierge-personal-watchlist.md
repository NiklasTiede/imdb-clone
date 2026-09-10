# Delegated personal watchlist slice

Status: Local text/voice slice implemented and verified. TMDb and web search remain deferred.

User outcome: sign in, ask for the actual watchlist, explicitly add one movie, see the updated
watchlist in the existing application. Both text and English voice reuse the same domain tools.

1. Identity: CSRF-protected capability issuance, session-bound validation and revocation tests.
2. Engagement: narrow assistant contract, concurrent-safe additions, durable replay receipts and retention.
3. Assistant: workload-protected personal MCP tools without model-selected account or operation IDs.
4. Python: verified conversation ownership, separate credential contexts, finalized-command/grounding gate.
5. React: bounded initial voice handshake, transient delegation, typed watchlist/login actions, cache refresh.
6. Verification: Java tests and architecture checks, PostgreSQL module integration, Python full gate,
   frontend full gate, generated API refresh and browser smoke. Record gaps without claiming rollout.

The identity/idempotency decision and scoped threat analysis are in ADR 0004.

Verification and remaining boundaries: [delivery record](../../reviews/2026-09-09-personal-watchlist-verification.md).
