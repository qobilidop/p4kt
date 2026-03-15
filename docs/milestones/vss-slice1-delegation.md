# Delegated Properties and Type References

## Goal

Replace string-based declaration names with Kotlin delegated properties, and allow declarations to be used directly as type references in field definitions. This makes the DSL more ergonomic and catches name typos at compile time.

## Design

### IR (Ir.kt)

New interface for declarations that can be used as types:

```kotlin
sealed interface P4TypeReference {
  val typeRef: P4Type.Named
}
```

`P4Typedef`, `P4Header`, `P4Struct` implement it:

```kotlin
data class P4Typedef(val name: String, val type: P4Type) : P4Declaration, P4TypeReference {
  override val typeRef get() = P4Type.Named(name)
}
```

Same pattern for `P4Header` and `P4Struct`. `P4Function` does not implement `P4TypeReference` (functions are not types).

### DSL (Dsl.kt)

`ProgramBuilder` methods return `ReadOnlyProperty` for use with `by`, following the existing `param` pattern:

```kotlin
fun typedef(type: P4Type): ReadOnlyProperty<Any?, P4Typedef> {
  var registered = false
  lateinit var decl: P4Typedef
  return ReadOnlyProperty { _, property ->
    if (!registered) {
      decl = p4Typedef(property.name, type)
      declarations.add(decl)
      registered = true
    }
    decl
  }
}
```

Same pattern for `header`, `struct`, `function`.

`FieldsBuilder` gets a new `field` overload:

```kotlin
fun field(name: String, type: P4TypeReference) {
  fields.add(P4Field(name, type.typeRef))
}
```

Standalone builders (`p4Typedef()`, `p4Header()`, etc.) keep their string-based API unchanged.

`typeName()` stays for referencing types not defined in the current program.

### Example

Before:

```kotlin
val program = p4Program {
  typedef("EthernetAddress", bit(48))
  header("Ethernet_h") {
    field("dstAddr", typeName("EthernetAddress"))
    field("etherType", bit(16))
  }
  struct("Parsed_packet") {
    field("ethernet", typeName("Ethernet_h"))
  }
}
```

After:

```kotlin
val program = p4Program {
  val EthernetAddress by typedef(bit(48))
  val Ethernet_h by header {
    field("dstAddr", EthernetAddress)
    field("etherType", bit(16))
  }
  val Parsed_packet by struct {
    field("ethernet", Ethernet_h)
  }
}
```

### Test strategy

- Unit tests for each delegated declaration type
- Unit test for `field()` with `P4TypeReference`
- Update golden tests (`identity.kt`, `vss_types.kt`) to use the new API
- Verify generated P4 output is unchanged

## Out of scope

- Object-based headers for IDE field navigation (deferred)
- Field name delegation (field names are not referenced elsewhere)
- Removing standalone string-based builders (still needed outside ProgramBuilder)
