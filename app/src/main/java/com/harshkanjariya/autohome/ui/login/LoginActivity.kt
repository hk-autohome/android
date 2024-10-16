package com.harshkanjariya.autohome.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.harshkanjariya.autohome.MainActivity
import com.harshkanjariya.autohome.R
import com.harshkanjariya.autohome.api.getAuthToken
import com.harshkanjariya.autohome.ui.theme.AutoHomeTheme
import com.harshkanjariya.autohome.utils.DataStoreKeys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    companion object {
        const val TAG = "LoginActivity"
    }

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getCredentials(authorizedAccounts = true)

        setContent {
            AutoHomeTheme {
                LoginScreen()
            }
        }
    }

    private fun getCredentials(authorizedAccounts: Boolean) {
        val clientId = getString(R.string.oauth_client_id)

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(authorizedAccounts)
            .setServerClientId(clientId)
            .setNonce("google_sign_in")
            .build()
        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = CredentialManager.create(this@LoginActivity).getCredential(
                    request = request,
                    context = this@LoginActivity,
                )
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential error: ${e.localizedMessage}")
                e.cause?.let { cause ->
                    Log.e(TAG, "Cause of error: ${cause.localizedMessage}")
                }
                // If fetching credentials fails, allow manual sign-in
                runOnUiThread {
                    showSignInButton()
                }
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        // Call getAuthToken with the idToken
                        val idToken = googleIdTokenCredential.idToken
                        getAuthToken(idToken)?.let { token ->
                            saveTokenToDataStore(token) {
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            }
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                    }
                } else {
                    Log.e(TAG, "Unexpected type of credential")
                }
            }
            else -> {
                Log.e(TAG, "Unexpected type of credential")
            }
        }
    }

    private fun saveTokenToDataStore(token: String, onSuccess: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.edit { preferences ->
                preferences[DataStoreKeys.TOKEN] = token
            }
            onSuccess()
        }
    }

    @Composable
    fun LoginScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                getCredentials(authorizedAccounts = false)
            }) {
                Text(stringResource(R.string.sign_in_with_google))
            }
        }
    }

    private fun showSignInButton() {
        // Logic to display the sign-in button if fetching credentials fails
        setContent {
            AutoHomeTheme {
                LoginScreen() // You can add logic to indicate an error or failure here
            }
        }
    }
}
