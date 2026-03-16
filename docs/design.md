# P4kt Design

## Goal

Provide an ergonomic Kotlin eDSL for writing P4 programs, targeting developers already familiar with P4. The current focus is the Very Simple Switch (VSS) example - the P4 spec's canonical reference program. Source: [p4c testdata](https://github.com/p4lang/p4c/tree/main/testdata/p4_16_samples)

## Design principles

1. **Ergonomics first.** The programming interface is the product. Every API decision should optimize for how natural and pleasant the DSL is to write and read. If a P4 construct cannot be given an ergonomic representation in Kotlin, it is better left unsupported than forced into a clumsy API.

2. **IDE support is non-negotiable.** Autocomplete, go-to-definition, and compile-time error checking must work. Without them, there is no reason to use P4kt over writing P4 directly.

3. **Small and useful over complete.** P4kt does not need to cover all of P4. A focused subset with a great interface is more valuable than full coverage with a mediocre one.

## Architecture

Three layers, all in the `p4kt` package:

```
User code (DSL) → IR (immutable data classes) → Renderer (P4 source text)
```

1. **IR** - Sealed class hierarchies for types, expressions, statements, and declarations. Immutable data classes. This is the core.
1. **DSL** - Mutable builder classes that produce IR nodes. Uses Kotlin delegated properties for type-safe variable references. The DSL is the public API; the IR is an internal implementation detail.
1. **Renderer** - Traverses IR nodes, emits P4 source text via `toP4()` extension functions using 4-space indentation, with no preamble or includes.

## VSS construct support

### Types

| Construct       | Example from VSS                         | Status  |
| --------------- | ---------------------------------------- | ------- |
| `bit<W>`        | `bit<48>`, `bit<4>`                      | Done    |
| `bool`          | implicit in conditions                   | Done    |
| `void`          | action return types                      | Done    |
| `typedef`       | `typedef bit<48> EthernetAddress`        | Done    |
| `header`        | `header Ethernet_h { ... }`              | Done    |
| `struct`        | `struct Parsed_packet { ... }`           | Done    |
| `error`         | `error { IPv4OptionsNotSupported, ... }` | Done    |
| type parameters | `<H>`, `<T>`                             | Partial |

### Declarations

| Construct             | Example from VSS                                          | Status |
| --------------------- | --------------------------------------------------------- | ------ |
| function              | `function bit<8> max(...)`                                | Done   |
| const                 | `const PortId DROP_PORT = 0xF`                            | Done   |
| action                | `action Set_nhop(...) { ... }`                            | Done   |
| table                 | `table ipv4_match { key, actions, size, default_action }` | Done   |
| control               | `control TopPipe(...) { ... apply { ... } }`              | Done   |
| parser                | `parser TopParser(...) { state start { ... } }`           | Done   |
| extern declaration    | `extern Ck16 { void clear(); ... }`                       | Done   |
| extern instantiation  | `Ck16() ck;`                                              | Done   |
| package declaration   | `package VSS<H>(...)`                                     | Todo   |
| package instantiation | `VSS(...) main;`                                          | Done   |

### Expressions

| Construct           | Example from VSS                      | Status |
| ------------------- | ------------------------------------- | ------ |
| literals            | `0x0800`, `4w4`, `16w0`               | Done   |
| variable references | `nextHop`, `p.ip.ttl`                 | Done   |
| field access        | `headers.ip.dstAddr`                  | Done   |
| arithmetic          | `headers.ip.ttl - 1`                  | Done   |
| comparison          | `p.ip.version == 4w4`                 | Done   |
| method calls        | `b.extract(p.ethernet)`, `ck.clear()` | Done   |

### Statements

| Construct             | Example from VSS                                                   | Status |
| --------------------- | ------------------------------------------------------------------ | ------ |
| assignment            | `nextHop = ipv4_dest`                                              | Done   |
| if/else               | `if (parseError != ...) { ... }`                                   | Done   |
| return                | `return;`                                                          | Done   |
| variable declaration  | `bit<8> tmp = x`                                                   | Done   |
| method call statement | `ipv4_match.apply()`                                               | Done   |
| transition            | `transition select(...) { ... }`                                   | Done   |
| transition select     | `transition select(p.ethernet.etherType) { 0x0800 : parse_ipv4; }` | Done   |

### Parser-specific

| Construct  | Example from VSS                                          | Status |
| ---------- | --------------------------------------------------------- | ------ |
| state      | `state start { ... }`                                     | Done   |
| extract    | `b.extract(p.ethernet)`                                   | Done   |
| verify     | `verify(p.ip.version == 4w4, error.IPv4IncorrectVersion)` | Done   |
| transition | `transition accept` / `transition select(...)`            | Done   |

### Table-specific

| Construct            | Example from VSS                       | Status |
| -------------------- | -------------------------------------- | ------ |
| key with match kind  | `headers.ip.dstAddr : lpm`             | Done   |
| actions list         | `actions = { Drop_action; Set_nhop; }` | Done   |
| size                 | `size = 1024`                          | Done   |
| default_action       | `default_action = Drop_action`         | Done   |
| const default_action | `const default_action = NoAction`      | Done   |

## Design decisions

- **Builder-centric**: DSL builders are the API. IR is internal. Expose IR only if a use case arises.
- **Gradle-convention layout**: Source in `src/main/kotlin/`, tests in `src/test/kotlin/`. Both Bazel and Gradle read from the same source tree.
- **Named operators over symbols**: Kotlin can't overload comparison/bitwise operators to return expression nodes. Named operators (`gt`, `band`) are the standard eDSL trade-off.
- **`lit()` for literals**: Required to bridge Kotlin values into P4 expression trees.
- **Trailing underscores for keywords**: `if_`, `return_`, `else_` - a common Kotlin eDSL convention.
- **Typed field access via ref classes**: P4 struct/header types are Kotlin classes extending `StructRef`/`HeaderRef`, registered via `struct(::ClassName)` / `header(::ClassName)`. Field access uses real Kotlin properties, enabling IDE autocomplete and compile-time checking.
- **Constructor references over reflection**: Factory lambdas (`::ClassName`) are used instead of reflection-based instantiation. Constructor references properly handle variable captures from enclosing scopes, avoiding the need for `kotlin-reflect`.
- **P4 naming for domain objects**: p4include objects (`core`, `v1model`) and examples (`vss_arch`, `vss_example`) use P4's naming convention (lowercase/snake_case) instead of Kotlin's PascalCase. This follows the precedent set by kotlinx.html and kotlin-css, which break Kotlin naming conventions to match their target domain.
- **`@P4DslMarker` annotation**: All builder classes used as DSL receivers are annotated with `@P4DslMarker` (a `@DslMarker` annotation). This prevents accidental scope leakage in nested DSL blocks.
- **Factory functions on `P4` object**: All factory functions (`P4.program {}`, `P4.action {}`, `P4.typedef()`, etc.) live on the `P4` object rather than as top-level functions. This avoids polluting the package namespace and is consistent with how users already use `P4.bit()`, `P4.ref()`, etc.
- **Delegate-based declarations in Library**: All Library declarations (`typedef`, `const_`, `extern`, `action`, `externFunction`) use Kotlin property delegates to infer names. No string names needed - the Kotlin property name becomes the P4 declaration name.
- **`P4.ErrorDecl` and `P4.MatchKindDecl`**: Error and match_kind declarations use nested objects with `member()` delegates, enabling IDE navigation (e.g., `core.error.NoError`, `core.match_kind.lpm`). Follows the P4 spec where both are sets of named members in global namespaces.
- **Method overloads via `overload()`**: P4 allows multiple methods with the same name. Since Kotlin property names must be unique, overloads use `val name2 by overload(original, ...)` which reuses the original method's P4 name. Same pattern for `externFunctionOverload()`.

## Future ideas

### Internal IR visibility

Make IR types (`P4Type`, `P4Expr`, `P4Statement`, etc.) internal. The design says "IR is an internal implementation detail," but these types are currently public. Enforce the boundary when multi-module support is added.
