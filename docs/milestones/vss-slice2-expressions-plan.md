# Expressions and Statements Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add types (bool, void), expressions (literals, field access, arithmetic, comparison), and statements (variable declaration, assignment, if/else) to P4kt.

**Architecture:** Extend IR sealed classes with new variants. Extract a `StatementBuilder` base class from `FunctionBuilder` for statement methods. Use a line-by-line indentation helper for nested statements (If) instead of threading an indent parameter. Always parenthesize `BinOp` for safety.

**Tech Stack:** Kotlin, Bazel, kotlin-test with JUnit 4.

---

## File structure

| File                       | Action | Responsibility                                                                                  |
| -------------------------- | ------ | ----------------------------------------------------------------------------------------------- |
| `p4kt/Ir.kt`               | Modify | Add Bool, Void, Lit, TypedLit, FieldAccess, BinOp, BinOpKind, VarDecl, Assign, If               |
| `p4kt/Dsl.kt`              | Modify | Add type factories, expression operators, StatementBuilder, IfBuilder, refactor FunctionBuilder |
| `p4kt/Emit.kt`             | Modify | Add toP4() for new nodes, indentation helper for nested statements                              |
| `p4kt/P4ExpressionTest.kt` | Create | Tests for all new expression types                                                              |
| `p4kt/P4StatementTest.kt`  | Create | Tests for varDecl, assign, if/else                                                              |
| `p4kt/BUILD.bazel`         | Modify | Add new test targets                                                                            |

---

## Chunk 1: Types and expressions

### Task 1: Bool, Void types and literal expressions

**Files:**

- Modify: `p4kt/Ir.kt`, `p4kt/Dsl.kt`, `p4kt/Emit.kt`
- Create: `p4kt/P4ExpressionTest.kt`
- Modify: `p4kt/BUILD.bazel`

- [ ] **Step 1: Write the failing test**

Create `p4kt/P4ExpressionTest.kt`:

```kotlin
package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ExpressionTest {
  @Test
  fun boolType() {
    assertEquals("bool", bool_.toP4())
  }

  @Test
  fun voidType() {
    assertEquals("void", void_.toP4())
  }

  @Test
  fun untypedLiteral() {
    assertEquals("42", lit(42).toP4())
  }

  @Test
  fun hexLiteral() {
    assertEquals("2048", lit(0x0800).toP4())
  }

  @Test
  fun typedLiteral() {
    assertEquals("4w4", lit(4, 4).toP4())
  }

  @Test
  fun typedLiteralZero() {
    assertEquals("16w0", lit(16, 0).toP4())
  }
}
```

Add test target to `p4kt/BUILD.bazel`:

```python
kt_jvm_test(
    name = "P4ExpressionTest",
    srcs = ["P4ExpressionTest.kt"],
    test_class = "p4kt.P4ExpressionTest",
    deps = [
        ":p4kt",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
        "@maven//:org_jetbrains_kotlin_kotlin_test_junit",
    ],
)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./dev bazel test //p4kt:P4ExpressionTest`
Expected: BUILD FAILURE

- [ ] **Step 3: Implement types and literals**

In `p4kt/Ir.kt`, add to `P4Type`:

```kotlin
data object Bool : P4Type()
data object Void : P4Type()
```

Add to `P4Expr`:

```kotlin
data class Lit(val value: Long) : P4Expr()
data class TypedLit(val width: Int, val value: Long) : P4Expr()
```

In `p4kt/Dsl.kt`, add:

```kotlin
val bool_ = P4Type.Bool
val void_ = P4Type.Void

fun lit(value: Long) = P4Expr.Lit(value)
fun lit(value: Int) = P4Expr.Lit(value.toLong())
fun lit(width: Int, value: Long) = P4Expr.TypedLit(width, value)
fun lit(width: Int, value: Int) = P4Expr.TypedLit(width, value.toLong())
```

In `p4kt/Emit.kt`, add to `P4Type.toP4()` when clause:

```kotlin
is P4Type.Bool -> "bool"
is P4Type.Void -> "void"
```

Add to `P4Expr.toP4()` when clause:

```kotlin
is P4Expr.Lit -> "$value"
is P4Expr.TypedLit -> "${width}w${value}"
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./dev bazel test //p4kt:P4ExpressionTest`
Expected: PASS

### Task 2: Field access, arithmetic, and comparison

**Files:**

- Modify: `p4kt/Ir.kt`, `p4kt/Dsl.kt`, `p4kt/Emit.kt`
- Modify: `p4kt/P4ExpressionTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `P4ExpressionTest.kt`:

```kotlin
@Test
fun fieldAccess() {
  val headers = P4Expr.Ref("headers")
  assertEquals("headers.ip", headers.dot("ip").toP4())
}

@Test
fun chainedFieldAccess() {
  val headers = P4Expr.Ref("headers")
  assertEquals("headers.ip.ttl", headers.dot("ip").dot("ttl").toP4())
}

@Test
fun subtraction() {
  val a = P4Expr.Ref("a")
  val b = P4Expr.Ref("b")
  assertEquals("(a - b)", (a - b).toP4())
}

@Test
fun equality() {
  val a = P4Expr.Ref("a")
  val b = P4Expr.Ref("b")
  assertEquals("(a == b)", (a eq b).toP4())
}

@Test
fun inequality() {
  val a = P4Expr.Ref("a")
  val b = P4Expr.Ref("b")
  assertEquals("(a != b)", (a ne b).toP4())
}

@Test
fun nestedExpression() {
  val headers = P4Expr.Ref("headers")
  val ttl = headers.dot("ip").dot("ttl")
  assertEquals("(headers.ip.ttl - 1)", (ttl - lit(1)).toP4())
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./dev bazel test //p4kt:P4ExpressionTest`
Expected: BUILD FAILURE

- [ ] **Step 3: Implement field access and operators**

In `p4kt/Ir.kt`, add to `P4Expr`:

```kotlin
data class FieldAccess(val expr: P4Expr, val field: String) : P4Expr()
data class BinOp(val op: BinOpKind, val left: P4Expr, val right: P4Expr) : P4Expr()
```

Add new enum (outside sealed classes):

```kotlin
enum class BinOpKind { SUB, EQ, NE }
```

In `p4kt/Dsl.kt`, add:

```kotlin
fun P4Expr.dot(field: String) = P4Expr.FieldAccess(this, field)
operator fun P4Expr.minus(other: P4Expr) = P4Expr.BinOp(BinOpKind.SUB, this, other)
infix fun P4Expr.eq(other: P4Expr) = P4Expr.BinOp(BinOpKind.EQ, this, other)
infix fun P4Expr.ne(other: P4Expr) = P4Expr.BinOp(BinOpKind.NE, this, other)
```

In `p4kt/Emit.kt`, add to `P4Expr.toP4()` when clause:

```kotlin
is P4Expr.FieldAccess -> "${expr.toP4()}.$field"
is P4Expr.BinOp -> "(${left.toP4()} ${op.toP4()} ${right.toP4()})"
```

Add `BinOpKind.toP4()`:

```kotlin
fun BinOpKind.toP4(): String =
  when (this) {
    BinOpKind.SUB -> "-"
    BinOpKind.EQ -> "=="
    BinOpKind.NE -> "!="
  }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./dev bazel test //p4kt:P4ExpressionTest`
Expected: PASS

- [ ] **Step 5: Run all tests**

Run: `./dev bazel test //...`
Expected: All PASS

- [ ] **Step 6: Commit**

```
git add p4kt/Ir.kt p4kt/Dsl.kt p4kt/Emit.kt p4kt/P4ExpressionTest.kt p4kt/BUILD.bazel
git commit -m "Add types (bool, void) and expressions (literals, field access, operators)

Adds Bool and Void types, untyped and typed literals, field access,
and binary operators (subtraction, equality, inequality) with
parenthesization. Foundation for VSS actions and control blocks.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

## Chunk 2: Statements and StatementBuilder

### Task 3: StatementBuilder and varDecl/assign

**Files:**

- Modify: `p4kt/Ir.kt`, `p4kt/Dsl.kt`, `p4kt/Emit.kt`
- Create: `p4kt/P4StatementTest.kt`
- Modify: `p4kt/BUILD.bazel`

- [ ] **Step 1: Write the failing test**

Create `p4kt/P4StatementTest.kt`:

```kotlin
package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4StatementTest {
  @Test
  fun varDeclWithInit() {
    val fn =
      p4Function("test", void_) {
        val x by param(bit(8), IN)
        val tmp by varDecl(bit(8), x)
      }

    assertEquals(
      """
            function void test(in bit<8> x) {
                bit<8> tmp = x;
            }
            """
        .trimIndent(),
      fn.toP4(),
    )
  }

  @Test
  fun varDeclWithoutInit() {
    val fn =
      p4Function("test", void_) {
        val tmp by varDecl(bit(8))
      }

    assertEquals(
      """
            function void test() {
                bit<8> tmp;
            }
            """
        .trimIndent(),
      fn.toP4(),
    )
  }

  @Test
  fun assignment() {
    val fn =
      p4Function("swap", void_) {
        val x by param(bit(8), INOUT)
        val y by param(bit(8), INOUT)
        val tmp by varDecl(bit(8), x)
        assign(x, y)
        assign(y, tmp)
      }

    assertEquals(
      """
            function void swap(inout bit<8> x, inout bit<8> y) {
                bit<8> tmp = x;
                x = y;
                y = tmp;
            }
            """
        .trimIndent(),
      fn.toP4(),
    )
  }
}
```

Add test target to `p4kt/BUILD.bazel`:

```python
kt_jvm_test(
    name = "P4StatementTest",
    srcs = ["P4StatementTest.kt"],
    test_class = "p4kt.P4StatementTest",
    deps = [
        ":p4kt",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
        "@maven//:org_jetbrains_kotlin_kotlin_test_junit",
    ],
)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./dev bazel test //p4kt:P4StatementTest`
Expected: BUILD FAILURE

- [ ] **Step 3: Implement VarDecl, Assign, and StatementBuilder**

In `p4kt/Ir.kt`, add to `P4Statement`:

```kotlin
data class VarDecl(val name: String, val type: P4Type, val init: P4Expr?) : P4Statement()
data class Assign(val target: P4Expr, val value: P4Expr) : P4Statement()
```

In `p4kt/Dsl.kt`, extract `StatementBuilder` base class from `FunctionBuilder`:

```kotlin
open class StatementBuilder {
  protected val body = mutableListOf<P4Statement>()

  fun varDecl(type: P4Type, init: P4Expr? = null): ReadOnlyProperty<Any?, P4Expr.Ref> {
    var registered = false
    return ReadOnlyProperty { _, property ->
      if (!registered) {
        body.add(P4Statement.VarDecl(property.name, type, init))
        registered = true
      }
      P4Expr.Ref(property.name)
    }
  }

  fun assign(target: P4Expr, value: P4Expr) {
    body.add(P4Statement.Assign(target, value))
  }

  fun statements() = body.toList()
}

class FunctionBuilder(
  private val name: String,
  private val returnType: P4Type,
) : StatementBuilder() {
  private val params = mutableListOf<P4Param>()

  fun param(type: P4Type, direction: Direction): ReadOnlyProperty<Any?, P4Expr.Ref> {
    var registered = false
    return ReadOnlyProperty { _, property ->
      if (!registered) {
        params.add(P4Param(property.name, type, direction))
        registered = true
      }
      P4Expr.Ref(property.name)
    }
  }

  fun return_(expr: P4Expr) {
    body.add(P4Statement.Return(expr))
  }

  fun build() = P4Function(name, returnType, params, body)
}
```

In `p4kt/Emit.kt`, add to `P4Statement.toP4()` when clause:

```kotlin
is P4Statement.VarDecl -> if (init != null) {
  "${type.toP4()} $name = ${init.toP4()};"
} else {
  "${type.toP4()} $name;"
}
is P4Statement.Assign -> "${target.toP4()} = ${value.toP4()};"
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./dev bazel test //p4kt:P4StatementTest`
Expected: PASS

### Task 4: If/else statement

**Files:**

- Modify: `p4kt/Ir.kt`, `p4kt/Dsl.kt`, `p4kt/Emit.kt`
- Modify: `p4kt/P4StatementTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `P4StatementTest.kt`:

```kotlin
@Test
fun ifStatement() {
  val fn =
    p4Function("test", void_) {
      val x by param(bit(8), IN)
      if_(x eq lit(0)) {
        return_(lit(1))
      }
    }

  assertEquals(
    """
          function void test(in bit<8> x) {
              if ((x == 0)) {
                  return 1;
              }
          }
          """
      .trimIndent(),
    fn.toP4(),
  )
}

@Test
fun ifElseStatement() {
  val fn =
    p4Function("max", bit(8)) {
      val a by param(bit(8), IN)
      val b by param(bit(8), IN)
      if_(a eq b) {
        return_(a)
      }.else_ {
        return_(b)
      }
    }

  assertEquals(
    """
          function bit<8> max(in bit<8> a, in bit<8> b) {
              if ((a == b)) {
                  return a;
              } else {
                  return b;
              }
          }
          """
      .trimIndent(),
    fn.toP4(),
  )
}

@Test
fun nestedIfInFunction() {
  val fn =
    p4Function("test", void_) {
      val x by param(bit(8), IN)
      if_(x eq lit(0)) {
        if_(x ne lit(1)) {
          return_(x)
        }
      }
    }

  assertEquals(
    """
          function void test(in bit<8> x) {
              if ((x == 0)) {
                  if ((x != 1)) {
                      return x;
                  }
              }
          }
          """
      .trimIndent(),
    fn.toP4(),
  )
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./dev bazel test //p4kt:P4StatementTest`
Expected: BUILD FAILURE

- [ ] **Step 3: Implement If statement, IfBuilder, and indentation helper**

In `p4kt/Ir.kt`, add to `P4Statement`:

```kotlin
data class If(
  val condition: P4Expr,
  val thenBody: List<P4Statement>,
  val elseBody: List<P4Statement>,
) : P4Statement()
```

In `p4kt/Dsl.kt`, add `if_` to `StatementBuilder` and `IfBuilder`:

```kotlin
// In StatementBuilder:
fun if_(condition: P4Expr, block: StatementBuilder.() -> Unit): IfBuilder {
  val thenBuilder = StatementBuilder()
  thenBuilder.block()
  body.add(P4Statement.If(condition, thenBuilder.statements(), emptyList()))
  return IfBuilder(body, body.size - 1)
}

class IfBuilder(
  private val parentBody: MutableList<P4Statement>,
  private val index: Int,
) {
  infix fun else_(block: StatementBuilder.() -> Unit) {
    val elseBuilder = StatementBuilder()
    elseBuilder.block()
    val oldIf = parentBody[index] as P4Statement.If
    parentBody[index] = oldIf.copy(elseBody = elseBuilder.statements())
  }
}
```

In `p4kt/Emit.kt`, add indentation helper and If emit:

```kotlin
fun indentBlock(statements: List<P4Statement>, indent: String): String =
  statements.joinToString("\n") { stmt ->
    stmt.toP4().lines().joinToString("\n") { "$indent$it" }
  }
```

Add to `P4Statement.toP4()` when clause:

```kotlin
is P4Statement.If -> {
  val thenStr = indentBlock(thenBody, "    ")
  if (elseBody.isEmpty()) {
    "if (${condition.toP4()}) {\n$thenStr\n}"
  } else {
    val elseStr = indentBlock(elseBody, "    ")
    "if (${condition.toP4()}) {\n$thenStr\n} else {\n$elseStr\n}"
  }
}
```

Update `P4Function.toP4()` to use `indentBlock` for correct multi-line statement indentation:

```kotlin
fun P4Function.toP4(): String {
  val paramStr = params.joinToString(", ") { it.toP4() }
  val bodyStr = indentBlock(body, "    ")
  return "function ${returnType.toP4()} $name($paramStr) {\n$bodyStr\n}"
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./dev bazel test //p4kt:P4StatementTest`
Expected: PASS

- [ ] **Step 5: Run all tests**

Run: `./dev bazel test //...`
Expected: All PASS

- [ ] **Step 6: Commit**

```
git add p4kt/Ir.kt p4kt/Dsl.kt p4kt/Emit.kt p4kt/P4StatementTest.kt p4kt/BUILD.bazel
git commit -m "Add statements (varDecl, assign, if/else) with StatementBuilder

Extracts StatementBuilder base class from FunctionBuilder for reuse
by future action/control builders. Adds variable declarations,
assignments, and if/else with correct nested indentation.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```
