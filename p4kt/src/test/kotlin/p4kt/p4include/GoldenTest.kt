package p4kt.p4include

import kotlin.test.Test
import kotlin.test.assertEquals

class GoldenTest {
  @Test
  fun coreMatchesGoldenFile() {
    val expected = java.io.File("p4kt/src/test/resources/p4kt/p4include/core.p4").readText()
    assertEquals(expected, core.toP4())
  }

  @Test
  fun v1modelMatchesGoldenFile() {
    val expected = java.io.File("p4kt/src/test/resources/p4kt/p4include/v1model.p4").readText()
    assertEquals(expected, v1model.toP4())
  }
}
