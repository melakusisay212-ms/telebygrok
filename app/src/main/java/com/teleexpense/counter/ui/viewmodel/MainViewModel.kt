package com.teleexpense.counter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teleexpense.counter.data.repository.ExpenseRepository
import com.teleexpense.counter.domain.calendar.EthiopianCalendarConverter
import com.teleexpense.counter.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.GregorianCalendar

class MainViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val _onboardingDone = MutableStateFlow(false)
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    val transactions: StateFlow<List<TelecomTransaction>> = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { _onboardingDone.value = repository.isOnboardingDone() }
    }

    fun scanThisMonth() {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning
            try {
                val r = repository.scanCurrentMonth()
                _scanState.value = ScanUiState.Done(r)
                _onboardingDone.value = true
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(e.message ?: "Scan failed")
            }
        }
    }

    fun startFromToday() {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning
            repository.startFromToday()
            _scanState.value = ScanUiState.Done(ExpenseRepository.ScanResult(0, 0, 0, emptyList()))
            _onboardingDone.value = true
        }
    }

    fun scanNewMessages() {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning
            try {
                _scanState.value = ScanUiState.Done(repository.scanNewMessages())
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(e.message ?: "Scan failed")
            }
        }
    }

    fun currentMonthSummary(): PeriodSummary {
        val eth = EthiopianCalendarConverter.today()
        val txs = transactions.value.filter {
            it.ethiopianYear == eth.year && it.ethiopianMonth == eth.month && !it.isExcluded
        }
        return buildSummary(txs, EthiopianCalendarConverter.monthLabel(eth.year, eth.month))
    }

    fun summaryForPeriod(period: Period): PeriodSummary {
        val now = System.currentTimeMillis()
        val cal = GregorianCalendar()
        val (from, label) = when (period) {
            Period.TODAY -> {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis to "Today"
            }
            Period.THIS_WEEK -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis to "This Week"
            }
            Period.THIS_MONTH -> {
                val eth = EthiopianCalendarConverter.today()
                EthiopianCalendarConverter.startOfEthiopianMonthMillis(eth.year, eth.month) to
                    EthiopianCalendarConverter.monthLabel(eth.year, eth.month)
            }
            Period.THIS_YEAR -> {
                val eth = EthiopianCalendarConverter.today()
                EthiopianCalendarConverter.startOfEthiopianMonthMillis(eth.year, 1) to "Year ${eth.year}"
            }
        }
        val txs = transactions.value.filter { !it.isExcluded && it.gregorianDateTimeMillis in from until now }
        return buildSummary(txs, label)
    }

    private fun buildSummary(txs: List<TelecomTransaction>, label: String): PeriodSummary {
        val total = txs.sumOf { it.amount }
        val byCat = txs.groupBy { simplify(it.category) }.map { (cat, list) ->
            val amt = list.sumOf { it.amount }
            CategoryBreakdown(cat, amt, if (total > 0) ((amt / total) * 100).toFloat() else 0f, list.size)
        }.sortedByDescending { it.amount }
        return PeriodSummary(
            label, label, total, txs.size,
            total / 1.0,
            if (txs.isNotEmpty()) total / txs.size else 0.0,
            byCat
        )
    }

    private fun simplify(c: Category) = when (c) {
        Category.AIRTIME, Category.AIRTIME_RECHARGE -> Category.AIRTIME
        Category.DATA_PACKAGE, Category.MIXED_PACKAGE -> Category.DATA_PACKAGE
        Category.SMS_PACKAGE -> Category.SMS_PACKAGE
        Category.VOICE_PACKAGE -> Category.VOICE_PACKAGE
        else -> Category.OTHER
    }

    fun deleteTransaction(id: String) = viewModelScope.launch { repository.deleteTransaction(id) }
    fun excludeTransaction(id: String) = viewModelScope.launch { repository.excludeTransaction(id) }
    fun getTransaction(id: String) = transactions.value.find { it.id == id }
}

enum class Period { TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR }

sealed class ScanUiState {
    data object Idle : ScanUiState()
    data object Scanning : ScanUiState()
    data class Done(val result: ExpenseRepository.ScanResult) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

class MainViewModelFactory(private val repo: ExpenseRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repo) as T
}
