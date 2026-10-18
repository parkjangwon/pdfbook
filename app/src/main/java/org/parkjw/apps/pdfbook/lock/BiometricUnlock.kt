package org.parkjw.apps.pdfbook.lock

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val BIOMETRIC_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

fun canAuthenticateBiometric(context: Context): Boolean = runCatching {
    val manager = context.getSystemService(BiometricManager::class.java) ?: return false
    manager.canAuthenticate(BIOMETRIC_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
}.getOrDefault(false)

internal suspend fun awaitBiometricPrompt(
    activity: ComponentActivity,
    session: AppLockState,
    title: String,
    negative: String,
    onSuccess: () -> Unit,
    onUsePin: () -> Unit,
) {
    if (!canAuthenticateBiometric(activity)) {
        onUsePin()
        return
    }
    session.cancelPrompt()
    val generation = session.nextPromptGeneration()
    val signal = CancellationSignal()
    session.bindSignal(signal)
    suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { session.cancelIfCurrent(signal) }
        val executor = activity.mainExecutor
        val launched = runCatching {
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle(title)
                .setAllowedAuthenticators(BIOMETRIC_AUTHENTICATORS)
                .setNegativeButton(negative, executor) { _, _ ->
                    if (session.isPromptCurrent(generation)) onUsePin()
                    if (cont.isActive) cont.resume(Unit)
                }
                .build()
            prompt.authenticate(
                signal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (session.isPromptCurrent(generation)) onSuccess()
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (session.isPromptCurrent(generation) &&
                            errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED &&
                            errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
                        ) {
                            onUsePin()
                        }
                        if (cont.isActive) cont.resume(Unit)
                    }
                },
            )
        }
        if (launched.isFailure) {
            if (session.isPromptCurrent(generation)) onUsePin()
            if (cont.isActive) cont.resume(Unit)
        }
    }
}
