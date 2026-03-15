# Typed Field Access for P4kt

## Problem

Field access in the current DSL is string-based:

```kotlin
outCtrl.dot("outputPort")
headers_ip.dot("ttl")
```

This is unreadable, error-prone, and provides no IDE autocomplete. Without type safety on field access, P4kt offers little advantage over writing P4 directly.

## Goal

Replace string-based field access with real Kotlin property access:

```kotlin
outCtrl.outputPort
headers_ip.ttl
```

Full autocomplete, go-to-definition, and compile-time checking on all field accesses.

## Approach

P4 struct and header types become Kotlin classes with real properties. Each class wraps a `P4Expr` and exposes fields as properties that produce `P4Expr.FieldAccess` nodes. No code generation - pure Kotlin language features.

This supersedes the "Object-based headers for IDE navigation" idea in `docs/milestones/dsl-ergonomics.md` with a more complete design.

## Design

### Ref base classes

Two abstract base classes in the `p4kt` library:

```kotlin
abstract class StructRef(val expr: P4Expr) {
    fun field(type: P4Type): FieldDelegate { ... }
    fun field(type: P4TypeReference): FieldDelegate { ... }
    inline fun <reified T : StructRef> field(): TypedFieldDelegate<T> { ... }
}

abstract class HeaderRef(expr: P4Expr) : StructRef(expr)
```

- `StructRef` wraps a `P4Expr` and provides `field()` methods that return property delegates.
- `HeaderRef` extends `StructRef` for headers (allows distinguishing struct vs header in IR generation).

### User-defined type classes

Users define P4 types as Kotlin classes:

```kotlin
class Ipv4_h(base: P4Expr) : HeaderRef(base) {
    val ttl by field(bit(8))
    val srcAddr by field(IPv4Address)
    val dstAddr by field(IPv4Address)
}

class OutControl(base: P4Expr) : StructRef(base) {
    val outputPort by field(PortId)
}
```

These are local classes inside `p4Program { }`, keeping everything in one scope. Class names use P4-style naming conventions (e.g., `Ipv4_h` not `Ipv4H`) since the class name becomes the P4 type name.

### Field delegates

Two delegate types:

**`FieldDelegate`** - for leaf fields (bit types, typedefs). Uses `provideDelegate` to capture the property name. `getValue` returns `P4Expr.FieldAccess(expr, fieldName)`. Also appends `P4Field(name, type)` to a `MutableList<P4Field>` on the `StructRef` base class for IR generation.

**`TypedFieldDelegate<T>`** - for nested struct/header fields. Instantiates `T` via reflection (`T::class.constructors.first().call(fieldAccessExpr)`). `getValue` returns the `T` instance wrapping `P4Expr.FieldAccess(expr, fieldName)`, enabling chained access like `headers.ip.ttl`. All three sites that instantiate ref classes via reflection - `TypedFieldDelegate`, `TypedParamDelegate`, and `struct<T>()`/`header<T>()` - share the same mechanism and must be validated together in a spike.

### Typed params

A new `param` overload using reified generics:

```kotlin
inline fun <reified T : StructRef> param(direction: Direction? = null): TypedParamDelegate<T>
```

`TypedParamDelegate<T>` does two things in `provideDelegate`:

1. Adds `P4Param(name, P4Type.Named(T::class.simpleName!!), direction)` to the params list.
2. Returns `T(P4Expr.Ref(name))` via reflection.

Leaf-type params (`val port by param(PortId)`) are unchanged - they still return `P4Expr.Ref`.

### Explicit type registration

Struct/header classes must be explicitly registered in the program builder to generate IR declarations:

```kotlin
class OutControl(base: P4Expr) : StructRef(base) {
    val outputPort by field(PortId)
}
struct<OutControl>()
```

`struct<T>()` and `header<T>()` instantiate the class with a dummy expression (e.g., `P4Expr.Ref("")`), which triggers all field delegates to run and populate the `fields` list on the `StructRef`. The function then reads that list and emits a `P4Struct` or `P4Header` IR node with `T::class.simpleName!!` as the type name.

This is preferred over auto-registration because it's explicit and predictable, mirroring P4 where type declarations are explicit.

### DSL API additions

Beyond the new ref class infrastructure, several existing DSL functions gain `P4TypeReference` overloads so that typedef/header/struct declarations can be passed directly instead of calling `.typeRef`:

- `param(type: P4TypeReference)` and `param(type: P4TypeReference, direction: Direction)` - for leaf-type params like `val port by param(PortId)`.
- `const_(type: P4TypeReference, value: P4Expr)` - for const declarations like `val DROP_PORT by const_(PortId, lit(4, 0xF))`.
- `ref(name: String)` - convenience function for `P4Expr.Ref(name)`, for referencing variables not defined in the current scope.

### Unchanged constructs

- `typedef` and `const_` keep the `by` delegate pattern for name capture.
- `action` and `function` declarations keep the `by` delegate pattern.
- `assign()`, `if_()`, `return_()` and other statement builders are unchanged.
- The IR layer (`P4Expr`, `P4Statement`, `P4Type`, etc.) is unchanged.
- The renderer (`Emit.kt`) is unchanged.

### Using ref expressions in statements

`StructRef` instances need to be usable wherever `P4Expr` is expected (e.g., in `assign()`, `operator fun minus`). Options:

- Implicit conversion via `expr` property: `assign(outCtrl.expr, ...)` - explicit but verbose.
- Overloads on `StatementBuilder` that accept `StructRef` and unwrap to `expr` - cleaner at call sites.
- Make `StructRef` implement an interface that `assign()` accepts.

The preferred approach is overloads or a common interface, so usage sites stay clean: `assign(outCtrl.outputPort, DROP_PORT)` works because `outputPort` is already a `P4Expr`. The ref itself (`outCtrl`) rarely needs to be used as a raw expression - it's the fields that appear in statements.

## Complete example

Before (current):

```kotlin
val program = p4Program {
    val PortId by typedef(bit(4))
    val IPv4Address by typedef(bit(32))
    val DROP_PORT by const_(PortId.typeRef, lit(4, 0xF))

    val Ipv4_h by header {
        field("ttl", bit(8))
        field("srcAddr", IPv4Address)
        field("dstAddr", IPv4Address)
    }
    val OutControl by struct { field("outputPort", PortId) }

    val Set_nhop by action {
        val ipv4_dest by param(IPv4Address.typeRef)
        val port by param(PortId.typeRef)
        val headers_ip by param(typeName("Ipv4_h"), INOUT)
        val outCtrl by param(typeName("OutControl"), INOUT)
        assign(P4Expr.Ref("nextHop"), ipv4_dest)
        assign(headers_ip.dot("ttl"), headers_ip.dot("ttl") - lit(1))
        assign(outCtrl.dot("outputPort"), port)
    }
}
```

After (typed field access):

```kotlin
val program = p4Program {
    val PortId by typedef(bit(4))
    val IPv4Address by typedef(bit(32))
    val DROP_PORT by const_(PortId, lit(4, 0xF))

    class Ipv4_h(base: P4Expr) : HeaderRef(base) {
        val ttl by field(bit(8))
        val srcAddr by field(IPv4Address)
        val dstAddr by field(IPv4Address)
    }
    header<Ipv4_h>()

    class OutControl(base: P4Expr) : StructRef(base) {
        val outputPort by field(PortId)
    }
    struct<OutControl>()

    val Set_nhop by action {
        val ipv4_dest by param(IPv4Address)
        val port by param(PortId)
        val headers_ip by param<Ipv4_h>(INOUT)
        val outCtrl by param<OutControl>(INOUT)
        assign(ref("nextHop"), ipv4_dest)
        assign(headers_ip.ttl, headers_ip.ttl - lit(1))
        assign(outCtrl.outputPort, port)
    }
}
```

## Impact on existing code

- `P4Expr.dot()` extension function is removed.
- `FieldsBuilder` is no longer used for struct/header definitions (may be kept if other constructs still need it).
- `typeName()` is no longer needed for types defined in the same program.
- Existing tests for field access expressions (`P4ExpressionTest`) remain valid since the IR is unchanged - only the DSL layer changes.
- Example files (`vss_actions.kt`, `ttl_decrement.kt`) are updated to use the new syntax.

## Risks and open questions

- **Local class reflection**: `TypedParamDelegate` and `struct<T>()`/`header<T>()` need to instantiate local classes via reflection. Kotlin local classes should work with `T::class.constructors`, but this needs a spike to confirm.
- **P4 name vs Kotlin name**: The class name becomes the P4 type name. If P4 conventions differ (e.g., `Ipv4_h` vs `Ipv4H`), we need a naming convention or an annotation. For now, use P4-style names as class names, consistent with the existing convention for delegates.
- **Field delegate ordering**: The delegates must record fields in declaration order. Kotlin guarantees property initialization order matches declaration order, so this should work.
