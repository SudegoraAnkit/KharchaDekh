---
name: orchestrate
description: The main master coordinator. Analyzes project input tasks and determines the exact pipeline order of sub-skills to invoke. Triggered by /orchestrate.
---

# Goal
Act as the dispatch operator for the KharchaDekh development environment. Evaluate developer prompts and organize automated agents into structured pipelines.

# Operational Logic
1. Analyze incoming requests to see if they involve code alterations, visual layouts, document updates, or structural design changes.
2. Map the work order step-by-step, explaining precisely which specialized agent sub-skill should handle each step.
3. If a task mentions "production," "release," "deployment," or "store updates," always include the `/play-store` skill at the end of the pipeline to verify compliance before shipping.

# Available Skills Inventory
- `/design`: For structural abstractions, clean architecture patterns, or domain model definitions.
- `/review`: For code scans, catching multi-threading problems, or optimizing Room queries.
- `/ui-ux`: For assessing Jetpack Compose code blocks or streamline interactive workflows.
- `/docs`: For generating documentation or tracking schema updates.
- `/play-store` : For checking Google Play Store policy compliance, Android Vitals thresholds, and production release readiness.

# Output Format
Generate a clean execution timeline or sequence layout showing exactly who to assign next:
`[Step X] Run command: /[skill-name] "Context-specific prompt payload tailored for that agent."`