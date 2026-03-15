package p4kt.examples

import p4kt.*

// Corresponds to the supported subset of:
// https://github.com/p4lang/p4c/blob/main/testdata/p4_16_samples/vss-example.p4

fun main() {
  val program = p4Program {
    val EthernetAddress by typedef(bit(48))
    val IPv4Address by typedef(bit(32))
    val PortId by typedef(bit(4))

    val DROP_PORT by const_(PortId, lit(4, 0xF))
    val CPU_OUT_PORT by const_(PortId, lit(4, 0xE))

    class Ethernet_h(base: P4Expr) : HeaderRef(base) {
      val dstAddr by field(EthernetAddress)
      val srcAddr by field(EthernetAddress)
      val etherType by field(bit(16))
    }
    header(::Ethernet_h)

    class Ipv4_h(base: P4Expr) : HeaderRef(base) {
      val version by field(bit(4))
      val ihl by field(bit(4))
      val diffserv by field(bit(8))
      val totalLen by field(bit(16))
      val identification by field(bit(16))
      val flags by field(bit(3))
      val fragOffset by field(bit(13))
      val ttl by field(bit(8))
      val protocol by field(bit(8))
      val hdrChecksum by field(bit(16))
      val srcAddr by field(IPv4Address)
      val dstAddr by field(IPv4Address)
    }
    header(::Ipv4_h)

    class Parsed_packet(base: P4Expr) : StructRef(base) {
      val ethernet by field(typeName("Ethernet_h"))
      val ip by field(typeName("Ipv4_h"))
    }
    struct(::Parsed_packet)

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

    val Send_to_cpu by action {
      val outCtrl by param(::OutControl, INOUT)
      assign(outCtrl.outputPort, CPU_OUT_PORT)
    }

    val Set_dmac by action {
      val dmac by param(EthernetAddress)
      val headers_ethernet by param(::Ethernet_h, INOUT)
      assign(headers_ethernet.dstAddr, dmac)
    }

    val Set_smac by action {
      val smac by param(EthernetAddress)
      val headers_ethernet by param(::Ethernet_h, INOUT)
      assign(headers_ethernet.srcAddr, smac)
    }
  }
  println(program.toP4())
}
