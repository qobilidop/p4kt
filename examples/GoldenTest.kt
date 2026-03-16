package p4kt.examples

import kotlin.test.Test
import kotlin.test.assertEquals

class GoldenTest {
  @Test
  fun v1modelBasicMatchesGoldenFile() {
    val expected = java.io.File("examples/v1model_basic.p4").readText()
    assertEquals(expected, v1model_basic.toP4())
  }

  @Test
  fun vssMatchesGoldenFile() {
    val expected = java.io.File("examples/vss.p4").readText()
    assertEquals(expected, vss.toP4())
  }

  @Test
  fun vssExampleMatchesGoldenFile() {
    val expected = java.io.File("examples/vss_example.p4").readText()
    assertEquals(expected, vss_example.toP4())
  }
}
