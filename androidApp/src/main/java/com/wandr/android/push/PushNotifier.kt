package com.wandr.android.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.wandr.android.MainActivity
import com.wandr.android.R
import com.wandr.domain.push.PushTokenManager
import org.koin.core.context.GlobalContext

/** Channel, token hand-over and local display of push notifications. All Firebase access is guarded: no config, no crash. */
object PushNotifier {
    const val CHANNEL_SOCIAL = "social"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_SOCIAL,
            context.getString(R.string.push_channel_social_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.push_channel_social_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun isFirebaseAvailable(context: Context): Boolean =
        runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)

    /** Fetches the current FCM token and hands it to the shared token manager. */
    fun fetchToken(context: Context) {
        if (!isFirebaseAvailable(context)) return
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { onToken(it) }
        }
    }

    fun onToken(token: String) {
        runCatching {
            GlobalContext.get().get<PushTokenManager>().onTokenReceived(token, PushTokenManager.PLATFORM_ANDROID)
        }
    }

    fun show(context: Context, data: Map<String, String>, fallbackTitle: String?, fallbackBody: String?) {
        val content = PushPayload.content(data, fallbackTitle, fallbackBody)
        val title = if (content.kind != null) context.getString(R.string.push_title)
        else content.fallbackTitle ?: context.getString(R.string.push_title)
        val body = bodyText(context, content) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val tap = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data["entity_type"]?.let { putExtra(PushExtras.ENTITY_TYPE, it) }
            data["entity_id"]?.let { putExtra(PushExtras.ENTITY_ID, it) }
        }
        val id = (data["notification_id"] ?: System.nanoTime().toString()).hashCode()
        val pending = PendingIntent.getActivity(
            context, id, tap, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    private fun bodyText(context: Context, c: PushContent): String? {
        val kind = c.kind ?: return c.fallbackBody
        val actor = c.actorName
        val base = when (kind) {
            PushBodyKind.LikeActivity -> context.getString(R.string.push_like_activity, actor)
            PushBodyKind.LikeChallenge -> context.getString(R.string.push_like_challenge, actor)
            PushBodyKind.CommentActivity -> context.getString(R.string.push_comment_activity, actor)
            PushBodyKind.CommentChallenge -> context.getString(R.string.push_comment_challenge, actor)
            PushBodyKind.ReactionComment -> context.getString(R.string.push_reaction_comment, actor, c.emoji.orEmpty())
                .replace("  ", " ")
        }
        val preview = c.preview
        return if (preview != null && (kind == PushBodyKind.CommentActivity || kind == PushBodyKind.CommentChallenge)) "$base: $preview" else base
    }
}
