# send-push

Edge Function that delivers push notifications. It is called by a Database Webhook on every `INSERT` into
`public.notifications` and sends to the recipient's devices (`public.device_tokens`):

- Android via FCM HTTP v1 (OAuth2 service account JWT)
- iOS via APNs HTTP/2 with token auth (.p8 key)

Tokens reported as invalid (FCM `UNREGISTERED`, APNs `410` / `BadDeviceToken`) are deleted.

## Deploy

```bash
supabase functions deploy send-push --no-verify-jwt
```

`--no-verify-jwt` is required: the webhook does not send a user JWT, the request is authenticated by the
`x-webhook-secret` header instead.

## Secrets

```bash
supabase secrets set \
  WEBHOOK_SECRET="<WEBHOOK_SECRET>" \
  FIREBASE_SERVICE_ACCOUNT="$(cat <service-account-key>.json)" \
  APNS_KEY_P8="$(cat AuthKey_XXXXXXXXXX.p8)" \
  APNS_KEY_ID="XXXXXXXXXX" \
  APNS_TEAM_ID="YYYYYYYYYY" \
  APNS_BUNDLE_ID="com.mediabeam.fitness" \
  APNS_HOST="api.sandbox.push.apple.com"
```

- `APNS_HOST`: `api.sandbox.push.apple.com` for debug builds, `api.push.apple.com` for production/TestFlight.
- `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` are provided automatically.
- Missing Firebase or APNs secrets only disable that platform.

## Database Webhook

Dashboard > Database > Webhooks > Create a new hook:

- Table: `public.notifications`, Events: `Insert`
- Type: Supabase Edge Functions, function `send-push`, method `POST`
- HTTP header: `x-webhook-secret: <WEBHOOK_SECRET>`

### If the dashboard fails with `schema "supabase_functions" does not exist`

The dashboard creates webhooks through the `supabase_functions` schema, which only exists after Database Webhooks were
enabled for the project (Dashboard > Database > Webhooks > "Enable webhooks"). Alternatively create the trigger with
SQL; it needs no dashboard webhook (replace `<PROJECT_REF>` and the secret, run once):

```sql
CREATE EXTENSION IF NOT EXISTS pg_net WITH SCHEMA extensions;
SELECT vault.create_secret('<WEBHOOK_SECRET>', 'push_webhook_secret');  -- skip if it exists already

CREATE OR REPLACE FUNCTION public.send_push_on_notification()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM net.http_post(
        url := 'https://<PROJECT_REF>.supabase.co/functions/v1/send-push',
        headers := jsonb_build_object(
            'Content-Type', 'application/json',
            'x-webhook-secret', (SELECT decrypted_secret FROM vault.decrypted_secrets WHERE name = 'push_webhook_secret')
        ),
        body := jsonb_build_object('type', 'INSERT', 'table', 'notifications', 'schema', 'public', 'record', to_jsonb(NEW))
    );
    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'send-push call failed: %', SQLERRM; -- a failing push must never block the like/comment itself
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

DROP TRIGGER IF EXISTS on_notification_created ON public.notifications;
CREATE TRIGGER on_notification_created
    AFTER INSERT ON public.notifications
    FOR EACH ROW EXECUTE FUNCTION public.send_push_on_notification();
```

Use only ONE of the two (dashboard webhook or this trigger), otherwise every notification is pushed twice.

## Test

```bash
curl -X POST "https://<PROJECT_REF>.supabase.co/functions/v1/send-push" \
  -H "x-webhook-secret: <WEBHOOK_SECRET>" -H "content-type: application/json" \
  -d '{"type":"INSERT","table":"notifications","record":{"id":"...","recipient_user_id":"...","actor_user_id":"...","notification_type":"like","target_id":"...","payload":{"entity_type":"activity","entity_id":"..."}}}'
```

Push data keys: `notification_id`, `type`, `entity_type`, `entity_id`, `comment_id`, `preview`, `emoji`,
plus `title_key` / `body_key` / `actor_name` for client-side localization (display text is an English fallback).
