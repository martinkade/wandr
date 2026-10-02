// Supabase Edge Function: send-push
// Triggered by a Database Webhook on INSERT into public.notifications.
// Sends a push to all registered devices of the recipient (FCM HTTP v1 for Android, APNs HTTP/2 for iOS)
// and removes device tokens that the services report as invalid.

import { createClient } from "npm:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const WEBHOOK_SECRET = Deno.env.get("WEBHOOK_SECRET") ?? "";
const FIREBASE_SERVICE_ACCOUNT = Deno.env.get("FIREBASE_SERVICE_ACCOUNT") ?? "";
const APNS_KEY_P8 = Deno.env.get("APNS_KEY_P8") ?? "";
const APNS_KEY_ID = Deno.env.get("APNS_KEY_ID") ?? "";
const APNS_TEAM_ID = Deno.env.get("APNS_TEAM_ID") ?? "";
const APNS_BUNDLE_ID = Deno.env.get("APNS_BUNDLE_ID") ?? "com.mediabeam.fitness";
const APNS_HOST = Deno.env.get("APNS_HOST") ?? "api.sandbox.push.apple.com"; // api.push.apple.com for production

const supabase = createClient(SUPABASE_URL, SERVICE_ROLE_KEY, {
  auth: { persistSession: false, autoRefreshToken: false },
});

type NotificationRow = {
  id: string;
  recipient_user_id: string;
  actor_user_id: string;
  notification_type: string;
  target_id: string;
  payload: Record<string, unknown> | null;
};

type DeviceToken = { id: string; token: string; platform: "android" | "ios" };

type PushMessage = {
  title: string;
  body: string;
  data: Record<string, string>; // FCM requires string values only
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function timingSafeEqual(a: string, b: string): boolean {
  const ea = new TextEncoder().encode(a);
  const eb = new TextEncoder().encode(b);
  let diff = ea.length ^ eb.length;
  const len = Math.max(ea.length, eb.length);
  for (let i = 0; i < len; i++) diff |= (ea[i] ?? 0) ^ (eb[i] ?? 0);
  return diff === 0;
}

function base64url(input: ArrayBuffer | Uint8Array | string): string {
  const bytes = typeof input === "string"
    ? new TextEncoder().encode(input)
    : input instanceof Uint8Array
    ? input
    : new Uint8Array(input);
  let bin = "";
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemToDer(pem: string): ArrayBuffer {
  const b64 = pem
    .replace(/\\n/g, "\n")
    .replace(/-----BEGIN [A-Z ]+-----/, "")
    .replace(/-----END [A-Z ]+-----/, "")
    .replace(/\s+/g, "");
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes.buffer;
}

async function signJwt(
  header: Record<string, unknown>,
  claims: Record<string, unknown>,
  pem: string,
  alg: "RS256" | "ES256",
): Promise<string> {
  const keyAlgo = alg === "RS256"
    ? { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }
    : { name: "ECDSA", namedCurve: "P-256" };
  const key = await crypto.subtle.importKey("pkcs8", pemToDer(pem), keyAlgo, false, ["sign"]);
  const signingInput = `${base64url(JSON.stringify(header))}.${base64url(JSON.stringify(claims))}`;
  const signAlgo = alg === "RS256" ? "RSASSA-PKCS1-v1_5" : { name: "ECDSA", hash: "SHA-256" };
  // ECDSA in WebCrypto returns the raw r||s signature, which is what JWT ES256 expects.
  const sig = await crypto.subtle.sign(signAlgo, key, new TextEncoder().encode(signingInput));
  return `${signingInput}.${base64url(sig)}`;
}

// ---------------------------------------------------------------------------
// Message building (generic English fallback, keys + args travel as data for client-side localization)
// ---------------------------------------------------------------------------

function buildMessage(n: NotificationRow, actorName: string): PushMessage | null {
  const p = (n.payload ?? {}) as Record<string, unknown>;
  const entityType = String(p.entity_type ?? "activity");
  const entityId = String(p.entity_id ?? n.target_id);
  const subject = entityType === "challenge" ? "challenge" : "activity";

  let bodyKey: string;
  let body: string;
  switch (n.notification_type) {
    case "like":
      bodyKey = `push.like.${subject}`;
      body = `${actorName} liked your ${subject}`;
      break;
    case "comment": {
      bodyKey = `push.comment.${subject}`;
      const preview = p.preview ? String(p.preview) : "";
      body = `${actorName} commented on your ${subject}` + (preview ? `: ${preview}` : "");
      break;
    }
    case "reaction": {
      bodyKey = "push.reaction.comment";
      const emoji = p.emoji ? String(p.emoji) : "";
      body = `${actorName} reacted ${emoji} to your comment`.replace("  ", " ");
      break;
    }
    default:
      return null; // e.g. 'invite' is not pushed yet
  }

  const data: Record<string, string> = {
    notification_id: n.id,
    type: n.notification_type,
    entity_type: entityType,
    entity_id: entityId,
    title_key: "push.title",
    body_key: bodyKey,
    actor_name: actorName,
  };
  if (p.comment_id) data.comment_id = String(p.comment_id);
  if (p.preview) data.preview = String(p.preview);
  if (p.emoji) data.emoji = String(p.emoji);

  return { title: "WANDR", body, data };
}

// ---------------------------------------------------------------------------
// FCM HTTP v1 (Android)
// ---------------------------------------------------------------------------

type ServiceAccount = { project_id: string; client_email: string; private_key: string; token_uri?: string };
let fcmAccessToken: { value: string; expiresAt: number } | null = null;

async function getFcmAccessToken(sa: ServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (fcmAccessToken && fcmAccessToken.expiresAt - 60 > now) return fcmAccessToken.value;

  const tokenUri = sa.token_uri ?? "https://oauth2.googleapis.com/token";
  const assertion = await signJwt(
    { alg: "RS256", typ: "JWT" },
    {
      iss: sa.client_email,
      scope: "https://www.googleapis.com/auth/firebase.messaging",
      aud: tokenUri,
      iat: now,
      exp: now + 3600,
    },
    sa.private_key,
    "RS256",
  );
  const res = await fetch(tokenUri, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!res.ok) throw new Error(`FCM OAuth failed: ${res.status} ${await res.text()}`);
  const json = await res.json();
  fcmAccessToken = { value: json.access_token, expiresAt: now + Number(json.expires_in ?? 3600) };
  return fcmAccessToken.value;
}

/** Returns true when the token is invalid and should be deleted. */
async function sendFcm(sa: ServiceAccount, token: string, msg: PushMessage): Promise<boolean> {
  const accessToken = await getFcmAccessToken(sa);
  const res = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
    method: "POST",
    headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
    body: JSON.stringify({
      message: {
        token,
        notification: { title: msg.title, body: msg.body },
        data: msg.data,
        android: { priority: "HIGH" },
      },
    }),
  });
  if (res.ok) {
    await res.body?.cancel();
    return false;
  }
  const text = await res.text();
  let errorCode = "";
  try {
    const err = JSON.parse(text)?.error;
    errorCode = err?.details?.find((d: { errorCode?: string }) => d.errorCode)?.errorCode ?? err?.status ?? "";
  } catch { /* ignore non-JSON error bodies */ }
  console.error(`FCM error ${res.status} (${errorCode}): ${text}`);
  return errorCode === "UNREGISTERED" || res.status === 404;
}

// ---------------------------------------------------------------------------
// APNs HTTP/2 token auth (iOS)
// ---------------------------------------------------------------------------

let apnsJwt: { value: string; issuedAt: number } | null = null;

async function getApnsJwt(): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  // Apple requires a fresh token between 20 and 60 minutes.
  if (apnsJwt && now - apnsJwt.issuedAt < 50 * 60) return apnsJwt.value;
  const value = await signJwt(
    { alg: "ES256", kid: APNS_KEY_ID },
    { iss: APNS_TEAM_ID, iat: now },
    APNS_KEY_P8,
    "ES256",
  );
  apnsJwt = { value, issuedAt: now };
  return value;
}

/** Returns true when the token is invalid and should be deleted. */
async function sendApns(token: string, msg: PushMessage): Promise<boolean> {
  const jwt = await getApnsJwt();
  const res = await fetch(`https://${APNS_HOST}/3/device/${token}`, {
    method: "POST",
    headers: {
      authorization: `bearer ${jwt}`,
      "apns-topic": APNS_BUNDLE_ID,
      "apns-push-type": "alert",
      "apns-priority": "10",
      "content-type": "application/json",
    },
    body: JSON.stringify({
      aps: { alert: { title: msg.title, body: msg.body }, sound: "default" },
      ...msg.data,
    }),
  });
  if (res.ok) {
    await res.body?.cancel();
    return false;
  }
  const text = await res.text();
  let reason = "";
  try {
    reason = JSON.parse(text)?.reason ?? "";
  } catch { /* ignore */ }
  console.error(`APNs error ${res.status} (${reason})`);
  return res.status === 410 || reason === "BadDeviceToken" || reason === "Unregistered";
}

// ---------------------------------------------------------------------------
// Handler
// ---------------------------------------------------------------------------

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("Method not allowed", { status: 405 });

  const provided = req.headers.get("x-webhook-secret") ?? "";
  if (!WEBHOOK_SECRET || !timingSafeEqual(provided, WEBHOOK_SECRET)) {
    return new Response("Unauthorized", { status: 401 });
  }

  let event: { type?: string; table?: string; record?: NotificationRow };
  try {
    event = await req.json();
  } catch {
    return new Response("Bad request", { status: 400 });
  }
  if (event.type !== "INSERT" || event.table !== "notifications" || !event.record) {
    return new Response("Ignored", { status: 200 });
  }
  const notification = event.record;

  const [{ data: actor }, { data: tokens, error: tokenError }] = await Promise.all([
    supabase.from("profiles").select("display_name").eq("id", notification.actor_user_id).maybeSingle(),
    supabase.from("device_tokens").select("id, token, platform").eq("user_id", notification.recipient_user_id),
  ]);
  if (tokenError) {
    console.error("Loading device tokens failed", tokenError);
    return new Response("Error", { status: 500 }); // webhook may retry
  }

  const message = buildMessage(notification, actor?.display_name ?? "Someone");
  if (!message || !tokens || tokens.length === 0) return new Response("Nothing to send", { status: 200 });

  let serviceAccount: ServiceAccount | null = null;
  if (FIREBASE_SERVICE_ACCOUNT) {
    try {
      serviceAccount = JSON.parse(FIREBASE_SERVICE_ACCOUNT);
    } catch {
      console.error("FIREBASE_SERVICE_ACCOUNT is not valid JSON");
    }
  }

  const invalidIds: string[] = [];
  await Promise.all((tokens as DeviceToken[]).map(async (t) => {
    try {
      let invalid = false;
      if (t.platform === "android") {
        if (!serviceAccount) return;
        invalid = await sendFcm(serviceAccount, t.token, message);
      } else if (t.platform === "ios") {
        if (!APNS_KEY_P8 || !APNS_KEY_ID || !APNS_TEAM_ID) return;
        invalid = await sendApns(t.token, message);
      }
      if (invalid) invalidIds.push(t.id);
    } catch (e) {
      console.error(`Push to ${t.platform} device failed`, e);
    }
  }));

  if (invalidIds.length > 0) {
    const { error } = await supabase.from("device_tokens").delete().in("id", invalidIds);
    if (error) console.error("Deleting invalid tokens failed", error);
  }

  return new Response(JSON.stringify({ sent: tokens.length, removed: invalidIds.length }), {
    status: 200,
    headers: { "content-type": "application/json" },
  });
});
