package p4kt.examples

import p4kt.*

fun main() {
  val program = p4Program {
    function("id", bit(8)) {
      val x by param(bit(8), IN)
      return_(x)
    }
  }
  println(program.toP4())
}
