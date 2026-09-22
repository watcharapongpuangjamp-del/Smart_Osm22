package com.example

import com.example.utils.NationalIdBarcodeParser
import org.junit.Assert.*
import org.junit.Test

class NationalIdBarcodeParserTest {

    @Test
    fun `test pure 13-digit barcode`() {
        val raw = "1509900123456"
        val result = NationalIdBarcodeParser.parse(raw)
        assertEquals("1509900123456", result.nationalId)
        assertEquals("1-5099-00123-45-6", result.formattedNationalId)
    }

    @Test
    fun `test delimited QR code with pipe`() {
        val raw = "1509900123456|นายสมชาย ใจดี|123/4|3|สารภี|สารภี|เชียงใหม่"
        val result = NationalIdBarcodeParser.parse(raw)
        assertEquals("1509900123456", result.nationalId)
        assertEquals("นายสมชาย ใจดี", result.fullName)
        assertEquals("123/4", result.houseNo)
        assertEquals("3", result.villageNo)
        assertEquals("สารภี", result.subdistrict)
        assertEquals("สารภี", result.district)
        assertEquals("เชียงใหม่", result.province)
    }

    @Test
    fun `test delimited QR code with full address string`() {
        val raw = "1509900123456#นายสมศักดิ์ รักไทย#บ้านเลขที่ 45/2 หมู่ 4 ต.ท่าศาลา อ.เมือง จ.เชียงใหม่"
        val result = NationalIdBarcodeParser.parse(raw)
        assertEquals("1509900123456", result.nationalId)
        assertEquals("นายสมศักดิ์ รักไทย", result.fullName)
        assertEquals("45/2", result.houseNo)
        assertEquals("4", result.villageNo)
        assertEquals("ท่าศาลา", result.subdistrict)
        assertEquals("เมือง", result.district)
        assertEquals("เชียงใหม่", result.province)
    }

    @Test
    fun `test JSON QR format`() {
        val json = """{"cid":"1509900123456","name":"นางสมศรี มีสุข","houseNo":"88/1","moo":"5","subdistrict":"หนองหอย","district":"เมือง","province":"เชียงใหม่"}"""
        val result = NationalIdBarcodeParser.parse(json)
        assertEquals("1509900123456", result.nationalId)
        assertEquals("นางสมศรี มีสุข", result.fullName)
        assertEquals("88/1", result.houseNo)
        assertEquals("5", result.villageNo)
        assertEquals("หนองหอย", result.subdistrict)
        assertEquals("เมือง", result.district)
        assertEquals("เชียงใหม่", result.province)
    }

    @Test
    fun `test formatted national id string`() {
        val formatted = NationalIdBarcodeParser.formatNationalId("1509900123456")
        assertEquals("1-5099-00123-45-6", formatted)
    }
}
