package com.hnimrod.biometricexample

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.hnimrod.biometricexample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var biometricPrompt: BiometricPrompt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupViews()
        setupBiometricPrompt()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val uri = "https://developer.android.com/training/sign-in/biometric-auth".toUri()
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun isBiometricHardWareAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                BiometricManager.BIOMETRIC_SUCCESS,
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> true
                else -> false
            }
        } else {
            when (biometricManager.canAuthenticate()) {
                BiometricManager.BIOMETRIC_SUCCESS -> true
                else -> false
            }
        }
    }

    private val canUseFingerprintUnlock: Boolean
        get() = packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) && isBiometricHardWareAvailable()

    private val canUseFaceUnlock: Boolean?
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)
            else null

    private val canUseIrisUnlock: Boolean?
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)
            else null

    private fun setupViews() {
        val availabilityText =
            if (isBiometricHardWareAvailable()) getText(R.string.biometric_authentication_text_available)
            else getString(R.string.biometric_authentication_text_disable)
        binding.biometricHardwareAvailability.text = getString(R.string.biometric_authentication_text_format, availabilityText)

        binding.fab.isVisible = isBiometricHardWareAvailable()
        setBiometricAvailability(false)

        if (isBiometricHardWareAvailable().not()) {
            binding.fingerprint.text = getString(R.string.has_no)
            binding.face.text = getString(R.string.has_no)
            binding.iris.text = getString(R.string.has_no)
            return
        }

        binding.fingerprint.text = if (canUseFingerprintUnlock) getString(R.string.has_yes) else getString(R.string.has_no)
        binding.face.text = when(canUseFaceUnlock) {
            true -> getString(R.string.has_yes)
            false -> getString(R.string.has_no)
            else -> getString(R.string.has_undetermined)
        }
        binding.iris.text = when(canUseIrisUnlock) {
            true -> getString(R.string.has_yes)
            false -> getString(R.string.has_no)
            else -> getString(R.string.has_undetermined)
        }
    }

    private fun setupBiometricPrompt() {
        if (isBiometricHardWareAvailable().not()) {
            return
        }

        biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    setBiometricAvailability(false)
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    setBiometricAvailability(true)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    setBiometricAvailability(false)
                }
            })

        val title = getString(R.string.biometric_prompt_title)
        val subtitle = getString(R.string.biometric_prompt_subtitle)
        val description = getString(R.string.biometric_prompt_description)
        val promptInfo: BiometricPrompt.PromptInfo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
        } else {
            @Suppress("DEPRECATION")
            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setDeviceCredentialAllowed(true)
                .build()
        }

        binding.fab.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun setBiometricAvailability(available: Boolean) {
        val imageRes =
            if (available) R.drawable.baseline_lock_open_24
            else R.drawable.baseline_lock_24
        binding.lockImage.setImageResource(imageRes)
    }
}