# Development

This project uses a [devcontainer](https://containers.dev/) for development. All tools and dependencies are included.

## Prerequisites

- [Docker](https://www.docker.com/)
- [devcontainer CLI](https://github.com/devcontainers/cli)
- (Optional) An IDE with devcontainer support, e.g. [VS Code](https://code.visualstudio.com/) with the [Dev Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)

## Setup

Install the git pre-commit hook:

```sh
ln -s ../../tools/pre-commit .git/hooks/pre-commit
```

## Commands

The `./dev` script runs commands inside the devcontainer. Omit it if you're already inside.

### Format

```sh
./dev format
```

To check without modifying (used in CI):

```sh
./dev format --check
```

### Lint

```sh
./dev lint
```

### Docs

Preview the documentation site locally:

```sh
./dev docs
```

Build the site to `site/`:

```sh
./dev docs build
```

### Build

Bazel:

```sh
./dev bazel build //...
```

Gradle:

```sh
./dev gradle build
```

### Test

Bazel:

```sh
./dev bazel test //...
```

Gradle:

```sh
./dev gradle test
```
