package com.harshkanjariya.autohome.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.harshkanjariya.autohome.R

@Composable
fun MainDrawer(navController: NavController, currentRoute: String, onLogout: () -> Unit, closeDrawer: () -> Unit) {
    val drawerItems = listOf(
        NavigationItem(
            title = "Home",
            route = "devicesList",
            icon = R.drawable.baseline_home_24
        ),
        NavigationItem(
            title = "Setup New Device",
            route = "new_device",
            icon = R.drawable.baseline_electrical_services_24
        )
    )

    ModalDrawerSheet(Modifier.fillMaxWidth(.8f)) {
        Text(
            text = "Navigation",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        Divider()

        drawerItems.forEach {
            NavigationDrawerItem(
                label = { Text(text = it.title) },
                selected = it.route === currentRoute,
                onClick = {
                    navController.navigate(it.route) {
                        closeDrawer()
                    }
                },
                icon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = it.icon),
                        contentDescription = null
                    )
                },
                shape = MaterialTheme.shapes.small,
            )
        }

        Divider()  // Optional: Add a divider before the logout item

        // Logout Item
        NavigationDrawerItem(
            label = { Text(text = "Logout") },
            selected = false,
            onClick = { onLogout() },
            icon = {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.baseline_logout_24), // Replace with your logout icon
                    contentDescription = null
                )
            },
            shape = MaterialTheme.shapes.small,
        )
    }
}

data class NavigationItem(
    val title: String,
    val route: String,
    val icon: Int,
)