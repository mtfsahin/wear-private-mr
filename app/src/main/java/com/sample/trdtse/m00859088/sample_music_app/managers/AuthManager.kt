package com.sample.trdtse.m00859088.sample_music_app.managers

import android.content.Context
import android.util.Log
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.AuthClient
import com.huawei.wearengine.auth.Permission
import com.huawei.wearengine.WearEngineException

class AuthManager(context: Context) {

    private val authClient: AuthClient = HiWear.getAuthClient(context)

    interface AuthCheckCallback {
        fun onResult(allPermissionsGranted: Boolean)
        fun onError(e: Exception?)
    }

    fun checkPermissions(callback: AuthCheckCallback) {
        authClient.checkPermissions(arrayOf(Permission.DEVICE_MANAGER))
            .addOnSuccessListener { permissions ->
                val granted = permissions?.all { it == true } ?: false
                Log.i(TAG, "checkPermissions granted=$granted")
                callback.onResult(granted)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "checkPermissions failed: ${describe(e)}", e)
                callback.onError(e)
            }
    }

    fun requestPermission(onSuccess: Runnable?, onCancel: Runnable?) {
        authClient.requestPermission(object : AuthCallback {
            override fun onOk(permissions: Array<Permission?>?) {
                onSuccess?.run()
            }

            override fun onCancel() {
                Log.w(TAG, "requestPermission cancelled")
                onCancel?.run()
            }
        }, Permission.DEVICE_MANAGER)
    }

    companion object {
        private const val TAG = "WearEngineAuth"

        fun describeError(e: Exception?): String = describe(e)

        fun isPermissionRefused(e: Exception?): Boolean =
            (e as? WearEngineException)?.errorCode == ERROR_CODE_PERMISSION_REFUSED

        private fun describe(e: Exception?): String {
            val code = (e as? WearEngineException)?.errorCode ?: return e?.message.orEmpty()
            val name = when (code) {
                ERROR_CODE_NO_BOUND_DEVICE ->
                    "no watch is paired in the Huawei Health app on this phone"
                ERROR_CODE_COMM_FAIL -> "the Huawei Health app refused the call"
                ERROR_CODE_P2P_WHITE_LIST_CHECK_FAIL ->
                    "package name or signing fingerprint does not match AppGallery Connect"
                else -> "see the Wear Engine error code list"
            }
            return "errorCode=$code ($name)"
        }

        private const val ERROR_CODE_NO_BOUND_DEVICE = 4
        private const val ERROR_CODE_COMM_FAIL = 6
        private const val ERROR_CODE_P2P_WHITE_LIST_CHECK_FAIL = 200540003
        private const val ERROR_CODE_PERMISSION_REFUSED = 200540004
    }
}
