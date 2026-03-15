package p4kt.examples

import p4kt.*

fun main() {
  val program = p4Program {
    val PortId by typedef(bit(4))
    val IPv4Address by typedef(bit(32))

    val DROP_PORT by const_(PortId.typeRef, lit(4, 0xF))

    val Ethernet_h by header {
      field("dstAddr", bit(48))
      field("srcAddr", bit(48))
      field("etherType", bit(16))
    }

    val Ipv4_h by header {
      field("ttl", bit(8))
      field("srcAddr", IPv4Address)
      field("dstAddr", IPv4Address)
    }

    val OutControl by struct { field("outputPort", PortId) }

    val Drop_action by action {
      val outCtrl by param(typeName("OutControl"), INOUT)
      assign(outCtrl.dot("outputPort"), DROP_PORT)
    }

    val Set_nhop by action {
      val ipv4_dest by param(IPv4Address.typeRef)
      val port by param(PortId.typeRef)
      val headers_ip by param(typeName("Ipv4_h"), INOUT)
      val outCtrl by param(typeName("OutControl"), INOUT)
      assign(P4Expr.Ref("nextHop"), ipv4_dest)
      assign(headers_ip.dot("ttl"), headers_ip.dot("ttl") - lit(1))
      assign(outCtrl.dot("outputPort"), port)
    }
  }
  println(program.toP4())
}
