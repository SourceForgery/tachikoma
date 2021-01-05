package jakarta.mail.internet

import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockkStatic
import java.util.concurrent.atomic.AtomicInteger

fun mockUniqueValue(prefix: String) {
    val seq = AtomicInteger()
    mockkStatic(UniqueValue::class)
    every { UniqueValue.getUniqueBoundaryValue() } answers { prefix + "." + seq.getAndIncrement() }
}

fun cleanUniqueValueMock() {
    clearStaticMockk(UniqueValue::class)
}
