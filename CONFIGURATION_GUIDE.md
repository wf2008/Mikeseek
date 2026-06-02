# Wfseek System Configuration & Deployment Guide

This document contains **all database security rules**, **application signing assets (Base64 keystore)**, and **environment variables** compiled together. You can copy this file or push it directly to your private repository for safe keeping and easy copy-pasting to Vercel, GitHub, and Firebase.

---

## 1. Firebase Realtime Database Security Rules

Paste the following JSON rules directly in your **Firebase Console** under **Realtime Database > Rules Tab**:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null && auth.uid == $uid",
        ".write": "auth != null && auth.uid == $uid",
        "devices": {
          ".read": "auth != null && auth.uid == $uid",
          ".write": "auth != null && auth.uid == $uid"
        }
      }
    },
    "tokens": {
      ".read": true,
      ".write": true
    },
    "discoveries": {
      ".read": true,
      ".write": true
    }
  }
}
```

---

## 2. Environment Variables & Secrets (Vercel / GitHub / Local)

Copy the lines below to construct your `.env` file locally, or define them individually inside **Vercel Project Settings > Environment Variables** and **GitHub Repository > Settings > Secrets and variables > Actions**:

```env
# =============================================================================
#             WFSEEK SYSTEM WIDE CONFIGURATION AND SECRETS
# =============================================================================

# 1. GOOGLE GEMINI AI API (Needed for smart Android features)
GEMINI_API_KEY=your_gemini_api_key_here

# 2. TELEGRAM ADMINISTRATION BOT SECRETS (Needed for Telegram bot script)
TELEGRAM_BOT_TOKEN=your_telegram_bot_token_here

# Whitelist of Telegram Chat IDs allowed to run /weekly, /monthly, /family, /expire, /list.
# (Obtain this by messaging @userinfobot on Telegram)
ADMIN_CHAT_ID=your_telegram_chat_id_here

# 3. FIREBASE INTEGRATION DETAILS
FIREBASE_PROJECT_ID=wfdmike

# 4. ANDROID RELEASE APP SECRETS & SIGNING PASSWORDS
STORE_PASSWORD=android
KEY_PASSWORD=android
```

---

## 3. Play Store / Release Application Signing Key (Base64 Keystore)

To build fully signed Release APKs automatically using **GitHub Actions**, go to your GitHub repository:
1. Navigate to **Settings > Secrets and variables > Actions**.
2. Click **New repository secret**.
3. Set name to `KEYSTORE_BASE64` and paste the exact Base64 block below as the secret value.

### Keystore Base64 String (`KEYSTORE_BASE64`):
```text
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
```

### Signing Properties Configuration (Already in Gradle)
- **Key Alias**: `upload`
- **Store Password**: `android`
- **Key Password**: `android`
- **File Name**: `my-upload-key.jks` (automatically committed or decoded dynamically in GitHub Actions)

---

## 4. Deploying the Telegram Bot script on Vercel

To deploy your serverless bot script directly to Vercel:
1. Ensure `scripts/wfseek_bot.py` is present in your repo.
2. Ensure you've declared `requests`, `pyTelegramBotAPI`, and `python-dotenv` in your requirements files (normally created dynamically by Vercel for python functions, or hosted as a simple web hook background worker).
3. Set your environment variables in Vercel:
   - `TELEGRAM_BOT_TOKEN`
   - `FIREBASE_PROJECT_ID`
   - `ADMIN_CHAT_ID`
