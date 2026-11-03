# FlickBoard PL / EN

Android flick keyboard fork with offline Polish and English word suggestions.

- Up to three suggestions above the keyboard: completion and spelling correction
  while typing, then next-word prediction after a space.
- Tap a suggestion to replace the word at the cursor and continue typing.
- Learns word frequencies and sequences of up to three words locally. Each
  language has its own history; no network access or external service is needed.
- Choose Polski / English, disable suggestions or learning, and erase learned
  history in Settings → Behaviour. Polish is the default suggestion language.
- Passwords, URLs, email addresses and numeric fields are excluded.

Prediction uses bundled frequency dictionaries plus a small personal n-gram model.
It does not have Gboard's large pretrained language model or automatic language
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
