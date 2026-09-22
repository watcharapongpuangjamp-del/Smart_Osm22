package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ThemeQuickToggleButton
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthKnowledgeScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("เกณฑ์ความดัน", "ดัชนีมวลกาย (BMI)", "ความรู้ตามช่วงวัย")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "คู่มือความรู้และเกณฑ์สุขภาพ อสม.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "กลับ", tint = Color.White)
                    }
                },
                actions = {
                    ThemeQuickToggleButton(iconTint = Color.White)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldPrimary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> BloodPressureGuideContent()
                    1 -> BmiGuideContent()
                    2 -> AgeGroupKnowledgeContent()
                }
            }
        }
    }
}

@Composable
fun BloodPressureGuideContent() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("เกณฑ์การอ่านค่าความดันโลหิต (Blood Pressure)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
        Text("การวัดความดันโลหิตประกอบด้วยตัวเลข 2 ค่า คือ ค่าบน (Systolic) และค่าล่าง (Diastolic) หน่วยเป็น มิลลิเมตรปรอท (mmHg)")

        BpCard("ระดับเหมาะสม (ปกติ)", "ตัวบน < 120 และ ตัวล่าง < 80", Color(0xFF2E7D32), "ความดันโลหิตอยู่ในเกณฑ์ดีเยี่ยม ควรักษาระดับพฤติกรรมสุขภาพ")
        BpCard("ระดับปกติ", "ตัวบน 120-129 และ/หรือ ตัวล่าง 80-84", Color(0xFF388E3C), "ปกติ แต่ควรควบคุมอาหารและออกกำลังกายสม่ำเสมอ")
        BpCard("ระดับปกติค่อนสูง", "ตัวบน 130-139 และ/or ตัวล่าง 85-89", Color(0xFFF57C00), "เริ่มสูงกว่าเกณฑ์ เสี่ยงต่อโรคความดันโลหิตสูง ควรตรวจวัดสม่ำเสมอ")
        BpCard("ระดับสูง (ระยะที่ 1)", "ตัวบน 140-159 และ/or ตัวล่าง 90-99", Color(0xFFD32F2F), "เข้าเกณฑ์ความดันโลหิตสูง ควรพบแพทย์เพื่อรับคำแนะนำและตรวจซ้ำ")
        BpCard("ระดับสูงมาก (ระยะที่ 2)", "ตัวบน ≥ 160 และ/or ตัวล่าง ≥ 100", Color(0xFFB71C1C), "อันตราย! ความดันสูงมาก ควรพบแพทย์ด่วนเพื่อรับการรักษา")
    }
}

@Composable
fun BpCard(title: String, range: String, color: Color, desc: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
                Badge(containerColor = color) {
                    Text(range, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BmiGuideContent() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("ดัชนีมวลกาย (BMI - Body Mass Index)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
        Text("สูตรคำนวณ: น้ำหนักตัว (กิโลกรัม) ÷ [ส่วนสูง (เมตร)]²")

        BmiCard("น้ำหนักน้อย / ผอม", "< 18.5 กก./ม.²", Color(0xFF0288D1), "เสี่ยงต่อภาวะขาดสารอาหาร โรคเรื้อรัง หรือภูมิคุ้มกันต่ำ")
        BmiCard("ปกติ (สมส่วน)", "18.5 - 22.9 กก./ม.²", Color(0xFF2E7D32), "อยู่ในเกณฑ์สุขภาพดี อัตราเสี่ยงต่อโรคเรื้อรังต่ำที่สุด")
        BmiCard("ท้วม / โรคอ้วนระดับ 1", "23.0 - 24.9 กก./ม.²", Color(0xFFF57C00), "เริ่มมีน้ำหนักเกิน เสี่ยงต่อโรคเบาหวานและความดันโลหิตสูง")
        BmiCard("อ้วน / โรคอ้วนระดับ 2", "25.0 - 29.9 กก./ม.²", Color(0xFFD32F2F), "เข้าเกณฑ์โรคอ้วน เสี่ยงสูงต่อโรคหัวใจ หลอดเลือด และข้อเข่าเสื่อม")
        BmiCard("อ้วนอันตราย / ระดับ 3", "≥ 30.0 กก./ม.²", Color(0xFFB71C1C), "โรคอ้วนอันตราย เสี่ยงต่อภาวะแทรกซ้อนรุนแรง ควรปรึกษาแพทย์")
    }
}

@Composable
fun BmiCard(title: String, range: String, color: Color, desc: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
                Badge(containerColor = color) {
                    Text(range, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AgeGroupKnowledgeContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ความรู้และคำแนะนำสุขภาพแยกตามกลุ่มวัย", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)

        AgeGroupCard(
            title = "👶 กลุ่มเด็กเล็ก (0 - 5 ปี)",
            focus = "พัฒนาการ สมส่วน และวัคซีน",
            details = listOf(
                "• ตรวจสมุดบันทึกสุขภาพและติดตามการรับวัคซีนตามวัย",
                "• ส่งเสริมการเลี้ยงลูกด้วยนมแม่อย่างเดียวอย่างน้อย 6 เดือน",
                "• ประเมินพัฒนาการด้านการเคลื่อนไหว ภาษา และการช่วยเหลือตัวเอง",
                "• ป้องกันอุบัติเหตุในเด็ก เช่น การพลัดตกหกคะเมน และการสำลักสิ่งแปลกปลอม"
            )
        )

        AgeGroupCard(
            title = "👦 กลุ่มวัยเรียนและวัยรุ่น (6 - 21 ปี)",
            focus = "โภชนาการ สายตา ทันตสุขภาพ และสุขภาพจิต",
            details = listOf(
                "• ส่งเสริมการรับประทานอาหารครบ 5 หมู่ หลีกเลี่ยงอาหารหวานจัด เค็มจัด มันจัด",
                "• ตรวจคัดกรองสายตา ทันตสุขภาพ และการแปรงฟันที่ถูกวิธี",
                "• ป้องกันภาวะซึมเศร้า การติดเกม และสารเสพติด",
                "• ส่งเสริมการออกกำลังกายสม่ำเสมออย่างน้อย 60 นาที/วัน"
            )
        )

        AgeGroupCard(
            title = "👩‍🦰 กลุ่มวัยทำงาน (22 - 59 ปี)",
            focus = "โรค NCDs (เบาหวาน ความดัน) และออฟฟิศซินโดรม",
            details = listOf(
                "• ตรวจคัดกรองความดันโลหิตและระดับน้ำตาลในเลือดประจำปี",
                "• ควบคุมน้ำหนักและดัชนีมวลกาย (BMI) ให้อยู่ในเกณฑ์ปกติ",
                "• หลีกเลี่ยงความเครียด สูบบุหรี่ และดื่มสุราเกินขนาด",
                "• ระวังโรคจากการทำงาน เช่น ออฟฟิศซินโดรม และอาการปวดหลังเรื้อรัง"
            )
        )

        AgeGroupCard(
            title = "👵 กลุ่มผู้สูงอายุ (60 ปีขึ้นไป)",
            focus = "การพลัดตกหกคะเมน ข้อเข่าเสื่อม และสมองเสื่อม",
            details = listOf(
                "• ประเมินความเสี่ยงต่อการพลัดตกหกคะเมนและปรับสภาพแวดล้อมในบ้าน",
                "• ตรวจคัดกรองภาวะสมองเสื่อม (AD8) และภาวะซึมเศร้าในผู้สูงอายุ",
                "• ตรวจสุขภาพช่องปาก ฟันเทียม และภาวะโภชนาการ",
                "• ส่งเสริมการทำกิจกรรมทางกายที่เหมาะสม เช่น การเดิน และรำไทเก็ก"
            )
        )
    }
}

@Composable
fun AgeGroupCard(title: String, focus: String, details: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = EmeraldPrimary, fontSize = 16.sp)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MintAccent.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "จุดเน้น: $focus",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            details.forEach { detail ->
                Text(text = detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
