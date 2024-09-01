package com.example.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.final_project.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainAppActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tokenManager = TokenManager(this)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        handleInitialNavigation()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.visibility = when (destination.id) {
                R.id.appFragment, R.id.loginFragment, R.id.registerFragment -> View.GONE
                else -> View.VISIBLE
            }
        }
    }

    private fun handleInitialNavigation() {
        if (tokenManager.isTokenPresent()) {
            navController.navigate(R.id.profileFragment)
        } else {
            navController.navigate(R.id.appFragment)
        }
    }

    fun handleLogout() {
        tokenManager.clearToken()
        // Clear any other app state here if necessary
        navController.navigate(R.id.appFragment)
        navController.popBackStack(R.id.appFragment, false)
    }
}