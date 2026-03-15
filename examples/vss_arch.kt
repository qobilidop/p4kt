package p4kt.examples

import p4kt.*

// Corresponds to the supported subset of:
// https://github.com/p4lang/p4c/blob/main/testdata/p4_16_samples/very_simple_model.p4

fun main() {
  val program = p4Program {
    val PortId by typedef(bit(4))

    val REAL_PORT_COUNT by const_(PortId, lit(4, 8))

    class InControl(base: P4Expr) : StructRef(base) {
      val inputPort by field(PortId)
    }
    struct(::InControl)

    val RECIRCULATE_IN_PORT by const_(PortId, lit(4, 0xD))
    val CPU_IN_PORT by const_(PortId, lit(4, 0xE))

    class OutControl(base: P4Expr) : StructRef(base) {
      val outputPort by field(PortId)
    }
    struct(::OutControl)

    val DROP_PORT by const_(PortId, lit(4, 0xF))
    val CPU_OUT_PORT by const_(PortId, lit(4, 0xE))
    val RECIRCULATE_OUT_PORT by const_(PortId, lit(4, 0xD))

    @Suppress("UnusedPrivateProperty")
    val Ck16 by extern {
      constructor_()
      method("clear", void_)
      method("update", void_) {
        val data by param(typeName("T"), IN)
      }
      method("get", bit(16))
    }
  }
  println(program.toP4())
}
