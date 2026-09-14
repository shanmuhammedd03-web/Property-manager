package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.model.BookingItem
import com.example.data.repository.PropertyBookingRepository
import com.example.ui.components.BookingDialog
import com.example.ui.screens.AvailabilityScreen
import com.example.ui.screens.BookingsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PropertiesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PropertyBookingViewModel
import com.example.ui.viewmodel.PropertyBookingViewModelFactory

enum class AppTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "tab_dashboard"),
    BOOKINGS("Bookings", Icons.Default.ListAlt, "tab_bookings"),
    AVAILABILITY("Availability", Icons.Default.EventAvailable, "tab_availability"),
    PROPERTIES("Properties", Icons.Default.Home, "tab_properties")
}

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: PropertyBookingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val repository = PropertyBookingRepository(database.propertyDao(), database.bookingDao())
        val factory = PropertyBookingViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[PropertyBookingViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: PropertyBookingViewModel) {
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val currentTab = AppTab.entries[selectedTabIndex]

    // Dialog state for creating / editing bookings
    var showBookingDialog by remember { mutableStateOf(false) }
    var bookingToEdit by remember { mutableStateOf<BookingItem?>(null) }
    var prefillPropertyId by remember { mutableStateOf<Long?>(null) }
    var prefillDate by remember { mutableStateOf<String?>(null) }
    var prefillStartTime by remember { mutableStateOf<String?>(null) }
    var prefillEndTime by remember { mutableStateOf<String?>(null) }

    // Collect user notification / snackbar messages
    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Booking Dialog (Add / Edit)
    if (showBookingDialog) {
        BookingDialog(
            properties = properties,
            bookingToEdit = bookingToEdit,
            initialPropertyId = prefillPropertyId,
            initialDate = prefillDate,
            initialStartTime = prefillStartTime,
            initialEndTime = prefillEndTime,
            onDismiss = {
                showBookingDialog = false
                bookingToEdit = null
                prefillPropertyId = null
                prefillDate = null
                prefillStartTime = null
                prefillEndTime = null
            },
            onSave = { bookingId, propertyId, customerName, date, startTime, endTime, amount, isPaid ->
                viewModel.saveBooking(
                    bookingId = bookingId,
                    propertyId = propertyId,
                    customerName = customerName,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    amount = amount,
                    isPaid = isPaid,
                    onSuccess = {
                        showBookingDialog = false
                        bookingToEdit = null
                        prefillPropertyId = null
                        prefillDate = null
                        prefillStartTime = null
                        prefillEndTime = null
                    }
                )
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentTab.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                AppTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToBookings = { selectedTabIndex = 1 },
                    onNavigateToAvailability = { selectedTabIndex = 2 },
                    onNavigateToProperties = { selectedTabIndex = 3 },
                    onNewBookingClick = {
                        bookingToEdit = null
                        showBookingDialog = true
                    },
                    onEditBooking = { item ->
                        bookingToEdit = item
                        showBookingDialog = true
                    }
                )

                AppTab.BOOKINGS -> BookingsScreen(
                    viewModel = viewModel,
                    onNewBookingClick = {
                        bookingToEdit = null
                        showBookingDialog = true
                    },
                    onEditBooking = { item ->
                        bookingToEdit = item
                        showBookingDialog = true
                    }
                )

                AppTab.AVAILABILITY -> AvailabilityScreen(
                    viewModel = viewModel,
                    onBookSlot = { propId, date, start, end ->
                        bookingToEdit = null
                        prefillPropertyId = propId
                        prefillDate = date
                        prefillStartTime = start
                        prefillEndTime = end
                        showBookingDialog = true
                    }
                )

                AppTab.PROPERTIES -> PropertiesScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
