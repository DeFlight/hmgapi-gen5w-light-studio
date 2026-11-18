#!/system/bin/sh
# Light Studio's lamp engine (Java, app_process, as root). When HMG-API stops it (the mod
# turned off or removed), the trap puts the car's own light settings back first.
J=$HMGAPI_MOD_DIR/engine.jar
restore() {
    echo "stopping: putting the car's own light back"
    CLASSPATH=$J app_process /system/bin com.deflight.lightstudio.engine.Engine restore
    exit 0
}
trap restore TERM INT HUP
CLASSPATH=$J app_process /system/bin com.deflight.lightstudio.engine.Engine run &
wait $!
echo "engine exited ($?)"
