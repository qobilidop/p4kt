package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("MagicNumber") private val testPortId = p4Typedef("PortId", P4.bit(4))

private class TestMyStruct(base: P4Expr) : P4.StructRef(base) {
  val port by field(testPortId)
}

@Suppress("MagicNumber")
private val testLib =
  object : P4.Library() {
    val PortId = typedef("PortId", P4.bit(4))
    val DROP_PORT = const_("DROP_PORT", PortId.typeRef, P4.lit(4, 0xF))

    init {
      struct(::TestMyStruct)
    }
  }

@Suppress("MagicNumber")
private val testLibExtern =
  object : P4.Library() {
    val Ck16 =
      extern("Ck16") {
        constructor_()
        method("get", P4.bit(16))
      }
  }

class P4ProgramTest {
  @Test
  fun programWithMultipleDeclarations() {
    val program = p4Program {
      val EthernetAddress by typedef(P4.bit(48))

      class Ethernet_h(base: P4Expr) : P4.HeaderRef(base) {
        val dstAddr by field(EthernetAddress)
        val etherType by field(P4.bit(16))
      }
      header(::Ethernet_h)

      class Parsed_packet(base: P4Expr) : P4.StructRef(base) {
        val ethernet by field(P4.typeName("Ethernet_h"))
      }
      struct(::Parsed_packet)
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

  @Test
  fun declarePreBuiltDeclarations() {
    val portId = p4Typedef("PortId", P4.bit(4))
    val dropPort = p4Const("DROP_PORT", portId.typeRef, P4.lit(4, 0xF))

    val program = p4Program {
      declare(portId)
      declare(dropPort)
    }

    assertEquals(
      """
            typedef bit<4> PortId;

            const PortId DROP_PORT = 4w15;
      """
        .trimIndent(),
      program.toP4(),
    )
  }

  @Test
  fun declareStructRefFromLibrary() {
    val portId = p4Typedef("PortId", P4.bit(4))

    class InControl(base: P4Expr) : P4.StructRef(base) {
      val inputPort by field(portId)
    }

    val program = p4Program {
      declare(portId)
      struct(::InControl)
    }

    assertEquals(
      """
            typedef bit<4> PortId;

            struct InControl {
                PortId inputPort;
            }
      """
        .trimIndent(),
      program.toP4(),
    )
  }

  @Test
  fun constRef() {
    val dropPort = p4Const("DROP_PORT", P4.typeName("PortId"), P4.lit(4, 0xF))

    val program = p4Program {
      @Suppress("UnusedPrivateProperty")
      val Drop_action by action { assign(P4.ref("out"), dropPort.ref) }
    }

    assert("DROP_PORT" in program.toP4()) { "DROP_PORT should be used as reference" }
  }

  @Test
  fun libraryDeclarationsInOrder() {
    assertEquals(
      """
            typedef bit<4> PortId;

            const PortId DROP_PORT = 4w15;

            struct TestMyStruct {
                PortId port;
            }
      """
        .trimIndent(),
      testLib.toP4(),
    )
  }

  @Test
  fun libraryExtern() {
    assertEquals(
      """
            extern Ck16 {
                Ck16();
                bit<16> get();
            }
      """
        .trimIndent(),
      testLibExtern.toP4(),
    )
  }

  @Test
  fun p4ExprMemberOperators() {
    val a = P4Expr.Ref("a")
    val b = P4Expr.Ref("b")

    assertEquals("a == b", (a eq b).toP4())
    assertEquals("a != b", (a ne b).toP4())
    assertEquals("a - b", (a - b).toP4())
    assertEquals("a.foo(b)", a.call("foo", b).toP4())
  }

  @Test
  @Suppress("MagicNumber")
  fun p4StructRefInsideP4Object() {
    class MyStruct(base: P4Expr) : P4.StructRef(base) {
      val port by field(P4.bit(4))
    }

    val s = MyStruct(P4Expr.Ref("s"))
    assertEquals(1, s.fields.size)
  }

  @Test
  fun libraryErrors() {
    val lib =
      object : P4.Library() {
        init {
          errors("NoError", "PacketTooShort")
        }
      }

    assertEquals(
      """
            error {
                NoError,
                PacketTooShort
            }
      """
        .trimIndent(),
      lib.toP4(),
    )
  }

  @Test
  fun packageInstantiation() {
    val pkg = P4PackageInstance("VSS", listOf("TopParser", "TopPipe", "TopDeparser"), "main")
    assertEquals("VSS(TopParser(), TopPipe(), TopDeparser()) main;", pkg.toP4())
  }

  @Test
  fun packageInstantiationDsl() {
    val program = p4Program {
      packageInstance("VSS", "main", "TopParser", "TopPipe", "TopDeparser")
    }
    assertEquals("VSS(TopParser(), TopPipe(), TopDeparser()) main;", program.toP4())
  }
}
