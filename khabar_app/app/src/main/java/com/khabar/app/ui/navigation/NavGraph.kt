package com.khabar.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.khabar.app.di.AppContainer
import com.khabar.app.ui.components.FloatingTabBar
import com.khabar.app.ui.components.MainTab
import com.khabar.app.ui.mails.MailDetailScreen
import com.khabar.app.ui.mails.MailDetailViewModel
import com.khabar.app.ui.mails.MailsScreen
import com.khabar.app.ui.mails.MailsViewModel
import com.khabar.app.ui.onboarding.OnboardingViewModel
import com.khabar.app.ui.onboarding.Screen1Welcome
import com.khabar.app.ui.onboarding.Screen2Language
import com.khabar.app.ui.onboarding.Screen3Program
import com.khabar.app.ui.onboarding.Screen3bJoinDate
import com.khabar.app.ui.onboarding.Screen4CampusMajor
import com.khabar.app.ui.onboarding.Screen5Courses
import com.khabar.app.ui.onboarding.Screen6Interests
import com.khabar.app.ui.onboarding.Screen7ConnectMail
import com.khabar.app.ui.onboarding.Screen8Permissions
import com.khabar.app.ui.reminders.RemindersScreen
import com.khabar.app.ui.reminders.RemindersViewModel
import com.khabar.app.ui.settings.AiSettingsScreen
import com.khabar.app.ui.settings.AlertSoundsScreen
import com.khabar.app.ui.settings.DeveloperToolsScreen
import com.khabar.app.ui.settings.EditProfileScreen
import com.khabar.app.ui.settings.SettingsScreen
import com.khabar.app.ui.settings.SettingsViewModel
import com.khabar.app.ui.today.TodayScreen
import com.khabar.app.ui.today.TodayViewModel

@Composable
fun NavGraph(
    appContainer: AppContainer,
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val onboardingViewModel = remember {
        OnboardingViewModel(
            context = context,
            profileRepository = appContainer.profileRepository,
            mailRepository = appContainer.mailRepository,
            userPreferences = appContainer.userPreferences
        )
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        // ONBOARDING 1-8
        composable(Screen.OnboardingWelcome.route) {
            Screen1Welcome(
                onGetStarted = { navController.navigate(Screen.OnboardingLanguage.route) }
            )
        }

        composable(Screen.OnboardingLanguage.route) {
            val state by onboardingViewModel.uiState.collectAsState()
            Screen2Language(
                selectedLanguage = state.summaryLanguage,
                onLanguageSelected = { onboardingViewModel.setSummaryLanguage(it) },
                onContinue = { navController.navigate(Screen.OnboardingProgram.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OnboardingProgram.route) {
            val state by onboardingViewModel.uiState.collectAsState()
            Screen3Program(
                programGroups = state.programGroups,
                selectedProgram = state.selectedProgram,
                onProgramSelected = { onboardingViewModel.selectProgram(it) },
                onContinue = { navController.navigate(Screen.OnboardingJoinDate.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OnboardingJoinDate.route) {
            val state by onboardingViewModel.uiState.collectAsState()
            Screen3bJoinDate(
                selectedProgram = state.selectedProgram,
                joinTerm = state.joinTerm,
                joinYear = state.joinYear,
                semesterIndex = state.semesterIndex,
                standing = state.standing,
                onJoinTermAndYearChanged = { term, year -> onboardingViewModel.setJoinTermAndYear(term, year) },
                onSemesterOverridden = { onboardingViewModel.overrideSemester(it) },
                onContinue = { navController.navigate(Screen.OnboardingCampus.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OnboardingCampus.route) {
            val state by onboardingViewModel.uiState.collectAsState()
            Screen4CampusMajor(
                selectedCampus = state.campus,
                selectedProgram = state.selectedProgram,
                mbaShift = state.mbaShift,
                mbaExecBatch = state.mbaExecBatch,
                selectedMajor = state.major,
                onCampusSelected = { onboardingViewModel.setCampus(it) },
                onMbaShiftSelected = { onboardingViewModel.setMbaShift(it) },
                onMbaExecBatchChanged = { onboardingViewModel.setMbaExecBatch(it) },
                onMajorSelected = { onboardingViewModel.setMajor(it) },
                onContinue = { navController.navigate(Screen.OnboardingCourses.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OnboardingCourses.route) {
            val state by onboardingViewModel.uiState.collectAsState()
            Screen5Courses(
                courses = state.courses,
                isOcrScanning = state.isOcrScanning,
                scannedCourses = state.ocrScannedCourses,
                onAddCourse = { code, sec, name, teacher, email -> onboardingViewModel.addCourse(code, sec, name, teacher, email) },
                onRemoveCourse = { onboardingViewModel.removeCourse(it) },
                onScreenshotPicked = { onboardingViewModel.scanScreenshot(it) },
                onApplyScannedCourses = { onboardingViewModel.applyScannedCourses(it) },
                onContinue = { navController.navigate(Screen.OnboardingInterests.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OnboardingInterests.route) {
            val state by onboardingViewModel.uiState.collectAsState()
            Screen6Interests(
                interests = state.interests,
                onToggleInterest = { key, enabled -> onboardingViewModel.toggleInterest(key, enabled) },
                onContinue = { navController.navigate(Screen.OnboardingConnectMail.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OnboardingConnectMail.route) {
            Screen7ConnectMail(
                onContinueToPermissions = { navController.navigate(Screen.OnboardingPermissions.route) },
                onLoadSampleMailsAndFinish = {
                    onboardingViewModel.loadSampleDataAndProceed {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OnboardingPermissions.route) {
            Screen8Permissions(
                onFinished = {
                    onboardingViewModel.finishOnboarding {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // MAIN TABS
        composable(Screen.Main.route) {
            // rememberSaveable ensures tab state survives child route push/pop and configuration changes
            var selectedTab by rememberSaveable { mutableStateOf(MainTab.TODAY) }

            // Standard Android behavior: Back press on any non-primary tab returns to TODAY tab
            BackHandler(enabled = selectedTab != MainTab.TODAY) {
                selectedTab = MainTab.TODAY
            }

            val todayViewModel = remember {
                TodayViewModel(
                    mailRepository = appContainer.mailRepository,
                    profileRepository = appContainer.profileRepository,
                    reminderRepository = appContainer.reminderRepository
                )
            }
            val mailsViewModel = remember {
                MailsViewModel(mailRepository = appContainer.mailRepository)
            }
            val remindersViewModel = remember {
                RemindersViewModel(reminderRepository = appContainer.reminderRepository)
            }
            val settingsViewModel = remember {
                SettingsViewModel(
                    profileRepository = appContainer.profileRepository,
                    mailRepository = appContainer.mailRepository,
                    userPreferences = appContainer.userPreferences,
                    secretManager = appContainer.secretManager
                )
            }

            val unreadCount by appContainer.mailRepository.unreadCountFlow.collectAsState(initial = 0)

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    MainTab.TODAY -> {
                        TodayScreen(
                            viewModel = todayViewModel,
                            onMailClick = { mailId ->
                                navController.navigate(Screen.MailDetail.createRoute(mailId))
                            },
                            onNavigateToMails = {
                                selectedTab = MainTab.MAILS
                            }
                        )
                    }
                    MainTab.MAILS -> {
                        MailsScreen(
                            viewModel = mailsViewModel,
                            onMailClick = { mailId ->
                                navController.navigate(Screen.MailDetail.createRoute(mailId))
                            }
                        )
                    }
                    MainTab.REMINDERS -> {
                        RemindersScreen(
                            viewModel = remindersViewModel
                        )
                    }
                    MainTab.SETTINGS -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateToEditProfile = {
                                navController.navigate(Screen.EditProfile.route)
                            },
                            onNavigateToAlertSounds = {
                                navController.navigate(Screen.AlertSounds.route)
                            },
                            onNavigateToAiSettings = {
                                navController.navigate(Screen.AiSettings.route)
                            },
                            onNavigateToDevTools = {
                                navController.navigate(Screen.DeveloperTools.route)
                            },
                            onDataDeleted = {
                                navController.navigate(Screen.OnboardingWelcome.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }

                FloatingTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    unreadMailsCount = unreadCount,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // MAIL DETAIL
        composable(
            route = Screen.MailDetail.route,
            arguments = listOf(navArgument("mailId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mailId = backStackEntry.arguments?.getString("mailId") ?: ""
            val detailViewModel = remember(mailId) {
                MailDetailViewModel(
                    mailId = mailId,
                    mailRepository = appContainer.mailRepository,
                    userPreferences = appContainer.userPreferences,
                    reminderDao = appContainer.database.reminderDao(),
                    aiSummarizer = appContainer.aiSummarizer,
                    secretManager = appContainer.secretManager
                )
            }
            MailDetailScreen(
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // SETTINGS SUB-SCREENS
        composable(Screen.AlertSounds.route) {
            AlertSoundsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AiSettings.route) {
            AiSettingsScreen(
                profileRepository = appContainer.profileRepository,
                secretManager = appContainer.secretManager,
                openAiClient = appContainer.openAiClient,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                profileRepository = appContainer.profileRepository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DeveloperTools.route) {
            DeveloperToolsScreen(
                demoMailSource = appContainer.demoMailSource,
                mailRepository = appContainer.mailRepository,
                profileRepository = appContainer.profileRepository,
                notificationManager = appContainer.notificationManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
