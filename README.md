# ליצור בקליק (Litsor BeKlik)

אפליקציית אנדרואיד לבניית אפליקציות אנדרואיד — ראו `ARCHITECTURE.md` (יתווסף) לתיאור המלא.

## פתיחה ב-Android Studio

1. פתחו את התיקייה הזו ב-Android Studio (Ladybug ואילך).
2. בפעם הראשונה, Android Studio ייצור אוטומטית Gradle Wrapper עבור הפרויקט (אין `gradlew` מחויב בריפו — ה-CI מתקין Gradle דרך `gradle/actions/setup-gradle`).
3. יש להשלים ב-`SupabaseModule.kt` את מפתח ה-anon/public key מה-Supabase Dashboard (Settings → API).

## חתימת APK (CI)

ה-workflow ב-`.github/workflows/build.yml` בונה וחותם release APK בכל push ל-`main`, ומעלה אותו כ-artifact.
יש להגדיר ב-Settings → Secrets and variables → Actions את הסודות הבאים (הערכים נמסרו בצ'אט הבניה):

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## מבנה מודולרי

- `data/engines/AiEngine.kt` — ממשק AI, עם `CloudAiEngine` (Gemini/GPT/Claude/Grok) ו-`LocalAiEngine` (on-device, בפיתוח).
- `data/engines/BuildEngine.kt` — ממשק בנייה, עם `GithubBuildEngine` (Actions) ו-`LocalBuildEngine` (בנייה על המכשיר, בפיתוח).
- `supabase/migrations/0001_init.sql` — סכימת בסיס הנתונים + מדיניות RLS.
