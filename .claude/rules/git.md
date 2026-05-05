## Git conventions

- Commit messages, PR titles, and PR descriptions must be written in English.
- Pull requests are squash-merged, so there is no need to amend or rebase to keep a feature branch as a single commit. Stack incremental commits as you go and rely on the squash to flatten history at merge time. Use `git commit --amend` only when fixing the most recent commit before it has been pushed.
