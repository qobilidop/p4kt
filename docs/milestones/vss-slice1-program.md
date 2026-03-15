# P4Program Node

## Goal

Introduce a `P4Program` node that collects top-level declarations and renders them with a single `toP4()` call, replacing manual multi-`println` output in examples.

## Design

### IR (Ir.kt)

New sealed interface for top-level declarations:

```kotlin
sealed interface P4Declaration
```

Existing types implement it:

```kotlin
data class P4Function(...) : P4Declaration
data class P4Typedef(...) : P4Declaration
data class P4Header(...) : P4Declaration
data class P4Struct(...) : P4Declaration
```

New program node:

```kotlin
data class P4Program(val declarations: List<P4Declaration>)
```

### DSL (Dsl.kt)

```kotlin
class ProgramBuilder {
  private val declarations = mutableListOf<P4Declaration>()

  fun typedef(name: String, type: P4Type): P4Typedef {
    val decl = p4Typedef(name, type)
    declarations.add(decl)
    return decl
  }

  fun header(name: String, block: FieldsBuilder.() -> Unit): P4Header {
    val decl = p4Header(name, block)
    declarations.add(decl)
    return decl
  }

  fun struct(name: String, block: FieldsBuilder.() -> Unit): P4Struct {
    val decl = p4Struct(name, block)
    declarations.add(decl)
    return decl
  }

  fun function(name: String, returnType: P4Type, block: FunctionBuilder.() -> Unit): P4Function {
    val decl = p4Function(name, returnType, block)
    declarations.add(decl)
    return decl
  }

  fun build() = P4Program(declarations.toList())
}

fun p4Program(block: ProgramBuilder.() -> Unit): P4Program {
  val builder = ProgramBuilder()
  builder.block()
  return builder.build()
}
```

Standalone `p4Typedef()`, `p4Header()`, `p4Struct()`, `p4Function()` remain unchanged.

### Emit (Emit.kt)

```kotlin
fun P4Declaration.toP4(): String =
  when (this) {
    is P4Function -> (this as P4Function).toP4()
    is P4Typedef -> (this as P4Typedef).toP4()
    is P4Header -> (this as P4Header).toP4()
    is P4Struct -> (this as P4Struct).toP4()
  }

fun P4Program.toP4(): String =
  declarations.joinToString("\n\n") { it.toP4() }
```

### Test

- Unit test for `P4Program.toP4()` composing multiple declarations
- Update `examples/vss_types.kt` golden test to use `p4Program { ... }`
- Update `examples/vss_types.p4` golden file - all declarations now separated by blank lines (including consecutive typedefs, which were previously separated by a single newline)
- Update `examples/identity.kt` to use `p4Program { ... }` for consistency

## Out of scope

- `#include` preamble generation
- Declaration ordering or validation
