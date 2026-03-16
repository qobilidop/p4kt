package p4kt.examples

import p4kt.P4
import p4kt.P4Expr

// Corresponds to the supported subset of:
// https://github.com/p4lang/p4c/blob/main/testdata/p4_16_samples/very_simple_model.p4

object vss_arch : P4.Library() {
  val PortId by typedef(P4.bit(4))

  val REAL_PORT_COUNT by const_(PortId.typeRef, P4.lit(4, 8))

  class InControl(base: P4Expr) : P4.StructRef(base) {
    val inputPort by field(PortId)
  }

  init {
    struct(::InControl)
  }

  val RECIRCULATE_IN_PORT by const_(PortId.typeRef, P4.lit(4, 0xD))
  val CPU_IN_PORT by const_(PortId.typeRef, P4.lit(4, 0xE))

  class OutControl(base: P4Expr) : P4.StructRef(base) {
    val outputPort by field(PortId)
  }

  init {
    struct(::OutControl)
  }

  val DROP_PORT by const_(PortId.typeRef, P4.lit(4, 0xF))
  val CPU_OUT_PORT by const_(PortId.typeRef, P4.lit(4, 0xE))
  val RECIRCULATE_OUT_PORT by const_(PortId.typeRef, P4.lit(4, 0xD))

  val Ck16 by extern {
    constructor_()
    val clear by method(P4.void_)
    val update by
      method(P4.void_) {
        val data by param(P4.typeName("T"), P4.IN)
      }
    val get by method(P4.bit(16))
  }
}
