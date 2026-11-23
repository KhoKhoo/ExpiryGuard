# ExpiryGuard

An Android app that tracks product expiration dates so nothing goes to waste.
Built with Java, XML layouts and SQLite for the CS 526 Mobile Application Programming course.

## Features

- Home screen with three status tiles: **Safe**, **Expiring soon**, **Expired** (tap a tile to filter)
- Filter by category and sort by date or name
- Categories: Food & beverages, Cosmetics, Medications, Skin care, Other
- Add / edit products with a photo (camera or gallery) and an expiration date
- Product details screen with a countdown
- Daily reminder notification for products that are expired or about to expire
- Settings: notifications on/off, how many days before expiry to warn, time of the daily check

## What you need

- [Android Studio](https://developer.android.com/studio) (a recent version, e.g. Koala or newer)
- An internet connection for the first Gradle sync
- An Android emulator **or** an Android phone (the app does not run on iPhone)
- A free GitHub account (this repo is private, so the owner must invite you first)

## Option A: open it from GitHub inside Android Studio (recommended)

1. Accept the invitation. The repo owner adds your GitHub username under
   *Settings → Collaborators*, and you get an email. Click **Accept invitation**.
2. Open Android Studio. On the Welcome screen choose **Get from VCS**
   (or **File → New → Project from Version Control**).
3. Choose **GitHub** on the left and sign in when asked.
   Pick **ExpiryGuard** from the list, choose a folder on your PC, and click **Clone**.
   Alternatively, choose **Repository URL** and paste:
   `https://github.com/<owner-username>/ExpiryGuard.git`
4. Click **Trust Project** if asked, then wait for **Gradle sync** to finish
   (progress bar at the bottom; the first time can take a few minutes).
5. Run it, see "Run the app" below.

## Option B: download a ZIP

1. On the repo page click the green **Code** button, then **Download ZIP**.
2. Unzip it. **Important:** open the folder that directly contains
   `app`, `gradle`, `build.gradle` and `settings.gradle`.
   Sometimes the ZIP unpacks into a folder inside a folder with the same name, so check which one you open.
3. In Android Studio choose **File → Open**, select that folder, click **Trust Project**,
   and wait for Gradle sync to finish.

## Run the app

1. Pick a device in the top toolbar:
   - **Emulator:** *Device Manager → Create device* (for example Pixel 4 or Pixel 6, API 34).
     Emulators use several GB of disk space.
   - **Real Android phone:** turn on *Developer options → USB debugging*
     (Settings → About phone → tap *Build number* 7 times), plug it in, and accept the prompt on the phone.
2. Make sure the run box next to the green button says **app**.
3. Press the green **Run** button.
4. On the first launch the app adds 7 sample products so the list is not empty.
   Allow notifications when asked.

## Project structure

```
app/src/main/java/com/expiryguard/app/
    SplashActivity            splash screen
    MainActivity              home screen (list, tiles, filters)
    FilterFragment            status tiles + category/sort buttons
    ProductAdapter            RecyclerView adapter for the cards
    AddEditProductActivity    add / edit form
    ProductDetailsActivity    details + countdown
    SettingsActivity / SettingsFragment
    DatabaseHelper            SQLite (products table)
    ExpiryUtils               days-left and status rules
    NotificationHelper / ExpiryReceiver   daily reminder
app/src/main/res/             layouts, strings, colors, icons
```

## Status rules

Days left = expiration date minus today.

| Days left | Status |
|---|---|
| less than 0 | Expired |
| 0 up to the warning days (default 7) | Expiring soon |
| more than the warning days | Safe |

## Working together on the code

- Before you start editing: **Git → Pull** (Ctrl+T) to get the latest changes.
- After you edit and test: **Commit (Ctrl+K) → Commit and Push**.
- Write a short commit message that says what changed.
- Avoid force-pushing. Never use *Force Push* on `master`.

## Troubleshooting

| Problem | Fix |
|---|---|
| Run button is grey, no "app" in the toolbar | You opened the wrong folder. Close the project and open the folder that contains `settings.gradle`. |
| Gradle sync fails | Check your internet, then **File → Sync Project with Gradle Files**. Make sure an Android SDK is installed (*Settings → Languages & Frameworks → Android SDK*). |
| "Resource not found" or other build errors | **Build → Clean Project**, then **Build → Rebuild Project**. |
| Emulator says there is not enough disk space | Delete unused emulators in Device Manager, or run on a real phone. |
| Can't see the repo or clone fails | You haven't accepted the invitation yet, or you are signed in to a different GitHub account. |
| No notification appears | Allow notifications for ExpiryGuard in the phone's app settings, then use *Settings → Send a test notification*. |
