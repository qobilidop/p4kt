@file:Suppress("MatchingDeclarationName", "ClassNaming")

package p4kt.p4include

import p4kt.P4

// Corresponds to: https://github.com/p4lang/p4c/blob/main/p4include/core.p4

object core : P4.Library() {
  init {
    errors(
      "NoError",
      "PacketTooShort",
      "NoMatch",
      "StackOutOfBounds",
      "HeaderTooShort",
      "ParserTimeout",
      "ParserInvalidArgument",
    )
  }

  val packet_in =
    extern("packet_in") {
      // TODO: extract<T>(out T hdr) - needs type parameters
      // TODO: lookahead<T>() - needs type parameters
      // TODO: advance(in bit<32> bits)
      // TODO: length() - returns bit<32>
    }

  val packet_out =
    extern("packet_out") {
      // TODO: emit<T>(in T hdr) - needs type parameters
    }

  // TODO: extern void verify(in bool check, in error toSignal) - built-in function
  // TODO: action NoAction() {} - built-in action
  // TODO: match_kind { exact, ternary, lpm } - needs match_kind IR support
}
