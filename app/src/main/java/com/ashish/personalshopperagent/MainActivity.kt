package com.ashish.personalshopperagent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ashish.personalshopperagent.ui.screens.HomeScreen
import com.ashish.personalshopperagent.ui.screens.ResultScreen
import com.ashish.personalshopperagent.ui.screens.SearchingScreen
import com.ashish.personalshopperagent.ui.theme.PersonalShopperTheme
import com.ashish.personalshopperagent.viewmodel.ShopperViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonalShopperTheme {
                val nav = rememberNavController()
                val vm: ShopperViewModel = viewModel()
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(onSearch = { query ->
                            vm.search(query)
                            nav.navigate("searching")
                        })
                    }
                    composable("searching") {
                        SearchingScreen(
                            viewModel = vm,
                            onResultReady = { nav.navigate("result") },
                            onBack = { vm.reset(); nav.popBackStack() }
                        )
                    }
                    composable("result") {
                        ResultScreen(
                            viewModel = vm,
                            onBack = {
                                vm.reset()
                                nav.navigate("home") { popUpTo("home") { inclusive = true } }
                            }
                        )
                    }
                }
            }
        }
    }
}
