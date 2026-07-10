# מסמך העברה ל-Claude Code — Litsor BeKlik (ליצור בקליק)

## מה זה הפרויקט
אפליקציית אנדרואיד (Kotlin + Jetpack Compose) שמאפשרת למשתמש לבנות אפליקציית אנדרואיד אחרת:
נרשמים → מתחברים ומזינים מפתח API של Gemini/GPT/Claude/Grok → מאפיינים אפליקציה (צ'אט או הדבקת
טקסט) → ה-AI כותב פרויקט מלא → בדיקת תקינות + לופ תיקונים → בנייה (GitHub Actions או מקומי) →
APK חתום. Supabase (מסלול חינמי) הוא בסיס הנתונים היחיד. אין שרת בבעלות המשתמש.

## סטטוס — מה כבר בנוי ועובד (לפי קוד, לא נבדק קומפילציה עדיין!)

- **Auth + Secrets**: הרשמה/התחברות מול Supabase Auth. מפתחות AI וטוקן GitHub מוצפנים בצד לקוח
  (AES-GCM, מפתח נגזר מסיסמת המשתמש) לפני שמירה ב-`user_secrets`.
  קבצים: `data/repository/AuthRepository.kt`, `SecretsRepository.kt`, `data/crypto/SecretCrypto.kt`,
  `data/session/SessionState.kt`.
- **CloudAiEngine**: קריאות HTTP אמיתיות לכל 4 הספקים (Gemini/OpenAI/Claude/Grok), עם פרומפט קשיח
  שמחייב JSON מדויק של קבצי פרויקט.
  קבצים: `data/engines/CloudAiEngine.kt`, `data/engines/net/*Client.kt`.
- **GithubBuildEngine**: דוחף קבצים דרך GitHub Contents API (בלי git/JGit על המכשיר), עוקב אחרי
  ריצה דרך Actions API. קובץ: `data/engines/GithubBuildEngine.kt`, `data/engines/net/GithubApiClient.kt`.
- **LocalAiEngine / LocalBuildEngine**: **שלד בלבד** — ראה "מה חסר".
- **ProjectValidator**: בדיקות מבנה בסיסיות לפני בנייה (קבצי חובה, applicationId, XML לא מאוזן וכו').
- **DeviceCapability + DeviceCapabilityProbe**: זיהוי RAM/NPU אמיתי (ActivityManager + Build.HARDWARE).
- **BuildOrchestrator**: מחבר generate → validate → fix-loop (עד 3 ניסיונות) → build, ושומר `build_runs`.
- **EngineSettingsScreen**: שמירת בחירת מנועים (`ai_engine`/`build_engine`) לכל פרויקט.
- **SpecChatScreen**: צ'אט אמיתי עם ה-AI לבניית אפיון + אופציה להדביק אפיון קיים (טקסט בלבד, לא קובץ).
- **Self-update (צד קליינט)**: בודק גרסה מול `app_versions`, מוריד מ-Supabase Storage, מתקין.
  קבצים: `data/repository/AppVersionsRepository.kt`, `domain/UpdateChecker.kt`, `data/update/ApkInstaller.kt`,
  `MainActivity.kt` (הפונקציה `SelfUpdateCheck`).
- **Supabase**: 7 טבלאות + RLS מלא (ראה `supabase/migrations/`), bucket ציבורי `app-releases`.
- **CI**: `.github/workflows/build.yml` בונה וחותם APK release בכל push ל-`main` (Secrets כבר מוגדרים בריפו).

## מה חסר — לפי סדר עדיפות מומלץ

1. **בדיקת קומפילציה בפועל** — הקוד הזה נכתב בלי גישה לקומפיילר אנדרואיד. המשימה הראשונה
   והקריטית: `./gradlew assembleDebug` (או `gradle assembleDebug` אם אין wrapper עדיין — להריץ
   `gradle wrapper` פעם אחת כדי לייצר `gradlew`), ולתקן כל שגיאה שתצא. יש הרבה מקום לשגיאות API
   קטנות (בעיקר בסינטקס של supabase-kt: `insert(){select()}`, `update(){filter{}}` וכו').
2. **LocalAiEngine אמיתי** — אינטגרציית LiteRT-LM או MLC-LLM, הורדת מודל on-demand (לא לצרף ל-APK),
   לפי הטירינג ב-`DeviceCapability`.
3. **LocalBuildEngine אמיתי** — toolchain מוטבע (aapt2/d8/apksigner מ-Android SDK Build-Tools) בלי
   Gradle מלא, או fallback לסביבת Termux+Gradle למכשירים חזקים.
4. **העלאת קובץ אפיון אמיתי** — כרגע `SpecChatScreen` תומך רק בהדבקת טקסט. להוסיף file picker
   (Storage Access Framework) + פרסור txt/pdf/docx.
5. **פרסום גרסה חדשה (הצד השני של ה-self-update) — ראה סעיף נפרד למטה, זה כרגע לא קיים בכלל.**
6. חילוץ אפיון מבני מהצ'אט (goal/screens/features) — היום זה simplistic (`buildDraftSpecFromHistory`
   ב-`SpecChatScreen.kt`), שווה פרומפט ייעודי ל-"תחזיר JSON מבני של האפיון".
7. בדיקות יחידה (`ProjectValidator`, `SecretCrypto` וכו') — עדיין אין תיקיית test.
8. אייקון/מיתוג מלא — כרגע אייקון וקטורי פלייסהולדר.

## מנגנון עדכון גרסה (Self-Update) — קיים לעומת חסר

**הצד שבודק ומתקין (קיים ועובד, בקליינט):** בכל עליית אפליקציה, `MainActivity.SelfUpdateCheck()`
קורא ל-`UpdateChecker.checkForUpdate(BuildConfig.VERSION_CODE)`, שמשווה מול `app_versions.version_code`
ב-Supabase. אם יש גרסה חדשה — `ApkInstaller.downloadAndInstall()` מוריד את ה-APK מה-URL הציבורי
של bucket `app-releases` ומפעיל את מתקין המערכת.

**הצד שמפרסם גרסה חדשה — לא קיים כלל, זה ה-TODO הכי ברור:** כרגע אין שום דבר שמעלה APK ל-Storage
או מכניס שורה ל-`app_versions` אחרי בנייה מוצלחת. עד שזה יהיה אוטומטי, כך עושים את זה ידנית:

```sql
-- 1) להעלות את קובץ ה-APK בשם, למשל, litsorbeklik-release-v0.2.0.apk
--    ל-bucket "app-releases" ב-Supabase Storage (דרך ה-Dashboard או ה-API).
-- 2) להריץ את זה ב-SQL editor של Supabase:
insert into app_versions (version_code, version_name, apk_storage_path, changelog)
values (2, '0.2.0', 'litsorbeklik-release-v0.2.0.apk', 'תיקוני קומפילציה ראשוניים');
```

**מומלץ ל-Claude Code להוסיף**: step בסוף `.github/workflows/build.yml` שאחרי `assembleRelease`
מעלה את ה-APK אוטומטית ל-Storage (Supabase REST API עם `service_role` key ששמור כ-GitHub Secret
**נפרד** — לא זה שבתוך האפליקציה) ומריץ את ה-`insert` הזה, כדי שכל push ל-`main` שמצליח יפרסם
גרסה חדשה למשתמשים קיימים אוטומטית.

## פרטי חיבור (כבר מוגדרים, לא צריך ליצור מחדש)

- Supabase project: `krhbgeewzruuvpdxjbiv` (anon key כבר מוטמע ב-`SupabaseModule.kt`).
- GitHub repo: `github.com/a0556770172-png/litsorbeklik` (ציבורי; git remote כבר מוגדר בתיקייה).
- GitHub Secrets (כבר מוגדרים בריפו): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Supabase Auth: לבדוק אם "Confirm email" מופעל — אם כן, ה-`AuthRepository.register()` הנוכחי
  יכשל כי הוא מצפה ל-session מיידי אחרי הרשמה. לכבות בשלב הפיתוח או להוסיף מסך אימות מייל.

## הנחיה מומלצת ל-Claude Code

1. להריץ בנייה מקומית ולתקן כל שגיאת קומפילציה (עדיפות עליונה).
2. להמשיך לפי רשימת ה-TODO לפי סדר העדיפויות שלמעלה.
3. `git commit` + `git push` לאחר כל שינוי משמעותי (יפעיל את ה-CI אוטומטית).
