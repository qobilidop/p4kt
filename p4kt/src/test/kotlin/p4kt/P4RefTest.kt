package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4RefTest {
  @Test
  fun structRefFieldProducesFieldAccessExpr() {
    class OutControl(base: P4Expr) : P4.StructRef(base) {
      val outputPort by field(P4.bit(4))
    }

    val ref = OutControl(P4Expr.Ref("outCtrl"))
    assertEquals("outCtrl.outputPort", ref.outputPort.toP4())
  }

  @Test
  fun structRefCollectsFieldMetadata() {
    class OutControl(base: P4Expr) : P4.StructRef(base) {
      val outputPort by field(P4.bit(4))
    }

    val ref = OutControl(P4Expr.Ref(""))
    assertEquals(listOf(P4Field("outputPort", P4.bit(4))), ref.fields)
  }

  @Test
  fun headerRefFieldProducesFieldAccessExpr() {
    class Ipv4_h(base: P4Expr) : P4.HeaderRef(base) {
      val ttl by field(P4.bit(8))
      val srcAddr by field(P4.bit(32))
    }

    val ref = Ipv4_h(P4Expr.Ref("ip"))
    assertEquals("ip.ttl", ref.ttl.toP4())
    assertEquals("ip.srcAddr", ref.srcAddr.toP4())
  }

  @Test
  fun headerRefCollectsFieldsInOrder() {
    class Ipv4_h(base: P4Expr) : P4.HeaderRef(base) {
      val ttl by field(P4.bit(8))
      val srcAddr by field(P4.bit(32))
    }

    val ref = Ipv4_h(P4Expr.Ref(""))
    assertEquals(listOf(P4Field("ttl", P4.bit(8)), P4Field("srcAddr", P4.bit(32))), ref.fields)
  }

  @Test
  fun fieldAcceptsTypeReference() {
    @Suppress("VariableNaming") val PortId = P4Typedef("PortId", P4.bit(4))

    class OutControl(base: P4Expr) : P4.StructRef(base) {
      val outputPort by field(PortId)
    }

    val ref = OutControl(P4Expr.Ref(""))
    assertEquals(listOf(P4Field("outputPort", P4Type.Named("PortId"))), ref.fields)
  }

  @Test
  fun structRegistrationGeneratesIrDeclaration() {
    val program =
      P4.program {
        class OutControl(base: P4Expr) : P4.StructRef(base) {
          val outputPort by field(P4.bit(4))
        }
        struct(::OutControl)
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
  fun typedParamInAction() {
    class OutControl(base: P4Expr) : P4.StructRef(base) {
      val outputPort by field(P4.bit(4))
    }

    val a =
      P4.action("Drop") {
        val outCtrl by param(::OutControl, P4.INOUT)
        assign(outCtrl.outputPort, P4.lit(4, 0xF))
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

  @Test
  fun paramAcceptsTypeReference() {
    @Suppress("VariableNaming") val PortId = P4Typedef("PortId", P4.bit(4))

    val a =
      P4.action("Set") {
        val port by param(PortId)
        assign(P4.ref("outPort"), port)
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
    @Suppress("VariableNaming") val PortId = P4Typedef("PortId", P4.bit(4))

    val a =
      P4.action("Set") {
        val port by param(PortId, P4.IN)
        assign(P4.ref("outPort"), port)
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
    assertEquals("foo", P4.ref("foo").toP4())
  }

  @Test
  fun voidReturn() {
    val a =
      P4.action("Drop") {
        assign(P4.ref("x"), P4.lit(1))
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

  @Test
  fun typedFieldProducesTypedRef() {
    class Ipv4_h(base: P4Expr) : P4.HeaderRef(base) {
      val ttl by field(P4.bit(8))
    }

    class Parsed_packet(base: P4Expr) : P4.StructRef(base) {
      val ip by field(::Ipv4_h)
    }

    val pkt = Parsed_packet(P4Expr.Ref("headers"))
    // pkt.ip returns Ipv4_h, so pkt.ip.ttl is a real property
    assertEquals("headers.ip.ttl", pkt.ip.ttl.toP4())
  }

  @Test
  fun typedFieldCollectsMetadata() {
    class Ipv4_h(base: P4Expr) : P4.HeaderRef(base) {
      val ttl by field(P4.bit(8))
    }

    class Parsed_packet(base: P4Expr) : P4.StructRef(base) {
      val ip by field(::Ipv4_h)
    }

    val pkt = Parsed_packet(P4Expr.Ref(""))
    assertEquals(listOf(P4Field("ip", P4Type.Named("Ipv4_h"))), pkt.fields)
  }

  @Test
  fun typedFieldRegistersCorrectly() {
    class Ethernet_h(base: P4Expr) : P4.HeaderRef(base) {
      val dstAddr by field(P4.bit(48))
    }

    class Ipv4_h(base: P4Expr) : P4.HeaderRef(base) {
      val ttl by field(P4.bit(8))
    }

    val program =
      P4.program {
        header(::Ethernet_h)
        header(::Ipv4_h)

        class Parsed_packet(base: P4Expr) : P4.StructRef(base) {
          val ethernet by field(::Ethernet_h)
          val ip by field(::Ipv4_h)
        }
        struct(::Parsed_packet)
      }

    assertEquals(
      """
          header Ethernet_h {
              bit<48> dstAddr;
          }

          header Ipv4_h {
              bit<8> ttl;
          }

          struct Parsed_packet {
              Ethernet_h ethernet;
              Ipv4_h ip;
          }
      """
        .trimIndent(),
      program.toP4(),
    )
  }

  @Test
  fun headerRegistrationGeneratesIrDeclaration() {
    val program =
      P4.program {
        class Ipv4_h(base: P4Expr) : P4.HeaderRef(base) {
          val ttl by field(P4.bit(8))
          val srcAddr by field(P4.bit(32))
        }
        header(::Ipv4_h)
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
}
