# P4kt

## Engineering principles

- **TDD**: Test-Driven Development
- **YAGNI**: You Aren't Gonna Need It
- **DAMP**: Descriptive and Meaningful Phrases
- **DRY**: Don't Repeat Yourself
- Local reasoning

## Doc style

- For titles, use Title Case.
- For non-title section headings, use Sentence case.
- Optimize for human readability.

## Code style

- For Kotlin, follow Google's [Kotlin style guide](https://developer.android.com/kotlin/style-guide).
- For Bazel, follow [BUILD Style Guide](https://bazel.build/build/style-guide).

## Build

- Bazel with Bzlmod. Run `bazel test //...` to test everything.
- `kt_jvm_test` uses JUnit 4 runner. Use `kotlin-test-junit` (not junit5).
- No lock files checked in for now (solo project). Re-add when collaborators join.

## Project layout

- Follow Google/Bazel conventions: flat, package-per-feature layout.
- Co-locate tests with the source they test in the same package.
- Development happens on the host. The devcontainer is reserved for non-build tooling (formatters, linters, etc.).

## Commit style

- Keep the subject line under 72 characters.
- Explain why the change is being made.
- Describe what has changed at a high level. Don't repeat what's obvious in the change itself.
- Prefer one commit per logical change. Don't split into many tiny commits when they form a single unit of work.
- Credit yourself (e.g. using the `Co-authored-by:` field) for commits you made.
