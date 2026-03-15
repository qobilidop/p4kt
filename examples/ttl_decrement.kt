package p4kt.examples

import p4kt.*

fun main() {
  val program = p4Program {
    class Ipv4_h(base: P4Expr) : HeaderRef(base) {
      val ttl by field(bit(8))
    }
    header<Ipv4_h>()

    val headers by
      function(void_) {
        val ip by param<Ipv4_h>(INOUT)
        assign(ip.ttl, ip.ttl - lit(1))
      }
  }
  println(program.toP4())
}
