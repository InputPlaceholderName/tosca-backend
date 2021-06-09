import org.junit.Test
import org.junit.jupiter.api.Assertions.*

internal class UserTest {
    @Test
    fun sampleTest() {
        val u = User()
        assertEquals(12, u.doWork())
    }
}