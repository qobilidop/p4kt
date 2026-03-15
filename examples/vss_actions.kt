package p4kt.examples

import p4kt.*

fun main() {
  val program = p4Program {
    val PortId by typedef(bit(4))
    val IPv4Address by typedef(bit(32))

    val DROP_PORT by const_(PortId, lit(4, 0xF))

    class Ethernet_h(base: P4Expr) : HeaderRef(base) {
      val dstAddr by field(bit(48))
      val srcAddr by field(bit(48))
      val etherType by field(bit(16))
    }
    header(::Ethernet_h)

    class Ipv4_h(base: P4Expr) : HeaderRef(base) {
      val ttl by field(bit(8))
      val srcAddr by field(IPv4Address)
      val dstAddr by field(IPv4Address)
    }
    header(::Ipv4_h)

    class OutControl(base: P4Expr) : StructRef(base) {
      val outputPort by field(PortId)
    }
    struct(::OutControl)

    val Drop_action by action {
      val outCtrl by param(::OutControl, INOUT)
      assign(outCtrl.outputPort, DROP_PORT)
    }

    val Set_nhop by action {
      val ipv4_dest by param(IPv4Address)
      val port by param(PortId)
      val headers_ip by param(::Ipv4_h, INOUT)
      val outCtrl by param(::OutControl, INOUT)
      assign(ref("nextHop"), ipv4_dest)
      assign(headers_ip.ttl, headers_ip.ttl - lit(1))
      assign(outCtrl.outputPort, port)
    }
  }
  println(program.toP4())
}
