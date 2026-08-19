---
name: docs
description: Automatically generates and updates technical documentation, database migration logs, and READMEs. Triggered by /docs.
---

# Goal
Keep KharchaDekh's internal and external technical documentation completely in sync with code updates, database migrations, and structural adjustments.

# Instructions
1. **Schema Tracking:** When changes are made to Room entities, verify that the Database Migration history documentation is explicitly appended.
2. **API & Offline Sync Notes:** Document any new endpoints, backup structures (like the 5MB compressed JSON spec), or background sync tasks.
3. **Clarity:** Keep writing style approachable, highly technical, and completely scannable using clear markdown anchors.

# Output Format
Provide the exact, updated `.md` file content blocks wrapped cleanly in code fences, explicitly noting which file needs to be modified or created.