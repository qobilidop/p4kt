package p4kt.examples

import p4kt.*

fun main() {
  val program = p4Program {
    val max by
      function(bit(8)) {
        val a by param(bit(8), IN)
        val b by param(bit(8), IN)
        if_(a eq b) { return_(a) }.else_ { return_(b) }
      }
  }
  println(program.toP4())
}
