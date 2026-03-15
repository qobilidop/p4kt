package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ErrorTest {
  @Test
  fun errorDeclaration() {
    val err =
      P4Error(listOf("IPv4OptionsNotSupported", "IPv4IncorrectVersion", "IPv4ChecksumError"))
    assertEquals(
      """
                error {
                    IPv4OptionsNotSupported,
                    IPv4IncorrectVersion,
                    IPv4ChecksumError
                }
            """
        .trimIndent(),
      err.toP4(),
    )
  }

  @Test
  fun errorInProgram() {
    val program = p4Program { errors("IPv4OptionsNotSupported", "IPv4IncorrectVersion") }
    assertEquals(
      """
                error {
                    IPv4OptionsNotSupported,
                    IPv4IncorrectVersion
                }
            """
        .trimIndent(),
      program.toP4(),
    )
  }
}
