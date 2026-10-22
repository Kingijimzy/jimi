# Install Jimi on Redmi (fix package error)

## Correct download

**Use this page only:**
https://github.com/Kingijimzy/jimi/releases/tag/latest

Download the asset named:

- `jimi-debug.apk`  **or**
- `jimi-0.5.0-debug.apk` (version may change)

### Wrong downloads that cause "package invalid"

| What you downloaded | Result |
|---------------------|--------|
| Actions **artifact zip** | Parse error if you install the zip |
| Incomplete download (few KB) | Invalid package |
| HTML error page saved as .apk | Invalid package |
| Old corrupted file | Invalid package |

## Steps (Redmi / MIUI / HyperOS)

1. **Uninstall** any existing app named **Jimi**.
2. Open the release link above in **Chrome**.
3. Tap **jimi-debug.apk** → Download.
4. Open **Files** / **Downloads** → tap the APK.
5. If blocked: **Settings → Apps → Manage apps →** three dots → **Other permissions / Install unknown apps** → allow **Files** (or Chrome).
6. Confirm install.

## Check the file

- Size should be **several megabytes** (usually 10MB+), not 2KB.
- Name must end with **`.apk`**.
- If you only have a `.zip`, **unzip** it, then install the `.apk` inside.

## Still failing?

Reply with:
1. Exact error text (English or your language)
2. File name you downloaded
3. Approximate file size shown in Downloads
