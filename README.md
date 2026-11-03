# FlickBoard PL / EN

Android flick keyboard fork with offline Polish and English word suggestions.

- Up to three suggestions above the keyboard: completion and spelling correction
  while typing, then next-word prediction after a space.
- Tap a suggestion to replace the word at the cursor and continue typing.
- While typing, the first suggestion is your current word exactly as written.
  Tap it to save it to the active language's personal dictionary and add a space.
  The remaining two slots offer completions or corrections. After a space,
  all three slots predict next words.
- Settings → Personal dictionary lists explicitly saved words for Polski / English
  and lets you delete them individually. Explicit saving works with automatic
  learning switched off; private editors requesting no personalized learning are excluded.
- Learns word frequencies and sequences of up to three words locally. Each
  language has its own history; no network access or external service is needed.
- Suggestions follow the active main letter layout automatically: Polish for the
  Polish layout, English for English layouts (including Thumb-Key and German/English).
  Settings → Behaviour lets you disable automatic switching, select a manual / fallback
  language, disable suggestions or learning, and erase learned history. Unsupported
  layouts use the saved fallback language (Polish by default).
- Passwords, email addresses and numeric fields are excluded. Text search fields
  and browser address bars support suggestions even if the app requests no
  suggestions. Address-bar input is not automatically added to the learned history.
- The default key height is 64 dp; saved custom heights are retained.
  Settings → Layout → Suggestion bar height adjusts the suggestion strip from
  32 to 80 dp (48 dp by default).
- The bundled `cat.jpg` is the default keyboard background; a custom background
  selected in settings takes precedence.

Prediction uses bundled frequency dictionaries plus a small personal n-gram model.
It does not have Gboard's large pretrained language model or text-based language
detection. Misspellings are offered as suggestions; words are never silently replaced.

The dictionaries contain 49,693 Polish and 46,968 English entries, adapted from
[FrequencyWords](https://github.com/hermitdave/FrequencyWords). See
[dictionary attribution](app/src/main/assets/SUGGESTIONS-LICENSE.md) and
[privacy information](PRIVACY.md).

Build with JDK 17 and Android SDK 35:

```sh
./gradlew assemblePlainDebug
./gradlew testPlainDebugUnitTest --tests 'se.nullable.flickboard.suggestions.*'
```

For signed releases, provide an ignored `signing.properties` file with `storeFile`,
`storePassword`, `keyAlias`, and `keyPassword`, then run `./gradlew assemblePlainRelease`.
The original `../flickboard.keystore.properties` path is also supported.
Keep a private backup of the signing key and credentials: future APK updates need
the same signing key. Never upload them as release assets or commit them.

Upstream FlickBoard is maintained on [Codeberg](https://codeberg.org/natkr/flickboard).
