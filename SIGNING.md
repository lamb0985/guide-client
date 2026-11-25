# Persistent Android APK signing

CI requires the same private PKCS#12 keystore for all distributed APKs. Never commit the private key or password. Local builds without signing credentials can still use the standard debug key and cannot upgrade the distributed APK.

## Initial setup (repository maintainer)

On a trusted machine with Java 17+ and authenticated GitHub CLI, run the following once in Bash. It creates a local backup outside the source tree and uploads the two encrypted repository secrets. Do not regenerate or replace an existing signing key. Retain a secure offline backup of both the keystore and password.

```bash
set -euo pipefail
umask 077
dir="$HOME/.local/share/guide-client-signing"
mkdir -p "$dir"
if [[ -e "$dir/guide-client.p12" || -e "$dir/password" ]]; then
  echo "Existing signing files found; refusing to overwrite."
  exit 1
fi
openssl rand -hex 32 > "$dir/password"
export GUIDE_SIGNING_PASSWORD="$(cat "$dir/password")"
keytool -genkeypair -storetype PKCS12 -keystore "$dir/guide-client.p12" -alias guide-client -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Guide Client' -storepass:env GUIDE_SIGNING_PASSWORD -keypass:env GUIDE_SIGNING_PASSWORD
base64 -w0 "$dir/guide-client.p12" | gh secret set GUIDE_SIGNING_KEY_B64 -R lamb0985/guide-client
gh secret set GUIDE_SIGNING_PASSWORD -R lamb0985/guide-client < "$dir/password"
unset GUIDE_SIGNING_PASSWORD
gh workflow run build.yml -R lamb0985/guide-client --ref main
```

The CI workflow refuses to publish a newly signed APK if either secret is missing. Build logs include the public certificate fingerprint, not the private key. Only trusted code should run with signing secrets.

## Upgrade

Uninstall versions prior to v0.1.3 without preserving data, install the first permanently signed build, then use normal in-place updates. The application ID is unchanged.
