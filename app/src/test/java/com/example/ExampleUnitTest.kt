package com.example

import com.example.data.ocr.CardOcrHelper
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testRealWorldCardOcrParsing() {
        val ocrSample = """
            Anyiling Ewnyling in laus
            Laser marking & cuting on
            All Kinds of metals & Non metals
            Ketan Shah
            98203 31 715
            Adii
            Nilesh Kamdar
            93222 559098
            Aditi Laser Tech
            142-D, Kandivali Co-op. Ind. Estate,
            Mumba
            Web. : www.aditilasergroup.in
            022 2868871
            amcmark@gmail.com
        """.trimIndent()

        val lines = ocrSample.lines()
        val parsed = CardOcrHelper.parseLinesToCard(lines, ocrSample)

        // Email must be extracted correctly
        assertEquals("amcmark@gmail.com", parsed.email)

        // Website must be the actual domain, NOT gmail.com
        assertEquals("www.aditilasergroup.in", parsed.website)
        assertFalse("Website should never be email domain", parsed.website.contains("gmail"))

        // Company must NOT be the website line
        assertFalse("Company should not contain Web prefix", parsed.company.startsWith("Web."))
        assertFalse("Company should not be a URL", parsed.company.startsWith("www."))

        // Primary name should be extracted (e.g. Ketan Shah)
        assertTrue("Name should be extracted", parsed.name.isNotBlank())
        assertTrue(
            "Name should be one of the person names",
            parsed.name.contains("Ketan") || parsed.name.contains("Nilesh") || parsed.name.contains("Shah")
        )

        // Secondary name should be extracted (e.g. Nilesh Kamdar)
        assertTrue("Secondary name should be extracted", parsed.secondaryName.isNotBlank())
        assertTrue(
            "Secondary name should be Nilesh Kamdar",
            parsed.secondaryName.contains("Nilesh") || parsed.secondaryName.contains("Kamdar")
        )

        // Primary Mobile phone
        assertTrue("Primary phone should be extracted", parsed.phone.isNotBlank())

        // Secondary Mobile phone
        assertTrue("Secondary mobile phone should be extracted", parsed.secondaryPhone.isNotBlank())

        // Telephone / Landline
        assertTrue("Telephone line should be extracted", parsed.telephone.isNotBlank())
        assertTrue("Telephone should contain 022", parsed.telephone.contains("022") || parsed.telephone.contains("2868871"))
    }
}
