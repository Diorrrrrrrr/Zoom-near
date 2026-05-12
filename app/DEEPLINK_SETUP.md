# 딥링크 설정 가이드

앱 scheme: `zoomnear`

## Android — AndroidManifest.xml

`app/android/app/src/main/AndroidManifest.xml`의 `<activity>` 태그 안에 추가:

```xml
<!-- 딥링크: zoomnear://invite?token=XXX -->
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="zoomnear" android:host="invite" />
</intent-filter>

<!-- HTTPS 앱 링크: https://zoomnear.kr/invite?token=XXX -->
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" android:host="zoomnear.kr" android:pathPrefix="/invite" />
</intent-filter>
```

## iOS — Info.plist

`app/ios/Runner/Info.plist`에 추가:

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleTypeRole</key>
        <string>Editor</string>
        <key>CFBundleURLName</key>
        <string>kr.zoomnear</string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>zoomnear</string>
        </array>
    </dict>
</array>
```

iOS Universal Links를 위해 `Associated Domains` capability에 추가:
`applinks:zoomnear.kr`

## Flutter — GoRouter 처리

`app_router.dart`에 이미 `/invite/accept` 라우트가 등록되어 있습니다.

`go_router`의 딥링크 처리는 `main.dart`의 `GoRouter` 설정에서 자동으로 처리됩니다.

추가로 `app/android/app/src/main/AndroidManifest.xml`에서 `flutter_deeplinking_enabled` 메타데이터를 확인하세요:

```xml
<meta-data android:name="flutter_deeplinking_enabled" android:value="true" />
```

## 테스트

```bash
# Android
adb shell am start -W -a android.intent.action.VIEW -d "zoomnear://invite?token=TEST123" kr.zoomnear

# iOS (Simulator)
xcrun simctl openurl booted "zoomnear://invite?token=TEST123"
```
