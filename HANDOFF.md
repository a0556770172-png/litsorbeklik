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

1. ~~בדיקת קומפילציה בפועל~~ — **בוצע (2026-07-16)**: `./gradlew assembleDebug` מקומית ו-CI
   (`Build & Sign Release APK`) שניהם עוברים בהצלחה על `main` הנוכחי (3ead61e). מספר קומיטים אחרי
   כתיבת המסמך הזה תיקנו שגיאות קומפילציה, קריסת התחברות, ובעיות RTL/CA-certs — ראה `git log`.
2. **LocalAiEngine אמיתי** — אינטגרציית LiteRT-LM או MLC-LLM, הורדת מודל on-demand (לא לצרף ל-APK),
   לפי הטירינג ב-`DeviceCapability`.
3. **LocalBuildEngine אמיתי** — toolchain מוטבע (aapt2/d8/apksigner מ-Android SDK Build-Tools) בלי
   Gradle מלא, או fallback לסביבת Termux+Gradle למכשירים חזקים.
4. ~~העלאת קובץ אפיון אמיתי~~ — **בוצע חלקית (2026-07-16)**: `SpecChatScreen`/`UploadSpecDialog`
   תומך עכשיו בבחירת קובץ טקסט אמיתי (SAF, `ActivityResultContracts.OpenDocument`) בנוסף להדבקה —
   txt/md בלבד. פרסור PDF/DOCX עדיין דורש ספרייה ייעודית (PdfBox-Android / Apache POI) ולא בוצע.
5. ~~פרסום גרסה חדשה (הצד השני של ה-self-update)~~ — **בוצע (2026-07-16)**: נוסף step ל-
   `.github/workflows/build.yml` (`Publish release to Supabase`) שרץ אחרי `assembleRelease` על כל
   push ל-`main`, מעלה את ה-APK ל-bucket `app-releases` ומכניס שורה ל-`app_versions`. `versionCode`
   נגזר אוטומטית מ-`github.run_number` (גם ב-build עצמו דרך `APP_VERSION_CODE`, גם ב-insert) כדי
   שה-versionCode המוטבע בפועל ב-APK תמיד יתאים למה שנשמר ב-DB.
   **דורש הגדרה ידנית לפני שזה עובד**: להוסיף GitHub secret בשם `SUPABASE_SERVICE_ROLE_KEY`
   (מ-Supabase Dashboard → Settings → API → `service_role` — **לא** ה-anon key שכבר מוטמע באפליקציה)
   דרך `gh secret set SUPABASE_SERVICE_ROLE_KEY --repo a0556770172-png/litsorbeklik`. בלי הסוד הזה
   ה-step פשוט מדלג (לא נכשל) — ה-CI הרגיל ממשיך לעבוד כרגיל.
6. חילוץ אפיון מבני מהצ'אט (goal/screens/features) — היום זה simplistic (`buildDraftSpecFromHistory`
   ב-`SpecChatScreen.kt`), שווה פרומפט ייעודי ל-"תחזיר JSON מבני של האפיון".
7. בדיקות יחידה (`ProjectValidator`, `SecretCrypto` וכו') — עדיין אין תיקיית test.
8. אייקון/מיתוג מלא — כרגע אייקון וקטורי פלייסהולדר.

**הערה:** מסמך זה מתעדכן ידנית ולכן נוטה להתיישן מהר יחסית לקוד/git log בפועל — לפני שמסתמכים
עליו, כדאי לבדוק `git log --oneline` ו-`gh run list` כדי לוודא שהסטטוס עדיין נכון.

## סקירה ותיקונים נוספים (2026-07-16) — הסיבה שלא היה אף build מוצלח

בדקתי בפועל את ה-DB (לא רק את הקוד): היה חשבון אמיתי אחד עם התחברות/שמירת מפתח/יצירת פרויקט/אפיון
תקינים, אבל `build_runs` היה **ריק לגמרי** — אף פעם לא הושלמה בנייה. מצאתי ותיקנתי:

1. **חוסם מוחלט**: לא הייתה שום דרך ב-UI להזין repo URL או GitHub PAT (`SecretsRepository.saveGithubToken`
   הוגדר אבל **אף קובץ לא קרא לו**). כל פרויקט חדש ברירת המחדל שלו `buildEngine="GITHUB"`, כך
   ש-`EngineFactory.buildBuildEngine` תמיד נכשל מיד. תוקן: `EngineSettingsScreen.kt` כולל עכשיו
   שדות repo URL + PAT, ו-`ProjectsRepository.updateRepoUrl` (חדש) + `SecretsRepository.saveGithubToken`
   (סוף סוף מחוברים) נקראים בשמירה.
2. **`GithubBuildEngine` דחף כל קובץ כקומיט נפרד** (N קומיטים, N הפעלות workflow לפרויקט אחד),
   ואז תפס "ה-run האחרון" בלי סינון לפי head_sha — עלול לתפוס run לא קשור. תוקן: `GithubApiClient`
   עכשיו דוחף commit אטומי יחיד (Git Data API: blobs→tree→commit→ref, כמו ב-Electron), ומחפש run
   לפי head_sha עם retry (עד 10 ניסיונות, 3 שניות בין ניסיון).
3. **`AnthropicClient.kt` השתמש ב-`"claude-sonnet-4-5"`** — לא מזהה מודל תקף (אומת מול תיעוד עדכני;
   המזהה הנכון הוא `claude-sonnet-5`) — כל קריאה ל-Claude כנראה נכשלה. תוקן, וגם הועלה `maxTokens`
   מ-8192 (סביר שקטע JSON של פרויקט שלם באמצע) ל-32000, עם timeout מוארך ל-300 שניות (הקריאה עדיין
   לא streaming — אם עדיין נכשל על פרויקטים גדולים, המעבר הבא הוא streaming אמיתי). גם ל-Gemini/OpenAI/Grok
   נוסף max output tokens מפורש (32000/32000) במקום להישען על ברירת המחדל הלא-ידועה של כל ספק.

**עדיין לא נבדק בפועל על מכשיר אמיתי** — כל האימות שנעשה כאן הוא `./gradlew assembleDebug` בלבד.
הצעד הבא המומלץ: להתקין ולנסות את כל הזרימה (התחברות → אפיון → הגדרות (כולל חיבור GitHub) → בנייה)
פעם אחת עד הסוף.

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
