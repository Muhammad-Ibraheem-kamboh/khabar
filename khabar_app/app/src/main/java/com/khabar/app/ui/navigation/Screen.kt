package com.khabar.app.ui.navigation

sealed class Screen(val route: String) {
    // Onboarding
    object OnboardingWelcome : Screen("onboarding_welcome")
    object OnboardingLanguage : Screen("onboarding_language")
    object OnboardingProgram : Screen("onboarding_program")
    object OnboardingJoinDate : Screen("onboarding_join_date")
    object OnboardingCampus : Screen("onboarding_campus")
    object OnboardingCourses : Screen("onboarding_courses")
    object OnboardingInterests : Screen("onboarding_interests")
    object OnboardingConnectMail : Screen("onboarding_connect_mail")
    object OnboardingPermissions : Screen("onboarding_permissions")

    // Main Tabs
    object Main : Screen("main")

    // Other screens
    object MailDetail : Screen("mail_detail/{mailId}") {
        fun createRoute(mailId: String) = "mail_detail/$mailId"
    }
    object AlertSounds : Screen("alert_sounds")
    object AiSettings : Screen("ai_settings")
    object EditProfile : Screen("edit_profile")
    object DeveloperTools : Screen("developer_tools")
}
