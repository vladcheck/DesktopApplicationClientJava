---
description: Create an issue, create a branch, commit changes, create a pr
agent: build
---

# issue-branch-commit-pr

## Workflow

1. Create an issue.
2. Check what branch you are on. If you are on a branch other than main, stash all changes, checkout to main, pull latest changes (`--ff-only`), and unstash changes. If no conflicts, create a new branch.
3. Commit any unstaged and staged files.
4. create a pr, link it to the issue, set appropriate labels, assignees (by default - the user) and reviewers.