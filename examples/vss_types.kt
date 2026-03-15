package p4kt.examples

import p4kt.*

fun main() {
  val program = p4Program {
    val EthernetAddress by typedef(bit(48))
    val IPv4Address by typedef(bit(32))

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
  }
  println(program.toP4())
}
