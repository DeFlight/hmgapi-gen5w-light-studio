#!/bin/sh
# Builds Light Studio into out/: the app (app/LightStudio.apk), the lamp engine (engine.jar),
# the signed mod zip and the USB zips.
#
# Needs a JDK (javac --release 8), and these environment variables:
#   ANDROID_JAR    android.jar for API 19 (Android 4.4)
#   BUILD_TOOLS    an Android build-tools folder (d8, aapt, zipalign, apksigner)
#   APP_KEYSTORE   keystore (.p12) that signs the app; APP_KEYSTORE_PASS its password
#   HMGAPI_CP      classpath holding hmgapi.engine.Signing (HMG-API's mod SDK)
#   MOD_KEY        the mod's HMG-API author key
#   MKNEMESISMOD   HMG-API's tools/mknemesismod.sh (optional: the NEMESIS zips)
# Keys never go in this repository.
set -eu
cd "$(dirname "$0")"
: "${ANDROID_JAR:?}" "${BUILD_TOOLS:?}" "${APP_KEYSTORE:?}" "${APP_KEYSTORE_PASS:?}" "${HMGAPI_CP:?}" "${MOD_KEY:?}"
ID=gen5w-light-studio
V=$(sed -n 's/.*"version": *"\([^"]*\)".*/\1/p' mod.json | head -1)
B=out/build
rm -rf out && mkdir -p $B/engine $B/app/classes $B/app/dex $B/mod/app $B/mod/svc $B/usb/HMGAPI/updates

# engine.jar: the lamp engine, run by svc/engine.sh under app_process
javac --release 8 -Xlint:-options -encoding UTF-8 -nowarn -cp "$ANDROID_JAR" -d $B/engine $(find src/engine src/common -name '*.java')
"$BUILD_TOOLS/d8" --release --min-api 19 --lib "$ANDROID_JAR" --output $B/mod/engine.jar $(find $B/engine -name '*.class')

# app/LightStudio.apk: the screen
javac --release 8 -Xlint:-options -encoding UTF-8 -nowarn -cp "$ANDROID_JAR" -d $B/app/classes $(find src/app/java src/common -name '*.java')
"$BUILD_TOOLS/d8" --release --min-api 19 --lib "$ANDROID_JAR" --output $B/app/dex $(find $B/app/classes -name '*.class')
"$BUILD_TOOLS/aapt" package -f -M src/app/AndroidManifest.xml -S src/app/res -I "$ANDROID_JAR" -F $B/app/unsigned.apk
(cd $B/app/dex && "$BUILD_TOOLS/aapt" add ../unsigned.apk classes.dex >/dev/null)
"$BUILD_TOOLS/zipalign" -f 4 $B/app/unsigned.apk $B/app/aligned.apk
"$BUILD_TOOLS/apksigner" sign --v4-signing-enabled false --ks "$APP_KEYSTORE" --ks-pass "pass:$APP_KEYSTORE_PASS" \
    --out $B/mod/app/LightStudio.apk $B/app/aligned.apk

# signed mod zip, fixed timestamps so the same sources give the same bytes
cp mod.json settings.json $B/mod/ && cp svc/engine.sh $B/mod/svc/
mkdir -p $B/mod/icon && cp icon/icon*.png $B/mod/icon/
java -cp "$HMGAPI_CP" hmgapi.engine.Signing sign $B/mod "$MOD_KEY"
(cd $B/mod && find . -exec touch -d '2026-01-01 00:00:00' {} + && \
    zip -X -q ../../$ID-v$V.zip META-HMGAPI/manifest.json META-HMGAPI/signature \
        app/LightStudio.apk engine.jar svc/engine.sh mod.json settings.json icon/*.png)

cp out/$ID-v$V.zip $B/usb/HMGAPI/updates/
(cd $B/usb && zip -r -q ../../$ID-v$V-hmgapi-usb.zip HMGAPI)
if [ -n "${MKNEMESISMOD:-}" ]; then
    sh "$MKNEMESISMOD" "$PWD/out/$ID-v$V.zip" "$PWD/$B/nemesis" >/dev/null
    mv $B/nemesis/*-nemesis.zip out/$ID-v$V-nemesis-usb.zip
    mv $B/nemesis/*-nemesis-package.zip out/$ID-v$V-nemesis-package.zip
fi
(cd out && sha256sum $ID-v$V*.zip > SHA256SUMS && cat SHA256SUMS)
