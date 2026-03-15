@file:Suppress("MagicNumber", "MatchingDeclarationName", "ClassNaming")

package p4kt.p4include

import p4kt.P4
import p4kt.P4Expr

// Corresponds to: https://github.com/p4lang/p4c/blob/main/p4include/v1model.p4

object v1model : P4.Library() {
  // TODO: match_kind { range, optional, selector } - needs match_kind IR support

  val PortId_t = typedef("PortId_t", P4.bit(9))

  class standard_metadata_t(base: P4Expr) : P4.StructRef(base) {
    val ingress_port by field(PortId_t)
    val egress_spec by field(PortId_t)
    val egress_port by field(PortId_t)
    val instance_type by field(P4.bit(32))
    val packet_length by field(P4.bit(32))
    val enq_timestamp by field(P4.bit(32))
    val enq_qdepth by field(P4.bit(19))
    val deq_timedelta by field(P4.bit(32))
    val deq_qdepth by field(P4.bit(19))
    val ingress_global_timestamp by field(P4.bit(48))
    val egress_global_timestamp by field(P4.bit(48))
    val mcast_grp by field(P4.bit(16))
    val egress_rid by field(P4.bit(16))
    val checksum_error by field(P4.bit(1))
    val parser_error by field(P4.errorType)
    val priority by field(P4.bit(3))
  }

  init {
    struct(::standard_metadata_t)
  }

  // TODO: enum CounterType { packets, bytes, packets_and_bytes } - needs enum IR support
  // TODO: enum MeterType { packets, bytes }
  // TODO: enum HashAlgorithm { crc32, ... }
  // TODO: enum CloneType { I2E, E2E }

  // TODO: extern counter<I> - needs type parameters
  // TODO: extern direct_counter
  // TODO: extern meter<I>
  // TODO: extern direct_meter<T>
  // TODO: extern register<T, I>

  val action_profile = extern("action_profile") { constructor_() }

  val action_selector = extern("action_selector") { constructor_() }

  // TODO: extern Checksum16 - get<D>() needs type parameters

  // TODO: random<T>(), digest<T>(), hash<O,T,D,M>() - need type parameters
  // TODO: verify_checksum<T,O>(), update_checksum<T,O>() - need type parameters
  // TODO: clone(), resubmit<T>(), recirculate<T>() - need type parameters
  // TODO: mark_to_drop() - needs function declaration in Library
  // TODO: assert(), assume(), log_msg() - need function declaration in Library
  // TODO: truncate()

  // TODO: Parser<H, M>, VerifyChecksum<H, M>, Ingress<H, M>, Egress<H, M>,
  //       ComputeChecksum<H, M>, Deparser<H> - need abstract/type parameter support
  // TODO: V1Switch<H, M> package declaration - needs type parameters
}
