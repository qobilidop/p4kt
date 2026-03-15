@file:JvmName("VssTypesKt")

package p4kt.examples

import p4kt.*

fun main() {
  val program = p4Program {
    typedef("EthernetAddress", bit(48))
    typedef("IPv4Address", bit(32))
    header("Ethernet_h") {
      field("dstAddr", typeName("EthernetAddress"))
      field("srcAddr", typeName("EthernetAddress"))
      field("etherType", bit(16))
    }
    header("Ipv4_h") {
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
      field("srcAddr", typeName("IPv4Address"))
      field("dstAddr", typeName("IPv4Address"))
    }
    struct("Parsed_packet") {
      field("ethernet", typeName("Ethernet_h"))
      field("ip", typeName("Ipv4_h"))
    }
  }
  println(program.toP4())
}
