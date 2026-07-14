# VModal Android SDK

Use VModal from a Kotlin Android app to search videos, manage collections, and
upload media. This guide starts with the smallest useful result: connect to the
API and print its health status.

> The SDK is currently added from this repository's source code. It is not yet
> published to a Maven repository.

## What you need

- An Android project that uses Kotlin and Gradle Kotlin DSL
- Java 17
- A checkout of this repository next to, or inside, your Android project
- A VModal API token from your application's approved sign-in flow

Do not put a real token in source control. Pass it to the client at runtime.

## Quick start: get your first API response

### 1. Add the SDK project

Open your Android project's `settings.gradle.kts` and add:

```kotlin
include(":vmodal-sdk-android")
project(":vmodal-sdk-android").projectDir =
    file("../vmx_api/uinterface/sdk_android")
```

The path passed to `file(...)` is relative to `settings.gradle.kts`. Change it
if your repository is in a different location.

In the same file, make sure `dependencyResolutionManagement.repositories`
contains `mavenCentral()`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

### 2. Add the app dependency

In the app module's `build.gradle.kts`, use Java 17 and add the SDK dependency:

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":vmodal-sdk-android"))
}
```

Sync the Gradle project before continuing.

### 3. Allow network access

Add this permission directly inside the `<manifest>` element of
`app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### 4. Connect and print the API status

VModal calls perform network I/O. Run them from `Dispatchers.IO`, WorkManager,
or another worker thread—not the Android main thread.

The following function authenticates the token, creates the ready-to-use
client, and returns the first visible result:

```kotlin
import com.vmodal.sdk.Client
import com.vmodal.sdk.PUBLIC_GATEWAY_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun checkVmodal(apiToken: String): Client = withContext(Dispatchers.IO) {
    val firstClient = Client(
        baseUrl = PUBLIC_GATEWAY_URL,
        token = apiToken,
        mode = "gateway",
    )
    val me = firstClient.auth.me()

    val sdk = Client(
        firstClient.cfg.copy(
            userId = requireNotNull(me.userId),
            tenantId = me.tenantId.orEmpty(),
            email = me.email.orEmpty(),
        )
    )

    val health = sdk.health()
    println("VModal connected: ${health.status}")
    sdk
}
```

For example, call it from an Activity or Fragment lifecycle scope:

```kotlin
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

lifecycleScope.launch {
    val sdk = checkVmodal(apiToken)
    // Keep or pass sdk to the code that needs VModal.
}
```

Use `viewModelScope.launch { ... }` instead when the client belongs to a
ViewModel. A printed `VModal connected: ...` message means installation,
authentication, and network access are working.

## Next result: list your collections

Once the quick start works, use the returned `sdk` client on the same worker
context:

```kotlin
val groups = sdk.collections.listGroups(mode = "vid_file")
println("Collections: ${groups.total}")
groups.data.forEach(::println)
```

This is a useful second check because it confirms that the authenticated user
can reach their VModal data.

## Search a collection

Replace `traffic-cameras` with a collection returned by `listGroups()`:

```kotlin
val result = sdk.searches.searchVideo(
    queryText = "red car at night",
    groupName = "traffic-cameras",
    streamName = "astream",
    limit = 20,
)

println("Matches returned: ${result.cntActual}")
result.data.forEach(::println)
```

If the call succeeds but returns no matches, first confirm the collection name,
stream name, and query text. An empty result is different from an API error.

## Upload a video

After authentication and search work, continue with the upload examples. The
Android-safe path is:

1. Let the user select a video and obtain a `content://` URI.
2. Convert the URI to an `UploadSource` with
   [example 08](examples/08_content_uri_source.kt).
3. Start the upload with [example 09](examples/09_async_video_upload.kt).
4. Keep the returned `UploadHandle` if the UI needs a Cancel action.

The SDK streams the video instead of loading the whole file into memory. Files
of at least 100 MiB use multipart upload by default.

## Common problems

### `VMODAL_API_KEY is required`

`Client.fromEnv()` is intended for JVM tools and CI, where environment
variables are available. In an Android app, pass the runtime token as shown in
the quick start.

### `auth/me returned no user_id` or an authentication error

Confirm that the token is current and belongs to the environment identified by
`PUBLIC_GATEWAY_URL`. Do not invent or hard-code a user ID; `auth.me()` resolves
the token owner.

### `NetworkOnMainThreadException` or frozen UI

Move blocking calls such as `auth.me()`, `health()`, `listGroups()`, and
`searchVideo()` to `Dispatchers.IO` or WorkManager. The
`videoUploadAsync()` orchestration already runs off the main thread, but its
callbacks also run off the main thread; switch to `Dispatchers.Main` before
updating views.

### Gradle cannot find the SDK project

Check the path in `settings.gradle.kts`. It must point to this exact directory:
`uinterface/sdk_android`.

## Verify the SDK checkout

These commands test the SDK itself; they are not required each time the Android
app runs:

```bash
cd uinterface/sdk_android
bash install.sh check
bash test.sh all
```

`install.sh check` verifies Java and Gradle. `test.sh all` runs the offline
regression suite and a simulated app. It does not require an emulator or API
token.

## Learn progressively

1. Finish the quick start on this page.
2. Follow the grouped [examples](examples/) for common tasks.
3. Read the [upload guide](docs/sdk_doc.md) when adding Android URI uploads,
   cancellation, WorkManager, or process-death resume.
4. Use the [API quick reference](DOC_REF.md) when you need a specific method or
   response type.

All typed response objects expose `raw: Map<String, Any?>` for server fields
that do not yet have a typed property. All SDK failures derive from `SdkError`;
applications can handle `AuthError`, `ValidationFailed`, `ApiError`, and
`FeatureDisabled` separately when needed.
