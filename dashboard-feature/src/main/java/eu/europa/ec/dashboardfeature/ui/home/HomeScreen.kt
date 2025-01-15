/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.dashboardfeature.ui.home

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIconAndText
import eu.europa.ec.uilogic.component.AppIconAndTextData
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ModalOptionUi
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SIZE_XX_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.ActionCardConfig
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextData
import eu.europa.ec.uilogic.component.wrap.BottomSheetWithTwoBigIcons
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.GenericBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapActionCard
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapIconButton
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.extension.openAppSettings
import eu.europa.ec.uilogic.extension.openBleSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

typealias DashboardEvent = eu.europa.ec.dashboardfeature.ui.dashboard_new.Event
typealias ShowSideMenuEvent = eu.europa.ec.dashboardfeature.ui.dashboard_new.Event.SideMenu.Show

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navHostController: NavController,
    viewModel: HomeViewModel,
    onDashboardEventSent: (DashboardEvent) -> Unit
) {
    val state = viewModel.viewState.value
    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.NONE,
        topBar = {
            TopBar(
                onEventSent = onDashboardEventSent
            )
        }
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSent = { event ->
                viewModel.setEvent(event)
            },
            onNavigationRequested = {
                handleNavigationEffect(it, navHostController, navHostController.context)
            },
            coroutineScope = scope,
            modalBottomSheetState = bottomSheetState,
            paddingValues = paddingValues
        )
    }

    if (isBottomSheetOpen) {
        WrapModalBottomSheet(
            onDismissRequest = {
                viewModel.setEvent(
                    Event.BottomSheet.UpdateBottomSheetState(
                        isOpen = false
                    )
                )
            },
            sheetState = bottomSheetState
        ) {
            HomeScreenSheetContent(
                sheetContent = state.sheetContent,
                onEventSent = { event -> viewModel.setEvent(event) },
                modalBottomSheetState = bottomSheetState
            )
        }
    }

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.Init)
    }
}

@Composable
private fun TopBar(
    onEventSent: (DashboardEvent) -> Unit
) {
    Row(
        modifier = Modifier
            .height(SIZE_XX_LARGE.dp)
            .fillMaxSize()
            .padding(SPACING_MEDIUM.dp),
        Arrangement.SpaceBetween
    ) {
        // home menu icon
        WrapIconButton(
            modifier = Modifier.offset(x = -SPACING_SMALL.dp),
            iconData = AppIcons.Menu,
            shape = null
        ) {
            onEventSent(ShowSideMenuEvent)
        }

        // wallet logo
        AppIconAndText(appIconAndTextData = AppIconAndTextData())

        HSpacer.ExtraLarge()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSent: ((event: Event) -> Unit),
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit,
    coroutineScope: CoroutineScope,
    modalBottomSheetState: SheetState,
    paddingValues: PaddingValues
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        VSpacer.Small()

        ContentTitle(
            title = state.welcomeUserMessage,
            titleStyle = MaterialTheme.typography.headlineMedium
        )

        WrapActionCard(
            config = state.authenticateCardConfig,
            onActionClick = {
                onEventSent(
                    Event.AuthenticateCard.AuthenticatePressed
                )
            },
            onLearnMoreClick = {
                onEventSent(
                    Event.AuthenticateCard.LearnMorePressed
                )
            }
        )

        WrapActionCard(
            config = state.signCardConfig,
            onActionClick = {
                onEventSent(
                    Event.SignDocumentCard.SignDocumentPressed
                )
            },
            onLearnMoreClick = {
                onEventSent(
                    Event.SignDocumentCard.LearnMorePressed
                )
            }
        )
    }

    if (state.bleAvailability == BleAvailability.NO_PERMISSION) {
        RequiredPermissionsAsk(state, onEventSent)
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)

                is Effect.CloseBottomSheet -> {
                    coroutineScope.launch {
                        if (effect.hasNextBottomSheet.not()) {
                            modalBottomSheetState.hide()
                        } else {
                            modalBottomSheetState.hide().also {
                                modalBottomSheetState.show()
                                onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                            }
                        }
                    }.invokeOnCompletion {
                        if (!modalBottomSheetState.isVisible) {
                            onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                        }
                    }
                }

                is Effect.ShowBottomSheet -> {
                    onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                }
            }
        }.collect()
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController,
    context: Context
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                popUpTo(navigationEffect.popUpToScreenRoute) {
                    inclusive = navigationEffect.inclusive
                }
            }
        }

        is Effect.Navigation.OnAppSettings -> context.openAppSettings()
        is Effect.Navigation.OnSystemSettings -> context.openBleSettings()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenSheetContent(
    sheetContent: HomeScreenBottomSheetContent,
    onEventSent: (event: Event) -> Unit,
    modalBottomSheetState: SheetState
) {
    when (sheetContent) {
        is HomeScreenBottomSheetContent.Authenticate -> {
            WrapModalBottomSheet(
                onDismissRequest = {
                    onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                },
                sheetState = modalBottomSheetState
            ) {
                BottomSheetWithTwoBigIcons(
                    textData = BottomSheetTextData(
                        title = stringResource(R.string.home_screen_authenticate),
                        message = stringResource(R.string.home_screen_authenticate_description)
                    ),
                    options = listOf(
                        ModalOptionUi(
                            title = stringResource(R.string.home_screen_authenticate_option_in_person),
                            leadingIcon = AppIcons.PresentDocumentInPerson,
                            leadingIconTint = MaterialTheme.colorScheme.primary,
                            event = Event.BottomSheet.Authenticate.OpenAuthenticateInPerson,
                        ),
                        ModalOptionUi(
                            title = stringResource(R.string.home_screen_add_document_option_online),
                            leadingIcon = AppIcons.PresentDocumentOnline,
                            leadingIconTint = MaterialTheme.colorScheme.primary,
                            event = Event.BottomSheet.Authenticate.OpenAuthenticateOnLine,
                        )
                    ),
                    onEventSent = { event ->
                        onEventSent(event)
                    }
                )
            }
        }

        /**
         * Bottom sheet for Sign Document click event,
         * will be implemented in the future
         *
        is HomeScreenBottomSheetContent.Sign -> {
            WrapModalBottomSheet(
                onDismissRequest = {
                    onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                },
                sheetState = modalBottomSheetState
            ) {
                BottomSheetWithTwoBigIcons(
                    textData = BottomSheetTextData(
                        title = stringResource(R.string.home_screen_sign_document),
                        message = stringResource(R.string.home_screen_sign_document_description)
                    ),
                    options = listOf(
                        ModalOptionUi(
                            title = stringResource(R.string.home_screen_sign_document_option_from_device),
                            leadingIcon = AppIcons.SignDocumentFromDevice,
                            leadingIconTint = MaterialTheme.colorScheme.primary,
                            event = Event.BottomSheet.SignDocument.OpenDocumentFromDevice,
                        ),
                        ModalOptionUi(
                            title = stringResource(R.string.home_screen_sign_document_option_scan_qr),
                            leadingIcon = AppIcons.SignDocumentFromQr,
                            leadingIconTint = MaterialTheme.colorScheme.primary,
                            enabled = false,
                            event = Event.BottomSheet.SignDocument.OpenDocumentFromQr,
                        )
                    ),
                    onEventSent = {
                        // invoke event
                    }
                )
            }
        }*/

        is HomeScreenBottomSheetContent.LearnMoreAboutAuthenticate -> {
            WrapModalBottomSheet(
                onDismissRequest = {
                    onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                },
                sheetState = modalBottomSheetState
            ) {
                GenericBottomSheet(
                    titleContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WrapIcon(
                                iconData = AppIcons.Info,
                                customTint = MaterialTheme.colorScheme.primary
                            )
                            HSpacer.Small()
                            Text(
                                text = stringResource(R.string.home_screen_authenticate),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                            )
                        }
                    },
                    bodyContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)) {
                            Text(
                                stringResource(R.string.home_screen_sign_learn_more_inner_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                stringResource(R.string.home_screen_sign_learn_more_description),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                )
            }
        }

        is HomeScreenBottomSheetContent.LearnMoreAboutSignDocument -> {
            WrapModalBottomSheet(
                onDismissRequest = {
                    onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                },
                sheetState = modalBottomSheetState
            ) {
                GenericBottomSheet(
                    titleContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WrapIcon(
                                iconData = AppIcons.Info,
                                customTint = MaterialTheme.colorScheme.primary
                            )
                            HSpacer.Small()
                            Text(
                                stringResource(R.string.home_screen_sign),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    },
                    bodyContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)) {
                            Text(
                                stringResource(R.string.home_screen_authenticate_learn_more_inner_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                stringResource(R.string.home_screen_authenticate_learn_more_description),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                )
            }
        }

        is HomeScreenBottomSheetContent.Bluetooth -> {
            DialogBottomSheet(
                textData = BottomSheetTextData(
                    title = stringResource(id = R.string.dashboard_bottom_sheet_bluetooth_title),
                    message = stringResource(id = R.string.dashboard_bottom_sheet_bluetooth_subtitle),
                    positiveButtonText = stringResource(id = R.string.dashboard_bottom_sheet_bluetooth_primary_button_text),
                    negativeButtonText = stringResource(id = R.string.dashboard_bottom_sheet_bluetooth_secondary_button_text),
                ),
                onPositiveClick = {
                    onEventSent(
                        Event.BottomSheet.Bluetooth.PrimaryButtonPressed(
                            sheetContent.availability
                        )
                    )
                },
                onNegativeClick = { onEventSent(Event.BottomSheet.Bluetooth.SecondaryButtonPressed) }
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequiredPermissionsAsk(
    state: State,
    onEventSend: (Event) -> Unit
) {
    val permissions: MutableList<String> = mutableListOf()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 && state.isBleCentralClientModeEnabled) {
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = permissions)

    when {
        permissionsState.allPermissionsGranted -> onEventSend(Event.StartProximityFlow)
        !permissionsState.allPermissionsGranted && permissionsState.shouldShowRationale -> {
            onEventSend(Event.OnShowPermissionsRational)
        }

        else -> {
            onEventSend(Event.OnPermissionStateChanged(BleAvailability.UNKNOWN))
            LaunchedEffect(Unit) {
                permissionsState.launchMultiplePermissionRequest()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun HomeScreenContentPreview() {
    PreviewTheme {
        Content(
            state = State(
                isBottomSheetOpen = false,
                welcomeUserMessage = "Welcome back, Alex",
                authenticateCardConfig = ActionCardConfig(
                    title = "Authenticate, authorise transactions and share your digital documents in person or online.",
                    icon = AppIcons.WalletActivated,
                    primaryButtonText = "Authenticate",
                    secondaryButtonText = "Learn more",
                ),
                signCardConfig = ActionCardConfig(
                    title = "Sign, authorise transactions and share your digital documents in person or online.",
                    icon = AppIcons.Contract,
                    primaryButtonText = "Sign",
                    secondaryButtonText = "Learn more",
                )

            ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onNavigationRequested = {},
            coroutineScope = rememberCoroutineScope(),
            modalBottomSheetState = rememberModalBottomSheetState(),
            onEventSent = {},
            paddingValues = PaddingValues(SPACING_MEDIUM.dp)
        )
    }
}