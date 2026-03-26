package com.kai.netpulse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kai.netpulse.NetworkViewModel
import com.kai.netpulse.PortResult
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetPulseScreen(viewModel: NetworkViewModel) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ping", "Port-Scan", "DNS", "WiFi")

    // WiFi Monitoring startet beim Oeffnen des WiFi-Tabs
    LaunchedEffect(selectedTab) {
        if (selectedTab == 3) viewModel.startWifiMonitoring()
        else viewModel.stopWifiMonitoring()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("NetPulse") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }
            when (selectedTab) {
                0 -> PingTab(viewModel = viewModel)
                1 -> PortScanTab(viewModel = viewModel)
                2 -> DnsTab(viewModel = viewModel)
                3 -> WifiTab(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun PingTab(viewModel: NetworkViewModel) {
    val state by viewModel.state.collectAsState()
    var host by remember { mutableStateOf("8.8.8.8") }
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(state.pingHistory) {
        if (state.pingHistory.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries { series(state.pingHistory.map { it.toDouble() }) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Host / IP") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.ping(host) })
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.ping(host) },
                enabled = !state.isPinging
            ) {
                Text(if (state.isPinging) "Pinge..." else "Ping")
            }
            OutlinedButton(
                onClick = { viewModel.startContinuousPing(host) },
                enabled = !state.isPinging
            ) {
                Text("20x Ping")
            }
        }

        state.latestPing?.let { result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (result.latencyMs != null) {
                        Text("${result.latencyMs} ms", style = MaterialTheme.typography.headlineMedium)
                        Text(result.host, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Hinweis: TCP-basiertes Ping (kein ICMP auf Android ohne Root)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Text("Nicht erreichbar", color = MaterialTheme.colorScheme.error)
                        result.error?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }

        if (state.pingHistory.size >= 2) {
            Text("Latenz-Verlauf", style = MaterialTheme.typography.titleSmall)
            CartesianChartHost(
                chart = rememberCartesianChart(rememberLineCartesianLayer()),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        }
    }
}

@Composable
private fun PortScanTab(viewModel: NetworkViewModel) {
    val state by viewModel.state.collectAsState()
    var host by remember { mutableStateOf("192.168.1.1") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Ziel-Host / IP") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = { viewModel.scanPorts(host) },
            enabled = !state.isScanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isScanning) "Scanne... (${"%.0f".format(state.scanProgress * 100)}%)" else "Top 100 Ports scannen")
        }

        if (state.isScanning) {
            LinearProgressIndicator(progress = { state.scanProgress }, modifier = Modifier.fillMaxWidth())
        }

        if (state.portResults.isNotEmpty()) {
            Text("Offene Ports (${state.portResults.size})", style = MaterialTheme.typography.titleSmall)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.portResults) { result ->
                    PortRow(result)
                }
            }
        } else if (!state.isScanning && state.scanProgress == 1f) {
            Text("Keine offenen Ports gefunden.", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun PortRow(result: PortResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Port ${result.port}", style = MaterialTheme.typography.bodyMedium)
            Text(result.service, color = MaterialTheme.colorScheme.primary)
            Text("OFFEN", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun DnsTab(viewModel: NetworkViewModel) {
    val state by viewModel.state.collectAsState()
    var hostname by remember { mutableStateOf("google.com") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = hostname,
            onValueChange = { hostname = it },
            label = { Text("Hostname") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.dnsLookup(hostname) })
        )
        Button(onClick = { viewModel.dnsLookup(hostname) }, modifier = Modifier.fillMaxWidth()) {
            Text("DNS auflösen")
        }

        state.dnsResult?.let { result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(result.hostname, style = MaterialTheme.typography.titleSmall)
                    result.addresses.forEach { addr ->
                        Text(addr, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiTab(viewModel: NetworkViewModel) {
    val state by viewModel.state.collectAsState()
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(state.wifiSignalHistory) {
        if (state.wifiSignalHistory.size >= 2) {
            modelProducer.runTransaction {
                lineSeries { series(state.wifiSignalHistory.map { it.toDouble() }) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("WiFi Signal", style = MaterialTheme.typography.titleSmall)
                if (state.wifiSsid.isNotEmpty()) {
                    Text(state.wifiSsid.replace("\"", ""), style = MaterialTheme.typography.bodyMedium)
                }
                val currentSignal = state.wifiSignalHistory.lastOrNull()
                if (currentSignal != null) {
                    Text(
                        "$currentSignal dBm (${signalQuality(currentSignal)})",
                        style = MaterialTheme.typography.headlineSmall
                    )
                } else {
                    Text("Nicht verbunden oder keine Daten", color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        if (state.wifiSignalHistory.size >= 2) {
            Text("Signal-Verlauf (dBm)", style = MaterialTheme.typography.titleSmall)
            CartesianChartHost(
                chart = rememberCartesianChart(rememberLineCartesianLayer()),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

private fun signalQuality(dBm: Int): String = when {
    dBm >= -50 -> "Ausgezeichnet"
    dBm >= -60 -> "Gut"
    dBm >= -70 -> "Mittel"
    dBm >= -80 -> "Schwach"
    else -> "Sehr schwach"
}
