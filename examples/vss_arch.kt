package p4kt.examples

import p4kt.*

// Corresponds to the supported subset of:
// https://github.com/p4lang/p4c/blob/main/testdata/p4_16_samples/very_simple_model.p4

object vss_arch : P4Library() {
  val PortId = typedef("PortId", bit(4))

  val REAL_PORT_COUNT = const_("REAL_PORT_COUNT", PortId.typeRef, lit(4, 8))

  class InControl(base: P4Expr) : StructRef(base) {
    val inputPort by field(PortId)
  }

  init {
    struct(::InControl)
  }

  val RECIRCULATE_IN_PORT = const_("RECIRCULATE_IN_PORT", PortId.typeRef, lit(4, 0xD))
  val CPU_IN_PORT = const_("CPU_IN_PORT", PortId.typeRef, lit(4, 0xE))

  class OutControl(base: P4Expr) : StructRef(base) {
    val outputPort by field(PortId)
  }

  init {
    struct(::OutControl)
  }

  val DROP_PORT = const_("DROP_PORT", PortId.typeRef, lit(4, 0xF))
  val CPU_OUT_PORT = const_("CPU_OUT_PORT", PortId.typeRef, lit(4, 0xE))
  val RECIRCULATE_OUT_PORT = const_("RECIRCULATE_OUT_PORT", PortId.typeRef, lit(4, 0xD))

  val Ck16 =
    extern("Ck16") {
      constructor_()
      method("clear", void_)
      method("update", void_) {
        val data by param(typeName("T"), IN)
      }
      method("get", bit(16))
    }
}

fun main() = println(vss_arch.toP4())
