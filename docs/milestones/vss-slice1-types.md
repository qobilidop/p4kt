# Typedef, Header, and Struct Support

## Goal

Add typedef, header, and struct declarations to P4kt - the foundational type constructs needed for the VSS milestone. These are the first slice of VSS support, chosen because nearly every other P4 construct operates on headers and structs.

## Scope

Three new top-level declarations:

- `typedef` - type alias (e.g., `typedef bit<48> EthernetAddress;`)
- `header` - named set of typed fields (e.g., `header Ethernet_h { ... }`)
- `struct` - named set of typed fields (e.g., `struct Parsed_packet { ... }`)

Plus one new type variant for referencing declared types by name.

## Design

### IR (Ir.kt)

New `P4Type` variant:

```kotlin
data class Named(val name: String) : P4Type()
```

New shared data class for fields:

```kotlin
data class P4Field(val name: String, val type: P4Type)
```

New top-level data classes (alongside `P4Function`):

```kotlin
data class P4Typedef(val name: String, val type: P4Type)
data class P4Header(val name: String, val fields: List<P4Field>)
data class P4Struct(val name: String, val fields: List<P4Field>)
```

### DSL (Dsl.kt)

Type reference factory:

```kotlin
fun typeName(name: String) = P4Type.Named(name)
```

Typedef builder:

```kotlin
fun p4Typedef(name: String, type: P4Type) = P4Typedef(name, type)
```

Shared fields builder for header and struct:

```kotlin
class FieldsBuilder {
    private val fields = mutableListOf<P4Field>()

    fun field(name: String, type: P4Type) {
        fields.add(P4Field(name, type))
    }

    fun build() = fields.toList()
}
```

Header and struct builders:

```kotlin
fun p4Header(name: String, block: FieldsBuilder.() -> Unit): P4Header {
    val builder = FieldsBuilder()
    builder.block()
    return P4Header(name, builder.build())
}

fun p4Struct(name: String, block: FieldsBuilder.() -> Unit): P4Struct {
    val builder = FieldsBuilder()
    builder.block()
    return P4Struct(name, builder.build())
}
```

### Emit (Emit.kt)

```kotlin
fun P4Type.Named.toP4(): String = name  // added to P4Type.toP4() when clause

fun P4Field.toP4(): String = "${type.toP4()} $name;"

fun P4Typedef.toP4(): String = "typedef ${type.toP4()} $name;"

fun P4Header.toP4(): String {
    val fieldsStr = fields.joinToString("\n") { "    ${it.toP4()}" }
    return "header $name {\n$fieldsStr\n}"
}

fun P4Struct.toP4(): String {
    val fieldsStr = fields.joinToString("\n") { "    ${it.toP4()}" }
    return "struct $name {\n$fieldsStr\n}"
}
```

### Test strategy

Unit tests in `p4kt/` for each construct:

- Typedef with bit type
- Typedef with named type (chained typedefs)
- Header with mixed field types
- Struct referencing header types

Golden test in `examples/` using the VSS type declarations (typedefs, Ethernet header, IPv4 header, Parsed_packet struct) to validate the complete flow.

## Out of scope

- Field access expressions (e.g., `headers.ip.dstAddr`)
- Header validity (`isValid()`, `setValid()`, `setInvalid()`)
- Header stacks
- Alignment or padding in emitted field formatting
