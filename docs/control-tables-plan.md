# Control Blocks and Tables Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add control blocks and tables to P4kt, enabling the VSS `TopPipe` control with forwarding tables and apply logic.

**Architecture:** Add IR nodes (`P4Control`, `P4Table`, `MatchKind`, `P4KeyEntry`, `MethodCall`), make `Return.expr` nullable, build `ControlBuilder` and `TableBuilder` DSL classes, add renderers, and update `vss_example.kt` to include the full `TopPipe` control.

**Tech Stack:** Kotlin, Bazel, JUnit 4, kotlin-test

**Spec:** `docs/control-tables-spec.md`

---

## Chunk 1: IR and rendering foundations

### Task 1: Make Return.expr nullable and add void return

**Files:**

- Modify: `p4kt/Ir.kt`
- Modify: `p4kt/Emit.kt`
- Modify: `p4kt/Dsl.kt`
- Modify: `p4kt/P4RefTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `p4kt/P4RefTest.kt`:

```kotlin
@Test
fun voidReturn() {
  val a =
    p4Action("Drop") {
      assign(ref("x"), lit(1))
      return_()
    }

  assertEquals(
    """
        action Drop() {
            x = 1;
            return;
        }
    """
      .trimIndent(),
    a.toP4(),
  )
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./dev bazel test //p4kt:P4RefTest`
Expected: FAIL - no `return_()` overload without args.

- [ ] **Step 3: Implement**

In `p4kt/Ir.kt`, change `P4Statement.Return`:

```kotlin
data class Return(val expr: P4Expr?) : P4Statement()
```

In `p4kt/Emit.kt`, update the `Return` case:

```kotlin
is P4Statement.Return ->
  if (expr != null) "return ${expr.toP4()};" else "return;"
```

In `p4kt/Dsl.kt`, add no-arg `return_()` to `StatementBuilder`:

```kotlin
fun return_() {
  body.add(P4Statement.Return(null))
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./dev bazel test //p4kt:P4RefTest`
Expected: PASS

- [ ] **Step 5: Run all tests**

Run: `./dev bazel test //p4kt:all`
Expected: All PASS.

- [ ] **Step 6: Commit**

```bash
git add p4kt/Ir.kt p4kt/Emit.kt p4kt/Dsl.kt p4kt/P4RefTest.kt
git commit -m "Support void return statement

Make Return.expr nullable and add no-arg return_() to StatementBuilder.
Renders as 'return;' when expr is null."
```

### Task 2: Add MethodCall statement and P4Table/P4Control IR

**Files:**

- Modify: `p4kt/Ir.kt`
- Modify: `p4kt/Emit.kt`
- Create: `p4kt/P4ControlTest.kt`
- Modify: `p4kt/BUILD.bazel`

- [ ] **Step 1: Write the failing test**

Create `p4kt/P4ControlTest.kt`:

```kotlin
package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ControlTest {
  @Test
  fun methodCallStatement() {
    val stmt = P4Statement.MethodCall(P4Expr.Ref("ipv4_match"), "apply", emptyList())
    assertEquals("ipv4_match.apply();", stmt.toP4())
  }

  @Test
  fun tableRendering() {
    val table = P4Table(
      name = "ipv4_match",
      keys = listOf(P4KeyEntry(P4Expr.FieldAccess(P4Expr.Ref("headers"), "dstAddr"), MatchKind.LPM)),
      actions = listOf("Drop_action", "Set_nhop"),
      size = 1024,
      defaultAction = "Drop_action",
      isDefaultActionConst = false,
    )

    assertEquals(
      """
          table ipv4_match {
              key = { headers.dstAddr : lpm; }
              actions = {
                  Drop_action;
                  Set_nhop;
              }
              size = 1024;
              default_action = Drop_action;
          }
      """
        .trimIndent(),
      table.toP4(),
    )
  }

  @Test
  fun tableWithConstDefaultAction() {
    val table = P4Table(
      name = "check_ttl",
      keys = listOf(P4KeyEntry(P4Expr.FieldAccess(P4Expr.Ref("headers"), "ttl"), MatchKind.EXACT)),
      actions = listOf("Send_to_cpu", "NoAction"),
      defaultAction = "NoAction",
      isDefaultActionConst = true,
    )

    assertEquals(
      """
          table check_ttl {
              key = { headers.ttl : exact; }
              actions = {
                  Send_to_cpu;
                  NoAction;
              }
              const default_action = NoAction;
          }
      """
        .trimIndent(),
      table.toP4(),
    )
  }

  @Test
  fun controlRendering() {
    val ctrl = P4Control(
      name = "MyCtrl",
      params = listOf(P4Param("outCtrl", P4Type.Named("OutControl"), Direction.OUT)),
      declarations = listOf(
        P4Action("Drop", emptyList(), listOf(
          P4Statement.Assign(
            P4Expr.FieldAccess(P4Expr.Ref("outCtrl"), "outputPort"),
            P4Expr.Ref("DROP_PORT"),
          ),
        )),
      ),
      body = listOf(
        P4Statement.MethodCall(P4Expr.Ref("ipv4_match"), "apply", emptyList()),
      ),
    )

    assertEquals(
      """
          control MyCtrl(out OutControl outCtrl) {
              action Drop() {
                  outCtrl.outputPort = DROP_PORT;
              }
              apply {
                  ipv4_match.apply();
              }
          }
      """
        .trimIndent(),
      ctrl.toP4(),
    )
  }
}
```

- [ ] **Step 2: Add test target to BUILD.bazel**

Add to `p4kt/BUILD.bazel`:

```starlark
kt_jvm_test(
    name = "P4ControlTest",
    srcs = ["P4ControlTest.kt"],
    test_class = "p4kt.P4ControlTest",
    deps = [
        ":p4kt",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
        "@maven//:org_jetbrains_kotlin_kotlin_test_junit",
    ],
)
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./dev bazel test //p4kt:P4ControlTest`
Expected: FAIL - `P4Table`, `P4Control`, `MatchKind`, etc. don't exist.

- [ ] **Step 4: Add IR nodes to Ir.kt**

```kotlin
enum class MatchKind { EXACT, LPM, TERNARY }

data class P4KeyEntry(val expr: P4Expr, val matchKind: MatchKind)

data class P4Table(
  val name: String,
  val keys: List<P4KeyEntry>,
  val actions: List<String>,
  val size: Int? = null,
  val defaultAction: String,
  val isDefaultActionConst: Boolean,
) : P4Declaration

data class P4Control(
  val name: String,
  val params: List<P4Param>,
  val declarations: List<P4Declaration>,
  val body: List<P4Statement>,
) : P4Declaration
```

Add `MethodCall` to `P4Statement`:

```kotlin
data class MethodCall(val expr: P4Expr, val method: String, val args: List<P4Expr>) : P4Statement()
```

- [ ] **Step 5: Add renderers to Emit.kt**

Add `MethodCall` to the `P4Statement.toP4()` when block:

```kotlin
is P4Statement.MethodCall -> {
  val argsStr = args.joinToString(", ") { it.toP4() }
  "${expr.toP4()}.$method($argsStr);"
}
```

Add `MatchKind.toP4()`:

```kotlin
fun MatchKind.toP4(): String =
  when (this) {
    MatchKind.EXACT -> "exact"
    MatchKind.LPM -> "lpm"
    MatchKind.TERNARY -> "ternary"
  }
```

Add `P4Table.toP4()`:

```kotlin
fun P4Table.toP4(): String {
  val keyEntries = keys.joinToString(" ") { "${it.expr.toP4()} : ${it.matchKind.toP4()};" }
  val actionsStr = actions.joinToString("\n") { "        $it;" }
  val sizeStr = if (size != null) "\n    size = $size;" else ""
  val constStr = if (isDefaultActionConst) "const " else ""
  return "table $name {\n" +
    "    key = { $keyEntries }\n" +
    "    actions = {\n$actionsStr\n    }$sizeStr\n" +
    "    ${constStr}default_action = $defaultAction;\n}"
}
```

Add `P4Control.toP4()`:

```kotlin
fun P4Control.toP4(): String {
  val paramStr = params.joinToString(", ") { it.toP4() }
  val declStr = declarations.joinToString("\n") { decl ->
    decl.toP4().lines().joinToString("\n") { "    $it" }
  }
  val bodyStr = indentBlock(body, "    ")
  val innerParts = mutableListOf<String>()
  if (declStr.isNotEmpty()) innerParts.add(declStr)
  innerParts.add("    apply {\n$bodyStr\n    }")
  return "control $name($paramStr) {\n${innerParts.joinToString("\n")}\n}"
}
```

Add `P4Table` and `P4Control` to `P4Declaration.toP4()`:

```kotlin
is P4Table -> (this as P4Table).toP4()
is P4Control -> (this as P4Control).toP4()
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./dev bazel test //p4kt:P4ControlTest`
Expected: PASS

- [ ] **Step 7: Run all tests**

Run: `./dev bazel test //p4kt:all`
Expected: All PASS.

- [ ] **Step 8: Commit**

```bash
git add p4kt/Ir.kt p4kt/Emit.kt p4kt/P4ControlTest.kt p4kt/BUILD.bazel
git commit -m "Add control, table, and method call IR and rendering

P4Control holds params, local declarations, and apply body.
P4Table holds keys, actions, size, and default action.
MethodCall statement renders as expr.method(args)."
```

## Chunk 2: DSL builders

### Task 3: Add stmt() to StatementBuilder, TableBuilder, and P4TableRef

**Files:**

- Modify: `p4kt/Dsl.kt`
- Modify: `p4kt/P4ControlTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `p4kt/P4ControlTest.kt`:

```kotlin
@Test
fun stmtAddsArbitraryStatement() {
  val a =
    p4Action("Test") {
      stmt(P4Statement.MethodCall(P4Expr.Ref("t"), "apply", emptyList()))
    }

  assertEquals(
    """
        action Test() {
            t.apply();
        }
    """
      .trimIndent(),
    a.toP4(),
  )
}

@Test
fun tableDsl() {
  val dropAction = p4Action("Drop_action") {}
  val setNhop = p4Action("Set_nhop") {}

  val table = p4Table("ipv4_match") {
    key(P4Expr.FieldAccess(P4Expr.Ref("headers"), "dstAddr"), LPM)
    actions(dropAction, setNhop)
    size(1024)
    defaultAction(dropAction)
  }

  assertEquals("Drop_action", table.actions[0])
  assertEquals("Set_nhop", table.actions[1])
  assertEquals(1024, table.size)
  assertEquals("Drop_action", table.defaultAction)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./dev bazel test //p4kt:P4ControlTest`
Expected: FAIL - `p4Table`, `TableBuilder`, `LPM` don't exist.

- [ ] **Step 3: Add stmt() to StatementBuilder and implement TableBuilder**

Add `stmt()` to `StatementBuilder` in `p4kt/Dsl.kt`:

```kotlin
fun stmt(statement: P4Statement) {
  body.add(statement)
}
```

This allows adding arbitrary statements (like `MethodCall`) to a builder. Usage: `stmt(ipv4_match.apply_())`.

Add to `p4kt/Dsl.kt`:

```kotlin
val LPM = MatchKind.LPM
val EXACT = MatchKind.EXACT
val TERNARY = MatchKind.TERNARY

class P4TableRef(val name: String) {
  fun apply_(): P4Statement.MethodCall =
    P4Statement.MethodCall(P4Expr.Ref(name), "apply", emptyList())
}

class TableBuilder {
  private val keys = mutableListOf<P4KeyEntry>()
  private val actions = mutableListOf<String>()
  private var size: Int? = null
  private var defaultAction: String = ""
  private var isDefaultActionConst: Boolean = false

  fun key(expr: P4Expr, matchKind: MatchKind) {
    keys.add(P4KeyEntry(expr, matchKind))
  }

  fun actions(vararg actionRefs: P4Action) {
    actions.addAll(actionRefs.map { it.name })
  }

  fun size(size: Int) {
    this.size = size
  }

  fun defaultAction(action: P4Action, const_: Boolean = false) {
    defaultAction = action.name
    isDefaultActionConst = const_
  }

  fun build(name: String) = P4Table(name, keys, actions, size, defaultAction, isDefaultActionConst)
}

fun p4Table(name: String, block: TableBuilder.() -> Unit): P4Table {
  val builder = TableBuilder()
  builder.block()
  return builder.build(name)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./dev bazel test //p4kt:P4ControlTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add p4kt/Dsl.kt p4kt/P4ControlTest.kt
git commit -m "Add TableBuilder DSL

Tables are built with key(), actions(), size(), and defaultAction().
P4TableRef provides apply_() for use in control apply blocks."
```

### Task 4: ControlBuilder

**Files:**

- Modify: `p4kt/Dsl.kt`
- Modify: `p4kt/P4ControlTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `p4kt/P4ControlTest.kt`:

```kotlin
@Test
fun controlDsl() {
  class OutControl(base: P4Expr) : StructRef(base) {
    val outputPort by field(bit(4))
  }

  val IPv4Address = P4Typedef("IPv4Address", bit(32))

  val ctrl = p4Control("TopPipe") {
    val outCtrl by param(::OutControl, OUT)

    val Drop_action by action {
      assign(outCtrl.outputPort, ref("DROP_PORT"))
    }

    val nextHop by varDecl(IPv4Address)

    val ipv4_match by table {
      key(P4Expr.FieldAccess(P4Expr.Ref("headers"), "dstAddr"), LPM)
      actions(Drop_action)
      size(1024)
      defaultAction(Drop_action)
    }

    apply {
      stmt(ipv4_match.apply_())
      if_(outCtrl.outputPort eq ref("DROP_PORT")) { return_() }
    }
  }

  assertEquals("TopPipe", ctrl.name)
  assertEquals(1, ctrl.params.size)
  assertEquals(3, ctrl.declarations.size) // action + varDecl + table
  assertEquals(2, ctrl.body.size) // method call + if
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./dev bazel test //p4kt:P4ControlTest`
Expected: FAIL - `p4Control`, `ControlBuilder` don't exist.

- [ ] **Step 3: Implement ControlBuilder**

Add to `p4kt/Dsl.kt`:

```kotlin
class ControlBuilder : StatementBuilder() {
  private val params = mutableListOf<P4Param>()
  private val declarations = mutableListOf<P4Declaration>()

  fun param(type: P4Type, direction: Direction) = ParamDelegate(params, type, direction)

  fun param(type: P4TypeReference, direction: Direction) =
    ParamDelegate(params, type.typeRef, direction)

  fun <T : StructRef> param(factory: (P4Expr) -> T, direction: Direction) =
    TypedParamDelegate(params, factory, direction)

  fun action(block: ActionBuilder.() -> Unit) =
    DeclDelegate<P4Action>(
      factory = { name -> p4Action(name, block) },
      register = { declarations.add(it) },
    )

  fun varDecl(type: P4Type): ControlVarDeclDelegate = ControlVarDeclDelegate(declarations, type)

  fun varDecl(type: P4TypeReference): ControlVarDeclDelegate =
    ControlVarDeclDelegate(declarations, type.typeRef)

  fun table(block: TableBuilder.() -> Unit) =
    TableDeclDelegate(
      factory = { name ->
        val builder = TableBuilder()
        builder.block()
        builder.build(name)
      },
      register = { declarations.add(it) },
    )

  fun apply(block: StatementBuilder.() -> Unit) {
    val applyBuilder = StatementBuilder()
    applyBuilder.block()
    body.addAll(applyBuilder.statements())
  }

  fun build(name: String) = P4Control(name, params, declarations, body)
}
```

Add `ControlVarDeclDelegate` (adds to declarations as a `P4Statement.VarDecl` wrapper, returns `P4Expr.Ref`):

Actually, local variables in a control are declarations rendered as `Type name;`. We can reuse `P4Statement.VarDecl` in the declarations list since it already renders correctly. But `P4Statement.VarDecl` is a `P4Statement`, not a `P4Declaration`. We need a simple wrapper:

```kotlin
data class P4LocalVar(val name: String, val type: P4Type) : P4Declaration
```

Add `P4LocalVar.toP4()` in Emit.kt:

```kotlin
fun P4LocalVar.toP4(): String = "${type.toP4()} $name;"
```

And in `P4Declaration.toP4()` when block:

```kotlin
is P4LocalVar -> (this as P4LocalVar).toP4()
```

Then `ControlVarDeclDelegate`:

```kotlin
class ControlVarDeclDelegate(
  private val declarations: MutableList<P4Declaration>,
  private val type: P4Type,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4Expr.Ref> {
    declarations.add(P4LocalVar(property.name, type))
    return ReadOnlyProperty { _, _ -> P4Expr.Ref(property.name) }
  }
}
```

And `TableDeclDelegate` (returns `P4TableRef` for `.apply_()` calls):

```kotlin
class TableDeclDelegate(
  private val factory: (String) -> P4Table,
  private val register: (P4Table) -> Unit,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4TableRef> {
    val table = factory(property.name)
    register(table)
    return ReadOnlyProperty { _, _ -> P4TableRef(property.name) }
  }
}
```

Add `p4Control` function:

```kotlin
fun p4Control(name: String, block: ControlBuilder.() -> Unit): P4Control {
  val builder = ControlBuilder()
  builder.block()
  return builder.build(name)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./dev bazel test //p4kt:P4ControlTest`
Expected: PASS

- [ ] **Step 5: Run all tests**

Run: `./dev bazel test //p4kt:all`
Expected: All PASS.

- [ ] **Step 6: Commit**

```bash
git add p4kt/Ir.kt p4kt/Emit.kt p4kt/Dsl.kt p4kt/P4ControlTest.kt
git commit -m "Add ControlBuilder DSL

Controls collect params, local declarations (actions, tables, variables),
and an apply block. Tables return P4TableRef for apply_() calls.
P4LocalVar represents control-scoped variable declarations."
```

### Task 5: Register control on ProgramBuilder

**Files:**

- Modify: `p4kt/Dsl.kt`
- Modify: `p4kt/P4ControlTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `p4kt/P4ControlTest.kt`:

```kotlin
@Test
fun controlInProgram() {
  class OutControl(base: P4Expr) : StructRef(base) {
    val outputPort by field(bit(4))
  }

  val program = p4Program {
    struct(::OutControl)

    val TopPipe by control {
      val outCtrl by param(::OutControl, OUT)

      val Drop by action {
        assign(outCtrl.outputPort, lit(4, 0xF))
      }

      apply {
        if_(outCtrl.outputPort eq lit(4, 0xF)) { return_() }
      }
    }
  }

  assertEquals(
    """
        struct OutControl {
            bit<4> outputPort;
        }

        control TopPipe(out OutControl outCtrl) {
            action Drop() {
                outCtrl.outputPort = 4w15;
            }
            apply {
                if ((outCtrl.outputPort == 4w15)) {
                    return;
                }
            }
        }
    """
      .trimIndent(),
    program.toP4(),
  )
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./dev bazel test //p4kt:P4ControlTest`
Expected: FAIL - `ProgramBuilder.control` doesn't exist.

- [ ] **Step 3: Add control() to ProgramBuilder**

Add to `ProgramBuilder` in `p4kt/Dsl.kt`:

```kotlin
fun control(block: ControlBuilder.() -> Unit) =
  DeclDelegate<P4Control>(
    factory = { name -> p4Control(name, block) },
    register = { declarations.add(it) },
  )
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./dev bazel test //p4kt:P4ControlTest`
Expected: PASS

- [ ] **Step 5: Run all tests**

Run: `./dev bazel test //p4kt:all`
Expected: All PASS.

- [ ] **Step 6: Commit**

```bash
git add p4kt/Dsl.kt p4kt/P4ControlTest.kt
git commit -m "Register controls on ProgramBuilder

Controls can now be declared with 'val TopPipe by control { ... }'
inside p4Program, consistent with other declaration types."
```

## Chunk 3: Example update

### Task 6: Update vss_example.kt with TopPipe control

**Files:**

- Modify: `examples/vss_example.kt`
- Modify: `examples/vss_example.p4`

- [ ] **Step 1: Update vss_example.kt**

Move actions inside a `TopPipe` control block, add tables, and add the apply block. Actions inside the control capture control params via closure, so they don't need explicit struct params.

Since `TypedFieldDelegate` for nested structs is deferred, the control takes `Ipv4_h` and `Ethernet_h` as separate params instead of `Parsed_packet`. This avoids the `headers.ip.ttl` nested access problem while demonstrating the full control/table DSL. The `Parsed_packet` struct is kept in the program for its own declaration but not used as a control param.

Replace the full file with:

```kotlin
package p4kt.examples

import p4kt.*

// Corresponds to the supported subset of:
// https://github.com/p4lang/p4c/blob/main/testdata/p4_16_samples/vss-example.p4

fun main() {
  val program = p4Program {
    val EthernetAddress by typedef(bit(48))
    val IPv4Address by typedef(bit(32))
    val PortId by typedef(bit(4))

    val DROP_PORT by const_(PortId, lit(4, 0xF))
    val CPU_OUT_PORT by const_(PortId, lit(4, 0xE))

    class Ethernet_h(base: P4Expr) : HeaderRef(base) {
      val dstAddr by field(EthernetAddress)
      val srcAddr by field(EthernetAddress)
      val etherType by field(bit(16))
    }
    header(::Ethernet_h)

    class Ipv4_h(base: P4Expr) : HeaderRef(base) {
      val version by field(bit(4))
      val ihl by field(bit(4))
      val diffserv by field(bit(8))
      val totalLen by field(bit(16))
      val identification by field(bit(16))
      val flags by field(bit(3))
      val fragOffset by field(bit(13))
      val ttl by field(bit(8))
      val protocol by field(bit(8))
      val hdrChecksum by field(bit(16))
      val srcAddr by field(IPv4Address)
      val dstAddr by field(IPv4Address)
    }
    header(::Ipv4_h)

    class Parsed_packet(base: P4Expr) : StructRef(base) {
      val ethernet by field(typeName("Ethernet_h"))
      val ip by field(typeName("Ipv4_h"))
    }
    struct(::Parsed_packet)

    class OutControl(base: P4Expr) : StructRef(base) {
      val outputPort by field(PortId)
    }
    struct(::OutControl)

    val TopPipe by control {
      val headers_ip by param(::Ipv4_h, INOUT)
      val headers_ethernet by param(::Ethernet_h, INOUT)
      val outCtrl by param(::OutControl, OUT)

      val Drop_action by action {
        assign(outCtrl.outputPort, DROP_PORT)
      }

      val nextHop by varDecl(IPv4Address)

      val Set_nhop by action {
        val ipv4_dest by param(IPv4Address)
        val port by param(PortId)
        assign(nextHop, ipv4_dest)
        assign(headers_ip.ttl, headers_ip.ttl - lit(1))
        assign(outCtrl.outputPort, port)
      }

      val ipv4_match by table {
        key(headers_ip.dstAddr, LPM)
        actions(Drop_action, Set_nhop)
        size(1024)
        defaultAction(Drop_action)
      }

      val Send_to_cpu by action {
        assign(outCtrl.outputPort, CPU_OUT_PORT)
      }

      val check_ttl by table {
        key(headers_ip.ttl, EXACT)
        actions(Send_to_cpu)
        defaultAction(Send_to_cpu, const_ = true)
      }

      val Set_dmac by action {
        val dmac by param(EthernetAddress)
        assign(headers_ethernet.dstAddr, dmac)
      }

      val dmac by table {
        key(nextHop, EXACT)
        actions(Drop_action, Set_dmac)
        size(1024)
        defaultAction(Drop_action)
      }

      val Set_smac by action {
        val smac by param(EthernetAddress)
        assign(headers_ethernet.srcAddr, smac)
      }

      val smac by table {
        key(outCtrl.outputPort, EXACT)
        actions(Drop_action, Set_smac)
        size(16)
        defaultAction(Drop_action)
      }

      apply {
        stmt(ipv4_match.apply_())
        if_(outCtrl.outputPort eq DROP_PORT) { return_() }

        stmt(check_ttl.apply_())
        if_(outCtrl.outputPort eq CPU_OUT_PORT) { return_() }

        stmt(dmac.apply_())
        if_(outCtrl.outputPort eq DROP_PORT) { return_() }

        stmt(smac.apply_())
      }
    }
  }
  println(program.toP4())
}
```

- [ ] **Step 2: Generate the golden file**

Run: `./dev bazel run //examples:vss_example_bin`
Capture output to `examples/vss_example.p4`.

- [ ] **Step 3: Run golden test**

Run: `./dev bazel test //examples:all`
Expected: All PASS.

- [ ] **Step 4: Run all tests**

Run: `./dev bazel test //...`
Expected: All PASS.

- [ ] **Step 5: Format and lint**

Run: `./dev format && ./dev lint`
Expected: No issues.

- [ ] **Step 6: Commit**

```bash
git add examples/vss_example.kt examples/vss_example.p4
git commit -m "Add TopPipe control with tables to VSS example

Demonstrates control blocks with local actions, tables, variables,
and apply block logic. Actions inside the control capture params
via Kotlin closure."
```
