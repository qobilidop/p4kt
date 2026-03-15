# Control Blocks and Tables

## Goal

Add control blocks and tables to P4kt, enabling the VSS example's `TopPipe` control with its forwarding tables and apply logic.

## IR additions

### Control

```kotlin
data class P4Control(
    val name: String,
    val params: List<P4Param>,
    val declarations: List<P4Declaration>,  // actions, tables, local variables
    val body: List<P4Statement>,            // the apply block
) : P4Declaration
```

### Table

```kotlin
data class P4Table(
    val name: String,
    val keys: List<P4KeyEntry>,
    val actions: List<String>,           // action names, extracted from P4Action refs in DSL
    val size: Int? = null,               // optional, omitted if null
    val defaultAction: String,           // action name, extracted from P4Action ref in DSL
    val isDefaultActionConst: Boolean,
) : P4Declaration

data class P4KeyEntry(val expr: P4Expr, val matchKind: MatchKind)

enum class MatchKind { EXACT, LPM, TERNARY }
```

### Statement additions

**Void return:** Make `P4Statement.Return.expr` nullable. Renders as `return;` when null.

**Method call statement:**

```kotlin
data class MethodCall(val expr: P4Expr, val method: String, val args: List<P4Expr>) : P4Statement()
```

Used for `table.apply()` now, general enough for future parser method calls (`b.extract()`, `ck.clear()`).

Note: method calls can also appear as expressions in P4 (e.g., `ck.get()` returning a value). A `P4Expr.MethodCall` variant will likely be needed for parser/extern support. For now, only the statement form is needed.

## DSL additions

### ControlBuilder

`ControlBuilder` collects params, local declarations (actions, tables, variables), and the apply block body.

```kotlin
val TopPipe by control {
    val headers by param(::Parsed_packet, INOUT)
    val outCtrl by param(::OutControl, OUT)

    val Drop_action by action { ... }
    val nextHop by varDecl(IPv4Address)
    val ipv4_match by table { ... }

    apply {
        ipv4_match.apply_()
        if_(outCtrl.outputPort eq DROP_PORT) { return_() }
    }
}
```

Actions inside a control reference the control's params via Kotlin closure - no special mechanism needed. These actions have no explicit `outCtrl` param (unlike program-level actions); they capture the control's `outCtrl` directly. When we add the control to `vss_example.kt`, the existing program-level actions (`Drop_action`, etc.) move inside the control with their param lists simplified accordingly.

`varDecl()` inside a control adds to the declarations list (rendered before `apply`), not to the apply body. This differs from `StatementBuilder.varDecl()` which adds to the statement body. `ControlBuilder` provides its own `varDecl()` that targets the declarations list.

The `apply { }` lambda fills the statement body. `ControlBuilder` collects both declarations and statements separately.

### TableBuilder

```kotlin
val ipv4_match by table {
    key(headers.ip.dstAddr, LPM)
    actions(Drop_action, Set_nhop)
    size(1024)
    defaultAction(Drop_action)
}
```

- `key()` can be called multiple times for multi-field keys.
- `actions()` takes action references (`P4Action` values) for compile-time checking.
- `defaultAction()` takes an action reference with optional `const_` flag: `defaultAction(NoAction, const_ = true)`.

### Table reference type

The `table { }` delegate returns a `P4TableRef` (or similar) that exposes `apply_()`:

```kotlin
ipv4_match.apply_()  // produces P4Statement.MethodCall
```

Trailing underscore avoids collision with Kotlin's `apply` scope function.

### Void return

Add no-arg `return_()` to `StatementBuilder`:

```kotlin
fun return_() { body.add(P4Statement.Return(null)) }
```

### Top-level DSL constants

```kotlin
val LPM = MatchKind.LPM
val EXACT = MatchKind.EXACT
val TERNARY = MatchKind.TERNARY
```

## Rendering

### Control

```p4
control TopPipe(inout Parsed_packet headers, out OutControl outCtrl) {
    action Drop_action() {
        outCtrl.outputPort = DROP_PORT;
    }
    IPv4Address nextHop;
    table ipv4_match {
        key = { headers.ip.dstAddr : lpm; }
        actions = {
            Drop_action;
            Set_nhop;
        }
        size = 1024;
        default_action = Drop_action;
    }
    apply {
        ipv4_match.apply();
    }
}
```

- Local variable declarations render as `Type name;` (same as `P4Statement.VarDecl` but at the control level).
- Table key entries render as `expr : matchKind;`.
- Actions list renders with one action per line.
- Apply block is indented like other statement blocks.

### Method call statement

Renders as `expr.method(args);` - e.g., `ipv4_match.apply();`.

### Void return

Renders as `return;` (no expression).

## Impact on existing code

- `P4Statement.Return.expr` changes from `P4Expr` to `P4Expr?` - existing call sites pass non-null and are unaffected.
- `P4Declaration` sealed interface gains `P4Control` and `P4Table` variants.
- `Emit.kt` gains `toP4()` for control, table, method call, and void return.
- `Dsl.kt` gains `ControlBuilder`, `TableBuilder`, `P4TableRef`, and `control { }` on `ProgramBuilder`.
- Examples: `vss_example.kt` updated to include `TopPipe` control with tables.
