package com.harshkanjariya.autohome.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
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

    private lateinit var googleSignInClient: GoogleSignInClient

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val clientId = getString(R.string.oauth_client_id)

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
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
                Log.e(TAG, "onCreate: $result")
            } catch (e: GetCredentialException) {
                Log.e(TAG, "onCreate: $e")
            }
        }

//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestEmail()
//            .requestIdToken(clientId)
//            .build()
//
//        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            AutoHomeTheme {
                LoginScreen()
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.e("asdf", "ResultCode: ${result.resultCode}, Intent: ${result.data}")
        val intentData = result.data?.extras?.keySet()?.joinToString(", ") { key ->
            "$key -> ${result.data?.extras?.get(key)}"
        } ?: "No intent data"
        Log.e("asdf", "Intent Data: $intentData")

        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.e("TAG", ": google sign in account $account")
                CoroutineScope(Dispatchers.IO).launch {
                    getAuthToken()?.let {
                        if (it.isNotEmpty()) {
                            saveTokenToDataStore(it) {
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            }
                        }
                    }
                }
            } catch (e: ApiException) {
                Log.w("LoginActivity", "Google sign in failed", e)
            }
        } else {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                Log.e("TAG", ": google sign in account $account")
            } catch (e: ApiException) {
                Log.e("LoginActivity", "SignIn Result: Failed. Status: ${e.statusCode}")
                e.printStackTrace()
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
            Button(onClick = { signInWithGoogle() }) {
                Text(stringResource(R.string.sign_in_with_google))
            }
        }
    }
}
