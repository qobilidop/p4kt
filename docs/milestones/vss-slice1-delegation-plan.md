# Delegated Properties and Type References Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace string-based declaration names with Kotlin delegated properties and allow declarations as type references in fields.

**Architecture:** Add `P4TypeReference` interface to IR, change `ProgramBuilder` methods to return `ReadOnlyProperty` for `by` delegation, add `field()` overload accepting `P4TypeReference`. Update all tests and golden tests.

**Tech Stack:** Kotlin, Bazel, kotlin-test with JUnit 4.

---

## File structure

| File                    | Action | Responsibility                                                        |
| ----------------------- | ------ | --------------------------------------------------------------------- |
| `p4kt/Ir.kt`            | Modify | Add `P4TypeReference`, implement on `P4Typedef`/`P4Header`/`P4Struct` |
| `p4kt/Dsl.kt`           | Modify | Change `ProgramBuilder` to delegation, add `field()` overload         |
| `p4kt/P4ProgramTest.kt` | Modify | Update test to use `by` delegation and type references                |
| `p4kt/P4TypedefTest.kt` | Modify | Keep standalone builder tests (no change to standalone API)           |
| `p4kt/P4HeaderTest.kt`  | Modify | Keep standalone builder tests (no change to standalone API)           |
| `p4kt/P4StructTest.kt`  | Modify | Keep standalone builder tests (no change to standalone API)           |
| `examples/vss_types.kt` | Modify | Use `by` delegation and type references                               |
| `examples/identity.kt`  | Modify | Use `by` delegation for function                                      |

---

## Chunk 1: P4TypeReference and delegated ProgramBuilder

One logical unit: add the interface, change ProgramBuilder to delegation, add field overload, update all tests.

### Task 1: P4TypeReference interface and field overload

**Files:**

- Modify: `p4kt/Ir.kt`, `p4kt/Dsl.kt`

- [ ] **Step 1: Add P4TypeReference to IR**

In `p4kt/Ir.kt`, add sealed interface and implement on three types:

```kotlin
sealed interface P4TypeReference {
  val typeRef: P4Type.Named
}

data class P4Typedef(val name: String, val type: P4Type) : P4Declaration, P4TypeReference {
  override val typeRef get() = P4Type.Named(name)
}

data class P4Header(val name: String, val fields: List<P4Field>) : P4Declaration, P4TypeReference {
  override val typeRef get() = P4Type.Named(name)
}

data class P4Struct(val name: String, val fields: List<P4Field>) : P4Declaration, P4TypeReference {
  override val typeRef get() = P4Type.Named(name)
}
```

`P4Function` does NOT implement `P4TypeReference`.

- [ ] **Step 2: Add field overload to FieldsBuilder**

In `p4kt/Dsl.kt`, add to `FieldsBuilder`:

```kotlin
fun field(name: String, type: P4TypeReference) {
  fields.add(P4Field(name, type.typeRef))
}
```

### Task 2: Delegated ProgramBuilder methods

**Files:**

- Modify: `p4kt/Dsl.kt`

- [ ] **Step 1: Change ProgramBuilder methods to return ReadOnlyProperty**

Replace the four methods in `ProgramBuilder` with delegated versions:

```kotlin
class ProgramBuilder {
  private val declarations = mutableListOf<P4Declaration>()

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

  fun header(block: FieldsBuilder.() -> Unit): ReadOnlyProperty<Any?, P4Header> {
    var registered = false
    lateinit var decl: P4Header
    return ReadOnlyProperty { _, property ->
      if (!registered) {
        decl = p4Header(property.name, block)
        declarations.add(decl)
        registered = true
      }
      decl
    }
  }

  fun struct(block: FieldsBuilder.() -> Unit): ReadOnlyProperty<Any?, P4Struct> {
    var registered = false
    lateinit var decl: P4Struct
    return ReadOnlyProperty { _, property ->
      if (!registered) {
        decl = p4Struct(property.name, block)
        declarations.add(decl)
        registered = true
      }
      decl
    }
  }

  fun function(returnType: P4Type, block: FunctionBuilder.() -> Unit): ReadOnlyProperty<Any?, P4Function> {
    var registered = false
    lateinit var decl: P4Function
    return ReadOnlyProperty { _, property ->
      if (!registered) {
        decl = p4Function(property.name, returnType, block)
        declarations.add(decl)
        registered = true
      }
      decl
    }
  }

  fun build() = P4Program(declarations.toList())
}
```

### Task 3: Update tests

**Files:**

- Modify: `p4kt/P4ProgramTest.kt`

- [ ] **Step 1: Update P4ProgramTest to use delegation and type references**

Replace `p4kt/P4ProgramTest.kt` contents:

```kotlin
package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ProgramTest {
  @Test
  fun programWithMultipleDeclarations() {
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

- [ ] **Step 2: Run all unit tests**

Run: `./dev bazel test //p4kt:all`
Expected: All PASS (standalone builder tests in P4TypedefTest, P4HeaderTest, P4StructTest, P4FunctionTest are unchanged since the standalone API didn't change)

### Task 4: Update golden tests

**Files:**

- Modify: `examples/vss_types.kt`, `examples/identity.kt`

- [ ] **Step 1: Update `examples/vss_types.kt`**

Replace contents:

```kotlin
@file:JvmName("VssTypesKt")

package p4kt.examples

import p4kt.*

fun main() {
  val program = p4Program {
    val EthernetAddress by typedef(bit(48))
    val IPv4Address by typedef(bit(32))
    val Ethernet_h by header {
      field("dstAddr", EthernetAddress)
      field("srcAddr", EthernetAddress)
      field("etherType", bit(16))
    }
    val Ipv4_h by header {
      field("version", bit(4))
      field("ihl", bit(4))
      field("diffserv", bit(8))
      field("totalLen", bit(16))
      field("identification", bit(16))
      field("flags", bit(3))
      field("fragOffset", bit(13))
      field("ttl", bit(8))
      field("protocol", bit(8))
      field("hdrChecksum", bit(16))
      field("srcAddr", IPv4Address)
      field("dstAddr", IPv4Address)
    }
    val Parsed_packet by struct {
      field("ethernet", Ethernet_h)
      field("ip", Ipv4_h)
    }
  }
  println(program.toP4())
}
```

Note: `vss_types.p4` does NOT change - the output is identical.

- [ ] **Step 2: Update `examples/identity.kt`**

Replace contents:

```kotlin
package p4kt.examples

import p4kt.*

fun main() {
  val program = p4Program {
    val id by function(bit(8)) {
      val x by param(bit(8), IN)
      return_(x)
    }
  }
  println(program.toP4())
}
```

Note: `identity.p4` does NOT change.

- [ ] **Step 3: Run all tests**

Run: `./dev bazel test //...`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```
git add p4kt/Ir.kt p4kt/Dsl.kt p4kt/P4ProgramTest.kt examples/vss_types.kt examples/identity.kt
git commit -m "Use delegated properties for declarations and type references

ProgramBuilder methods now use Kotlin's 'by' delegation to derive
P4 names from variable names, eliminating string duplication.
Declarations implement P4TypeReference so they can be passed
directly to field() instead of using typeName().

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```
