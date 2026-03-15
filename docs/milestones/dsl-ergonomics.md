# DSL Ergonomics Ideas

Ideas for improving P4kt's DSL safety and ergonomics, collected during development. Not committed to any timeline - revisit as the DSL grows.

## Delegated properties for declarations

**Problem:** Declaration names are strings, repeated when referencing types. Misspelling a string produces valid Kotlin that generates broken P4.

**Idea:** Use Kotlin delegated properties (the same `by` pattern already used for `param`) to derive P4 names from Kotlin variable names:

```kotlin
// Before (string-based, error-prone)
val ethernetH = header("Ethernet_h") { ... }
struct("Parsed_packet") {
  field("ethernet", typeName("Ethernet_h"))  // string can diverge
}

// After (delegation, compiler-checked)
val Ethernet_h by header { ... }
struct {
  field("ethernet", Ethernet_h)  // Kotlin variable, compiler catches typos
}
```

Applies to all declaration types: `typedef`, `header`, `struct`, `function`, `action`.

**Tradeoff:** P4 naming conventions (e.g., `Ethernet_h`) differ from Kotlin conventions (e.g., `ethernetH`). Decision: P4kt is its own language - use P4-style names.

## Declarations as type references

**Problem:** Referencing a declared type requires `typeName("Ethernet_h")` - a raw string with no connection to the declaration.

**Idea:** Declarations implement a common interface so they can be passed directly to `field()`:

```kotlin
val EthernetAddress by typedef(bit(48))
val Ethernet_h by header {
  field("dstAddr", EthernetAddress)  // pass declaration directly
}
```

`typeName("...")` stays for types not defined in the current program (e.g., types from an architecture library).

## Object-based headers for IDE navigation

Superseded by the typed field access design (`docs/milestones/typed-field-access.md`). Struct/header types are now Kotlin classes with `StructRef`/`HeaderRef` base classes, registered via `struct(::ClassName)` / `header(::ClassName)`.

## Typed field access for nested structs

**Problem:** Cross-type field references (e.g., `Parsed_packet` referencing `Ethernet_h`) still use `typeName("Ethernet_h")` - a raw string. This happens because `TypedFieldDelegate<T>` for nested struct/header fields is not yet implemented.

**Idea:** Add `inline fun <reified T : StructRef> field()` that returns a `TypedFieldDelegate<T>`, enabling chained access like `headers.ip.ttl` through real Kotlin properties. Constructor references (`::ClassName`) should handle the instantiation, avoiding the reflection capture issue.

```kotlin
class Parsed_packet(base: P4Expr) : StructRef(base) {
  val ethernet by field(::Ethernet_h)  // typed, not typeName("Ethernet_h")
  val ip by field(::Ipv4_h)
}

// Enables chained access:
val pkt by param(::Parsed_packet, INOUT)
pkt.ip.ttl  // fully typed chain
```

**Status:** Deferred. The typed `param()` approach covers the most common use case.
