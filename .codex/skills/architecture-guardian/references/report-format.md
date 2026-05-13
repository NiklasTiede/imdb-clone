# Guardian Report Format

Use this structure for final reports. Omit empty sections.

```markdown
**Architecture Guardian Report**
Mode: <quick|persistence|backend-modulith|api-contract|frontend|integration|full>
Scope: <short sentence>
Checks run: <commands or "none">

**Findings**

1. <Severity> - <Title>
   Evidence: <file links and exact observations>
   Risk: <why this matters>
   Suggested fix: <focused next change>
   Verification: <test/check to prove it>

**Readiness Gaps**

1. <Title>
   Evidence: <file links and observations>
   Needed before: <architecture goal, hard check, or explicit decision>
   Suggested next step: <small next action>

**Cross-System Risks**

- <Only include when a finding crosses backend/frontend/storage/search boundaries>

**Suggested Automated Checks**

- <ArchUnit, Spring Modulith verification, schema test, OpenAPI diff, frontend build/test, etc.>

**No Issues Found In**

- <Scope that was actually inspected>

**Residual Risk**

- <What was not checked, or checks that could not run>
```

Rules:

- Lead with findings, not summaries.
- Use severity labels: `Critical`, `High`, `Medium`, `Low`, `Readiness gap`.
- Separate confirmed evidence from hypotheses.
- If no issues are found, say that clearly and name the remaining test gaps.
- Do not list generic advice without a repo-specific evidence line.
- Do not claim checks passed unless the command output was observed.
