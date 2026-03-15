package p4kt.examples

import p4kt.*

fun main() {
  val id =
    p4Function("id", bit(8)) {
      val x by param(bit(8), IN)
      return_(x)
    }
  println(id.toP4())
}
