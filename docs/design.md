# P4kt v0.1 Design

## Goal

Provide an ergonomic Kotlin eDSL for writing P4 functions, targeting developers already familiar with P4. The eDSL should feel natural to P4 programmers while leveraging Kotlin's type system for safety.

v0.1 focuses on functions and the types/expressions/statements they need. Broader P4 constructs (headers, parsers, tables, controls) come later.

## Architecture

Three layers, all in the `p4kt/` package (flat layout, split later when warranted):

```
User code (DSL) → IR (immutable data classes) → Renderer (P4 source text)
```

1. **IR** - Sealed class hierarchies for types, expressions, statements, and declarations. Immutable data classes. This is the core.
2. **DSL** - Mutable builder classes that produce IR nodes. Uses Kotlin delegated properties for type-safe variable references. The DSL is the public API; the IR is an internal implementation detail.
3. **Renderer** - Traverses IR nodes, emits P4 source text via `toP4()` extension functions. `toP4()` on a function declaration returns a complete, formatted P4 function definition as a string using 4-space indentation, with no preamble or includes.

## Scope

### Types

- `bit<W>` - unsigned integer of width W
- `int<W>` - signed integer of width W
- `bool`
- `void`

IR representation: sealed class `P4Type` with data class variants. DSL factory functions: `bit(8)`, `int_(16)`, `bool_`, `void_`.

### Expressions

- Literals (integer, boolean)
- Variable references (type-safe via Kotlin delegated properties)
- Arithmetic: `+`, `-`, `*`
- Bitwise: `band`, `bor`, `bxor`, `bnot`, `shl`, `shr`
- Comparison: `eq`, `ne`, `gt`, `ge`, `lt`, `le`
- Logical: `and`, `or`, `not`

Operators use named functions (e.g. `a gt b`) because Kotlin's operator overloading constraints prevent returning P4 expression nodes from operators like `>`.

Arithmetic operators (`+`, `-`, `*`) use Kotlin's operator overloading (which allows custom return types). Comparison and bitwise operators use named functions because Kotlin constrains `>`, `<`, `==` to return `Boolean`/`Int`.

Integer and boolean literals use `lit(...)` to wrap Kotlin values into P4 expressions. `lit(n)` produces an untyped integer literal in the IR, matching P4's semantics where integer literals have no inherent width and are coerced by context. The P4 compiler handles width inference.

### Statements

- Variable declaration: `val tmp by varDecl(bit(8), initialValue)` - initial value is optional (P4 allows `bit<8> tmp;`)
- Assignment: `assign(x, y)`
- If/else: `if_(condition) { ... }.else_ { ... }` - `else_` chains on the return value of `if_`
- Return: `return_(expr)`

Trailing underscores on `if_`, `else_`, `return_` avoid Kotlin keyword collisions. Type factory functions follow the same rule: `int_`, `bool_`, `void_` have underscores because they collide with Kotlin keywords or built-in types; `bit` does not.

### Declarations

- Functions: `p4Function(name, returnType) { ... }`
- Parameters with direction: `val a by param(bit(8), IN)` (directions: `IN`, `OUT`, `INOUT`)

## DSL examples

### Simple function

P4:
```p4
function bit<8> max(in bit<8> a, in bit<8> b) {
    if (a > b) {
        return a;
    }
    return b;
}
```

P4kt:
```kotlin
val max = p4Function("max", bit(8)) {
    val a by param(bit(8), IN)
    val b by param(bit(8), IN)

    if_(a gt b) {
        return_(a)
    }
    return_(b)
}
```

### Void function with out parameters

P4:
```p4
function void swap(inout bit<8> x, inout bit<8> y) {
    bit<8> tmp = x;
    x = y;
    y = tmp;
}
```

P4kt:
```kotlin
val swap = p4Function("swap", void_) {
    val x by param(bit(8), INOUT)
    val y by param(bit(8), INOUT)

    val tmp by varDecl(bit(8), x)
    assign(x, y)
    assign(y, tmp)
}
```

### Function with bool and bitwise ops

P4:
```p4
function bool is_even(in bit<16> n) {
    return (n & 1) == 0;
}
```

P4kt:
```kotlin
val isEven = p4Function("is_even", bool_) {
    val n by param(bit(16), IN)

    return_((n band lit(1)) eq lit(0))
}
```

## Design decisions

- **Builder-centric**: DSL builders are the API. IR is internal. Expose IR only if a use case arises.
- **Flat package layout**: Everything in `p4kt/`. Split into sub-packages when complexity warrants it.
- **Named operators over symbols**: Kotlin can't overload comparison/bitwise operators to return expression nodes. Named operators (`gt`, `band`) are the standard eDSL trade-off.
- **`lit()` for literals**: Required to bridge Kotlin values into P4 expression trees.
- **Trailing underscores for keywords**: `if_`, `return_`, `else_` - a common Kotlin eDSL convention.

## Out of scope

- Headers, structs, typedefs
- Parsers, tables, actions, controls, externs
- `else if` chaining (add when needed)
- Validation (type checking, scope checking)
- Direct IR construction API
- Transformation or optimization passes
