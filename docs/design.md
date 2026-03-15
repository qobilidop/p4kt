# P4kt Design

## Goal

Provide an ergonomic Kotlin eDSL for writing P4 programs, targeting developers already familiar with P4. The current focus is the Very Simple Switch (VSS) example - the P4 spec's canonical reference program. Source: [p4c testdata](https://github.com/p4lang/p4c/tree/main/testdata/p4_16_samples)

## Design principles

1. **Ergonomics first.** The programming interface is the product. Every API decision should optimize for how natural and pleasant the DSL is to write and read. If a P4 construct cannot be given an ergonomic representation in Kotlin, it is better left unsupported than forced into a clumsy API.

2. **IDE support is non-negotiable.** Autocomplete, go-to-definition, and compile-time error checking must work. Without them, there is no reason to use P4kt over writing P4 directly.

3. **Small and useful over complete.** P4kt does not need to cover all of P4. A focused subset with a great interface is more valuable than full coverage with a mediocre one.

## Architecture

Three layers, all in the `p4kt/` package (flat layout, split later when warranted):

```
User code (DSL) → IR (immutable data classes) → Renderer (P4 source text)
```

1. **IR** - Sealed class hierarchies for types, expressions, statements, and declarations. Immutable data classes. This is the core.
1. **DSL** - Mutable builder classes that produce IR nodes. Uses Kotlin delegated properties for type-safe variable references. The DSL is the public API; the IR is an internal implementation detail.
1. **Renderer** - Traverses IR nodes, emits P4 source text via `toP4()` extension functions using 4-space indentation, with no preamble or includes.

## VSS construct support

### Types

| Construct       | Example from VSS                         | Status |
| --------------- | ---------------------------------------- | ------ |
| `bit<W>`        | `bit<48>`, `bit<4>`                      | Done   |
| `bool`          | implicit in conditions                   | Done   |
| `void`          | action return types                      | Done   |
| `typedef`       | `typedef bit<48> EthernetAddress`        | Done   |
| `header`        | `header Ethernet_h { ... }`              | Done   |
| `struct`        | `struct Parsed_packet { ... }`           | Done   |
| `error`         | `error { IPv4OptionsNotSupported, ... }` | Todo   |
| type parameters | `<H>`, `<T>`                             | Todo   |

### Declarations

| Construct             | Example from VSS                                          | Status |
| --------------------- | --------------------------------------------------------- | ------ |
| function              | `function bit<8> max(...)`                                | Done   |
| const                 | `const PortId DROP_PORT = 0xF`                            | Done   |
| action                | `action Set_nhop(...) { ... }`                            | Done   |
| table                 | `table ipv4_match { key, actions, size, default_action }` | Done   |
| control               | `control TopPipe(...) { ... apply { ... } }`              | Done   |
| parser                | `parser TopParser(...) { state start { ... } }`           | Todo   |
| extern declaration    | `extern Ck16 { void clear(); ... }`                       | Todo   |
| extern instantiation  | `Ck16() ck;`                                              | Todo   |
| package declaration   | `package VSS<H>(...)`                                     | Todo   |
| package instantiation | `VSS(...) main;`                                          | Todo   |

### Expressions

| Construct           | Example from VSS                      | Status |
| ------------------- | ------------------------------------- | ------ |
| literals            | `0x0800`, `4w4`, `16w0`               | Done   |
| variable references | `nextHop`, `p.ip.ttl`                 | Done   |
| field access        | `headers.ip.dstAddr`                  | Done   |
| arithmetic          | `headers.ip.ttl - 1`                  | Done   |
| comparison          | `p.ip.version == 4w4`                 | Done   |
| method calls        | `b.extract(p.ethernet)`, `ck.clear()` | Todo   |

### Statements

| Construct             | Example from VSS                                                   | Status |
| --------------------- | ------------------------------------------------------------------ | ------ |
| assignment            | `nextHop = ipv4_dest`                                              | Done   |
| if/else               | `if (parseError != ...) { ... }`                                   | Done   |
| return                | `return;`                                                          | Done   |
| variable declaration  | `bit<8> tmp = x`                                                   | Done   |
| method call statement | `ipv4_match.apply()`                                               | Done   |
| transition            | `transition select(...) { ... }`                                   | Todo   |
| transition select     | `transition select(p.ethernet.etherType) { 0x0800 : parse_ipv4; }` | Todo   |

### Parser-specific

| Construct  | Example from VSS                                          | Status |
| ---------- | --------------------------------------------------------- | ------ |
| state      | `state start { ... }`                                     | Todo   |
| extract    | `b.extract(p.ethernet)`                                   | Todo   |
| verify     | `verify(p.ip.version == 4w4, error.IPv4IncorrectVersion)` | Todo   |
| transition | `transition accept` / `transition select(...)`            | Todo   |

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
- **Flat package layout**: Everything in `p4kt/`. Split into sub-packages when complexity warrants it.
- **Named operators over symbols**: Kotlin can't overload comparison/bitwise operators to return expression nodes. Named operators (`gt`, `band`) are the standard eDSL trade-off.
- **`lit()` for literals**: Required to bridge Kotlin values into P4 expression trees.
- **Trailing underscores for keywords**: `if_`, `return_`, `else_` - a common Kotlin eDSL convention.
- **Typed field access via ref classes**: P4 struct/header types are Kotlin classes extending `StructRef`/`HeaderRef`, registered via `struct(::ClassName)` / `header(::ClassName)`. Field access uses real Kotlin properties, enabling IDE autocomplete and compile-time checking.
- **Constructor references over reflection**: Factory lambdas (`::ClassName`) are used instead of reflection-based instantiation. Constructor references properly handle variable captures from enclosing scopes, avoiding the need for `kotlin-reflect`.

## Future ideas

### Architecture as a library

The VSS architecture (`very_simple_model.p4`) should be expressible as a P4kt library - a Kotlin package that provides pre-defined declarations. This validates the design where:

- `core.p4` built-ins are part of P4kt core
- Architecture definitions are P4kt libraries using Kotlin's package system
- Users import architecture libraries like any other Kotlin dependency
