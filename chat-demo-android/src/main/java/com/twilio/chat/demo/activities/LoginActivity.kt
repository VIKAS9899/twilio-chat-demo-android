package com.twilio.chat.demo.activities

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.twilio.chat.ChatClient
import com.twilio.chat.demo.BasicChatClient.LoginListener
import com.twilio.chat.demo.R
import com.twilio.chat.demo.BuildConfig
import android.net.Uri
import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.preference.PreferenceManager
import com.twilio.chat.demo.TwilioApplication
import com.twilio.chat.demo.services.RegistrationIntentService
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.*

import timber.log.Timber

class LoginActivity : Activity(), LoginListener {
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val userName = sharedPreferences.getString("userName", DEFAULT_CLIENT_NAME)

        clientNameTextBox.setText(userName)

        login.setOnClickListener {
            val idChosen = clientNameTextBox.text.toString()
            sharedPreferences.edit().putString("userName", idChosen).apply()

            val url = Uri.parse(BuildConfig.ACCESS_TOKEN_SERVICE_URL)
                    .buildUpon()
                    .appendQueryParameter("identity", idChosen)
                    .build()
                    .toString()
            Timber.d("url string : " + url)
            TwilioApplication.instance.basicClient.login(idChosen, url, this@LoginActivity)
        }

        if (checkPlayServices()) {
            fcmAvailable.isChecked = true
            // Start IntentService to register this application with GCM.
            startService<RegistrationIntentService>()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.login, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_about) {
            showAboutDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAboutDialog() {
        alert("Version: ${ChatClient.getSdkVersion()}", "About") {
            positiveButton("OK") { dialog -> dialog.cancel() }
        }.show()
    }

    override fun onLoginStarted() {
        Timber.d("Log in started")
        progressDialog = ProgressDialog.show(this, "", "Logging in. Please wait...", true)
    }

    override fun onLoginFinished() {
        progressDialog?.dismiss()
        startActivity<ChannelActivity>()
    }

    override fun onLoginError(errorMessage: String) {
        progressDialog?.dismiss()
        TwilioApplication.instance.showToast("Error logging in : " + errorMessage, Toast.LENGTH_LONG)
    }

    override fun onLogoutFinished() {
        TwilioApplication.instance.showToast("Log out finished")
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show()
            } else {
                Timber.i("This device is not supported.")
                finish()
            }
            return false
        }
        return true
    }

    companion object {
        private val DEFAULT_CLIENT_NAME = "TestUser"
        private val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
    }
}
