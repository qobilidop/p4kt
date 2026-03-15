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

**Problem:** With the builder pattern, fields are local variables inside a lambda - they go out of scope. When field access expressions are added (e.g., `headers.ip.dstAddr`), the IDE can't navigate from the access to the field definition.

**Idea:** Use Kotlin objects instead of builder lambdas for headers/structs, similar to Kotlin Exposed (SQL DSL):

```kotlin
// Object-based (enables IDE go-to-definition)
object Ethernet_h : HeaderDef() {
  val dstAddr = field(typeName("EthernetAddress"))
  val srcAddr = field(typeName("EthernetAddress"))
  val etherType = field(bit(16))
}

// Later, field access:
Ethernet_h.dstAddr  // IDE can go-to-definition
```

**Tradeoff:** More verbose, significant departure from the current builder DSL. Revisit when building field access expressions.
