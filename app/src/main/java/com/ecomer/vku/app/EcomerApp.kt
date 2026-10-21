package com.ecomer.vku.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.ecomer.vku.app.data.FirebaseExt

/**
 * Application cua app ECOMER.
 *
 * Khoi tao Firebase neu google-services.json co mat. Neu khong co (hoac sai),
 * app van chay duoc voi du lieu mau nen khong bao gio crash ngay luc mo.
 */
class EcomerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseExt.available = runCatching {
            FirebaseApp.initializeApp(this)
        }.isSuccess
    }
}
