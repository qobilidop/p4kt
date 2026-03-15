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
- Use plain hyphens (-) instead of em dashes.
- Optimize for human readability.

## Code style

- For Kotlin, follow Google's [Kotlin style guide](https://developer.android.com/kotlin/style-guide).
- For Bazel, follow [BUILD Style Guide](https://bazel.build/build/style-guide).

## Build

- Dual build system: Bazel (with Bzlmod) and Gradle. See `docs/dev.md` for commands.
- `kt_jvm_test` / `tasks.test` uses JUnit 4 runner. Use `kotlin-test-junit` (not junit5).
- No lock files checked in for now (solo project). Re-add when collaborators join.

## Project layout

- Follow Gradle conventions: `src/main/kotlin/` for source, `src/test/kotlin/` for tests.
- The `examples` module uses a flat layout (source files in the module root).
- Development uses devcontainer. See `docs/dev.md`.

## Quality checks

- Run `./dev lint` before committing. Fix all lint errors before the commit.
- Run `./dev bazel test //...` to verify no regressions before committing.
- Run `./dev gradle test` to verify Gradle build also passes.

## Commit style

- Keep the subject line under 72 characters.
- Explain why the change is being made.
- Describe what has changed at a high level. Don't repeat what's obvious in the change itself.
- Prefer one commit per logical change. Don't split into many tiny commits when they form a single unit of work.
- Credit yourself (e.g. using the `Co-authored-by:` field) for commits you made.
