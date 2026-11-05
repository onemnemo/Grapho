# Releasing

Debug and CI builds are signed with the public `android/test.keystore`, so they
install over each other. Store and sideload releases use a private key that only
the Release workflow can see.

## One time: make the key

You need a JDK (for `keytool`). On Windows, `winget install EclipseAdoptium.Temurin.17.JDK` works.
Open a new terminal and run:

```powershell
keytool -genkeypair -v -keystore grapho-release.jks -alias grapho -keyalg RSA -keysize 4096 -validity 10000
```

It asks for a password and some name details. Keep the keystore password and the
key password the same if you can, it makes life easier.

Back up `grapho-release.jks` and the passwords somewhere safe (password manager
plus an offline copy). If the key is lost, the app can never be updated on Play
under the same listing. Never commit it.

## Add the secrets

Turn the keystore into text:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("grapho-release.jks")) | Set-Clipboard
```

In the repo go to Settings, Secrets and variables, Actions, and add:

| Secret | Value |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | the base64 text from the clipboard |
| `RELEASE_KEYSTORE_PASSWORD` | keystore password |
| `RELEASE_KEY_ALIAS` | `grapho` |
| `RELEASE_KEY_PASSWORD` | key password |

## Make a release

```sh
git tag v0.1.0
git push origin v0.1.0
```

The Release workflow builds a signed APK and AAB, checks the APK isn't signed
with the test key, and attaches both to a draft GitHub release. The version name
comes from the tag and the version code is the workflow run number, so it
always goes up. Publish the draft when it looks right. Upload the AAB to Play.

You can also run the workflow by hand from the Actions tab to get the files as
a build artifact.

## Switching from the test key

Android won't update an app over a build with a different signature. The first
time you install a release build on a device that has a test-key build, uninstall
the old one first. Export your notes from the app before that, uninstalling
deletes them.
