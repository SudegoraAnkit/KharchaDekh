---
name: git
description: Automates staging, committing, and pushing unstaged local changes to structured version/feature or version/bug branches. Triggered by /git.
---

# Goal
Automate the process of cleaning up, packaging, and pushing unstaged local development modifications straight to a strict version-controlled feature or bug branch pattern.

# Branching Naming Pattern
Every push must target a clean branch format extracted from the feature intent or ongoing sprint:
- For new features: `version/feature_name` (e.g., `v1.2/grocery-scheduling`)
- For bug resolutions: `version/bug_name` (e.g., `v1.2/fix-usd-calc-crash`)

# Operational Sequence
1. **Identify Unstaged Modifications:** Run a local status check to capture all current uncommitted, untracked, and unstaged modifications.
2. **Determine Target Details:** Extract or prompt for the current release version version number and short target descriptive task title to construct the branch string.
3. **Branch Migration:** If the branch does not yet exist locally or on origin, create it and check it out cleanly using `git checkout -b <branch_name>`.
4. **Stage & Commit Payload:** Execute a unified staging step (`git add .`) and generate a clean commit message following conventional patterns matching the scope.
5. **Upstream Push:** Ship the commits to the remote tracking branch safely using `git push -u origin <branch_name>`.

# Constraints
- Never execute a forced push (`git push --force`) under any circumstances.
- If there are massive, unrelated files across multiple modules, warn the developer to verify before running a generic blanket stage.