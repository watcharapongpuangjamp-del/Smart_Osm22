package com.example.utils

data class IdCardScanResult(
    val nationalId: String? = null,
    val fullName: String? = null,
    val houseNo: String? = null,
    val villageNo: String? = null,
    val subdistrict: String? = null,
    val district: String? = null,
    val province: String? = null,
    val rawText: String = "",
    val scanType: String = "BARCODE"
) {
    val formattedNationalId: String?
        get() = nationalId?.let { NationalIdBarcodeParser.formatNationalId(it) }

    val hasAddressInfo: Boolean
        get() = !houseNo.isNullOrBlank() || !villageNo.isNullOrBlank() || !subdistrict.isNullOrBlank()

    val displaySummary: String
        get() {
            val parts = mutableListOf<String>()
            fullName?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            formattedNationalId?.let { parts.add("เลขประจำตัว: $it") }
            houseNo?.takeIf { it.isNotBlank() }?.let { parts.add("บ้านเลขที่ $it") }
            villageNo?.takeIf { it.isNotBlank() }?.let { parts.add("หมู่ $it") }
            subdistrict?.takeIf { it.isNotBlank() }?.let { parts.add("ต.$it") }
            district?.takeIf { it.isNotBlank() }?.let { parts.add("อ.$it") }
            province?.takeIf { it.isNotBlank() }?.let { parts.add("จ.$it") }
            return if (parts.isEmpty()) "ไม่พบข้อมูลที่ตรงกับรูปแบบบัตรประชาชน" else parts.joinToString(" • ")
        }
}

object NationalIdBarcodeParser {

    private val THAI_PREFIXES = listOf(
        "นาย", "นางสาว", "นาง", "น.ส.", "เด็กชาย", "เด็กหญิง", "ด.ช.", "ด.ญ.",
        "MR.", "MRS.", "MS.", "MISS"
    )

    fun parse(rawInput: String): IdCardScanResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return IdCardScanResult(rawText = rawInput)
        }

        // 1. Check if raw input is a pure 13-digit National ID (Standard Code 128 / 39 on back of card)
        val pureDigits = trimmed.replace(Regex("[^0-9]"), "")
        if (pureDigits.length == 13 && (trimmed.length == 13 || trimmed.matches(Regex("\\d-\\d{4}-\\d{5}-\\d{2}-\\d")))) {
            return IdCardScanResult(
                nationalId = pureDigits,
                rawText = rawInput,
                scanType = "1D_BARCODE_NATIONAL_ID"
            )
        }

        // 2. Try JSON parsing
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val fromJson = parseJson(trimmed)
            if (fromJson != null) return fromJson
        }

        // 3. Try Delimited parsing (Pipe |, Hash #, Comma ,, Semicolon ;, Tab \t, Newline \n)
        val delimiter = findDelimiter(trimmed)
        if (delimiter != null) {
            val fromDelimited = parseDelimited(trimmed, delimiter)
            if (fromDelimited != null) return fromDelimited
        }

        // 4. Free text / Regex extraction from structured text
        return parseFreeText(trimmed)
    }

    private fun parseJson(text: String): IdCardScanResult? {
        fun extractKey(key: String): String? {
            val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
            return pattern.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
        }

        val rawId = extractKey("nationalId") ?: extractKey("cid") ?: extractKey("id") ?: extractKey("idCard") ?: ""
        val cleanId = rawId.replace(Regex("[^0-9]"), "").takeIf { it.length == 13 }

        val fullName = extractKey("name") ?: extractKey("fullName") ?: extractKey("headName")
        val houseNo = extractKey("houseNo") ?: extractKey("house_no") ?: extractKey("address")
        val villageNo = extractKey("villageNo") ?: extractKey("village_no") ?: extractKey("moo")
        val subdistrict = extractKey("subdistrict") ?: extractKey("tambon")
        val district = extractKey("district") ?: extractKey("amphoe")
        val province = extractKey("province") ?: extractKey("changwat")

        if (cleanId == null && fullName == null && houseNo == null) return null

        return IdCardScanResult(
            nationalId = cleanId,
            fullName = fullName,
            houseNo = houseNo,
            villageNo = villageNo,
            subdistrict = subdistrict,
            district = district,
            province = province,
            rawText = text,
            scanType = "JSON_QR"
        )
    }

    private fun findDelimiter(text: String): Char? {
        val candidates = listOf('|', '#', ';', '\t', '\n')
        for (c in candidates) {
            if (text.count { it == c } >= 2) return c
        }
        if (text.count { it == ',' } >= 3) return ','
        return null
    }

    private fun parseDelimited(text: String, delimiter: Char): IdCardScanResult? {
        val tokens = text.split(delimiter).map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null

        var nationalId: String? = null
        var fullName: String? = null
        var houseNo: String? = null
        var villageNo: String? = null
        var subdistrict: String? = null
        var district: String? = null
        var province: String? = null

        for (token in tokens) {
            val digits = token.replace(Regex("[^0-9]"), "")
            // Check for 13-digit ID
            if (nationalId == null && digits.length == 13) {
                nationalId = digits
                continue
            }

            // Check if token contains full Thai address
            if (token.contains("ต.") || token.contains("ตำบล") || token.contains("หมู่") || token.contains("ม.")) {
                val parsedAddress = parseAddressString(token)
                if (houseNo == null && parsedAddress.houseNo != null) houseNo = parsedAddress.houseNo
                if (villageNo == null && parsedAddress.villageNo != null) villageNo = parsedAddress.villageNo
                if (subdistrict == null && parsedAddress.subdistrict != null) subdistrict = parsedAddress.subdistrict
                if (district == null && parsedAddress.district != null) district = parsedAddress.district
                if (province == null && parsedAddress.province != null) province = parsedAddress.province
                continue
            }

            // Check for Name
            if (fullName == null && isLikelyName(token)) {
                fullName = cleanName(token)
                continue
            }

            // Check for house number
            if (houseNo == null && token.matches(Regex("^[0-9]+(/[0-9]+)?$"))) {
                houseNo = token
                continue
            }

            // Check for village number
            if (villageNo == null && token.matches(Regex("^(หมู่ที่|หมู่|ม\\.)?\\s*[0-9]+$"))) {
                villageNo = token.replace(Regex("[^0-9]"), "")
                continue
            }
        }

        // If tokens were strictly positional (ID, Name, House, Moo, Subdistrict, District, Province)
        if (tokens.size >= 3) {
            val firstDigits = tokens[0].replace(Regex("[^0-9]"), "")
            if (firstDigits.length == 13) {
                nationalId = firstDigits
                if (fullName == null && tokens.size > 1 && isLikelyName(tokens[1])) {
                    fullName = cleanName(tokens[1])
                }
                if (houseNo == null && tokens.size > 2) {
                    houseNo = tokens[2].replace(Regex("^(บ้านเลขที่|เลขที่)\\s*"), "").trim()
                }
                if (villageNo == null && tokens.size > 3) {
                    villageNo = tokens[3].replace(Regex("[^0-9]"), "").trim().takeIf { it.isNotEmpty() }
                }
                if (subdistrict == null && tokens.size > 4) {
                    subdistrict = cleanLocationName(tokens[4], listOf("ต.", "ตำบล"))
                }
                if (district == null && tokens.size > 5) {
                    district = cleanLocationName(tokens[5], listOf("อ.", "อำเภอ"))
                }
                if (province == null && tokens.size > 6) {
                    province = cleanLocationName(tokens[6], listOf("จ.", "จังหวัด"))
                }
            }
        }

        return IdCardScanResult(
            nationalId = nationalId,
            fullName = fullName,
            houseNo = houseNo,
            villageNo = villageNo,
            subdistrict = subdistrict,
            district = district,
            province = province,
            rawText = text,
            scanType = "DELIMITED_QR"
        )
    }

    private fun parseFreeText(text: String): IdCardScanResult {
        // National ID search
        val idMatch = Regex("\\b(\\d{13}|\\d-\\d{4}-\\d{5}-\\d{2}-\\d)\\b").find(text)
        val nationalId = idMatch?.value?.replace(Regex("[^0-9]"), "")

        val addressParsed = parseAddressString(text)

        // Name search
        var fullName: String? = null
        for (prefix in THAI_PREFIXES) {
            val prefixRegex = Regex("(?:$prefix)\\s*([ก-๙a-zA-Z]+(?:\\s+[ก-๙a-zA-Z]+)?)")
            val match = prefixRegex.find(text)
            if (match != null) {
                fullName = "$prefix ${match.groupValues[1]}".trim()
                break
            }
        }

        if (fullName == null) {
            val nameRegex = Regex("(?:ชื่อ(?:-สกุล)?|ชื่อผู้ถือบัตร)\\s*[:=]?\\s*([ก-๙a-zA-Z]+(?:\\s+[ก-๙a-zA-Z]+)?)")
            fullName = nameRegex.find(text)?.groupValues?.get(1)?.trim()
        }

        return IdCardScanResult(
            nationalId = nationalId,
            fullName = fullName,
            houseNo = addressParsed.houseNo,
            villageNo = addressParsed.villageNo,
            subdistrict = addressParsed.subdistrict,
            district = addressParsed.district,
            province = addressParsed.province,
            rawText = text,
            scanType = "TEXT_BARCODE"
        )
    }

    data class ParsedAddress(
        val houseNo: String? = null,
        val villageNo: String? = null,
        val subdistrict: String? = null,
        val district: String? = null,
        val province: String? = null
    )

    fun parseAddressString(text: String): ParsedAddress {
        // House No
        val houseMatch = Regex("(?:บ้านเลขที่|เลขที่|ที่อยู่)\\s*([0-9]+(?:/[0-9]+)?)").find(text)
            ?: Regex("\\b([0-9]+/[0-9]+)\\b").find(text)
        val houseNo = houseMatch?.groupValues?.get(1)

        // Village No (หมู่ที่ / ม.)
        val villageMatch = Regex("(?:หมู่ที่|หมู่|ม\\.)\\s*([0-9]+)").find(text)
        val villageNo = villageMatch?.groupValues?.get(1)

        // Subdistrict (ต. / ตำบล)
        val subdistrictMatch = Regex("(?:ตำบล|ต\\.)\\s*([ก-๙]+)").find(text)
        val subdistrict = subdistrictMatch?.groupValues?.get(1)

        // District (อ. / อำเภอ)
        val districtMatch = Regex("(?:อำเภอ|อ\\.)\\s*([ก-๙]+)").find(text)
        val district = districtMatch?.groupValues?.get(1)

        // Province (จ. / จังหวัด)
        val provinceMatch = Regex("(?:จังหวัด|จ\\.)\\s*([ก-๙]+)").find(text)
        val province = provinceMatch?.groupValues?.get(1)

        return ParsedAddress(
            houseNo = houseNo,
            villageNo = villageNo,
            subdistrict = subdistrict,
            district = district,
            province = province
        )
    }

    private fun isLikelyName(token: String): Boolean {
        if (THAI_PREFIXES.any { token.startsWith(it) }) return true
        // Contains Thai letters and space, no numbers
        if (token.any { it in '\u0E01'..'\u0E5B' } && !token.any { it.isDigit() }) {
            return token.length in 4..60
        }
        return false
    }

    private fun cleanName(raw: String): String {
        return raw.replace(Regex("^(ชื่อ-สกุล|ชื่อ|Name)\\s*[:=]?\\s*"), "").trim()
    }

    private fun cleanLocationName(raw: String, prefixes: List<String>): String {
        var result = raw.trim()
        for (p in prefixes) {
            if (result.startsWith(p)) {
                result = result.substring(p.length).trim()
                break
            }
        }
        return result
    }

    fun formatNationalId(id: String): String {
        val clean = id.replace(Regex("[^0-9]"), "")
        if (clean.length != 13) return id
        return "${clean.substring(0, 1)}-${clean.substring(1, 5)}-${clean.substring(5, 10)}-${clean.substring(10, 12)}-${clean.substring(12, 13)}"
    }
}
