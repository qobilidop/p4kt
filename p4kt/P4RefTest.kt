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
    assertEquals(listOf(P4Field("ttl", bit(8)), P4Field("srcAddr", bit(32))), ref.fields)
  }

  @Test
  fun fieldAcceptsTypeReference() {
    val PortId = P4Typedef("PortId", bit(4))

    class OutControl(base: P4Expr) : StructRef(base) {
      val outputPort by field(PortId)
    }

    val ref = OutControl(P4Expr.Ref(""))
    assertEquals(listOf(P4Field("outputPort", P4Type.Named("PortId"))), ref.fields)
  }

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
}
