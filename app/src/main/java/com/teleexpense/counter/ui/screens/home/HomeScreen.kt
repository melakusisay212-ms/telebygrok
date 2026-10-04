package com.teleexpense.counter.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleexpense.counter.domain.calendar.EthiopianCalendarConverter
import com.teleexpense.counter.domain.model.Category
import com.teleexpense.counter.domain.model.TelecomTransaction
import com.teleexpense.counter.ui.theme.EthioGreen
import com.teleexpense.counter.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(viewModel: MainViewModel, onTransactionClick: (String) -> Unit) {
    val summary = viewModel.currentMonthSummary()
    val recent = viewModel.transactions.collectAsState().value.take(8)
    val eth = EthiopianCalendarConverter.today()

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Tele Expense", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text("Where is your money going?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EthioGreen)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(EthiopianCalendarConverter.monthLabel(eth.year, eth.month), color = Color.White.copy(0.9f))
                    Text("This Month", color = Color.White.copy(0.8f))
                    Spacer(Modifier.height(8.dp))
                    Text(formatEtb(summary.totalAmount), color = Color.White, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
                    Text("${summary.transactionCount} transactions", color = Color.White.copy(0.85f))
                }
            }
        }
        item {
            Text("Breakdown", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (summary.breakdown.isEmpty()) {
                Text("No expenses this month yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        val colors = listOf(Color(0xFF008C45), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFF9C27B0))
                        summary.breakdown.forEachIndexed { i, item ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).background(colors[i % colors.size], CircleShape))
                                Spacer(Modifier.width(10.dp))
                                Text(iconFor(item.category), fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(categoryLabel(item.category), Modifier.weight(1f))
                                Text(formatEtb(item.amount), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        item { Text("Recent", fontWeight = FontWeight.SemiBold) }
        if (recent.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No telecom expenses yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(recent) { tx -> TransactionCard(tx) { onTransactionClick(tx.id) } }
        }
    }
}

@Composable
fun TransactionCard(tx: TelecomTransaction, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(iconFor(tx.category), fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tx.packageName ?: categoryLabel(tx.category), fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    "${EthiopianCalendarConverter.monthName(tx.ethiopianMonth)} ${tx.ethiopianDay}, ${tx.ethiopianYear}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(formatEtb(tx.amount), fontWeight = FontWeight.Bold, color = EthioGreen)
        }
    }
}

fun formatEtb(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.US)
    nf.maximumFractionDigits = 2
    nf.minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    return "${nf.format(amount)} ETB"
}

fun categoryLabel(c: Category) = when (c) {
    Category.AIRTIME, Category.AIRTIME_RECHARGE -> "Airtime"
    Category.DATA_PACKAGE, Category.MIXED_PACKAGE -> "Data"
    Category.SMS_PACKAGE -> "SMS"
    Category.VOICE_PACKAGE -> "Voice"
    else -> "Other"
}

fun iconFor(c: Category) = when (c) {
    Category.DATA_PACKAGE, Category.MIXED_PACKAGE -> "📶"
    Category.AIRTIME, Category.AIRTIME_RECHARGE -> "📞"
    Category.SMS_PACKAGE -> "💬"
    else -> "📦"
}
