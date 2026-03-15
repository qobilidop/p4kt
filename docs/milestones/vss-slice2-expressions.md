# Expressions and Statements

## Goal

Add the expression and statement infrastructure needed for VSS actions and control blocks: types (bool, void), expressions (literals, field access, arithmetic, comparison), and statements (variable declaration, assignment, if/else).

## Design

### IR (Ir.kt)

**New types:**

```kotlin
data object Bool : P4Type()
data object Void : P4Type()
```

**New expressions:**

```kotlin
data class Lit(val value: Long) : P4Expr()
data class TypedLit(val width: Int, val value: Long) : P4Expr()
data class FieldAccess(val expr: P4Expr, val field: String) : P4Expr()
data class BinOp(val op: BinOpKind, val left: P4Expr, val right: P4Expr) : P4Expr()

enum class BinOpKind { SUB, EQ, NE }
```

**New statements:**

```kotlin
data class VarDecl(val name: String, val type: P4Type, val init: P4Expr?) : P4Statement()
data class Assign(val target: P4Expr, val value: P4Expr) : P4Statement()
data class If(
  val condition: P4Expr,
  val thenBody: List<P4Statement>,
  val elseBody: List<P4Statement>,
) : P4Statement()
```

### DSL (Dsl.kt)

**Type factories:**

```kotlin
val bool_ = P4Type.Bool
val void_ = P4Type.Void
```

**Expression factories** (top-level, usable in any builder):

```kotlin
fun lit(value: Long) = P4Expr.Lit(value)
fun lit(width: Int, value: Long) = P4Expr.TypedLit(width, value)
```

**Expression operators** (extensions on `P4Expr`):

```kotlin
fun P4Expr.dot(field: String) = P4Expr.FieldAccess(this, field)
operator fun P4Expr.minus(other: P4Expr) = P4Expr.BinOp(BinOpKind.SUB, this, other)
infix fun P4Expr.eq(other: P4Expr) = P4Expr.BinOp(BinOpKind.EQ, this, other)
infix fun P4Expr.ne(other: P4Expr) = P4Expr.BinOp(BinOpKind.NE, this, other)
```

**Statement builders:**

`FunctionBuilder` already has `return_()`. Add a `StatementBuilder` base that both `FunctionBuilder` and future action/control builders share:

```kotlin
open class StatementBuilder {
  protected val body = mutableListOf<P4Statement>()

  fun varDecl(type: P4Type, init: P4Expr? = null): ReadOnlyProperty<Any?, P4Expr.Ref>
  fun assign(target: P4Expr, value: P4Expr)
  fun if_(condition: P4Expr, block: StatementBuilder.() -> Unit): IfBuilder
}
```

`varDecl` follows the existing `param` delegation pattern - captures the variable name via `property.name`, adds a `VarDecl` statement to the body, and returns a `P4Expr.Ref`.

`FunctionBuilder` extends `StatementBuilder` and adds `param()` and `return_()`.

**IfBuilder mechanism:**

`if_` creates a `StatementBuilder`, runs the block to collect `thenBody` statements, creates an `If` node with empty `elseBody`, adds it to the parent's statement list, and returns an `IfBuilder` holding a reference to the parent's statement list and the index of the `If` node.

`IfBuilder.else_` creates another `StatementBuilder`, runs the block to collect `elseBody` statements, and replaces the `If` node in the parent's list with a new one containing the `elseBody`.

```kotlin
class IfBuilder(
  private val parentBody: MutableList<P4Statement>,
  private val index: Int,
) {
  fun else_(block: StatementBuilder.() -> Unit) {
    val builder = StatementBuilder()
    builder.block()
    val oldIf = parentBody[index] as P4Statement.If
    parentBody[index] = oldIf.copy(elseBody = builder.statements())
  }
}
```

### Emit (Emit.kt)

**Parenthesization:** `BinOp.toP4()` always wraps in parentheses for safety. This avoids precedence bugs at the cost of slightly verbose output. Optimize later if needed.

**Indentation:** Statement `toP4()` takes an `indent` parameter (number of spaces). Default is 0 for top-level. Each nesting level adds 4 spaces.

```
P4Type.Bool → "bool"
P4Type.Void → "void"

P4Expr.Lit → "$value"
P4Expr.TypedLit → "${width}w${value}"
P4Expr.FieldAccess → "${expr.toP4()}.${field}"
P4Expr.BinOp(SUB) → "(${left.toP4()} - ${right.toP4()})"
P4Expr.BinOp(EQ) → "(${left.toP4()} == ${right.toP4()})"
P4Expr.BinOp(NE) → "(${left.toP4()} != ${right.toP4()})"

P4Statement.VarDecl (with init) → "${type.toP4()} $name = ${init.toP4()};"
P4Statement.VarDecl (no init) → "${type.toP4()} $name;"
P4Statement.Assign → "${target.toP4()} = ${value.toP4()};"
P4Statement.If (no else) →
    "if (${cond.toP4()}) {\n${thenBody with indent+4}\n${indent}}"
P4Statement.If (with else) →
    "if (${cond.toP4()}) {\n${thenBody with indent+4}\n${indent}} else {\n${elseBody with indent+4}\n${indent}}"
```

### Test strategy

Unit tests for each construct:

- Types: bool and void rendering
- Expressions: untyped lit, typed lit, field access, arithmetic, comparison, nested expressions (verify parenthesization)
- Statements: varDecl with/without init, assignment, if, if/else (verify indentation)
- Integration: a function using varDecl, assign, if/else together

## Out of scope

- Bitwise operators (add when needed)
- Logical operators (add when needed)
- Additional comparison operators (gt, ge, lt, le - add when needed)
- Else-if chaining (add when needed)
- `int<W>` signed integer type (deferred from v0.1 - add when needed)
