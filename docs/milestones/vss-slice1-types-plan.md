# Typedef, Header, and Struct Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add typedef, header, and struct declarations to P4kt with TDD.

**Architecture:** Three new IR data classes (`P4Typedef`, `P4Header`, `P4Struct`) plus `P4Field` and `P4Type.Named`. Each gets a DSL builder and `toP4()` emitter. Follows the existing three-layer pattern (IR -> DSL -> Emit).

**Tech Stack:** Kotlin, Bazel, kotlin-test with JUnit 4.

---

## File structure

| File                    | Action | Responsibility                                                               |
| ----------------------- | ------ | ---------------------------------------------------------------------------- |
| `p4kt/Ir.kt`            | Modify | Add `P4Type.Named`, `P4Field`, `P4Typedef`, `P4Header`, `P4Struct`           |
| `p4kt/Dsl.kt`           | Modify | Add `typeName()`, `FieldsBuilder`, `p4Typedef()`, `p4Header()`, `p4Struct()` |
| `p4kt/Emit.kt`          | Modify | Add `toP4()` for new IR nodes                                                |
| `p4kt/P4TypedefTest.kt` | Create | Unit tests for typedef and named type references                             |
| `p4kt/P4HeaderTest.kt`  | Create | Unit tests for header                                                        |
| `p4kt/P4StructTest.kt`  | Create | Unit tests for struct                                                        |
| `p4kt/BUILD.bazel`      | Modify | Add new test targets                                                         |
| `examples/vss_types.kt` | Create | Golden test example using VSS type declarations                              |
| `examples/vss_types.p4` | Create | Expected P4 output for golden test                                           |
| `examples/BUILD.bazel`  | Modify | Add `golden_test(name = "vss_types")`                                        |

---

## Chunk 1: Typedef, named type, header, and struct

This is one logical unit of work: the type declaration constructs needed for VSS. TDD within the chunk (write test, verify fail, implement, verify pass) but one commit at the end.

### Task 1: Typedef

**Files:**

- Create: `p4kt/P4TypedefTest.kt`
- Modify: `p4kt/Ir.kt`, `p4kt/Dsl.kt`, `p4kt/Emit.kt`, `p4kt/BUILD.bazel`

- [ ] **Step 1: Write the failing test**

Create `p4kt/P4TypedefTest.kt`:

```kotlin
package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4TypedefTest {
  @Test
  fun typedefBitType() {
    val ethernetAddress = p4Typedef("EthernetAddress", bit(48))

    assertEquals("typedef bit<48> EthernetAddress;", ethernetAddress.toP4())
  }
}
```

Add test target to `p4kt/BUILD.bazel`:

```python
kt_jvm_test(
    name = "P4TypedefTest",
    srcs = ["P4TypedefTest.kt"],
    test_class = "p4kt.P4TypedefTest",
    deps = [
        ":p4kt",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
        "@maven//:org_jetbrains_kotlin_kotlin_test_junit",
    ],
)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `bazel test //p4kt:P4TypedefTest`
Expected: BUILD FAILURE (unresolved reference `p4Typedef`)

- [ ] **Step 3: Implement typedef**

Add to `p4kt/Ir.kt`:

```kotlin
data class P4Typedef(val name: String, val type: P4Type)
```

Add to `p4kt/Dsl.kt`:

```kotlin
fun p4Typedef(name: String, type: P4Type) = P4Typedef(name, type)
```

Add to `p4kt/Emit.kt`:

```kotlin
fun P4Typedef.toP4(): String = "typedef ${type.toP4()} $name;"
```

- [ ] **Step 4: Run test to verify it passes**

Run: `bazel test //p4kt:P4TypedefTest`
Expected: PASS

### Task 2: Named type reference

**Files:**

- Modify: `p4kt/Ir.kt`, `p4kt/Dsl.kt`, `p4kt/Emit.kt`
- Modify: `p4kt/P4TypedefTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `P4TypedefTest.kt`:

```kotlin
@Test
fun typedefNamedType() {
  val addr = p4Typedef("Addr", typeName("EthernetAddress"))

  assertEquals("typedef EthernetAddress Addr;", addr.toP4())
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `bazel test //p4kt:P4TypedefTest`
Expected: BUILD FAILURE (unresolved reference `typeName`)

- [ ] **Step 3: Implement Named type**

Add to `P4Type` sealed class in `p4kt/Ir.kt`:

```kotlin
data class Named(val name: String) : P4Type()
```

Add `is P4Type.Named -> name` to `P4Type.toP4()` when clause in `p4kt/Emit.kt`.

Add to `p4kt/Dsl.kt`:

```kotlin
fun typeName(name: String) = P4Type.Named(name)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `bazel test //p4kt:P4TypedefTest`
Expected: PASS

### Task 3: Header with fields

**Files:**

- Create: `p4kt/P4HeaderTest.kt`
- Modify: `p4kt/Ir.kt`, `p4kt/Dsl.kt`, `p4kt/Emit.kt`, `p4kt/BUILD.bazel`

- [ ] **Step 1: Write the failing test**

Create `p4kt/P4HeaderTest.kt`:

```kotlin
package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4HeaderTest {
  @Test
  fun ethernetHeader() {
    val ethernetH =
      p4Header("Ethernet_h") {
        field("dstAddr", typeName("EthernetAddress"))
        field("srcAddr", typeName("EthernetAddress"))
        field("etherType", bit(16))
      }

    assertEquals(
      """
            header Ethernet_h {
                EthernetAddress dstAddr;
                EthernetAddress srcAddr;
                bit<16> etherType;
            }
            """
        .trimIndent(),
      ethernetH.toP4(),
    )
  }
}
```

Add test target to `p4kt/BUILD.bazel`:

```python
kt_jvm_test(
    name = "P4HeaderTest",
    srcs = ["P4HeaderTest.kt"],
    test_class = "p4kt.P4HeaderTest",
    deps = [
        ":p4kt",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
        "@maven//:org_jetbrains_kotlin_kotlin_test_junit",
    ],
)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `bazel test //p4kt:P4HeaderTest`
Expected: BUILD FAILURE (unresolved reference `p4Header`)

- [ ] **Step 3: Implement header**

Add to `p4kt/Ir.kt`:

```kotlin
data class P4Field(val name: String, val type: P4Type)

data class P4Header(val name: String, val fields: List<P4Field>)
```

Add to `p4kt/Dsl.kt`:

```kotlin
class FieldsBuilder {
  private val fields = mutableListOf<P4Field>()

  fun field(name: String, type: P4Type) {
    fields.add(P4Field(name, type))
  }

  fun build() = fields.toList()
}

fun p4Header(name: String, block: FieldsBuilder.() -> Unit): P4Header {
  val builder = FieldsBuilder()
  builder.block()
  return P4Header(name, builder.build())
}
```

Add to `p4kt/Emit.kt`:

```kotlin
fun P4Field.toP4(): String = "${type.toP4()} $name;"

fun P4Header.toP4(): String {
  val fieldsStr = fields.joinToString("\n") { "    ${it.toP4()}" }
  return "header $name {\n$fieldsStr\n}"
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `bazel test //p4kt:P4HeaderTest`
Expected: PASS

### Task 4: Struct with fields

**Files:**

- Create: `p4kt/P4StructTest.kt`
- Modify: `p4kt/Ir.kt`, `p4kt/Dsl.kt`, `p4kt/Emit.kt`, `p4kt/BUILD.bazel`

- [ ] **Step 1: Write the failing test**

Create `p4kt/P4StructTest.kt`:

```kotlin
package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4StructTest {
  @Test
  fun parsedPacketStruct() {
    val parsedPacket =
      p4Struct("Parsed_packet") {
        field("ethernet", typeName("Ethernet_h"))
        field("ip", typeName("Ipv4_h"))
      }

    assertEquals(
      """
            struct Parsed_packet {
                Ethernet_h ethernet;
                Ipv4_h ip;
            }
            """
        .trimIndent(),
      parsedPacket.toP4(),
    )
  }
}
```

Add test target to `p4kt/BUILD.bazel`:

```python
kt_jvm_test(
    name = "P4StructTest",
    srcs = ["P4StructTest.kt"],
    test_class = "p4kt.P4StructTest",
    deps = [
        ":p4kt",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
        "@maven//:org_jetbrains_kotlin_kotlin_test_junit",
    ],
)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `bazel test //p4kt:P4StructTest`
Expected: BUILD FAILURE (unresolved reference `p4Struct`)

- [ ] **Step 3: Implement struct**

Add to `p4kt/Ir.kt`:

```kotlin
data class P4Struct(val name: String, val fields: List<P4Field>)
```

Add to `p4kt/Dsl.kt`:

```kotlin
fun p4Struct(name: String, block: FieldsBuilder.() -> Unit): P4Struct {
  val builder = FieldsBuilder()
  builder.block()
  return P4Struct(name, builder.build())
}
```

Add to `p4kt/Emit.kt`:

```kotlin
fun P4Struct.toP4(): String {
  val fieldsStr = fields.joinToString("\n") { "    ${it.toP4()}" }
  return "struct $name {\n$fieldsStr\n}"
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `bazel test //p4kt:P4StructTest`
Expected: PASS

- [ ] **Step 5: Run all tests**

Run: `bazel test //...`
Expected: All tests PASS (including existing `P4ktTest`, `P4FunctionTest`, `identity_test`)

- [ ] **Step 6: Commit**

```
git add p4kt/Ir.kt p4kt/Dsl.kt p4kt/Emit.kt p4kt/P4TypedefTest.kt p4kt/P4HeaderTest.kt p4kt/P4StructTest.kt p4kt/BUILD.bazel
git commit -m "Add typedef, header, and struct support

These are the foundational type declarations for the VSS milestone.
Typedef aliases types by name, header and struct group typed fields.
Named type references allow fields to use declared type names.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

## Chunk 2: Golden test

### Task 5: VSS types golden test

**Files:**

- Create: `examples/vss_types.kt`
- Create: `examples/vss_types.p4`
- Modify: `examples/BUILD.bazel`

- [ ] **Step 1: Write the golden test example**

Create `examples/vss_types.kt`:

```kotlin
package p4kt.examples

import p4kt.*

fun main() {
  val ethernetAddress = p4Typedef("EthernetAddress", bit(48))
  val ipv4Address = p4Typedef("IPv4Address", bit(32))

  val ethernetH =
    p4Header("Ethernet_h") {
      field("dstAddr", typeName("EthernetAddress"))
      field("srcAddr", typeName("EthernetAddress"))
      field("etherType", bit(16))
    }

  val ipv4H =
    p4Header("Ipv4_h") {
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
      field("srcAddr", typeName("IPv4Address"))
      field("dstAddr", typeName("IPv4Address"))
    }

  val parsedPacket =
    p4Struct("Parsed_packet") {
      field("ethernet", typeName("Ethernet_h"))
      field("ip", typeName("Ipv4_h"))
    }

  println(ethernetAddress.toP4())
  println(ipv4Address.toP4())
  println()
  println(ethernetH.toP4())
  println()
  println(ipv4H.toP4())
  println()
  println(parsedPacket.toP4())
}
```

- [ ] **Step 2: Write the expected output**

Create `examples/vss_types.p4` (must end with a trailing newline):

```p4
typedef bit<48> EthernetAddress;
typedef bit<32> IPv4Address;

header Ethernet_h {
    EthernetAddress dstAddr;
    EthernetAddress srcAddr;
    bit<16> etherType;
}

header Ipv4_h {
    bit<4> version;
    bit<4> ihl;
    bit<8> diffserv;
    bit<16> totalLen;
    bit<16> identification;
    bit<3> flags;
    bit<13> fragOffset;
    bit<8> ttl;
    bit<8> protocol;
    bit<16> hdrChecksum;
    IPv4Address srcAddr;
    IPv4Address dstAddr;
}

struct Parsed_packet {
    Ethernet_h ethernet;
    Ipv4_h ip;
}
```

- [ ] **Step 3: Register the golden test**

Add to `examples/BUILD.bazel`:

```python
golden_test(name = "vss_types")
```

- [ ] **Step 4: Run the golden test**

Run: `bazel test //examples:vss_types_test`
Expected: PASS

- [ ] **Step 5: Run all tests to verify nothing is broken**

Run: `bazel test //...`
Expected: All tests PASS

- [ ] **Step 6: Commit**

```
git add examples/vss_types.kt examples/vss_types.p4 examples/BUILD.bazel
git commit -m "Add VSS types golden test

Validates typedef, header, and struct using the type declarations
from the P4 spec's Very Simple Switch example.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```
