@file:Suppress("MagicNumber", "ClassNaming", "UnusedPrivateProperty")

package p4kt.examples

import p4kt.P4
import p4kt.P4Expr
import p4kt.p4include.core
import p4kt.p4include.v1model

// Corresponds to the supported subset of:
// https://github.com/p4lang/tutorials/blob/master/exercises/basic/solution/basic.p4

val v1model_basic =
  P4.program {
    val TYPE_IPV4 by const_(P4.bit(16), P4.lit(0x800))

    val egressSpec_t by typedef(P4.bit(9))
    val macAddr_t by typedef(P4.bit(48))
    val ip4Addr_t by typedef(P4.bit(32))

    class ethernet_t(base: P4Expr) : P4.HeaderRef(base) {
      val dstAddr by field(macAddr_t)
      val srcAddr by field(macAddr_t)
      val etherType by field(P4.bit(16))
    }
    header(::ethernet_t)

    class ipv4_t(base: P4Expr) : P4.HeaderRef(base) {
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
      val srcAddr by field(ip4Addr_t)
      val dstAddr by field(ip4Addr_t)
    }
    header(::ipv4_t)

    class metadata(base: P4Expr) : P4.StructRef(base)
    struct(::metadata)

    class headers(base: P4Expr) : P4.StructRef(base) {
      val ethernet by field(::ethernet_t)
      val ipv4 by field(::ipv4_t)
    }
    struct(::headers)

    val MyParser by parser {
      val packet by param(core.packet_in)
      val hdr by param(::headers, P4.OUT)
      val meta by param(::metadata, P4.INOUT)
      val standard_metadata by param(v1model::standard_metadata_t, P4.INOUT)

      val parse_ipv4 by state {
        call(packet, "extract", hdr.ipv4)
        transition(P4.accept)
      }

      val parse_ethernet by state {
        call(packet, "extract", hdr.ethernet)
        select(hdr.ethernet.etherType) {
          TYPE_IPV4 to parse_ipv4
          // TODO: default: accept (default select case not yet supported)
        }
      }

      val start by state { transition(parse_ethernet) }
    }

    val MyVerifyChecksum by control {
      val hdr by param(::headers, P4.INOUT)
      val meta by param(::metadata, P4.INOUT)
      apply {}
    }

    val MyIngress by control {
      val hdr by param(::headers, P4.INOUT)
      val meta by param(::metadata, P4.INOUT)
      val standard_metadata by param(v1model::standard_metadata_t, P4.INOUT)

      val drop by action { call("mark_to_drop", standard_metadata.expr) }

      val ipv4_forward by action {
        val dstAddr by param(macAddr_t)
        val port by param(egressSpec_t)
        assign(standard_metadata.egress_spec, port)
        assign(hdr.ethernet.srcAddr, hdr.ethernet.dstAddr)
        assign(hdr.ethernet.dstAddr, dstAddr)
        assign(hdr.ipv4.ttl, hdr.ipv4.ttl - P4.lit(1))
      }

      val ipv4_lpm by table {
        key(hdr.ipv4.dstAddr, core.match_kind.lpm)
        actions(ipv4_forward, drop)
        actionByName("NoAction")
        size(1024)
        defaultAction(drop)
      }

      apply { if_(hdr.ipv4.call("isValid")) { ipv4_lpm.apply_() } }
    }

    val MyEgress by control {
      val hdr by param(::headers, P4.INOUT)
      val meta by param(::metadata, P4.INOUT)
      val standard_metadata by param(v1model::standard_metadata_t, P4.INOUT)
      apply {}
    }

    // TODO: update_checksum needs list expressions and HashAlgorithm enum
    val MyComputeChecksum by control {
      val hdr by param(::headers, P4.INOUT)
      val meta by param(::metadata, P4.INOUT)
      apply {}
    }

    val MyDeparser by control {
      val packet by param(core.packet_out)
      val hdr by param(::headers, P4.IN)
      apply {
        call(packet, "emit", hdr.ethernet)
        call(packet, "emit", hdr.ipv4)
      }
    }

    packageInstance(
      "V1Switch",
      "main",
      "MyParser",
      "MyVerifyChecksum",
      "MyIngress",
      "MyEgress",
      "MyComputeChecksum",
      "MyDeparser",
    )
  }
