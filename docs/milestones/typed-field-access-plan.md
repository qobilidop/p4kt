# Typed Field Access Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace string-based field access (`outCtrl.dot("outputPort")`) with typed Kotlin property access (`outCtrl.outputPort`) for structs and headers.

**Architecture:** Add `StructRef`/`HeaderRef` base classes to the DSL layer. User-defined P4 types become Kotlin classes with real properties backed by `FieldDelegate`. Typed `param<T>()` and `struct<T>()`/`header<T>()` use reified generics and reflection to instantiate ref classes.

**Tech Stack:** Kotlin, Bazel, JUnit 4, kotlin-test, kotlin-reflect

**Spec:** `docs/milestones/typed-field-access.md`

**Scope note:** `TypedFieldDelegate<T>` for nested struct/header field access (e.g., `headers.ip.ttl` via chained typed refs) is deferred. This plan uses `typeName("...")` for cross-type references in field definitions. The typed `param<T>()` approach covers the most common use case (accessing fields on action/function parameters).

---

## Chunk 1: Foundation

### Task 1: Add kotlin-reflect dependency and spike local class reflection

Before building anything, add `kotlin-reflect` as a dependency (required for `T::class.constructors`) and confirm that Kotlin local classes can be instantiated via reflection inside a lambda builder.

**Files:**

- Modify: `MODULE.bazel` (add kotlin-reflect to maven artifacts)
- Modify: `p4kt/BUILD.bazel` (add kotlin-reflect dep to p4kt library)
- Create: `p4kt/ReflectionSpikeTest.kt` (temporary)

- [ ] **Step 1: Add kotlin-reflect to MODULE.bazel**

Add `"org.jetbrains.kotlin:kotlin-reflect:2.1.10"` to the `artifacts` list in the `maven.install` call in `MODULE.bazel`.

- [ ] **Step 2: Add kotlin-reflect dep to p4kt library in BUILD.bazel**

Add `"@maven//:org_jetbrains_kotlin_kotlin_reflect"` to the `deps` of the `p4kt` library target in `p4kt/BUILD.bazel`.

- [ ] **Step 3: Write a spike test**

Create `p4kt/ReflectionSpikeTest.kt`:

```kotlin
package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class ReflectionSpikeTest {
  @Test
  fun localClassCanBeInstantiatedViaReifiedGeneric() {
    class MyRef(val value: String)

    fun <T : Any> create(clazz: kotlin.reflect.KClass<T>, arg: String): T {
      return clazz.constructors.first().call(arg)
    }

    val instance = create(MyRef::class, "hello")
    assertEquals("hello", instance.value)
  }

  @Test
  fun localClassWorksWithReifiedInline() {
    class MyRef(val value: String)

    inline fun <reified T : Any> create(arg: String): T {
      return T::class.constructors.first().call(arg)
    }

    val instance = create<MyRef>("world")
    assertEquals("world", instance.value)
  }
}
```

- [ ] **Step 4: Add test target to BUILD.bazel**

Add to `p4kt/BUILD.bazel`:

```starlark
kt_jvm_test(
    name = "ReflectionSpikeTest",
    srcs = ["ReflectionSpikeTest.kt"],
    test_class = "p4kt.ReflectionSpikeTest",
    deps = [
        ":p4kt",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
        "@maven//:org_jetbrains_kotlin_kotlin_test_junit",
    ],
)
```

- [ ] **Step 5: Run the spike test**

Run: `./dev bazel test //p4kt:ReflectionSpikeTest`
Expected: PASS - both tests pass, confirming local class reflection works with kotlin-reflect.

- [ ] **Step 6: If spike fails, stop and reassess**

If reflection on local classes does not work, we need to pivot to factory lambdas. Do not proceed with the remaining tasks.

- [ ] **Step 7: Delete the spike test and commit**

Remove `p4kt/ReflectionSpikeTest.kt` and its BUILD target. The spike has served its purpose. Keep the kotlin-reflect dependency changes.

```bash
git add MODULE.bazel p4kt/BUILD.bazel
git commit -m "Add kotlin-reflect dependency for typed field access"
```

### Task 2: StructRef base class with FieldDelegate

Implement the core `StructRef` class and `FieldDelegate` for leaf fields.

**Files:**

- Modify: `p4kt/Dsl.kt` (add StructRef, HeaderRef, FieldDelegate)
- Modify: `p4kt/BUILD.bazel` (add P4RefTest target)
- Create: `p4kt/P4RefTest.kt`

- [ ] **Step 1: Write the failing test for FieldDelegate**

Create `p4kt/P4RefTest.kt`:

```kotlin
package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4RefTest {
  @Test
  fun structRefFieldProducesFieldAccessExpr() {
    class OutControl(base: P4Expr) : StructRef(base) {
      val outputPort by field(bit(4))
    }

    val ref = OutControl(P4Expr.Ref("outCtrl"))
    assertEquals("outCtrl.outputPort", ref.outputPort.toP4())
  }

  @Test
  fun structRefCollectsFieldMetadata() {
    class OutControl(base: P4Expr) : StructRef(base) {
      val outputPort by field(bit(4))
    }

    val ref = OutControl(P4Expr.Ref(""))
    assertEquals(listOf(P4Field("outputPort", bit(4))), ref.fields)
  }

  @Test
  fun headerRefFieldProducesFieldAccessExpr() {
    class Ipv4_h(base: P4Expr) : HeaderRef(base) {
      val ttl by field(bit(8))
      val srcAddr by field(bit(32))
    }

    val ref = Ipv4_h(P4Expr.Ref("ip"))
    assertEquals("ip.ttl", ref.ttl.toP4())
    assertEquals("ip.srcAddr", ref.srcAddr.toP4())
  }

  @Test
  fun headerRefCollectsFieldsInOrder() {
    class Ipv4_h(base: P4Expr) : HeaderRef(base) {
      val ttl by field(bit(8))
      val srcAddr by field(bit(32))
    }

    val ref = Ipv4_h(P4Expr.Ref(""))
    assertEquals(
      listOf(P4Field("ttl", bit(8)), P4Field("srcAddr", bit(32))),
      ref.fields,
    )
  }

  @Test
  fun fieldAcceptsTypeReference() {
    // P4Typedef implements P4TypeReference, so it can be passed to field() directly.
    // In real usage, typedefs are declared via `val PortId by typedef(bit(4))` inside p4Program.
    val PortId = P4Typedef("PortId", bit(4))

    class OutControl(base: P4Expr) : StructRef(base) {
      val outputPort by field(PortId)
    }

    val ref = OutControl(P4Expr.Ref(""))
    assertEquals(listOf(P4Field("outputPort", P4Type.Named("PortId"))), ref.fields)
  }
}
```

- [ ] **Step 2: Add test target to BUILD.bazel**

Add to `p4kt/BUILD.bazel`:

```starlark
kt_jvm_test(
    name = "P4RefTest",
    srcs = ["P4RefTest.kt"],
    test_class = "p4kt.P4RefTest",
    deps = [
        ":p4kt",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
        "@maven//:org_jetbrains_kotlin_kotlin_test_junit",
    ],
)
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./dev bazel test //p4kt:P4RefTest`
Expected: FAIL - `StructRef`, `HeaderRef`, `FieldDelegate` do not exist yet.

- [ ] **Step 4: Implement StructRef, HeaderRef, and FieldDelegate**

Add to `p4kt/Dsl.kt` (the `ReadOnlyProperty` import already exists at line 3):

```kotlin
class FieldDelegate(
  private val fields: MutableList<P4Field>,
  private val expr: P4Expr,
  private val type: P4Type,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4Expr> {
    fields.add(P4Field(property.name, type))
    val fieldAccess = P4Expr.FieldAccess(expr, property.name)
    return ReadOnlyProperty { _, _ -> fieldAccess }
  }
}

abstract class StructRef(val expr: P4Expr) {
  val fields = mutableListOf<P4Field>()

  fun field(type: P4Type) = FieldDelegate(fields, expr, type)

  fun field(type: P4TypeReference) = FieldDelegate(fields, expr, type.typeRef)
}

abstract class HeaderRef(expr: P4Expr) : StructRef(expr)
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./dev bazel test //p4kt:P4RefTest`
Expected: PASS

- [ ] **Step 6: Run all tests to check for regressions**

Run: `./dev bazel test //p4kt:all`
Expected: All tests PASS.

- [ ] **Step 7: Commit**

```bash
git add p4kt/P4RefTest.kt p4kt/Dsl.kt p4kt/BUILD.bazel
git commit -m "Add StructRef and HeaderRef with FieldDelegate

Introduce base classes for typed field access. StructRef wraps a P4Expr
and provides field() delegates that produce P4Expr.FieldAccess nodes and
collect P4Field metadata for IR generation."
```

### Task 3: struct<T>() and header<T>() registration

Add the reified generic functions to `ProgramBuilder` that register struct/header classes as IR declarations.

**Files:**

- Modify: `p4kt/Dsl.kt` (add struct<T>() and header<T>() to ProgramBuilder, change declarations visibility)
- Modify: `p4kt/P4RefTest.kt` (add registration tests)

- [ ] **Step 1: Write the failing test**

Add to `p4kt/P4RefTest.kt`:

```kotlin
@Test
fun structRegistrationGeneratesIrDeclaration() {
  val program = p4Program {
    class OutControl(base: P4Expr) : StructRef(base) {
      val outputPort by field(bit(4))
    }
    struct<OutControl>()
  }

  assertEquals(
    """
        struct OutControl {
            bit<4> outputPort;
        }
    """
      .trimIndent(),
    program.toP4(),
  )
}

@Test
fun headerRegistrationGeneratesIrDeclaration() {
  val program = p4Program {
    class Ipv4_h(base: P4Expr) : HeaderRef(base) {
      val ttl by field(bit(8))
      val srcAddr by field(bit(32))
    }
    header<Ipv4_h>()
  }

  assertEquals(
    """
        header Ipv4_h {
            bit<8> ttl;
            bit<32> srcAddr;
        }
    """
      .trimIndent(),
    program.toP4(),
  )
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./dev bazel test //p4kt:P4RefTest`
Expected: FAIL - `struct<T>()` and `header<T>()` do not exist.

- [ ] **Step 3: Implement struct<T>() and header<T>()**

First, change `ProgramBuilder.declarations` from `private` to `@PublishedApi internal` so inline functions can access it:

```kotlin
@PublishedApi internal val declarations = mutableListOf<P4Declaration>()
```

Then add to `ProgramBuilder` in `p4kt/Dsl.kt`:

```kotlin
inline fun <reified T : StructRef> struct() {
  val dummy = T::class.constructors.first().call(P4Expr.Ref(""))
  val name = T::class.simpleName!!
  declarations.add(P4Struct(name, dummy.fields.toList()))
}

inline fun <reified T : HeaderRef> header() {
  val dummy = T::class.constructors.first().call(P4Expr.Ref(""))
  val name = T::class.simpleName!!
  declarations.add(P4Header(name, dummy.fields.toList()))
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./dev bazel test //p4kt:P4RefTest`
Expected: PASS

- [ ] **Step 5: Run all tests**

Run: `./dev bazel test //p4kt:all`
Expected: All tests PASS.

- [ ] **Step 6: Commit**

```bash
git add p4kt/Dsl.kt p4kt/P4RefTest.kt
git commit -m "Add struct<T>() and header<T>() registration

ProgramBuilder gains reified generic functions that instantiate a ref
class with a dummy expression, collect its field metadata, and emit
the corresponding P4Struct or P4Header IR declaration."
```

## Chunk 2: Typed params and API additions

### Task 4: TypedParamDelegate

Add `param<T>()` overload that returns a typed ref instance instead of `P4Expr.Ref`.

**Files:**

- Modify: `p4kt/Dsl.kt` (add TypedParamDelegate, param<T>() overloads, change params visibility)
- Modify: `p4kt/P4RefTest.kt` (add typed param tests)

- [ ] **Step 1: Write the failing test**

Add to `p4kt/P4RefTest.kt`:

```kotlin
@Test
fun typedParamInAction() {
  class OutControl(base: P4Expr) : StructRef(base) {
    val outputPort by field(bit(4))
  }

  val a =
    p4Action("Drop") {
      val outCtrl by param<OutControl>(INOUT)
      assign(outCtrl.outputPort, lit(4, 0xF))
    }

  assertEquals(
    """
        action Drop(inout OutControl outCtrl) {
            outCtrl.outputPort = 4w15;
        }
    """
      .trimIndent(),
    a.toP4(),
  )
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./dev bazel test //p4kt:P4RefTest`
Expected: FAIL - `param<T>()` does not exist.

- [ ] **Step 3: Implement TypedParamDelegate and param<T>()**

Add to `p4kt/Dsl.kt`:

```kotlin
class TypedParamDelegate<T : StructRef>(
  private val params: MutableList<P4Param>,
  private val clazz: kotlin.reflect.KClass<T>,
  private val direction: Direction? = null,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, T> {
    params.add(P4Param(property.name, P4Type.Named(clazz.simpleName!!), direction))
    val instance = clazz.constructors.first().call(P4Expr.Ref(property.name))
    return ReadOnlyProperty { _, _ -> instance }
  }
}
```

Change `params` visibility in both `ActionBuilder` and `FunctionBuilder` from `private` to `@PublishedApi internal`:

```kotlin
@PublishedApi internal val params = mutableListOf<P4Param>()
```

Add `param<T>()` overloads to `ActionBuilder`:

```kotlin
inline fun <reified T : StructRef> param() = TypedParamDelegate(params, T::class)

inline fun <reified T : StructRef> param(direction: Direction) =
  TypedParamDelegate(params, T::class, direction)
```

Add the same overloads to `FunctionBuilder`:

```kotlin
inline fun <reified T : StructRef> param() = TypedParamDelegate(params, T::class)

inline fun <reified T : StructRef> param(direction: Direction) =
  TypedParamDelegate(params, T::class, direction)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./dev bazel test //p4kt:P4RefTest`
Expected: PASS

- [ ] **Step 5: Run all tests**

Run: `./dev bazel test //p4kt:all`
Expected: All tests PASS.

- [ ] **Step 6: Commit**

```bash
git add p4kt/Dsl.kt p4kt/P4RefTest.kt
git commit -m "Add typed param<T>() for struct/header parameters

Actions and functions can now declare parameters with typed refs via
param<T>(direction). The delegate returns a ref instance with real
Kotlin properties instead of a plain P4Expr.Ref."
```

### Task 5: P4TypeReference overloads and ref() convenience

Add `P4TypeReference` overloads for `param()` and `const_()`, and the `ref()` convenience function.

**Files:**

- Modify: `p4kt/Dsl.kt`
- Modify: `p4kt/P4RefTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `p4kt/P4RefTest.kt`:

```kotlin
@Test
fun paramAcceptsTypeReference() {
  val PortId = P4Typedef("PortId", bit(4))

  val a =
    p4Action("Set") {
      val port by param(PortId)
      assign(ref("outPort"), port)
    }

  assertEquals(
    """
        action Set(PortId port) {
            outPort = port;
        }
    """
      .trimIndent(),
    a.toP4(),
  )
}

@Test
fun paramAcceptsTypeReferenceWithDirection() {
  val PortId = P4Typedef("PortId", bit(4))

  val a =
    p4Action("Set") {
      val port by param(PortId, IN)
      assign(ref("outPort"), port)
    }

  assertEquals(
    """
        action Set(in PortId port) {
            outPort = port;
        }
    """
      .trimIndent(),
    a.toP4(),
  )
}

@Test
fun refConvenienceFunction() {
  assertEquals("foo", ref("foo").toP4())
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./dev bazel test //p4kt:P4RefTest`
Expected: FAIL - `param(P4TypeReference)`, `ref()` do not exist.

- [ ] **Step 3: Implement the overloads and ref()**

Add to `p4kt/Dsl.kt`:

```kotlin
fun ref(name: String) = P4Expr.Ref(name)
```

Add to `ActionBuilder`:

```kotlin
fun param(type: P4TypeReference) = ParamDelegate(params, type.typeRef)

fun param(type: P4TypeReference, direction: Direction) = ParamDelegate(params, type.typeRef, direction)
```

Add to `FunctionBuilder`:

```kotlin
fun param(type: P4TypeReference, direction: Direction) = ParamDelegate(params, type.typeRef, direction)
```

Add `P4TypeReference` overload to `ProgramBuilder`:

```kotlin
fun const_(type: P4TypeReference, value: P4Expr) = ConstDelegate(type.typeRef, value) { declarations.add(it) }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./dev bazel test //p4kt:P4RefTest`
Expected: PASS

- [ ] **Step 5: Run all tests**

Run: `./dev bazel test //p4kt:all`
Expected: All tests PASS.

- [ ] **Step 6: Commit**

```bash
git add p4kt/Dsl.kt p4kt/P4RefTest.kt
git commit -m "Add P4TypeReference overloads and ref() convenience

param() and const_() now accept P4TypeReference directly, removing the
need for .typeRef calls. ref() is a shorthand for P4Expr.Ref()."
```

## Chunk 3: Migration and cleanup

### Task 6: Migrate tests, examples, and remove old API

Migrate all usage of `.dot()` to either typed ref properties or direct `P4Expr.FieldAccess` IR construction, then remove the `.dot()` extension function.

Note: `P4StructTest` and `P4HeaderTest` use `p4Struct()`/`p4Header()` with `FieldsBuilder`, which test IR-level helpers and do not use `.dot()`. They are intentionally kept as-is.

**Files:**

- Modify: `p4kt/P4ProgramTest.kt` (use new registration syntax)
- Modify: `p4kt/P4ExpressionTest.kt` (replace `.dot()` with direct `P4Expr.FieldAccess`)
- Modify: `p4kt/P4ActionTest.kt` (replace `.dot()` with direct `P4Expr.FieldAccess`)
- Modify: `p4kt/Dsl.kt` (remove `dot()`)
- Modify: `examples/vss_actions.kt`
- Modify: `examples/vss_types.kt`
- Modify: `examples/ttl_decrement.kt`

- [ ] **Step 1: Update P4ProgramTest to use new registration syntax**

Modify `p4kt/P4ProgramTest.kt`:

```kotlin
package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ProgramTest {
  @Test
  fun programWithMultipleDeclarations() {
    val program = p4Program {
      val EthernetAddress by typedef(bit(48))

      class Ethernet_h(base: P4Expr) : HeaderRef(base) {
        val dstAddr by field(EthernetAddress)
        val etherType by field(bit(16))
      }
      header<Ethernet_h>()

      class Parsed_packet(base: P4Expr) : StructRef(base) {
        val ethernet by field(typeName("Ethernet_h"))
      }
      struct<Parsed_packet>()
    }

    assertEquals(
      """
            typedef bit<48> EthernetAddress;

            header Ethernet_h {
                EthernetAddress dstAddr;
                bit<16> etherType;
            }

            struct Parsed_packet {
                Ethernet_h ethernet;
            }
      """
        .trimIndent(),
      program.toP4(),
    )
  }
}
```

- [ ] **Step 2: Update P4ExpressionTest**

Replace `.dot()` calls with direct `P4Expr.FieldAccess` construction since these are IR-level tests:

In `fieldAccess`:

```kotlin
assertEquals("headers.ip", P4Expr.FieldAccess(headers, "ip").toP4())
```

In `chainedFieldAccess`:

```kotlin
assertEquals("headers.ip.ttl", P4Expr.FieldAccess(P4Expr.FieldAccess(headers, "ip"), "ttl").toP4())
```

In `nestedExpression`:

```kotlin
val ttl = P4Expr.FieldAccess(P4Expr.FieldAccess(headers, "ip"), "ttl")
```

- [ ] **Step 3: Update P4ActionTest**

Replace `.dot()` calls with direct `P4Expr.FieldAccess` and `ref()`:

```kotlin
@Test
fun actionWithDirectionlessParams() {
  val a =
    p4Action("Set_nhop") {
      val ipv4_dest by param(typeName("IPv4Address"))
      val port by param(typeName("PortId"))
      assign(ref("nextHop"), ipv4_dest)
      assign(
        P4Expr.FieldAccess(P4Expr.FieldAccess(ref("headers"), "ip"), "ttl"),
        P4Expr.FieldAccess(P4Expr.FieldAccess(ref("headers"), "ip"), "ttl") - lit(1),
      )
      assign(P4Expr.FieldAccess(ref("outCtrl"), "outputPort"), port)
    }

  assertEquals(
    """
          action Set_nhop(IPv4Address ipv4_dest, PortId port) {
              nextHop = ipv4_dest;
              headers.ip.ttl = (headers.ip.ttl - 1);
              outCtrl.outputPort = port;
          }
    """
      .trimIndent(),
    a.toP4(),
  )
}
```

- [ ] **Step 4: Update vss_types.kt**

```kotlin
package p4kt.examples

import p4kt.*

fun main() {
  val program = p4Program {
    val EthernetAddress by typedef(bit(48))
    val IPv4Address by typedef(bit(32))

    class Ethernet_h(base: P4Expr) : HeaderRef(base) {
      val dstAddr by field(EthernetAddress)
      val srcAddr by field(EthernetAddress)
      val etherType by field(bit(16))
    }
    header<Ethernet_h>()

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
    header<Ipv4_h>()

    class Parsed_packet(base: P4Expr) : StructRef(base) {
      val ethernet by field(typeName("Ethernet_h"))
      val ip by field(typeName("Ipv4_h"))
    }
    struct<Parsed_packet>()
  }
  println(program.toP4())
}
```

- [ ] **Step 5: Update vss_actions.kt**

```kotlin
package p4kt.examples

import p4kt.*

fun main() {
  val program = p4Program {
    val PortId by typedef(bit(4))
    val IPv4Address by typedef(bit(32))

    val DROP_PORT by const_(PortId, lit(4, 0xF))

    class Ethernet_h(base: P4Expr) : HeaderRef(base) {
      val dstAddr by field(bit(48))
      val srcAddr by field(bit(48))
      val etherType by field(bit(16))
    }
    header<Ethernet_h>()

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

    val Drop_action by action {
      val outCtrl by param<OutControl>(INOUT)
      assign(outCtrl.outputPort, DROP_PORT)
    }

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
  println(program.toP4())
}
```

- [ ] **Step 6: Update ttl_decrement.kt**

```kotlin
package p4kt.examples

import p4kt.*

fun main() {
  val program = p4Program {
    class Ipv4_h(base: P4Expr) : HeaderRef(base) {
      val ttl by field(bit(8))
    }
    header<Ipv4_h>()

    val headers by
      function(void_) {
        val ip by param<Ipv4_h>(INOUT)
        assign(ip.ttl, ip.ttl - lit(1))
      }
  }
  println(program.toP4())
}
```

- [ ] **Step 7: Remove dot() from Dsl.kt**

Remove this line from `p4kt/Dsl.kt`:

```kotlin
fun P4Expr.dot(field: String) = P4Expr.FieldAccess(this, field)
```

- [ ] **Step 8: Run all tests including golden tests**

Run: `./dev bazel test //...`
Expected: All tests PASS. Golden tests confirm P4 output is unchanged.

- [ ] **Step 9: Format and lint**

Run: `./dev format && ./dev lint`
Expected: No issues.

- [ ] **Step 10: Commit**

```bash
git add p4kt/Dsl.kt p4kt/P4ProgramTest.kt p4kt/P4ExpressionTest.kt p4kt/P4ActionTest.kt examples/vss_actions.kt examples/vss_types.kt examples/ttl_decrement.kt
git commit -m "Migrate to typed field access and remove dot()

Replace string-based .dot() field access with typed ref classes.
Struct/header types are now Kotlin classes with real properties, enabling
IDE autocomplete and compile-time checking. Tests and examples updated."
```
