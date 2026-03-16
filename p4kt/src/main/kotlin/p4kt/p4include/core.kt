@file:Suppress("MatchingDeclarationName", "ClassNaming", "UnusedPrivateProperty", "VariableNaming")

package p4kt.p4include

import p4kt.P4

// Corresponds to: https://github.com/p4lang/p4c/blob/main/p4include/core.p4

object core : P4.Library(P4.systemInclude("core.p4")) {
  object error : P4.ErrorDecl() {
    val NoError by member()
    val PacketTooShort by member()
    val NoMatch by member()
    val StackOutOfBounds by member()
    val HeaderTooShort by member()
    val ParserTimeout by member()
    val ParserInvalidArgument by member()
  }

  init {
    register(error)
  }

  val packet_in by extern {
    val extract by
      method(P4.void_) {
        val T by typeParam()
        val hdr by param(T, P4.OUT)
      }
    val extractVarSize by
      overload(extract, P4.void_) {
        val T by typeParam()
        val variableSizeHeader by param(T, P4.OUT)
        val variableFieldSizeInBits by param(P4.bit(32), P4.IN)
      }
    val lookahead by method {
      val T by typeParam()
      returnType(T)
    }
    val advance by
      method(P4.void_) {
        val sizeInBits by param(P4.bit(32), P4.IN)
      }
    val length by method(P4.bit(32))
  }

  val packet_out by extern {
    val emit by
      method(P4.void_) {
        val T by typeParam()
        val hdr by param(T, P4.IN)
      }
  }

  val verify by
    externFunction(P4.void_) {
      val check by param(P4.bool_, P4.IN)
      val toSignal by param(P4.errorType, P4.IN)
    }

  val NoAction by action {}

  object match_kind : P4.MatchKindDecl() {
    val exact by member()
    val ternary by member()
    val lpm by member()
  }

  init {
    register(match_kind)
  }

  val static_assert by
    externFunction(P4.bool_) {
      val check by param(P4.bool_)
      val msg by param(P4.string_)
    }

  val static_assert_no_msg by
    externFunctionOverload(static_assert, P4.bool_) {
      val check by param(P4.bool_)
    }
}
