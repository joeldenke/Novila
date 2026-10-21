package se.novila.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class NovilaSkyTest {
    @Test fun `the sun rises at five and the moon comes out at ten in the evening`() {
        assertEquals(NovilaSky.Night, NovilaSky.at(4))
        assertEquals(NovilaSky.Morning, NovilaSky.at(5))
        assertEquals(NovilaSky.Morning, NovilaSky.at(9))
        assertEquals(NovilaSky.Day, NovilaSky.at(10))
        assertEquals(NovilaSky.Day, NovilaSky.at(16))
        assertEquals(NovilaSky.Evening, NovilaSky.at(17))
        assertEquals(NovilaSky.Evening, NovilaSky.at(21))
        assertEquals(NovilaSky.Night, NovilaSky.at(22))
        assertEquals(NovilaSky.Night, NovilaSky.at(0))
    }
}
