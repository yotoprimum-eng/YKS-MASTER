package com.yksmaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

private val Navy = Color(0xFF101827)
private val Blue = Color(0xFF3767C8)
private val Teal = Color(0xFF18BFAE)
private val Orange = Color(0xFFFFB454)
private val Muted = Color(0xFF7D8BA3)
private val Surface = Color(0xFFF3F6FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("yks_master", MODE_PRIVATE)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Blue, secondary = Teal, background = Surface)) { YksApp(prefs) } }
    }
}

data class Topic(val id: String, val exam: String, val subject: String, val title: String, val done: Boolean = false)
data class MockExam(val id: String, val name: String, val date: String, val correct: Int, val wrong: Int, val blank: Int) {
    val net: Double get() = correct - wrong / 4.0
    val total: Int get() = correct + wrong + blank
}

@Composable
fun YksApp(prefs: android.content.SharedPreferences) {
    var name by remember { mutableStateOf(prefs.getString("name", "Öğrenci") ?: "Öğrenci") }
    var school by remember { mutableStateOf(prefs.getString("school", "12. sınıf") ?: "12. sınıf") }
    var target by remember { mutableStateOf(prefs.getString("target", "90") ?: "90") }
    var page by remember { mutableStateOf("Ana Sayfa") }
    var drawer by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var topics by remember { mutableStateOf(defaultTopics().map { it.copy(done = prefs.getBoolean("topic_${it.id}", false)) }) }
    fun loadExams(): List<MockExam> {
        val raw = prefs.getString("exams", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i -> arr.getJSONObject(i).let { o -> MockExam(o.getString("id"), o.getString("name"), o.getString("date"), o.getInt("correct"), o.getInt("wrong"), o.getInt("blank")) } }
        } catch (_: Exception) { emptyList() }
    }
    var exams by remember { mutableStateOf(loadExams()) }
    var showExamDialog by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val examDate = LocalDate.of(2027, 6, 19)
    val daysLeft = ChronoUnit.DAYS.between(today, examDate).coerceAtLeast(0)
    val finished = topics.count { it.done }

    ModalNavigationDrawer(
        drawerState = rememberDrawerState(if (drawer) DrawerValue.Open else DrawerValue.Closed),
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxWidth().background(Navy).padding(20.dp)) {
                    Text("YKS MASTER", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Kişisel sınav asistanın", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))
                    Text(name, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(school, color = Color.LightGray, fontSize = 12.sp)
                }
                listOf("Ana Sayfa", "YKS Sayacı", "Konu Takibi", "Haftalık Program", "Deneme Takibi", "Profil").forEach { item ->
                    NavigationDrawerItem(label = { Text(item) }, selected = page == item, onClick = { page = item; drawer = false; if (item == "Profil") showProfile = true }, icon = {
                        Icon(when (item) { "Ana Sayfa" -> Icons.Default.Home; "YKS Sayacı" -> Icons.Default.Timer; "Konu Takibi" -> Icons.Default.MenuBook; "Haftalık Program" -> Icons.Default.CalendarMonth; "Deneme Takibi" -> Icons.Default.BarChart; else -> Icons.Default.Person }, null)
                    }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                }
            }
        }
    ) {
        Scaffold(containerColor = Surface, topBar = {
            CenterAlignedTopAppBar(
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Text("YKS ", fontWeight = FontWeight.ExtraBold, color = Color.White); Text("MASTER", fontWeight = FontWeight.ExtraBold, color = Color(0xFF38DCC8)) } },
                navigationIcon = { IconButton(onClick = { drawer = true }) { Icon(Icons.Default.Menu, "Menü", tint = Color.White) } },
                actions = { IconButton(onClick = { showProfile = true }) { Icon(Icons.Default.AccountCircle, "Profil", tint = Color.White) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Navy)
            )
        }) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                when (page) {
                    "Ana Sayfa" -> HomePage(name, school, daysLeft, finished, topics.size, exams, target,
                        onNavigate = { page = it }, onAddExam = { showExamDialog = true })
                    "YKS Sayacı" -> CountdownPage(daysLeft, examDate.toString())
                    "Konu Takibi" -> TopicPage(topics) { id -> topics = topics.map { if (it.id == id) it.copy(done = !it.done) else it } }
                    "Haftalık Program" -> WeeklyPage()
                    "Deneme Takibi" -> ExamPage(exams, onAdd = { showExamDialog = true })
                    else -> HomePage(name, school, daysLeft, finished, topics.size, exams, target, { page = it }, { showExamDialog = true })
                }
            }
        }
    }

    if (showProfile) ProfileDialog(name, school, target, { name = it; prefs.edit().putString("name", it).apply() }, { school = it; prefs.edit().putString("school", it).apply() }, { target = it; prefs.edit().putString("target", it).apply() }, { showProfile = false })
    if (showExamDialog) ExamDialog(onDismiss = { showExamDialog = false }, onSave = { exams = listOf(it) + exams; val arr = JSONArray(); exams.forEach { e -> arr.put(JSONObject().put("id", e.id).put("name", e.name).put("date", e.date).put("correct", e.correct).put("wrong", e.wrong).put("blank", e.blank)) }; prefs.edit().putString("exams", arr.toString()).apply(); showExamDialog = false })
}

@Composable
fun HomePage(name: String, school: String, days: Long, done: Int, total: Int, exams: List<MockExam>, target: String, onNavigate: (String) -> Unit, onAddExam: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)) {
                Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF101B35), Color(0xFF173B62), Color(0xFF126B70))), RoundedCornerShape(24.dp)).padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("YKS MASTER · KİŞİSEL PANELİN", color = Color(0xFF72E5D8), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
                            Spacer(Modifier.height(9.dp))
                            Text("Merhaba, $name 👋", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Hedefine her gün bir adım daha yaklaş.", color = Color(0xFFD0DCEC), fontSize = 12.sp)
                        }
                        Box(Modifier.size(48.dp).background(Color(0x2638DCC8), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.School, null, tint = Color(0xFF55E1D0), modifier = Modifier.size(29.dp))
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatBox("YKS'ye kalan", "$days gün", Modifier.weight(1f))
                        StatBox("Bitirilen konu", "$done / $total", Modifier.weight(1f))
                    }
                }
            }
        }
        item { Text("Çalışma alanın", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Navy) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    QuickCard("Konu takibi", "${if (total > 0) done * 100 / total else 0}% tamamlandı", Icons.Default.MenuBook, Modifier.weight(1f)) { onNavigate("Konu Takibi") }
                    QuickCard("Haftalık plan", "7 günlük program", Icons.Default.CalendarMonth, Modifier.weight(1f)) { onNavigate("Haftalık Program") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    QuickCard("Denemeler", "${exams.size} kayıt", Icons.Default.BarChart, Modifier.weight(1f)) { onNavigate("Deneme Takibi") }
                    QuickCard("YKS sayacı", "Geri sayım", Icons.Default.Timer, Modifier.weight(1f)) { onNavigate("YKS Sayacı") }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Son denemeler", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
                Text("Tümünü gör", color = Blue, modifier = Modifier.clickable { onNavigate("Deneme Takibi") })
            }
        }
        if (exams.isEmpty()) item { EmptyCard("Henüz deneme eklemedin.") }
        items(exams.take(2)) { e -> ExamMiniCard(e) }
        item { Button(onClick = onAddExam, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Deneme ekle") } }
    }
}

@Composable fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color(0x332D5476), RoundedCornerShape(16.dp)).padding(13.dp)) { Text(label, color = Color(0xFFB9D2E4), fontSize = 11.sp, fontWeight = FontWeight.Medium); Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
}

@Composable fun QuickCard(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, click: () -> Unit) {
    Card(modifier.clickable { click() }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(19.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Box(Modifier.size(38.dp).background(Color(0xFFE8F8F6), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Teal, modifier = Modifier.size(21.dp)) }
            Spacer(Modifier.height(12.dp)); Text(title, fontWeight = FontWeight.Bold, color = Navy); Text(sub, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable fun CountdownPage(days: Long, date: String) {
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("2027 YKS", color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) { drawArc(Color(0xFFE1E7F0), -90f, 360f, false, style = Stroke(15.dp.toPx(), cap = StrokeCap.Round)); drawArc(Blue, -90f, 270f, false, style = Stroke(15.dp.toPx(), cap = StrokeCap.Round)) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(days.toString(), fontSize = 58.sp, fontWeight = FontWeight.Bold, color = Navy); Text("GÜN KALDI", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(24.dp)); Text("TYT · 19 Haziran 2027", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("AYT / YDT · 20 Haziran 2027", color = Muted)
        Text("Tarih: $date", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(20.dp)); Text("Sayaç cihazının güncel tarihine göre hesaplanır.", color = Muted, fontSize = 12.sp)
    }
}

fun defaultTopics() = listOf(
    Topic("1","TYT","Matematik","Temel Kavramlar"), Topic("2","TYT","Matematik","Sayı Basamakları"), Topic("3","TYT","Matematik","Bölme ve Bölünebilme"), Topic("4","TYT","Matematik","EBOB - EKOK"), Topic("5","TYT","Matematik","Problemler"),
    Topic("6","TYT","Kimya","Atom ve Periyodik Sistem"), Topic("7","TYT","Kimya","Kimyasal Türler Arası Etkileşimler"), Topic("8","TYT","Türkçe","Sözcükte Anlam"), Topic("9","TYT","Türkçe","Cümlede Anlam"), Topic("10","TYT","Türkçe","Paragraf"),
    Topic("11","AYT","Matematik","Fonksiyonlar"), Topic("12","AYT","Matematik","Polinomlar"), Topic("13","AYT","Matematik","İkinci Dereceden Denklemler"), Topic("14","AYT","Fizik","Kuvvet ve Hareket"), Topic("15","AYT","Kimya","Modern Atom Teorisi")
)

@Composable fun TopicPage(topics: List<Topic>, onToggle: (String) -> Unit) {
    var exam by remember { mutableStateOf("TYT") }
    var subject by remember { mutableStateOf("Tümü") }
    val shown = topics.filter { it.exam == exam && (subject == "Tümü" || it.subject == subject) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Konu takip çizelgesi", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Bitirdiğin konunun yanındaki kutucuğa dokun.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("TYT", "AYT").forEach { FilterChip(selected = exam == it, onClick = { exam = it; subject = "Tümü" }, label = { Text(it) }) } }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Tümü", "Matematik", "Türkçe", "Kimya", "Fizik").forEach { FilterChip(selected = subject == it, onClick = { subject = it }, label = { Text(it, fontSize = 10.sp) }) } }
        val completed = shown.count { it.done }
        Text("İlerleme: $completed / ${shown.size}", color = Blue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 10.dp))
        LinearProgressIndicator(progress = { if (shown.isEmpty()) 0f else completed.toFloat() / shown.size }, modifier = Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(shown) { t -> Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.fillMaxWidth().clickable { onToggle(t.id) }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = t.done, onCheckedChange = { onToggle(t.id) })
                Column(Modifier.weight(1f)) { Text(t.title, fontWeight = FontWeight.SemiBold); Text(t.subject, color = Muted, fontSize = 11.sp) }
                Text(if (t.done) "+" else "○", color = if (t.done) Teal else Muted, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            } } }
        }
    }
}

@Composable fun WeeklyPage() {
    val days = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
    val plan = listOf(listOf("Matematik: Temel Kavramlar", "Türkçe: Paragraf", "Kimya: Atom"), listOf("Matematik: Sayı Basamakları", "Fizik: Hareket", "Biyoloji: Hücre"), listOf("Matematik: Bölünebilme", "Türkçe: Sözcükte Anlam", "Kimya: Periyodik Sistem"), listOf("Matematik: EBOB-EKOK", "Coğrafya: Harita Bilgisi", "Tarih: İlk Çağ"), listOf("Matematik: Problemler", "Fizik: Kuvvet", "Biyoloji: Kalıtım"), listOf("TYT genel tekrar", "AYT Matematik: Fonksiyonlar", "Branş denemesi"), listOf("Haftalık deneme", "Yanlış analizi", "Eksik konu tekrarı"))
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { Text("7 günlük konu bitirme planı", fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("Örnek başlangıç planı · Seviyene göre düzenlenebilir", color = Muted, fontSize = 12.sp) }
        items(days.indices.toList()) { i -> Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text(days[i], color = Blue, fontWeight = FontWeight.Bold); plan[i].forEach { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 7.dp)) { Icon(Icons.Default.RadioButtonUnchecked, null, tint = Muted, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(it, fontSize = 13.sp) } } } } }
    }
}

@Composable fun ExamPage(exams: List<MockExam>, onAdd: () -> Unit) {
    val latest = exams.firstOrNull()
    val correct = exams.sumOf { it.correct }.toFloat(); val wrong = exams.sumOf { it.wrong }.toFloat(); val blank = exams.sumOf { it.blank }.toFloat()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("Deneme analizi", fontSize = 22.sp, fontWeight = FontWeight.Bold); IconButton(onClick = onAdd) { Icon(Icons.Default.AddCircle, "Deneme ekle", tint = Blue) } }
        Text("Doğru, yanlış, boş ve net gelişimini takip et.", color = Muted, fontSize = 12.sp)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(Modifier.padding(15.dp)) {
                Text("Toplam soru dağılımı", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(correct, wrong, blank, Modifier.size(150.dp))
                    Column(Modifier.padding(start = 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Legend("Doğru", correct.toInt(), Teal); Legend("Yanlış", wrong.toInt(), Color(0xFFE56D72)); Legend("Boş", blank.toInt(), Orange) }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Column(Modifier.padding(15.dp)) {
                Text("Net gelişim grafiği", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (exams.size >= 2) NetTrendChart(exams.reversed().map { it.net }, Modifier.fillMaxWidth().height(125.dp))
                else Text("Gelişim grafiği için en az 2 deneme kaydet.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 24.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
            SmallMetric("Deneme", exams.size.toString(), Modifier.weight(1f)); SmallMetric("Ortalama net", if (exams.isEmpty()) "—" else String.format("%.2f", exams.map { it.net }.average()), Modifier.weight(1f))
        }
        Text("Deneme geçmişi", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) { items(exams) { ExamMiniCard(it) } }
        if (exams.isEmpty()) EmptyCard("Deneme kaydı bulunmuyor. + düğmesiyle ekleyebilirsin.")
    }
}


@Composable fun NetTrendChart(values: List<Double>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val minV = (values.minOrNull() ?: 0.0).toFloat()
        val maxV = (values.maxOrNull() ?: 1.0).toFloat().let { if (it == minV) it + 1f else it }
        val pad = 12.dp.toPx()
        val usableW = size.width - pad * 2
        val usableH = size.height - pad * 2
        repeat(3) { i ->
            val y = pad + usableH * i / 2f
            drawLine(Color(0xFFE7ECF3), Offset(pad, y), Offset(size.width - pad, y), 1.dp.toPx())
        }
        val pts = values.mapIndexed { i, v ->
            Offset(pad + usableW * i / (values.size - 1), pad + usableH - ((v.toFloat() - minV) / (maxV - minV)) * usableH)
        }
        pts.zipWithNext().forEach { (a, b) -> drawLine(Blue, a, b, 3.dp.toPx(), cap = StrokeCap.Round) }
        pts.forEach { drawCircle(Teal, 4.dp.toPx(), it) }
    }
}

@Composable fun DonutChart(correct: Float, wrong: Float, blank: Float, modifier: Modifier = Modifier) {
    val total = (correct + wrong + blank).coerceAtLeast(1f)
    Canvas(modifier) {
        var start = -90f
        val values = listOf(correct to Teal, wrong to Color(0xFFE56D72), blank to Orange)
        values.forEach { (v, c) -> val sweep = 360f * v / total; drawArc(c, start, sweep, false, style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt)); start += sweep }
        drawCircle(Color(0xFFF4F6FA), radius = size.minDimension * .29f)
    }
}
@Composable fun Legend(label: String, value: Int, color: Color) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(9.dp).background(color, CircleShape)); Spacer(Modifier.width(7.dp)); Text("$label  $value", fontSize = 12.sp) } }
@Composable fun SmallMetric(label: String, value: String, modifier: Modifier = Modifier) { Column(modifier.background(Color.White, RoundedCornerShape(14.dp)).padding(13.dp)) { Text(label, color = Muted, fontSize = 11.sp); Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold) } }
@Composable fun ExamMiniCard(e: MockExam) { Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(e.name, fontWeight = FontWeight.SemiBold); Text(e.date, color = Muted, fontSize = 11.sp); Text("D ${e.correct} · Y ${e.wrong} · B ${e.blank}", color = Muted, fontSize = 11.sp) }; Text(String.format("%.2f net", e.net), color = Blue, fontWeight = FontWeight.Bold) } } }
@Composable fun EmptyCard(text: String) { Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Text(text, Modifier.padding(18.dp), color = Muted) } }

@Composable fun ProfileDialog(name: String, school: String, target: String, onName: (String) -> Unit, onSchool: (String) -> Unit, onTarget: (String) -> Unit, close: () -> Unit) {
    var n by remember { mutableStateOf(name) }; var s by remember { mutableStateOf(school) }; var t by remember { mutableStateOf(target) }
    AlertDialog(onDismissRequest = close, title = { Text("Öğrenci profili") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(n, { n = it }, label = { Text("Adın") }, singleLine = true)
            Text("Eğitim durumun", fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(s == "12. sınıf", { s = "12. sınıf" }); Text("12. sınıf"); Spacer(Modifier.width(12.dp)); RadioButton(s == "Mezun", { s = "Mezun" }); Text("Mezun") }
            OutlinedTextField(t, { t = it }, label = { Text("TYT net hedefi") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        }
    }, confirmButton = { TextButton(onClick = { onName(n.ifBlank { "Öğrenci" }); onSchool(s); onTarget(t); close() }) { Text("Kaydet") } }, dismissButton = { TextButton(onClick = close) { Text("Vazgeç") } })
}

@Composable fun ExamDialog(onDismiss: () -> Unit, onSave: (MockExam) -> Unit) {
    var title by remember { mutableStateOf("TYT denemesi") }; var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var c by remember { mutableStateOf("") }; var w by remember { mutableStateOf("") }; var b by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Deneme ekle") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Deneme adı") }, singleLine = true)
            OutlinedTextField(date, { date = it }, label = { Text("Tarih (YYYY-AA-GG)") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedTextField(c, { c = it }, label = { Text("Doğru") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(w, { w = it }, label = { Text("Yanlış") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(b, { b = it }, label = { Text("Boş") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
            Text("Net = Doğru − Yanlış / 4", color = Muted, fontSize = 11.sp)
        }
    }, confirmButton = { TextButton(onClick = { val cc = c.toIntOrNull() ?: 0; val ww = w.toIntOrNull() ?: 0; val bb = b.toIntOrNull() ?: 0; if (cc >= 0 && ww >= 0 && bb >= 0 && cc + ww + bb > 0) onSave(MockExam(UUID.randomUUID().toString(), title, date, cc, ww, bb)) }) { Text("Kaydet") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } })
}
