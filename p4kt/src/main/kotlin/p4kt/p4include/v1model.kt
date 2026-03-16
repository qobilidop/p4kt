@file:Suppress("MagicNumber", "MatchingDeclarationName", "ClassNaming", "UnusedPrivateProperty")

package p4kt.p4include

import p4kt.P4
import p4kt.P4Expr

// Corresponds to: https://github.com/p4lang/p4c/blob/main/p4include/v1model.p4

object v1model : P4.Library() {
  object match_kind : P4.MatchKindDecl() {
    val range by member()
    val optional by member()
    val selector by member()
  }

  init {
    register(match_kind)
  }

  val PortId_t by typedef(P4.bit(9))

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

  // TODO: extern counter<I> - needs extern-level type params
  // TODO: extern direct_counter - needs CounterType enum
  // TODO: extern meter<I> - needs extern-level type params
  // TODO: extern direct_meter<T> - needs extern-level type params
  // TODO: extern register<T, I> - needs extern-level type params

  val action_profile by extern { constructor_() }

  val action_selector by extern { constructor_() }

  val Checksum16 by extern {
    constructor_()
    val get by method {
      val D by typeParam()
      returnType(P4.bit(16))
      val data by param(D, P4.IN)
    }
  }

  // TODO: random<T>(), digest<T>(), hash<O,T,D,M>() - need extern function IR
  // TODO: verify_checksum<T,O>(), update_checksum<T,O>() - need extern function IR
  // TODO: clone(), resubmit<T>(), recirculate<T>() - need extern function IR
  // TODO: mark_to_drop(), truncate() - need extern function IR
  // TODO: assert(), assume(), log_msg() - need extern function IR

  // TODO: Parser<H, M>, VerifyChecksum<H, M>, Ingress<H, M>, Egress<H, M>,
  //       ComputeChecksum<H, M>, Deparser<H> - need abstract/type parameter support
  // TODO: V1Switch<H, M> package declaration - needs type parameters
}
