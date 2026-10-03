package com.wandr.android.ui.profile.components

import android.content.Context
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Scans a QR code with Google's code scanner (a system UI: no camera permission is needed for the app).
 * [onScanned] gets the raw content; [onFailed] is not called when the user just cancelled.
 */
internal fun scanInviteQr(context: Context, onScanned: (String) -> Unit, onFailed: () -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .enableAutoZoom()
        .build()
    GmsBarcodeScanning.getClient(context, options).startScan()
        .addOnSuccessListener { barcode -> barcode.rawValue?.let(onScanned) ?: onFailed() }
        .addOnFailureListener { error ->
            if ((error as? MlKitException)?.errorCode != MlKitException.CODE_SCANNER_CANCELLED) onFailed()
        }
}
