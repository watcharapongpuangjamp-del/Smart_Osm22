package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed class GeminiUiState {
    object Idle : GeminiUiState()
    object Loading : GeminiUiState()
    data class Success(val response: String) : GeminiUiState()
    data class Error(val message: String) : GeminiUiState()
}

class GeminiViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<GeminiUiState>(GeminiUiState.Idle)
    val uiState: StateFlow<GeminiUiState> = _uiState.asStateFlow()

    private val apiKey = BuildConfig.GEMINI_API_KEY

    fun generateHealthAdvice(
        personName: String,
        age: Int,
        gender: String,
        weightKg: Double,
        heightCm: Double,
        systolic: Int,
        diastolic: Int,
        sugar: Double
    ) {
        if (apiKey.isEmpty()) {
            _uiState.value = GeminiUiState.Error("API Key is missing. Please set it in the Secrets panel.")
            return
        }

        viewModelScope.launch {
            _uiState.value = GeminiUiState.Loading
            
            val bmi = if (heightCm > 0) weightKg / ((heightCm / 100.0) * (heightCm / 100.0)) else 0.0
            val prompt = """
                คุณคือที่ปรึกษาด้านสุขภาพอัจฉริยะ (Smart Health Advisor) สำหรับ อสม. (อาสาสมัครสาธารณสุขประจำหมู่บ้าน)
                วิเคราะห์ข้อมูลสุขภาพของบุคคลดังนี้:
                - ชื่อ: $personName
                - อายุ: $age ปี
                - เพศ: $gender
                - น้ำหนัก: $weightKg กก., ส่วนสูง: $heightCm ซม. (BMI: ${"%.2f".format(bmi)})
                - ความดันโลหิต: $systolic/$diastolic mmHg
                - ระดับน้ำตาลในเลือด: $sugar mg/dL

                ช่วยสรุปสถานะสุขภาพเบื้องต้น และให้คำแนะนำที่ อสม. ควรแจ้งให้คนไข้ทราบ (เป็นภาษาไทยที่เป็นกันเองแต่ถูกต้อง)
                แบ่งเป็นส่วนๆ ดังนี้:
                1. การประเมินสถานะ (เช่น ปกติ, กลุ่มเสี่ยง, หรือต้องส่งต่อแพทย์)
                2. คำแนะนำด้านการรับประทานอาหาร
                3. คำแนะนำด้านการออกกำลังกาย
                4. ข้อควรระวังหรือสิ่งที่ต้องติดตามเป็นพิเศษ
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )

            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (responseText != null) {
                    _uiState.value = GeminiUiState.Success(responseText)
                } else {
                    _uiState.value = GeminiUiState.Error("ไม่ได้รับคำตอบจากระบบ AI")
                }
            } catch (e: Exception) {
                _uiState.value = GeminiUiState.Error("เกิดข้อผิดพลาด: ${e.message}")
            }
        }
    }

    fun clearState() {
        _uiState.value = GeminiUiState.Idle
    }
}
