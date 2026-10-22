# WakePC

A tiny Wake-on-LAN app for Android. Minimal list UI, a one-tap home-screen widget, launcher shortcuts, and voice hooks.

## Build

1. Open the `WakePC` folder in **Android Studio** (it downloads Gradle and the SDK pieces it needs; the wrapper jar is generated on first sync).
2. Plug in your phone (USB debugging on) and press **Run**. Or *Build > Build APK(s)* and install `app/build/outputs/apk/debug/app-debug.apk`.

**No Android Studio? Let GitHub build the APK:** create a new GitHub repo, upload these files (keep the `.github` folder), open the **Actions** tab > *Build APK* > the finished run, and download the **WakePC-apk** artifact. Unzip it to get `app-debug.apk`, copy it to your phone and install it (allow "install unknown apps" when asked).

Kotlin, minSdk 26, targetSdk 35. No third-party libraries beyond AndroidX + Material.

## Use

- **Add a PC:** tap **+**, enter a name and the PC's MAC address. Leave *Address* empty and the port at 9 for normal home use.
- **Wake:** tap a PC in the list. **Long-press** to edit, delete, or mark it as the default.
- **Widget:** long-press the home screen > Widgets > WakePC, then pick which PC it wakes. One tap sends the packet.
- **Shortcuts:** long-press the app icon for "Wake PC" (default PC) and one shortcut per PC (up to 4). You can drag them to the home screen.

## Voice

Google replaced Assistant with Gemini on Android in September 2026, so the dependable route is:

1. In WakePC open the menu (⋮) and enable **Wake default PC when app opens**.
2. Say **"Hey Google, open WakePC"**. The app opens and sends the wake signal.

Also included (untested here, may or may not be honored by Gemini): App Actions capability bindings (`res/xml/shortcuts.xml` and `Shortcuts.kt`) so phrases like "wake Desktop in WakePC" can map to `WakeActivity`. App Actions generally require the app to be uploaded to Google Play (an internal test track is enough). If you sideload only, rely on the open-app route, the widget, or shortcuts.

## Make the PC actually wake up

- BIOS/UEFI: enable *Wake on LAN* / *Power on by PCI-E*.
- Windows: Device Manager > network adapter > Power Management > allow this device to wake the computer, and Advanced > *Wake on Magic Packet* = Enabled. Disable *Fast Startup* if it won't wake from shutdown.
- Use Ethernet on the PC (Wi-Fi WoL rarely works). The phone must be on the same network/subnet.
- Find the MAC: `ipconfig /all` (Windows) or `ip link` (Linux).

## Waking from outside your home

Set the PC's *Address* to your router's public hostname/IP (DDNS) and the *Port* to a UDP port you forward on the router to the PC (or to the broadcast address, if your router supports it). Many routers are fiddly about this; a VPN to your home network (then using the normal LAN settings) is usually simpler.

## Notes

- Network: the packet goes to `255.255.255.255` and the subnet broadcast (for example `192.168.1.255`) on the active network.
- If you raise `targetSdk` to 37 or higher, Android 17 requires the `ACCESS_LOCAL_NETWORK` runtime permission for local-network traffic like this. At targetSdk 36 or lower, the `INTERNET` permission is enough. The app targets 35 for now.
- Files: `WolSender.kt` (magic packet), `WakeWidgetProvider.kt` + `WidgetConfigActivity.kt` (widget), `WakeActivity.kt` (invisible entry for shortcuts/voice), `Shortcuts.kt` (per-PC shortcuts), `MainActivity.kt` (UI).
