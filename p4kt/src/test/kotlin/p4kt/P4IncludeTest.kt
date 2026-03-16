package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4IncludeTest {
  @Test
  fun systemInclude() {
    val lib =
      object : P4.Library() {
        init {
          include(P4.systemInclude("core.p4"))
        }

        val myType by typedef(P4.bit(8))
      }
    assertEquals(
      """
          #include <core.p4>

          typedef bit<8> myType;
      """
        .trimIndent(),
      lib.toP4(),
    )
  }

  @Test
  fun localInclude() {
    val lib =
      object : P4.Library() {
        init {
          include(P4.localInclude("myheader.p4"))
        }

        val myType by typedef(P4.bit(8))
      }
    assertEquals(
      """
          #include "myheader.p4"

          typedef bit<8> myType;
      """
        .trimIndent(),
      lib.toP4(),
    )
  }

  @Test
  fun includeFromLibraryWithPath() {
    val dep =
      object : P4.Library(P4.systemInclude("dep.p4")) {
        val DepType by typedef(P4.bit(16))
      }
    val lib =
      object : P4.Library() {
        init {
          include(dep)
        }

        val myType by typedef(P4.bit(8))
      }
    assertEquals(
      """
          #include <dep.p4>

          typedef bit<8> myType;
      """
        .trimIndent(),
      lib.toP4(),
    )
  }
}
