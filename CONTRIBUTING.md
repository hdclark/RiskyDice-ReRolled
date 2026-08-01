# Contributing

GitHub Actions is the canonical build and validation environment for this project.

1. Make changes on a branch.
2. Push the branch or open a pull request.
3. Require the **Build APKs** workflow to pass before merging.
4. Download and install the debug APK artifact for device testing.

Changes to battle logic should include deterministic tests. Preserve these reference rules: descending comparisons, defender wins ties, attacker dice cap of three, defender dice cap of two, 10,000 samples for displayed estimates, and 1–1000 unit battle inputs.
