<!-- generatedBy: sot@2.5.7 -->
---
name: openspec-superpowers-workflow
description: "Use when the user explicitly wants the full OpenSpec plus Superpowers path from clarification through proposal, design, tasks, implementation, verification, and optional archive."
model_hint: sonnet
tags:
  - orchestration
  - openspec
  - full-flow
category: orchestration
---


# OpenSpec + Superpowers Workflow

## Overview

Use this skill as the team entrypoint for feature delivery. It coordinates the order of work; it does not replace the detailed OpenSpec or Superpowers sub-skills.

This is an explicit opt-in workflow. Do not use it by default. Only use it when the user explicitly asks for this workflow, names this skill, or a repository policy explicitly requires it.

If `.superpowers-memory/` exists in the repository, treat it as shared project memory: read it before planning and update it before closing the workflow.

## Required Order

1. Run `$superpowers-feature-workflow` to clarify the request, compare approaches, confirm the design, and prepare implementation.
2. Run `$openspec-feature-workflow` to create the change and complete `proposal`, `design`, `specs`, and `tasks`.
3. Return to the Superpowers track for plan execution, worktree setup, TDD, and verification.
4. If the project uses OpenSpec archive flow and code, specs, and verification are aligned, archive the change as the final OpenSpec step.
5. Do not claim completion until verification evidence exists.
6. If `.superpowers-memory/` exists, update `CURRENT_STATE.md` and add a short journal entry for the session outcome.

## When to Use

- The user explicitly asks for `OpenSpec + Superpowers`
- The user explicitly names `$openspec-superpowers-workflow`
- The user explicitly asks for brainstorm, then proposal/design/tasks, then implementation, then verification
- A repository policy explicitly requires this workflow

## Deliverables

- Design doc in `docs/superpowers/specs/`
- OpenSpec change artifacts in `openspec/changes/<change-name>/`
- Implementation plan in `docs/superpowers/plans/`
- Code, tests, and fresh verification output
- Archived OpenSpec change when archive flow is part of the project workflow
- Updated Superpowers memory when `.superpowers-memory/` is present

## OMC Team Acceleration (Optional)

When oh-my-claudecode is installed and the task is large enough to benefit from parallel agents, you may use OMC's `/team` to accelerate execution. This is optional — sequential single-agent execution is always valid.

**When to consider teams:**
- The feature has 3+ independent implementation tasks after planning
- Verification and implementation can run in parallel
- The user explicitly asks for parallel or team-based execution

**How to use:**
1. After design approval (step 1), use `TeamCreate` to set up a team
2. Decompose the implementation plan into tasks via `TaskCreate` with dependency chains
3. Spawn teammates via `Agent` tool with `team_name` — use roles like `executor`, `verifier`, `critic`
4. The team lead (you) orchestrates; teammates execute tasks and report via `SendMessage`
5. After all tasks complete, use `TeamDelete` to clean up

**Recommended team pattern for this workflow:**
- **Phase 1 (sequential):** Design + OpenSpec artifacts — single agent, requires judgment
- **Phase 2 (parallel):** Implementation tasks — multiple `executor` agents working independent tasks
- **Phase 3 (parallel):** Verification — `verifier` agent checks spec compliance while `executor` fixes issues

If OMC is not installed or the task is small, proceed sequentially as described above.

## Guardrails

- Do not start implementation before the design is approved
- Do not skip OpenSpec artifacts for behavior changes
- Do not archive the change until code, tests, and specs are aligned
- Do not skip worktree, TDD, or verification when the request includes them
- Keep the skill portable: use repo-local paths and avoid machine-specific assumptions

<!-- checksum: sha256:b90c5f5e25ff1f0a627186f4083d77b93f942dc565f554187fce93d89d93882a -->