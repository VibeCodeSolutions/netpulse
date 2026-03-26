package com.kai.netpulse

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.ArrayDeque

data class PingResult(val host: String, val latencyMs: Long?, val error: String? = null)
data class PortResult(val port: Int, val open: Boolean, val service: String)
data class DnsResult(val hostname: String, val addresses: List<String>)

data class NetPulseState(
    val pingHistory: List<Long> = emptyList(),   // ms latencies
    val latestPing: PingResult? = null,
    val isPinging: Boolean = false,
    val portResults: List<PortResult> = emptyList(),
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val dnsResult: DnsResult? = null,
    val wifiSignalHistory: List<Int> = emptyList(), // dBm values
    val wifiSsid: String = ""
)

class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(NetPulseState())
    val state: StateFlow<NetPulseState> = _state.asStateFlow()

    private val wifiManager = application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val pingBuffer = ArrayDeque<Long>(60)
    private val wifiBuffer = ArrayDeque<Int>(60)

    private var wifiPollingActive = false

    // TCP-Ping (kein echtes ICMP auf Android ohne Root)
    fun ping(host: String, port: Int = 80) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isPinging = true)
            val start = System.currentTimeMillis()
            val result = try {
                val address = InetAddress.getByName(host)
                val reachable = address.isReachable(3000)
                val latency = System.currentTimeMillis() - start
                if (reachable) PingResult(host, latency)
                else {
                    // Fallback: TCP Connect auf Port 80 oder 443
                    val socket = Socket()
                    socket.connect(InetSocketAddress(host, port), 3000)
                    socket.close()
                    val tcpLatency = System.currentTimeMillis() - start
                    PingResult(host, tcpLatency)
                }
            } catch (e: Exception) {
                PingResult(host, null, e.message ?: "Nicht erreichbar")
            }

            result.latencyMs?.let { ms ->
                if (pingBuffer.size >= 60) pingBuffer.pollFirst()
                pingBuffer.addLast(ms)
            }

            _state.value = _state.value.copy(
                latestPing = result,
                pingHistory = pingBuffer.toList(),
                isPinging = false
            )
        }
    }

    fun startContinuousPing(host: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repeat(20) {
                if (!_state.value.isPinging) ping(host)
                delay(1000)
            }
        }
    }

    fun dnsLookup(hostname: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val addresses = InetAddress.getAllByName(hostname).map { it.hostAddress ?: "?" }
                DnsResult(hostname, addresses)
            } catch (e: Exception) {
                DnsResult(hostname, listOf("Fehler: ${e.message}"))
            }
            _state.value = _state.value.copy(dnsResult = result)
        }
    }

    fun scanPorts(host: String) {
        val top100Ports = listOf(
            21 to "FTP", 22 to "SSH", 23 to "Telnet", 25 to "SMTP", 53 to "DNS",
            80 to "HTTP", 110 to "POP3", 111 to "RPC", 135 to "RPC", 139 to "NetBIOS",
            143 to "IMAP", 161 to "SNMP", 194 to "IRC", 443 to "HTTPS", 445 to "SMB",
            465 to "SMTPS", 587 to "SMTP", 631 to "IPP", 993 to "IMAPS", 995 to "POP3S",
            1080 to "SOCKS", 1194 to "OpenVPN", 1433 to "MSSQL", 1723 to "PPTP",
            2049 to "NFS", 3306 to "MySQL", 3389 to "RDP", 5432 to "PostgreSQL",
            5900 to "VNC", 6379 to "Redis", 7070 to "RealServer", 8080 to "HTTP-Alt",
            8443 to "HTTPS-Alt", 8888 to "HTTP-Alt", 9200 to "Elasticsearch",
            27017 to "MongoDB", 5000 to "UPnP", 5001 to "UPnP", 5357 to "WSD",
            7 to "Echo", 9 to "Discard", 13 to "Daytime", 17 to "QOTD", 19 to "Chargen",
            37 to "Time", 43 to "WHOIS", 69 to "TFTP", 79 to "Finger", 98 to "Linuxconf",
            109 to "POP2", 113 to "Auth", 119 to "NNTP", 123 to "NTP", 137 to "NetBIOS",
            138 to "NetBIOS", 179 to "BGP", 389 to "LDAP", 427 to "SLP", 500 to "IKE",
            502 to "Modbus", 512 to "rexec", 513 to "rlogin", 514 to "syslog",
            515 to "LPD", 520 to "RIP", 548 to "AFP", 554 to "RTSP", 636 to "LDAPS",
            873 to "rsync", 989 to "FTPS", 990 to "FTPS", 1025 to "NFS", 1026 to "IIS",
            1027 to "IIS", 1028 to "IIS", 1029 to "IIS", 1110 to "POP3", 1433 to "MSSQL",
            1521 to "Oracle", 2000 to "Cisco", 2001 to "Cisco", 2121 to "FTP-Alt",
            2717 to "PN-Requester", 3000 to "Dev", 3128 to "Squid", 3986 to "3COM",
            4444 to "Krb524", 4899 to "RAdmin", 5009 to "AirPort", 5050 to "Yahoo",
            5051 to "ita-agent", 5100 to "cam", 5101 to "admdog", 5190 to "AOL",
            5631 to "pcANYWHERE", 5632 to "pcANYWHERE", 6000 to "X11", 6001 to "X11",
            6112 to "Blizzard", 6665 to "IRC", 6666 to "IRC", 6667 to "IRC",
            6668 to "IRC", 6669 to "IRC", 8008 to "HTTP-Alt", 9999 to "Urchin",
            10000 to "Webmin", 32768 to "RPC", 49152 to "RPC", 49153 to "RPC"
        ).distinctBy { it.first }.take(100)

        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isScanning = true, portResults = emptyList(), scanProgress = 0f)
            val results = mutableListOf<PortResult>()

            top100Ports.forEachIndexed { index, (port, service) ->
                val open = try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(host, port), 500)
                    socket.close()
                    true
                } catch (e: Exception) {
                    false
                }
                if (open) results.add(PortResult(port, true, service))
                _state.value = _state.value.copy(
                    scanProgress = (index + 1).toFloat() / top100Ports.size,
                    portResults = results.toList()
                )
            }

            _state.value = _state.value.copy(isScanning = false, scanProgress = 1f)
        }
    }

    fun startWifiMonitoring() {
        if (wifiPollingActive) return
        wifiPollingActive = true
        viewModelScope.launch(Dispatchers.IO) {
            while (wifiPollingActive) {
                val info = wifiManager.connectionInfo
                val rssi = info.rssi
                val ssid = info.ssid ?: ""

                if (wifiBuffer.size >= 60) wifiBuffer.pollFirst()
                if (rssi != -127) wifiBuffer.addLast(rssi)  // -127 = nicht verbunden

                _state.value = _state.value.copy(
                    wifiSignalHistory = wifiBuffer.toList(),
                    wifiSsid = ssid
                )
                delay(1000)
            }
        }
    }

    fun stopWifiMonitoring() {
        wifiPollingActive = false
    }

    override fun onCleared() {
        wifiPollingActive = false
        super.onCleared()
    }
}
