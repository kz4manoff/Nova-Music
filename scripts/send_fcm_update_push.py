#!/usr/bin/env python3
"""Send NOVA MUSIC update push notifications via Firebase Cloud Messaging HTTP v1 API.

Credentials are never stored in this repository or the Android app. Supply a Firebase
Service Account JSON via FIREBASE_SERVICE_ACCOUNT_JSON (raw JSON or Base64) or via
--service-account-file.
"""

from __future__ import annotations

import argparse
import base64
import json
import os
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
from typing import Any


FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token"
DEFAULT_TOPIC = "nova_updates"
CLICK_ACTION = "com.novamusic.player.ACTION_OPEN_UPDATE_DETAILS"
CHANNEL_ID = "nova_updates"


def _b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def load_service_account(file_path: str | None) -> dict[str, Any]:
    raw_text = ""
    if file_path:
        with open(file_path, "r", encoding="utf-8") as handle:
            raw_text = handle.read().strip()
    else:
        raw_text = os.environ.get("FIREBASE_SERVICE_ACCOUNT_JSON", "").strip()

    if not raw_text:
        raise ValueError(
            "Missing Firebase service account credentials. Provide --service-account-file "
            "or set FIREBASE_SERVICE_ACCOUNT_JSON."
        )

    if not raw_text.startswith("{"):
        try:
            raw_text = base64.b64decode(raw_text).decode("utf-8").strip()
        except Exception as exc:
            raise ValueError(
                f"FIREBASE_SERVICE_ACCOUNT_JSON is neither valid JSON nor Base64 JSON ({exc})."
            ) from exc

    parsed = json.loads(raw_text)
    for required_key in ("project_id", "client_email", "private_key"):
        if not parsed.get(required_key):
            raise ValueError(f"Service account JSON is missing required field '{required_key}'.")
    return parsed


def sanitize_short_description(raw: str, max_chars: int = 140) -> str:
    collapsed = " ".join((raw or "").split())
    if not collapsed:
        return "Performance improvements and playback refinements."
    if len(collapsed) <= max_chars:
        return collapsed
    clipped = collapsed[: max_chars - 1].rstrip()
    last_space = clipped.rfind(" ")
    if last_space > max_chars // 2:
        clipped = clipped[:last_space]
    return f"{clipped}…"


def build_fcm_message(
    *,
    version_name: str,
    version_code: int,
    description: str,
    apk_url: str = "",
    sha256: str = "",
    mandatory: bool = False,
    topic: str = DEFAULT_TOPIC,
    token: str = "",
) -> dict[str, Any]:
    clean_version = version_name.strip().lstrip("vV")
    if not clean_version:
        raise ValueError("version_name must not be empty.")
    if version_code <= 0:
        raise ValueError("version_code must be a positive integer.")

    short_desc = sanitize_short_description(description)
    title = "New Update Available!"
    body = f"Version {clean_version} — {short_desc}"

    message: dict[str, Any] = {
        "notification": {
            "title": title,
            "body": body,
        },
        "data": {
            "type": "nova_update",
            "versionName": clean_version,
            "versionCode": str(version_code),
            "shortDescription": short_desc,
            "releaseTitle": title,
            "apkUrl": apk_url.strip(),
            "sha256": sha256.strip().lower(),
            "mandatoryUpdate": "true" if mandatory else "false",
        },
        "android": {
            "priority": "HIGH",
            "collapse_key": "nova_update",
            "ttl": "604800s",
            "notification": {
                "channel_id": CHANNEL_ID,
                "icon": "ic_nova_notification",
                "color": "#FF1744",
                "tag": "nova_update_latest",
                "click_action": CLICK_ACTION,
            },
        },
    }

    if token.strip():
        message["token"] = token.strip()
    else:
        message["topic"] = topic.strip() or DEFAULT_TOPIC

    return {"message": message}


def sign_jwt_rs256(service_account: dict[str, Any], lifetime_sec: int = 3600) -> str:
    now = int(time.time())
    header = {"alg": "RS256", "typ": "JWT"}
    if service_account.get("private_key_id"):
        header["kid"] = service_account["private_key_id"]

    claims = {
        "iss": service_account["client_email"],
        "scope": FCM_SCOPE,
        "aud": OAUTH_TOKEN_URL,
        "iat": now,
        "exp": now + lifetime_sec,
    }

    signing_input = (
        f"{_b64url(json.dumps(header, separators=(',', ':')).encode('utf-8'))}."
        f"{_b64url(json.dumps(claims, separators=(',', ':')).encode('utf-8'))}"
    ).encode("ascii")

    key_pem = service_account["private_key"].replace("\\n", "\n")
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", delete=False) as key_file:
        key_file.write(key_pem)
        key_path = key_file.name

    try:
        os.chmod(key_path, 0o600)
        proc = subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", key_path],
            input=signing_input,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )
        if proc.returncode != 0:
            raise RuntimeError(
                f"Failed to sign OAuth2 JWT via openssl: {proc.stderr.decode('utf-8', errors='replace')}"
            )
        signature = proc.stdout
    finally:
        if os.path.exists(key_path):
            os.remove(key_path)

    return f"{signing_input.decode('ascii')}.{_b64url(signature)}"


def fetch_oauth_access_token(service_account: dict[str, Any]) -> str:
    assertion = sign_jwt_rs256(service_account)
    form = urllib.parse.urlencode(
        {
            "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
            "assertion": assertion,
        }
    ).encode("utf-8")

    req = urllib.request.Request(
        OAUTH_TOKEN_URL,
        data=form,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as err:
        detail = err.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Google OAuth2 token exchange failed (HTTP {err.code}): {detail}") from err

    access_token = payload.get("access_token")
    if not access_token:
        raise RuntimeError("OAuth2 response did not contain access_token.")
    return access_token


def send_fcm_http_v1(project_id: str, access_token: str, body: dict[str, Any]) -> dict[str, Any]:
    url = f"https://fcm.googleapis.com/v1/projects/{project_id}/messages:send"
    encoded = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=encoded,
        headers={
            "Authorization": f"Bearer {access_token}",
            "Content-Type": "application/json; charset=UTF-8",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as err:
        detail = err.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"FCM HTTP v1 send failed (HTTP {err.code}): {detail}") from err


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Send NOVA MUSIC FCM update notification")
    parser.add_argument("--version-name", required=True, help="Semantic release version (e.g. 1.2.1)")
    parser.add_argument("--version-code", required=True, type=int, help="Integer Android versionCode")
    parser.add_argument("--description", required=True, help="Short release description shown in alert")
    parser.add_argument("--apk-url", default="", help="Verified signed APK download URL")
    parser.add_argument("--sha256", default="", help="Verified APK SHA-256 hex digest")
    parser.add_argument("--mandatory", action="store_true", help="Mark release as mandatory")
    parser.add_argument("--topic", default=DEFAULT_TOPIC, help=f"FCM topic (default: {DEFAULT_TOPIC})")
    parser.add_argument("--token", default="", help="Optional single-device FCM token instead of topic")
    parser.add_argument(
        "--service-account-file",
        default=None,
        help="Optional path to Firebase service account JSON file (falls back to FIREBASE_SERVICE_ACCOUNT_JSON)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Validate and print payload JSON without authenticating or sending network requests",
    )

    args = parser.parse_args(argv)
    payload = build_fcm_message(
        version_name=args.version_name,
        version_code=args.version_code,
        description=args.description,
        apk_url=args.apk_url,
        sha256=args.sha256,
        mandatory=args.mandatory,
        topic=args.topic,
        token=args.token,
    )

    if args.dry_run:
        print(json.dumps(payload, indent=2))
        return 0

    service_account = load_service_account(args.service_account_file)
    access_token = fetch_oauth_access_token(service_account)
    response = send_fcm_http_v1(service_account["project_id"], access_token, payload)
    print(f"FCM notification sent successfully: {json.dumps(response)}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        raise SystemExit(1)
