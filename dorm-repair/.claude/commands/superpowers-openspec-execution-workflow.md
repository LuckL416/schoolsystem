<!-- generatedBy: sot@2.5.7 -->
---
name: superpowers-openspec-execution-workflow
description: Use when the team explicitly wants the Superpowers exploration, OpenSpec specification, Superpowers execution, and OpenSpec archive workflow for a feature
model_hint: sonnet
---


# Superpowers -> OpenSpec -> Superpowers Workflow

## Overview

Use this skill when the team wants this four-step delivery path:

1. Explore with Superpowers
2. Lock the change with OpenSpec
3. Execute with Superpowers and finish with implementation, testing, and verification
4. Archive the completed OpenSpec change

This skill is an orchestrator. It should delegate detail work to the existing workflow skills instead of duplicating them.

This is an explicit opt-in workflow. Do not use it by default. Only use it when the user explicitly asks for this workflow, names this skill, or a repository policy explicitly requires it.

If `.superpowers-memory/` exists in the repository, read `PROJECT_CONTEXT.md`, `CURRENT_STATE.md`, `DECISIONS.md`, `KNOWN_FAILURES.md`, `VERIFICATION_BASELINE.md`, `TEAM_PREFERENCES.md`, `USER_PROFILE.md`, `AGENT_NOTES.md`, and the newest session journal entries at the start, then update the relevant files before final archive so the next session can resume with real context.

## Required Order

1. Start with `$superpowers-feature-workflow`.
   Use it to clarify scope, compare approaches, confirm the solution shape, and capture the design draft.
2. Move to `$openspec-feature-workflow`.
   Use it to create the change and complete `proposal.md`, `design.md`, `specs/.../spec.md`, and `tasks.md`.
3. Return to `$superpowers-feature-workflow`.
   Use it to write the implementation plan, prefer a worktree, execute with TDD, and run fresh verification.
4. If implementation and specs are aligned after verification, use `$openspec-archive-change` to archive the completed change.
5. If `.superpowers-memory/` exists, perform a memory alignment check after verification and archive decisions: ensure durable facts, current state, decisions, failure patterns, and session outcome are reflected in the right files.
6. Prefer `scripts/run-superpowers-memory-closeout.ps1` when you want one command to review the checklist, get update suggestions, and optionally run validation after execution or archive work.
7. Use `scripts/suggest-superpowers-memory-updates.ps1` if it is still unclear which memory surfaces should be updated after implementation, verification, or archive work.
8. When memory quality matters for the project, run `scripts/validate-superpowers-memory.ps1` before the final completion claim.

## OMC Team Acceleration (Optional)

When oh-my-claudecode is installed and the task is large enough to benefit from parallel agents, you may use OMC's `/team` to accelerate execution. This is optional — sequential single-agent execution is always valid.

**When to consider teams:**
- The feature has 3+ independent implementation tasks after planning
- Verification and implementation can run in parallel
- The user explicitly asks for parallel or team-based execution

**How to use:**
1. After design is locked (step 2), use `TeamCreate` to set up a team
2. Decompose the implementation plan into tasks via `TaskCreate` with dependency chains
3. Spawn teammates via `Agent` tool with `team_name` — use roles like `executor`, `verifier`, `critic`
4. The team lead (you) orchestrates; teammates execute tasks and report via `SendMessage`
5. After all tasks complete, use `TeamDelete` to clean up

**Recommended team pattern for this workflow:**
- **Step 1-2 (sequential):** Explore + OpenSpec — single agent, requires judgment
- **Step 3 (parallel):** Implementation tasks — multiple `executor` agents working independent tasks
- **Step 3 verify (parallel):** `verifier` agent checks spec compliance while `executor` fixes issues
- **Step 4 (sequential):** Archive — single agent, requires context

If OMC is not installed or the task is small, proceed sequentially as described above.

## Decision Gates

- Do not create implementation code during the exploration stage.
- Do not start coding until required OpenSpec artifacts are complete.
- Do not claim success until fresh verification output exists.
- Do not archive the change until code, tests, and specs are aligned.
- Do not leave memory out of sync with the final archive decision when `.superpowers-memory/` exists.

## When to Use

- The user explicitly asks for "explore first, spec second, execute third"
- The user explicitly names `$superpowers-openspec-execution-workflow`
- The user explicitly asks for Superpowers exploration, OpenSpec locking, then Superpowers execution and archive
- A repository policy explicitly requires this workflow

## Deliverables

- Design draft in `docs/superpowers/specs/`
- OpenSpec artifacts under `openspec/changes/<change-name>/`
- Implementation plan in `docs/superpowers/plans/`
- Code, tests, and fresh verification evidence
- Updated Superpowers memory and memory validation evidence when memory is in use
- Optional closeout helper output when the closeout helper was used
- Archived OpenSpec change when the work is complete

## Recommended Prompt

```text
Use $superpowers-openspec-execution-workflow for this feature: first explore with Superpowers, then lock the change with OpenSpec, then return to Superpowers for implementation, testing, verification, and archive.
```

<!-- checksum: sha256:5efd5e9125144007690fc9d145d2a53d6c05c61e36595a99b2aa4bcc13b7ab64 -->