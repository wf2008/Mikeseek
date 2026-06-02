#!/usr/bin/env python3
"""
=============================================================================
             WFSEEK AUTOMATED SUREBET ALERTS & PLAN TOKEN BOT
=============================================================================
This is the complete, production-ready python script for your Wfseek Telegram Bot.
Deploy this script on any server (Heroku, AWS VPS, Repl.it, local terminal, etc.) to:
1. Generate subscription tokens (Weekly, Monthly, Family Lifetime).
2. Write tokens directly to your Firebase Realtime Database.
3. List active family licenses by custom operator name.
4. Manually expire/remove tokens at any time.

Prerequisites:
  pip install pyTelegramBotAPI requests python-dotenv

Usage:
  1. Add your TELEGRAM_BOT_TOKEN and FIREBASE_PROJECT_ID in a .env file or environment.
  2. Start the bot: python wfseek_bot.py
"""

import os
import random
import string
import requests
from dotenv import load_dotenv
import telebot
from telebot import types

# Load secret environment variables
load_dotenv()

BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "YOUR_TELEGRAM_BOT_TOKEN_HERE")
FIREBASE_PROJECT_ID = os.getenv("FIREBASE_PROJECT_ID", "wfseek-secure")

if BOT_TOKEN == "YOUR_TELEGRAM_BOT_TOKEN_HERE":
    print("[WARNING] Please configure your TELEGRAM_BOT_TOKEN in .env or system coordinates.")

bot = telebot.TeleBot(BOT_TOKEN)

# Base URL details for Firebase Realtime Database
def get_db_url(path):
    return f"https://{FIREBASE_PROJECT_ID}-default-rtdb.firebaseio.com/{path}.json"

def generate_random_token(prefix="", length=8):
    """Generates secure alphanumeric activation codes."""
    chars = string.ascii_uppercase + string.digits
    rand_part = "".join(random.choice(chars) for _ in range(length))
    return f"WFS-{prefix}-{rand_part}"

# Command: /start
@bot.message_code_handler if hasattr(bot, 'message_code_handler') else bot.message_handler(commands=['start', 'help'])
def send_welcome(message):
    help_text = (
        "🤖 *Welcome to the Wfseek Node Administrator Bot!* 🤖\n\n"
        "Use me to manage, generate, and expire access licenses securely:\n\n"
        "💳 *Subscription Plans:*\n"
        "• *Weekly Plan (2k NGN):* Expires 7 days after activation.\n"
        "• *Monthly Plan (8k NGN):* Expires 30 days after activation.\n"
        "• *Family Plan:* Unlimited lifespan, listed and managed by custom names.\n\n"
        "🛠️ *Administrator Commands:*\n"
        "🔑 `/weekly` - Generate a Weekly Plan token (2,000 NGN)\n"
        "💎 `/monthly` - Generate a Monthly Plan token (8,000 NGN)\n"
        "🏠 `/family <LabelName>` - Generate a Named Family token (Never expires automatically)\n"
        "📋 `/list` - List all Named Family licenses currently active\n"
        "❌ `/expire <TokenOrName>` - Expire/Deactivate an active or unused token instantly\n"
    )
    bot.send_message(message.chat.id, help_text, parse_mode="Markdown")

# Command: /weekly
@bot.message_handler(commands=['weekly'])
def make_weekly(message):
    token = generate_random_token("WKL", 6)
    db_path = f"tokens/{token}"
    
    payload = {
        "tokenCode": token,
        "planType": "weekly",
        "status": "unused",
        "price": "2,000 NGN",
        "generatedAt": {".sv": "timestamp"}
    }
    
    try:
        r = requests.put(get_db_url(db_path), json=payload)
        if r.status_code == 200:
            msg = (
                "🆕 *WEEKLY PLAN TOKEN GENERATED!* 🆕\n\n"
                f"🔑 *Code:* `{token}`\n"
                "💰 *Price:* 2,000 NGN\n"
                "⌛ *Lifespan:* 7 Days (Triggers upon activation)\n"
                "📌 *Status:* Unused"
            )
            bot.send_message(message.chat.id, msg, parse_mode="Markdown")
        else:
            bot.reply_to(message, f"❌ Firebase Database Error: {r.status_code}\n{r.text}")
    except Exception as e:
        bot.reply_to(message, f"❌ Connection Error: {str(e)}")

# Command: /monthly
@bot.message_handler(commands=['monthly'])
def make_monthly(message):
    token = generate_random_token("MTH", 6)
    db_path = f"tokens/{token}"
    
    payload = {
        "tokenCode": token,
        "planType": "monthly",
        "status": "unused",
        "price": "8,000 NGN",
        "generatedAt": {".sv": "timestamp"}
    }
    
    try:
        r = requests.put(get_db_url(db_path), json=payload)
        if r.status_code == 200:
            msg = (
                "🆕 *MONTHLY PLAN TOKEN GENERATED!* 🆕\n\n"
                f"🔑 *Code:* `{token}`\n"
                "💰 *Price:* 8,000 NGN\n"
                "⌛ *Lifespan:* 30 Days (Triggers upon activation)\n"
                "📌 *Status:* Unused"
            )
            bot.send_message(message.chat.id, msg, parse_mode="Markdown")
        else:
            bot.reply_to(message, f"❌ Firebase Database Error: {r.status_code}\n{r.text}")
    except Exception as e:
        bot.reply_to(message, f"❌ Connection Error: {str(e)}")

# Command: /family <LabelName>
@bot.message_handler(commands=['family'])
def make_family(message):
    args = message.text.split(maxsplit=1)
    if len(args) < 2:
        bot.reply_to(message, "⚠️ Usage: `/family <Name/Label>`\nExample: `/family MichaelChukwuemeka`")
        return
    
    label_name = args[1].strip()
    token = generate_random_token("FAM", 6)
    db_path = f"tokens/{token}"
    
    payload = {
        "tokenCode": token,
        "planType": "family",
        "name": label_name,
        "status": "unused",
        "price": "Lifetime Custom",
        "generatedAt": {".sv": "timestamp"}
    }
    
    try:
        r = requests.put(get_db_url(db_path), json=payload)
        if r.status_code == 200:
            msg = (
                "🆕 *FAMILY LIFETIME TOKEN GENERATED!* 🆕\n\n"
                f"👤 *Label Name:* `{label_name}`\n"
                f"🔑 *Code:* `{token}`\n"
                "⌛ *Lifespan:* Never Expires Automatically\n"
                "📌 *Status:* Unused"
            )
            bot.send_message(message.chat.id, msg, parse_mode="Markdown")
        else:
            bot.reply_to(message, f"❌ Firebase Database Error: {r.status_code}\n{r.text}")
    except Exception as e:
        bot.reply_to(message, f"❌ Connection Error: {str(e)}")

# Command: /list
@bot.message_handler(commands=['list'])
def list_tokens(message):
    try:
        r = requests.get(get_db_url("tokens"))
        if r.status_code != 200:
            bot.reply_to(message, f"❌ Firebase Database Error: {r.status_code}")
            return
        
        data = r.json()
        if not data:
            bot.send_message(message.chat.id, "📭 No active plan tokens found in database.")
            return

        response_lines = ["📋 *ACTIVE FAMILY LIFETIME PLAN TOKENS:* \n"]
        count = 0
        
        for code, details in data.items():
            if details.get("planType") == "family":
                status = details.get("status", "unused")
                name = details.get("name", "Unnamed")
                activated_by = details.get("activatedBy", "None")
                
                status_emoji = "🟢 Unused" if status == "unused" else f"🔴 Active by {activated_by}"
                response_lines.append(f"• *Name:* `{name}`\n  🔑 *Code:* `{code}` ({status_emoji})")
                count += 1
                
        if count == 0:
            bot.send_message(message.chat.id, "📭 No family tokens exist in the database.")
        else:
            bot.send_message(message.chat.id, "\n\n".join(response_lines), parse_mode="Markdown")
            
    except Exception as e:
        bot.reply_to(message, f"❌ Connection Error: {str(e)}")

# Command: /expire <TokenOrName>
@bot.message_handler(commands=['expire'])
def expire_token(message):
    args = message.text.split(maxsplit=1)
    if len(args) < 2:
        bot.reply_to(message, "⚠️ Usage: `/expire <TokenCode_OR_FamilyLabelName>`\nExample: `/expire WFS-FAM-ABC123`")
        return
    
    target = args[1].strip()
    
    try:
        # Load all tokens to check both by KEY (TokenCode) or by family VALUE (name)
        r = requests.get(get_db_url("tokens"))
        if r.status_code != 200:
            bot.reply_to(message, f"❌ Firebase Database Error: {r.status_code}")
            return
            
        tokens = r.json() or {}
        found_code = None
        details = {}
        
        # Exact match check
        if target in tokens:
            found_code = target
            details = tokens[target]
        else:
            # Check family names
            for code, info in tokens.items():
                if info.get("name") == target:
                    found_code = code
                    details = info
                    break
        
        if not found_code:
            bot.reply_to(message, f"🔍 Target `{target}` not matched in registration directories.")
            return
            
        # Delete token node from database to secure expire it
        del_r = requests.delete(get_db_url(f"tokens/{found_code}"))
        if del_r.status_code == 200:
            name_label = f" ({details.get('name')})" if details.get('name') else ""
            msg = (
                "🛑 *TOKEN EXPIRED SUCCESSFULLY!* 🛑\n\n"
                f"🔑 *Code:* `{found_code}`{name_label}\n"
                f"📋 *Plan:* {details.get('planType', 'unknown')}\n"
                "⚡ *Action:* Suspended and removed permanently from whitelist records."
            )
            bot.send_message(message.chat.id, msg, parse_mode="Markdown")
        else:
            bot.reply_to(message, f"❌ Firebase delete failure: {del_r.status_code}")
            
    except Exception as e:
        bot.reply_to(message, f"❌ Connection Error: {str(e)}")

if __name__ == "__main__":
    print("[ACTIVE] Wfseek Administration Telegram Bot Pipeline Listening...")
    bot.infinity_polling()
