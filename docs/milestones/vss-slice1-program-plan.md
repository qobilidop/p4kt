# P4Program Node Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `P4Program` node that collects top-level declarations and renders them with a single `toP4()` call.

**Architecture:** Introduce `sealed interface P4Declaration` implemented by all top-level constructs, a `P4Program` data class holding a list of declarations, a `ProgramBuilder` DSL class, and a `P4Declaration.toP4()` dispatch function. Update both golden tests to use the new API.

**Tech Stack:** Kotlin, Bazel, kotlin-test with JUnit 4.

---

## File structure

| File                    | Action | Responsibility                                    |
| ----------------------- | ------ | ------------------------------------------------- |
| `p4kt/Ir.kt`            | Modify | Add `P4Declaration` sealed interface, `P4Program` |
| `p4kt/Dsl.kt`           | Modify | Add `ProgramBuilder`, `p4Program()`               |
| `p4kt/Emit.kt`          | Modify | Add `P4Declaration.toP4()`, `P4Program.toP4()`    |
| `p4kt/P4ProgramTest.kt` | Create | Unit test for P4Program                           |
| `p4kt/BUILD.bazel`      | Modify | Add P4ProgramTest target                          |
| `examples/vss_types.kt` | Modify | Rewrite to use `p4Program { ... }`                |
| `examples/vss_types.p4` | Modify | Add blank line between consecutive typedefs       |
| `examples/identity.kt`  | Modify | Rewrite to use `p4Program { ... }`                |

---

## Chunk 1: P4Program implementation and golden test updates

This is one logical unit of work: introduce P4Program and update examples to use it.

### Task 1: P4Program with P4Declaration

**Files:**

- Create: `p4kt/P4ProgramTest.kt`
- Modify: `p4kt/Ir.kt`, `p4kt/Dsl.kt`, `p4kt/Emit.kt`, `p4kt/BUILD.bazel`

- [ ] **Step 1: Write the failing test**

Create `p4kt/P4ProgramTest.kt`:

```kotlin
package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ProgramTest {
  @Test
  fun programWithMultipleDeclarations() {
    val program =
      p4Program {
        typedef("EthernetAddress", bit(48))
        header("Ethernet_h") {
          field("dstAddr", typeName("EthernetAddress"))
          field("etherType", bit(16))
        }
        struct("Parsed_packet") {
          field("ethernet", typeName("Ethernet_h"))
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

Add test target to `p4kt/BUILD.bazel`:

```python
kt_jvm_test(
    name = "P4ProgramTest",
    srcs = ["P4ProgramTest.kt"],
    test_class = "p4kt.P4ProgramTest",
    deps = [
        ":p4kt",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
        "@maven//:org_jetbrains_kotlin_kotlin_test_junit",
    ],
)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `bazel test //p4kt:P4ProgramTest`
Expected: BUILD FAILURE (unresolved reference `p4Program`)

- [ ] **Step 3: Implement P4Declaration, P4Program, ProgramBuilder, and emitters**

In `p4kt/Ir.kt`, add `sealed interface P4Declaration` and have existing types implement it. Add `P4Program`:

```kotlin
sealed interface P4Declaration

data class P4Typedef(val name: String, val type: P4Type) : P4Declaration

data class P4Header(val name: String, val fields: List<P4Field>) : P4Declaration

data class P4Struct(val name: String, val fields: List<P4Field>) : P4Declaration

data class P4Function(
  val name: String,
  val returnType: P4Type,
  val params: List<P4Param>,
  val body: List<P4Statement>,
) : P4Declaration

data class P4Program(val declarations: List<P4Declaration>)
```

In `p4kt/Dsl.kt`, add `ProgramBuilder` and `p4Program()` at the end:

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

In `p4kt/Emit.kt`, add at the end:

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

- [ ] **Step 4: Run test to verify it passes**

Run: `bazel test //p4kt:P4ProgramTest`
Expected: PASS

### Task 2: Update golden tests

**Files:**

- Modify: `examples/vss_types.kt`, `examples/vss_types.p4`
- Modify: `examples/identity.kt`

- [ ] **Step 1: Update `examples/vss_types.kt`**

Replace contents with:

```kotlin
@file:JvmName("VssTypesKt")

package p4kt.examples

import p4kt.*

fun main() {
  val program =
    p4Program {
      typedef("EthernetAddress", bit(48))
      typedef("IPv4Address", bit(32))
      header("Ethernet_h") {
        field("dstAddr", typeName("EthernetAddress"))
        field("srcAddr", typeName("EthernetAddress"))
        field("etherType", bit(16))
      }
      header("Ipv4_h") {
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
      struct("Parsed_packet") {
        field("ethernet", typeName("Ethernet_h"))
        field("ip", typeName("Ipv4_h"))
      }
    }
  println(program.toP4())
}
```

- [ ] **Step 2: Update `examples/vss_types.p4`**

The output changes: all declarations are now separated by blank lines (including the two consecutive typedefs which previously had no blank line between them). Replace contents with:

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

- [ ] **Step 3: Update `examples/identity.kt`**

Replace contents with:

```kotlin
package p4kt.examples

import p4kt.*

fun main() {
  val program =
    p4Program {
      function("id", bit(8)) {
        val x by param(bit(8), IN)
        return_(x)
      }
    }
  println(program.toP4())
}
```

Note: `identity.p4` does not change - a single-declaration program produces identical output.

- [ ] **Step 4: Run all tests**

Run: `bazel test //...`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```
git add p4kt/Ir.kt p4kt/Dsl.kt p4kt/Emit.kt p4kt/P4ProgramTest.kt p4kt/BUILD.bazel examples/vss_types.kt examples/vss_types.p4 examples/identity.kt
git commit -m "Add P4Program node for composing declarations

Introduces P4Declaration sealed interface and P4Program to collect
top-level declarations and render them with a single toP4() call.
Updates both golden tests to use the p4Program { ... } DSL.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```
