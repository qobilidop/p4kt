@file:JvmName("VssTypesKt")

package p4kt.examples

import p4kt.*

fun main() {
  val ethernetAddress = p4Typedef("EthernetAddress", bit(48))
  val ipv4Address = p4Typedef("IPv4Address", bit(32))

  val ethernetH =
    p4Header("Ethernet_h") {
      field("dstAddr", typeName("EthernetAddress"))
      field("srcAddr", typeName("EthernetAddress"))
      field("etherType", bit(16))
    }

  val ipv4H =
    p4Header("Ipv4_h") {
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

  val parsedPacket =
    p4Struct("Parsed_packet") {
      field("ethernet", typeName("Ethernet_h"))
      field("ip", typeName("Ipv4_h"))
    }

  println(ethernetAddress.toP4())
  println(ipv4Address.toP4())
  println()
  println(ethernetH.toP4())
  println()
  println(ipv4H.toP4())
  println()
  println(parsedPacket.toP4())
}
