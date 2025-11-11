package com.example.carcharger

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.konektis.Devices
import io.konektis.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CarChargerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val appContainer = (LocalContext.current.applicationContext as CarChargerApplication).container
    val factory = ViewModelFactory(appContainer)
    val loginViewModel: LoginViewModel = viewModel(factory = factory)

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                navController = navController,
                viewModel = loginViewModel
            )
        }
        composable("main/{username}") { backStackEntry ->
            MainScreen(navController, backStackEntry.arguments?.getString("username") ?: "", factory)
        }
        composable("profile/{username}") { backStackEntry ->
            ProfileScreen(
                navController = navController,
                username = backStackEntry.arguments?.getString("username") ?: "",
                onLogout = {
                    loginViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
fun LoginScreen(navController: NavController, viewModel: LoginViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginState by viewModel.loginState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        if (loginState is LoginState.Loading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Logging in...", fontSize = 16.sp)
        } else {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.login(username, password) }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.login(username, password) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }

            if (loginState is LoginState.Error) {
                Text(
                    text = (loginState as LoginState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        LaunchedEffect(loginState) {
            if (loginState is LoginState.Success) {
                val successState = loginState as LoginState.Success
                navController.navigate("main/${successState.username}") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }
}


@Composable
fun CarChargerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = MaterialTheme.colorScheme.primary,
            secondary = MaterialTheme.colorScheme.secondary,
            background = MaterialTheme.colorScheme.background
        ),
        content = content
    )
}

data class BottomNavigationItem(
    val label: String = "",
    val icon: ImageVector = Icons.Filled.Home,
    val screen: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, username: String, factory: ViewModelFactory) {
    val items = listOf(
        BottomNavigationItem("Overview", Icons.Filled.Home) { OverviewScreen(viewModel(factory = factory)) },
        BottomNavigationItem("Charger", Icons.Filled.EvStation) { ChargerScreen(viewModel(factory = factory)) },
        BottomNavigationItem("Heat Pump", Icons.Filled.Thermostat) { HeatPumpScreen() }
    )

    var selectedIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Energy App") },
                actions = {
                    IconButton(onClick = { navController.navigate("profile/$username") }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            items[selectedIndex].screen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, username: String, onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Username", fontSize = 16.sp)
                    Text(username, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun ChargerScreen(viewModel: ChargerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Charging Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (uiState.isCharging) "Charging at %.1f kW".format(uiState.power / 1000.0) else "Not charging",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isCharging)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    )
                }


            }
        }

        // Power/Stop Button
        Button(
            onClick = { viewModel.toggleCharging() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isCharging) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (uiState.isCharging) "STOP CHARGING" else "START CHARGING",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Charging Mode Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Charging Mode",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Solar Mode Radio Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = uiState.chargingMode == ChargingMode.SOLAR,
                            onClick = { viewModel.setChargingMode(ChargingMode.SOLAR) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.chargingMode == ChargingMode.SOLAR,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Excess power production",
                        fontSize = 16.sp
                    )
                }
                
                // Manual Mode Radio Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = uiState.chargingMode == ChargingMode.MANUAL,
                            onClick = { viewModel.setChargingMode(ChargingMode.MANUAL) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.chargingMode == ChargingMode.MANUAL,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fixed Power",
                        fontSize = 16.sp
                    )
                }
            }
        }
        
        // Manual Current Control (only visible in manual mode)
        if (uiState.chargingMode == ChargingMode.MANUAL) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Maximum Power",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${uiState.manualPower} Watt",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Slider(
                        value = uiState.manualPower.toFloat(),
                        onValueChange = { viewModel.setManualPower(it.toInt()) },
                        valueRange = 1440f..7680f,
                        steps = 25,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "1.4 kW", fontSize = 12.sp)
                        Text(text = "7.7 kW", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun OverviewScreen(viewModel: OverviewViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PowerSummary(
            gridPower = uiState.gridPower,
            solarPower = uiState.solarPower,
            chargerPower = uiState.chargerPower,
            heatPumpPower = uiState.heatPumpPower
        )
        BatterySummary(
            batteryLevel = uiState.batteryLevel,
            batteryPower = uiState.batteryPower
        )
    }
}

@Composable
fun PowerSummary(
    gridPower: Double,
    solarPower: Double,
    chargerPower: Double,
    heatPumpPower: Double
) {
    val totalPower = gridPower + solarPower + chargerPower + heatPumpPower
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Power Consumption", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            PowerSource(label = "Grid", power = gridPower)
            PowerSource(label = "Solar", power = solarPower, isProduction = true)
            PowerSource(label = "Car Charger", power = chargerPower)
            PowerSource(label = "Heat Pump", power = heatPumpPower)
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%.2f kW".format(kotlin.math.abs(totalPower) / 1000),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalPower < 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun PowerSource(label: String, power: Double, isProduction: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp)
        Text(
            text = "%.2f kW".format(kotlin.math.abs(power) / 1000),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (power < 0) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    }
}

@Composable
fun BatterySummary(batteryLevel: Int, batteryPower: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Battery", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Charge Level", fontSize = 16.sp)
                Text("$batteryLevel%", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        batteryPower > 0 -> "Discharging"
                        batteryPower < 0 -> "Charging"
                        else -> "Idle"
                    },
                    fontSize = 16.sp
                )
                Text(
                    text = "%.2f kW".format(kotlin.math.abs(batteryPower) / 1000),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        batteryPower > 0 -> MaterialTheme.colorScheme.secondary
                        batteryPower < 0 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}


@Composable
fun HeatPumpScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Heat Pump Screen", fontSize = 24.sp)
    }
}

enum class ChargingMode {
    SOLAR,
    MANUAL
}

data class ChargerUiState(
    val isCharging: Boolean = false,
    val chargingMode: ChargingMode = ChargingMode.SOLAR,
    val manualPower: Int = 1440,
    val power: Double = 0.0
)

class ChargerViewModel(private val webSocketClient: WebSocketClient) : ViewModel() {
    private val _uiState = MutableStateFlow(ChargerUiState())
    val uiState: StateFlow<ChargerUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            webSocketClient.messages.collect { message ->
                if (message is Message.PowerUsageUpdate) {
                    message.updates.find { it.device == Devices.CAR_CHARGER }?.let { 
                        _uiState.value = _uiState.value.copy(power = it.power.toDouble(), isCharging = it.power > 0)
                    }
                }
            }
        }
    }

    fun toggleCharging() {
        val newCharging = !_uiState.value.isCharging
        val power = if (newCharging) _uiState.value.manualPower.toDouble() else 0.0
        _uiState.value = _uiState.value.copy(
            isCharging = newCharging,
            power = power
        )
    }

    fun setChargingMode(mode: ChargingMode) {
        _uiState.value = _uiState.value.copy(
            chargingMode = mode
        )
    }

    fun setManualPower(power: Int) {
        val coercedPower = power.coerceIn(1440, 7680)
        val power = if (_uiState.value.isCharging) coercedPower.toDouble() else 0.0
        _uiState.value = _uiState.value.copy(
            manualPower = coercedPower,
            power = power
        )
    }
}

data class OverviewUiState(
    val gridPower: Double = 0.0,
    val solarPower: Double = 0.0,
    val chargerPower: Double = 0.0,
    val heatPumpPower: Double = 0.0,
    val batteryLevel: Int = 0,
    val batteryPower: Double = 0.0
)

class OverviewViewModel(private val webSocketClient: WebSocketClient) : ViewModel() {
    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            webSocketClient.messages.collect { message ->
                if (message is Message.PowerUsageUpdate) {
                    var gridPower = 0.0
                    var solarPower = 0.0
                    var chargerPower = 0.0
                    var heatPumpPower = 0.0
                    var batteryPower = 0.0

                    message.updates.forEach { 
                        when (it.device) {
                            Devices.GRID -> gridPower = it.power.toDouble()
                            Devices.SOLAR -> solarPower = it.power.toDouble()
                            Devices.CAR_CHARGER -> chargerPower = it.power.toDouble()
                            Devices.HEATPUMP -> heatPumpPower = it.power.toDouble()
                            Devices.BATTERY -> batteryPower = it.power.toDouble()
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        gridPower = gridPower,
                        solarPower = solarPower,
                        chargerPower = chargerPower,
                        heatPumpPower = heatPumpPower,
                        batteryPower = batteryPower
                    )
                }
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val username: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(
    private val webSocketClient: WebSocketClient,
    private val credentialsManager: CredentialsManager
) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()
    
    private var currentUsername = ""

    init {
        viewModelScope.launch {
            webSocketClient.connectionStatus.collect { status ->
                when (status) {
                    is ConnectionStatus.Connected -> _loginState.value = LoginState.Success(currentUsername)
                    is ConnectionStatus.Error -> _loginState.value = LoginState.Error(status.message)
                    is ConnectionStatus.Unauthorized -> _loginState.value = LoginState.Error("Invalid username or password")
                    is ConnectionStatus.Idle -> {
                        if (_loginState.value is LoginState.Loading) {
                            // Keep loading state during auto-login
                        } else {
                            _loginState.value = LoginState.Idle
                        }
                    }
                }
            }
        }
        
        // Attempt auto-login with saved credentials
        viewModelScope.launch {
            credentialsManager.credentials.collect { credentials ->
                if (credentials != null && _loginState.value is LoginState.Idle) {
                    currentUsername = credentials.username
                    _loginState.value = LoginState.Loading
                    webSocketClient.connect(credentials.username, credentials.password)
                }
            }
        }
    }

    fun login(username: String, password: String) {
        if (username.isNotEmpty() && password.isNotEmpty()) {
            currentUsername = username
            viewModelScope.launch {
                credentialsManager.saveCredentials(username, password)
            }
            webSocketClient.connect(username, password)
        } else {
            _loginState.value = LoginState.Error("Username and password cannot be empty")
        }
    }

    fun logout() {
        viewModelScope.launch {
            credentialsManager.clearCredentials()
        }
        webSocketClient.disconnect()
    }
}
