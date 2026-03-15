package p4kt.examples

import p4kt.*

fun main() {
  val program = p4Program {
    val EthernetAddress by typedef(bit(48))
    val IPv4Address by typedef(bit(32))
    val Ethernet_h by header {
      field("dstAddr", EthernetAddress)
      field("srcAddr", EthernetAddress)
      field("etherType", bit(16))
    }
    val Ipv4_h by header {
      field("version", bit(4))
      field("ihl", bit(4))
      field("diffserv", bit(8))
      field("totalLen", bit(16))
      field("identification", bit(16))
      field("flags", bit(3))
      field("fragOffset", bit(13))
      field("ttl", bit(8))
      field("protocol", bit(8))
      field("hdrChecksum", bit(16))
      field("srcAddr", IPv4Address)
      field("dstAddr", IPv4Address)
    }
    val Parsed_packet by struct {
      field("ethernet", Ethernet_h)
      field("ip", Ipv4_h)
    }
  }
  println(program.toP4())
}
