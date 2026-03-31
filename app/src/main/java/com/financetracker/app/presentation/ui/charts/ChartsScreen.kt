package com.financetracker.app.presentation.ui.charts

import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financetracker.app.presentation.viewmodel.ChartPeriod
import com.financetracker.app.presentation.viewmodel.ChartsViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

private val CATEGORY_COLORS = listOf(
    AndroidColor.parseColor("#FF5722"),
    AndroidColor.parseColor("#2196F3"),
    AndroidColor.parseColor("#9C27B0"),
    AndroidColor.parseColor("#607D8B"),
    AndroidColor.parseColor("#FF9800"),
    AndroidColor.parseColor("#F44336"),
    AndroidColor.parseColor("#4CAF50"),
    AndroidColor.parseColor("#009688"),
    AndroidColor.parseColor("#9E9E9E")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(vm: ChartsViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Charts") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period selector
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartPeriod.values().forEach { period ->
                    FilterChip(
                        selected = state.period == period,
                        onClick  = { vm.setPeriod(period) },
                        label    = { Text(period.name.lowercase().replaceFirstChar { it.titlecase() }) }
                    )
                }
            }

            // Category Pie / Donut chart
            if (state.categorySpends.isNotEmpty()) {
                ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Spending by Category", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        AndroidView(
                            modifier = Modifier.fillMaxWidth().height(260.dp),
                            factory  = { ctx ->
                                PieChart(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setUsePercentValues(true)
                                    description.isEnabled = false
                                    isDrawHoleEnabled = true
                                    holeRadius = 42f
                                    setHoleColor(AndroidColor.TRANSPARENT)
                                    setEntryLabelColor(AndroidColor.WHITE)
                                    legend.isEnabled = true
                                    legend.textColor = AndroidColor.WHITE
                                }
                            },
                            update = { chart ->
                                val entries = state.categorySpends.mapIndexed { i, cs ->
                                    PieEntry(cs.total.toFloat(), cs.category)
                                }
                                val dataSet = PieDataSet(entries, "").apply {
                                    colors = CATEGORY_COLORS.take(entries.size)
                                    valueTextColor = AndroidColor.WHITE
                                    valueTextSize = 11f
                                }
                                chart.data = PieData(dataSet)
                                chart.invalidate()
                            }
                        )
                    }
                }
            }

            // Bar chart — monthly income vs expenses placeholder
            ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Income vs Expenses", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        factory  = { ctx ->
                            BarChart(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                description.isEnabled = false
                                legend.textColor = AndroidColor.WHITE
                                axisLeft.textColor = AndroidColor.WHITE
                                axisRight.isEnabled = false
                                xAxis.textColor = AndroidColor.WHITE
                                xAxis.position = XAxis.XAxisPosition.BOTTOM
                                xAxis.granularity = 1f
                                setFitBars(true)
                            }
                        },
                        update = { chart ->
                            val labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            // Placeholder data — replace with real data from ViewModel
                            val incomeEntries = labels.indices.map { i -> BarEntry(i.toFloat(), (20000 + i * 500).toFloat()) }
                            val expenseEntries = labels.indices.map { i -> BarEntry(i.toFloat(), (15000 + i * 300).toFloat()) }
                            val incomeSet  = BarDataSet(incomeEntries, "Income").apply { color = AndroidColor.parseColor("#43A047"); valueTextColor = AndroidColor.WHITE }
                            val expenseSet = BarDataSet(expenseEntries, "Expenses").apply { color = AndroidColor.parseColor("#E53935"); valueTextColor = AndroidColor.WHITE }
                            val data = BarData(incomeSet, expenseSet)
                            data.barWidth = 0.35f
                            chart.data = data
                            chart.groupBars(0f, 0.3f, 0f)
                            chart.invalidate()
                        }
                    )
                }
            }

            // Line chart — savings trend
            ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Savings Trend", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        factory  = { ctx ->
                            LineChart(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                description.isEnabled = false
                                legend.textColor = AndroidColor.WHITE
                                axisLeft.textColor = AndroidColor.WHITE
                                axisRight.isEnabled = false
                                xAxis.textColor = AndroidColor.WHITE
                                xAxis.position = XAxis.XAxisPosition.BOTTOM
                                xAxis.granularity = 1f
                            }
                        },
                        update = { chart ->
                            val labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            val savingsEntries = labels.indices.map { i -> Entry(i.toFloat(), (4000 + i * 200).toFloat()) }
                            val dataSet = LineDataSet(savingsEntries, "Savings").apply {
                                color = AndroidColor.parseColor("#009688")
                                valueTextColor = AndroidColor.WHITE
                                lineWidth = 2f
                                setDrawCircles(true)
                                setCircleColor(AndroidColor.parseColor("#009688"))
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                            }
                            chart.data = LineData(dataSet)
                            chart.invalidate()
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
