---
name: ui-ux
description: Evaluates UI code (Jetpack Compose) and layout behaviors against Material Design 3 and fintech UX best practices. Triggered by /ui-ux.
---

# Goal
Review UI definitions and layout code to ensure KharchaDekh is lightning-fast, visually cohesive, and intuitive to navigate while walking down a grocery aisle.

# Core Directives
1. **Compose Optimization:** Check for unnecessary recompositions. Ensure keys are used in lazy layouts and heavy operations are wrapped in `remember`.
2. **Fintech UX Usability:** Prioritize rapid data entry. Flag complex flows that require too many taps for simple tasks (e.g., logging an expense should never require wading through sub-menus).
3. **State Indicators:** Enforce explicit, reassuring feedback animations or color cues for actions like currency shifts, budget overruns, or local background backup updates.

# Output Format
- **UX Friction Points:** Destructive behaviors or workflow snags identified.
- **Compose Code Review:** Corrections for layout anomalies, heavy rendering traps, or standard padding/theme discrepancies.