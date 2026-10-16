package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTest {
    @Test fun `a new profile is empty`() {
        assertTrue(Profile().isEmpty)
        assertFalse(Profile(name = "Anna").isEmpty)
    }

    @Test fun `the age is the one turned this year`() {
        assertEquals(36, Profile(birthYear = 1990).ageIn(2026))
        assertNull(Profile().ageIn(2026))
    }

    @Test fun `names are trimmed and kept short`() {
        assertEquals("Anna", Profile.cleanName("  Anna "))
        assertEquals(Profile.MAX_NAME_LENGTH, Profile.cleanName("a".repeat(100)).length)
    }

    @Test fun `numbers in range are valid, empty is empty and anything else is invalid`() {
        assertEquals(NumberInput.Valid(178), NumberInput.parse(" 178 ", Profile.HEIGHT_CM))
        assertEquals(NumberInput.Empty, NumberInput.parse("  ", Profile.HEIGHT_CM))
        assertEquals(NumberInput.Invalid, NumberInput.parse("17", Profile.HEIGHT_CM))
        assertEquals(NumberInput.Invalid, NumberInput.parse("1,78", Profile.HEIGHT_CM))
        assertEquals(NumberInput.Invalid, NumberInput.parse("abc", Profile.WEIGHT_KG))
    }

    @Test fun `birth years stop at this year and 120 years back`() {
        val years = Profile.birthYears(2026)
        assertEquals(NumberInput.Valid(2026), NumberInput.parse("2026", years))
        assertEquals(NumberInput.Valid(1906), NumberInput.parse("1906", years))
        assertEquals(NumberInput.Invalid, NumberInput.parse("2027", years))
        assertEquals(NumberInput.Invalid, NumberInput.parse("1905", years))
    }

    @Test fun `only a valid number is saved`() {
        assertEquals(70, NumberInput.Valid(70).valueOrNull)
        assertNull(NumberInput.Empty.valueOrNull)
        assertNull(NumberInput.Invalid.valueOrNull)
    }

    @Test fun `the form shows what is saved and saves what is typed`() {
        val saved = Profile(name = "Alex", birthYear = 1990, heightCm = 178, weightKg = 74)
        val form = ProfileForm.of(saved)
        assertEquals(ProfileForm(name = "Alex", birthYear = "1990", height = "178", weight = "74"), form)
        assertEquals(saved, form.toProfile(Profile(), currentYear = 2026))
        assertEquals(ProfileForm(), ProfileForm.of(Profile()))
    }

    @Test fun `a wrong number keeps the saved value and an emptied one clears it`() {
        val saved = Profile(name = "Alex", birthYear = 1990, heightCm = 178, weightKg = 74)
        val typing = ProfileForm(name = " Alex ", birthYear = "19", height = "", weight = "75")

        assertEquals(
            Profile(name = "Alex", birthYear = 1990, heightCm = null, weightKg = 75),
            typing.toProfile(saved, currentYear = 2026),
        )
    }
}
