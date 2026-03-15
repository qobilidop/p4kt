package p4kt.examples

import p4kt.P4
import p4kt.P4Expr
import p4kt.p4Program
import p4kt.p4include.core

// Corresponds to:
// https://github.com/p4lang/p4c/blob/main/testdata/p4_16_samples/vss-example.p4

val vss_example = p4Program {
  val EthernetAddress by typedef(P4.bit(48))
  val IPv4Address by typedef(P4.bit(32))

  class Ethernet_h(base: P4Expr) : P4.HeaderRef(base) {
    val dstAddr by field(EthernetAddress)
    val srcAddr by field(EthernetAddress)
    val etherType by field(P4.bit(16))
  }
  header(::Ethernet_h)

  class Ipv4_h(base: P4Expr) : P4.HeaderRef(base) {
    val version by field(P4.bit(4))
    val ihl by field(P4.bit(4))
    val diffserv by field(P4.bit(8))
    val totalLen by field(P4.bit(16))
    val identification by field(P4.bit(16))
    val flags by field(P4.bit(3))
    val fragOffset by field(P4.bit(13))
    val ttl by field(P4.bit(8))
    val protocol by field(P4.bit(8))
    val hdrChecksum by field(P4.bit(16))
    val srcAddr by field(IPv4Address)
    val dstAddr by field(IPv4Address)
  }
  header(::Ipv4_h)

  class Parsed_packet(base: P4Expr) : P4.StructRef(base) {
    val ethernet by field(::Ethernet_h)
    val ip by field(::Ipv4_h)
  }
  struct(::Parsed_packet)

  errors("IPv4OptionsNotSupported", "IPv4IncorrectVersion", "IPv4ChecksumError")

  @Suppress("UnusedPrivateProperty")
  val TopParser by parser {
    val b by param(core.packet_in)
    val p by param(::Parsed_packet, P4.OUT)
    val ck by externInstance(vss_arch.Ck16)

    val parse_ipv4 by state {
      call(b, "extract", p.ip)
      verify(p.ip.version eq P4.lit(4, 4), P4.error_("IPv4IncorrectVersion"))
      verify(p.ip.ihl eq P4.lit(4, 5), P4.error_("IPv4OptionsNotSupported"))
      call(ck, "clear")
      call(ck, "update", p.ip)
      verify(ck.call("get") eq P4.lit(16, 0), P4.error_("IPv4ChecksumError"))
      transition(P4.accept)
    }

    val start by state {
      call(b, "extract", p.ethernet)
      select(p.ethernet.etherType) { P4.lit(0x0800) to parse_ipv4 }
    }
  }

  @Suppress("UnusedPrivateProperty")
  val TopPipe by control {
    val headers by param(::Parsed_packet, P4.INOUT)
    val parseError by param(P4.errorType, P4.IN)
    val inCtrl by param(vss_arch::InControl, P4.IN)
    val outCtrl by param(vss_arch::OutControl, P4.OUT)

    val Drop_action by action { assign(outCtrl.outputPort, vss_arch.DROP_PORT.ref) }

    val nextHop by varDecl(IPv4Address)

    val Set_nhop by action {
      val ipv4_dest by param(IPv4Address)
      val port by param(vss_arch.PortId)
      assign(nextHop, ipv4_dest)
      assign(headers.ip.ttl, headers.ip.ttl - P4.lit(1))
      assign(outCtrl.outputPort, port)
    }

    val ipv4_match by table {
      key(headers.ip.dstAddr, P4.LPM)
      actions(Drop_action, Set_nhop)
      size(1024)
      defaultAction(Drop_action)
    }

    val Send_to_cpu by action { assign(outCtrl.outputPort, vss_arch.CPU_OUT_PORT.ref) }

    val check_ttl by table {
      key(headers.ip.ttl, P4.EXACT)
      actions(Send_to_cpu)
      actionByName("NoAction")
      defaultAction("NoAction", const_ = true)
    }

    val Set_dmac by action {
      val dmac by param(EthernetAddress)
      assign(headers.ethernet.dstAddr, dmac)
    }

    val dmac by table {
      key(nextHop, P4.EXACT)
      actions(Drop_action, Set_dmac)
      size(1024)
      defaultAction(Drop_action)
    }

    val Set_smac by action {
      val smac by param(EthernetAddress)
      assign(headers.ethernet.srcAddr, smac)
    }

    val smac by table {
      key(outCtrl.outputPort, P4.EXACT)
      actions(Drop_action, Set_smac)
      size(16)
      defaultAction(Drop_action)
    }

    apply {
      if_(parseError ne P4.error_("NoError")) {
        call("Drop_action")
        return_()
      }

      ipv4_match.apply_()
      if_(outCtrl.outputPort eq vss_arch.DROP_PORT.ref) { return_() }

      check_ttl.apply_()
      if_(outCtrl.outputPort eq vss_arch.CPU_OUT_PORT.ref) { return_() }

      dmac.apply_()
      if_(outCtrl.outputPort eq vss_arch.DROP_PORT.ref) { return_() }

      smac.apply_()
    }
  }

  @Suppress("UnusedPrivateProperty")
  val TopDeparser by control {
    val p by param(::Parsed_packet, P4.INOUT)
    val b by param(core.packet_out)
    val ck by externInstance(vss_arch.Ck16)

    apply {
      call(b, "emit", p.ethernet)
      if_(p.ip.call("isValid")) {
        call(ck, "clear")
        assign(p.ip.hdrChecksum, P4.lit(16, 0))
        call(ck, "update", p.ip)
        assign(p.ip.hdrChecksum, ck.call("get"))
      }
      call(b, "emit", p.ip)
    }
  }

  packageInstance("VSS", "main", "TopParser", "TopPipe", "TopDeparser")
}
