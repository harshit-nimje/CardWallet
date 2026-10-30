package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.data.model.BusinessCard
import com.example.ui.screens.AiFinderScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CardDetailScreen
import com.example.ui.screens.ContactsImportScreen
import com.example.ui.screens.ScanCardScreen
import com.example.ui.screens.WalletDeckScreen
import com.example.ui.screens.signOutUser
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WalletViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

sealed interface WalletScreen {
    data object Deck : WalletScreen
    data class Detail(val card: BusinessCard) : WalletScreen
    data object Scan : WalletScreen
    data object AiFinder : WalletScreen
    data object ContactsImport : WalletScreen
    data object Settings : WalletScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check contract with launch intent before UI/ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            AppRoot()
        }
    }
}

@Composable
fun AppRoot(
    viewModel: WalletViewModel = viewModel()
) {
    val useMaterialDynamicTheme by viewModel.useMaterialDynamicTheme.collectAsState()
    val selectedPaletteTheme by viewModel.selectedPaletteTheme.collectAsState()

    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    val isGuest by viewModel.isGuestMode.collectAsState()

    MyApplicationTheme(
        darkTheme = true,
        dynamicColor = useMaterialDynamicTheme,
        palette = selectedPaletteTheme
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    currentUser = auth.currentUser
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose {
                    Firebase.auth.removeAuthStateListener(listener)
                }
            }

            if (currentUser == null && !isGuest) {
                AuthScreen(
                    onAuthSuccess = {
                        currentUser = Firebase.auth.currentUser
                    },
                    onContinueAsGuest = {
                        viewModel.continueAsGuest()
                    }
                )
            } else {
                WalletMainFlow(
                    user = currentUser,
                    isGuest = isGuest,
                    onSignOutRequested = {
                        viewModel.exitGuestMode()
                        currentUser = null
                    },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun WalletMainFlow(
    user: FirebaseUser?,
    isGuest: Boolean,
    onSignOutRequested: () -> Unit,
    viewModel: WalletViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentScreen by remember { mutableStateOf<WalletScreen>(WalletScreen.Deck) }
    val useMaterialTheme by viewModel.useMaterialDynamicTheme.collectAsState()
    val paletteTheme by viewModel.selectedPaletteTheme.collectAsState()

    MyApplicationTheme(darkTheme = true, dynamicColor = useMaterialTheme, palette = paletteTheme) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                is WalletScreen.Deck -> {
                    WalletDeckScreen(
                        viewModel = viewModel,
                        onCardClick = { card -> currentScreen = WalletScreen.Detail(card) },
                        onNavigateScan = { currentScreen = WalletScreen.Scan },
                        onNavigateAiFinder = { currentScreen = WalletScreen.AiFinder },
                        onNavigateContactsImport = { currentScreen = WalletScreen.ContactsImport },
                        onNavigateSettings = { currentScreen = WalletScreen.Settings },
                        onSignOut = {
                            signOutUser(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignOutRequested,
                                scope = coroutineScope
                            )
                        }
                    )
                }
                is WalletScreen.Detail -> {
                    CardDetailScreen(
                        card = screen.card,
                        viewModel = viewModel,
                        onBack = { currentScreen = WalletScreen.Deck }
                    )
                }
                is WalletScreen.Scan -> {
                    ScanCardScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = WalletScreen.Deck },
                        onSaved = { currentScreen = WalletScreen.Deck }
                    )
                }
                is WalletScreen.AiFinder -> {
                    AiFinderScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = WalletScreen.Deck },
                        onCardClick = { card -> currentScreen = WalletScreen.Detail(card) }
                    )
                }
                is WalletScreen.ContactsImport -> {
                    ContactsImportScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = WalletScreen.Deck },
                        onCardImported = { currentScreen = WalletScreen.Deck }
                    )
                }
                is WalletScreen.Settings -> {
                    com.example.ui.screens.SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = WalletScreen.Deck },
                        onSignOut = {
                            signOutUser(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignOutRequested,
                                scope = coroutineScope
                            )
                        }
                    )
                }
            }
        }
    }
}
