@file:Suppress("UnusedPrivateProperty")

package p4kt.examples

import p4kt.P4
import p4kt.P4Expr
import p4kt.p4include.core

// Corresponds to:
// https://github.com/p4lang/p4c/blob/main/testdata/p4_16_samples/very_simple_model.p4

object vss_arch : P4.Library() {
  init {
    include(core)
  }

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

  val Parser by parserTypeDecl {
    val H by typeParam()
    val b by param(core.packet_in)
    val parsedHeaders by param(H, P4.OUT)
  }

  val Pipe by controlTypeDecl {
    val H by typeParam()
    val headers by param(H, P4.INOUT)
    val parseError by param(P4.errorType, P4.IN)
    val inCtrl by param(::InControl, P4.IN)
    val outCtrl by param(::OutControl, P4.OUT)
  }

  val Deparser by controlTypeDecl {
    val H by typeParam()
    val outputHeaders by param(H, P4.INOUT)
    val b by param(core.packet_out)
  }

  val VSS by packageTypeDecl {
    val H by typeParam()
    val p by param(Parser)
    val map by param(Pipe)
    val d by param(Deparser)
  }

  val Ck16 by extern {
    constructor_()
    val clear by method(P4.void_)
    val update by
      method(P4.void_) {
        val T by typeParam()
        val data by param(T, P4.IN)
      }
    val get by method(P4.bit(16))
  }
}
