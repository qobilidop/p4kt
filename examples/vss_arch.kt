package p4kt.examples

import p4kt.P4
import p4kt.P4Expr

// Corresponds to the supported subset of:
// https://github.com/p4lang/p4c/blob/main/testdata/p4_16_samples/very_simple_model.p4

object vss_arch : P4.Library() {
  val PortId = typedef("PortId", P4.bit(4))

  val REAL_PORT_COUNT = const_("REAL_PORT_COUNT", PortId.typeRef, P4.lit(4, 8))

  class InControl(base: P4Expr) : P4.StructRef(base) {
    val inputPort by field(PortId)
  }

  init {
    struct(::InControl)
  }

  val RECIRCULATE_IN_PORT = const_("RECIRCULATE_IN_PORT", PortId.typeRef, P4.lit(4, 0xD))
  val CPU_IN_PORT = const_("CPU_IN_PORT", PortId.typeRef, P4.lit(4, 0xE))

  class OutControl(base: P4Expr) : P4.StructRef(base) {
    val outputPort by field(PortId)
  }

  init {
    struct(::OutControl)
  }

  val DROP_PORT = const_("DROP_PORT", PortId.typeRef, P4.lit(4, 0xF))
  val CPU_OUT_PORT = const_("CPU_OUT_PORT", PortId.typeRef, P4.lit(4, 0xE))
  val RECIRCULATE_OUT_PORT = const_("RECIRCULATE_OUT_PORT", PortId.typeRef, P4.lit(4, 0xD))

  val Ck16 =
    extern("Ck16") {
      constructor_()
      method("clear", P4.void_)
      method("update", P4.void_) {
        val data by param(P4.typeName("T"), P4.IN)
      }
      method("get", P4.bit(16))
    }
}

fun main() = println(vss_arch.toP4())
