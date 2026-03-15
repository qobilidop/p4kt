# Milestone: Very Simple Switch (VSS)

## Goal

Express the P4 spec's Very Simple Switch example (section 5) as a P4kt program. This is the canonical P4 reference program and only depends on `core.p4`.

The VSS example consists of two parts:

- **Architecture definition** (`very_simple_model.p4`) - declares the VSS architecture, including port types, metadata structs, parser/control/deparser prototypes, a checksum extern, and the top-level package.
- **User program** (`vss-example.p4`) - implements a simple IPv4 forwarder using the VSS architecture.

Source: [p4c testdata](https://github.com/p4lang/p4c/tree/main/testdata/p4_16_samples)

## Why this milestone

- It is the single most canonical P4 program - the spec's own reference.
- It only includes `core.p4`, avoiding v1model dependency.
- It exercises most of the P4 language constructs needed for real programs.
- Once P4kt can express VSS, the remaining features (header stacks, registers, counters, etc.) are incremental.

## P4 constructs required

### Types

| Construct       | Example from VSS                         | Status         |
| --------------- | ---------------------------------------- | -------------- |
| `bit<W>`        | `bit<48>`, `bit<4>`                      | Done           |
| `bool`          | implicit in conditions                   | Planned (v0.1) |
| `void`          | action return types                      | Planned (v0.1) |
| `error`         | `error { IPv4OptionsNotSupported, ... }` | New            |
| `typedef`       | `typedef bit<48> EthernetAddress`        | New            |
| `header`        | `header Ethernet_h { ... }`              | New            |
| `struct`        | `struct Parsed_packet { ... }`           | New            |
| type parameters | `<H>`, `<T>`                             | New            |

### Declarations

| Construct             | Example from VSS                                          | Status |
| --------------------- | --------------------------------------------------------- | ------ |
| function              | `function bit<8> max(...)`                                | Done   |
| const                 | `const PortId DROP_PORT = 0xF`                            | New    |
| action                | `action Set_nhop(...) { ... }`                            | New    |
| table                 | `table ipv4_match { key, actions, size, default_action }` | New    |
| control               | `control TopPipe(...) { ... apply { ... } }`              | New    |
| parser                | `parser TopParser(...) { state start { ... } }`           | New    |
| extern declaration    | `extern Ck16 { void clear(); ... }`                       | New    |
| extern instantiation  | `Ck16() ck;`                                              | New    |
| package declaration   | `package VSS<H>(...)`                                     | New    |
| package instantiation | `VSS(...) main;`                                          | New    |

### Expressions

| Construct           | Example from VSS                      | Status                    |
| ------------------- | ------------------------------------- | ------------------------- |
| literals            | `0x0800`, `4w4`, `16w0`               | Partial (untyped only)    |
| variable references | `nextHop`, `p.ip.ttl`                 | Partial (no field access) |
| field access        | `headers.ip.dstAddr`                  | New                       |
| arithmetic          | `headers.ip.ttl - 1`                  | Done                      |
| comparison          | `p.ip.version == 4w4`                 | Planned (v0.1)            |
| method calls        | `b.extract(p.ethernet)`, `ck.clear()` | New                       |

### Statements

| Construct             | Example from VSS                                                   | Status         |
| --------------------- | ------------------------------------------------------------------ | -------------- |
| assignment            | `nextHop = ipv4_dest`                                              | Planned (v0.1) |
| if/else               | `if (parseError != ...) { ... }`                                   | Planned (v0.1) |
| return                | `return;`                                                          | Done           |
| method call statement | `ipv4_match.apply()`                                               | New            |
| transition            | `transition select(...) { ... }`                                   | New            |
| transition select     | `transition select(p.ethernet.etherType) { 0x0800 : parse_ipv4; }` | New            |

### Parser-specific

| Construct  | Example from VSS                                          | Status |
| ---------- | --------------------------------------------------------- | ------ |
| state      | `state start { ... }`                                     | New    |
| extract    | `b.extract(p.ethernet)`                                   | New    |
| verify     | `verify(p.ip.version == 4w4, error.IPv4IncorrectVersion)` | New    |
| transition | `transition accept` / `transition select(...)`            | New    |

### Table-specific

| Construct            | Example from VSS                       | Status |
| -------------------- | -------------------------------------- | ------ |
| key with match kind  | `headers.ip.dstAddr : lpm`             | New    |
| actions list         | `actions = { Drop_action; Set_nhop; }` | New    |
| size                 | `size = 1024`                          | New    |
| default_action       | `default_action = Drop_action`         | New    |
| const default_action | `const default_action = NoAction`      | New    |

## Architecture considerations

The VSS architecture (`very_simple_model.p4`) should be expressible as a P4kt library - a Kotlin package that provides pre-defined declarations. This validates the design where:

- `core.p4` built-ins are part of P4kt core
- Architecture definitions are P4kt libraries using Kotlin's package system
- Users import architecture libraries like any other Kotlin dependency

## Test strategy

Following the existing golden test pattern:

1. Write the VSS architecture definition in P4kt
2. Write the VSS program in P4kt
3. Render both to P4 source text
4. Compare against the reference P4 files from p4c
