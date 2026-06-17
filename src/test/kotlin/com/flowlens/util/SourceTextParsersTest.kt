package com.flowlens.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceTextParsersTest {
    @Test
    fun `extracts compose route declarations`() {
        val route = SourceTextParsers.extractRouteDeclaration("""composable(route = "login") { LoginScreen() }""")
        assertEquals("login", route)
    }

    @Test
    fun `extracts start destination`() {
        val start = SourceTextParsers.extractStartDestination(
            """NavHost(navController = navController, startDestination = "home") { }""",
        )
        assertEquals("home", start)
    }

    @Test
    fun `extracts intent target from kotlin class literal`() {
        val target = SourceTextParsers.extractIntentTarget(
            """startActivity(Intent(this, HomeActivity::class.java))""",
        )
        assertEquals("HomeActivity", target)
    }

    @Test
    fun `finds deep links and view models`() {
        val deepLinks = SourceTextParsers.extractDeepLinks(
            """composable("profile", deepLinks = listOf(navDeepLink { uriPattern = "app://profile" })) { }""",
        )
        val viewModels = SourceTextParsers.extractViewModels("val vm: ProfileViewModel = hiltViewModel()")
        assertTrue("app://profile" in deepLinks)
        assertTrue("ProfileViewModel" in viewModels)
    }
}

