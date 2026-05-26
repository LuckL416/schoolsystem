<!-- generatedBy: sot@2.5.7 -->
---
name: superpowers-feature-workflow
description: "Use when feature work needs the Superpowers stages before or during implementation: brainstorming, design confirmation, implementation planning, worktree setup, test-driven development, and verification. Trigger when the user asks to brainstorm first, wants a plan before coding, or wants disciplined execution with TDD and verification."
model_hint: sonnet
tags:
  - feature
  - tdd
  - workflow
category: engineering
---


# Superpowers Feature Workflow

## Overview

Use this skill for the Superpowers half of feature delivery. It covers clarification, design, plan, worktree, TDD, verification, and repo-persisted memory, but it does not manage OpenSpec artifacts.

This is an explicit opt-in workflow. Do not use it by default. Only use it when the user explicitly asks for this workflow, names this skill, or a repository policy explicitly requires it.

## Workflow

1. Explore project context before proposing solutions. If `.superpowers-memory/` exists, read `PROJECT_CONTEXT.md`, `CURRENT_STATE.md`, `DECISIONS.md`, `KNOWN_FAILURES.md`, `VERIFICATION_BASELINE.md`, `TEAM_PREFERENCES.md`, `USER_PROFILE.md`, `AGENT_NOTES.md`, and the latest session journal entries first.
2. Clarify requirements one question at a time.
3. Present 2-3 approaches with a recommendation.
4. Write the approved design to `docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md`.
5. Ask for user confirmation on the written design before moving on.
6. Write the implementation plan to `docs/superpowers/plans/YYYY-MM-DD-<topic>.md`.
7. Prefer a repo-local worktree for implementation.
8. Implement with TDD: failing test first, then minimal code, then green.
9. Run fresh verification commands before any completion claim.
10. Before finishing the session, run a short memory closeout check: durable facts changed, current state changed, important decisions added, failure patterns discovered, verification rules changed, and reusable lessons identified.
11. Prefer `scripts/run-superpowers-memory-closeout.ps1` when you want one command to review the checklist, get update suggestions, and optionally run validation.
12. Use `scripts/suggest-superpowers-memory-updates.ps1` when the right memory surface is still unclear after implementation or verification.
13. When the repo uses Superpowers memory, update the relevant memory files, including `.superpowers-memory/CURRENT_STATE.md` and a short session note under `.superpowers-memory/session-journal/`.
14. When memory quality matters for the task, run `scripts/validate-superpowers-memory.ps1` before the final completion claim.

## When to Use

- The user explicitly asks to brainstorm first
- The user explicitly names `$superpowers-feature-workflow`
- The user explicitly asks for a written plan, TDD, or verification workflow
- A repository policy explicitly requires this workflow

## Outputs

- Confirmed design doc
- Implementation plan
- Verified implementation evidence
- Updated Superpowers memory when `.superpowers-memory/` is present
- Optional closeout helper output when the closeout helper was used
- Memory validation evidence when memory updates were part of the workflow

## OMC Team Acceleration (Optional)

When oh-my-claudecode is installed and the task is large enough to benefit from parallel agents, you may use OMC's `/team` to accelerate execution. This is optional — sequential single-agent execution is always valid.

**When to consider teams:**
- The implementation plan has 3+ independent tasks
- Verification can run in parallel with the next implementation task
- The user explicitly asks for parallel or team-based execution

**How to use:**
1. After the plan is approved (step 6), use `TeamCreate` to set up a team
2. Decompose the plan into tasks via `TaskCreate` with dependency chains
3. Spawn teammates via `Agent` tool with `team_name` — use roles like `executor`, `verifier`, `critic`
4. The team lead (you) orchestrates; teammates execute tasks and report via `SendMessage`
5. After all tasks complete, use `TeamDelete` to clean up

**Recommended team pattern for this workflow:**
- **Steps 1-6 (sequential):** Clarify, design, plan — single agent, requires judgment
- **Steps 7-8 (parallel):** Implementation tasks — multiple `executor` agents on independent tasks
- **Step 9 (parallel):** `verifier` agent runs verification while `executor` works on the next task
- **Steps 10-14 (sequential):** Memory closeout — single agent, requires full context

If OMC is not installed or the task is small, proceed sequentially as described above.

## Guardrails

- Do not write production code before design approval
- Do not skip the failing-test step for new behavior
- Do not report success without fresh command output
- Do not overwrite stable project memory with temporary notes; keep long-term facts in `PROJECT_CONTEXT.md` and session-specific updates in the journal
- Do not leave new durable decisions, known failures, or verification rules only in chat history when the repo uses Superpowers memory

<!-- checksum: sha256:2ab58b743867f1e69c40ad4e06c80e7b9150719dcfe84bc1a386f0c21f1d964d -->