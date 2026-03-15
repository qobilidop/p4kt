@file:JvmName("TtlDecrementKt")

package p4kt.examples

import p4kt.*

fun main() {
  val program = p4Program {
    val Ipv4_h by header { field("ttl", bit(8)) }
    val headers by
      function(void_) {
        val ip by param(typeName("Ipv4_h"), INOUT)
        assign(ip.dot("ttl"), ip.dot("ttl") - lit(1))
      }
  }
  println(program.toP4())
}
