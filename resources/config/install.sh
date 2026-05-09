# copy these files to the appropriate directories on Sota
cp .profile ~/
cp ./system_sounds/vol_up.wav /home/vstone/vstonemagic/volume/
cp ./system_sounds/vol_down.wav /home/vstone/vstonemagic/volume/
cp ./system_sounds/ohayou.wav /home/vstone/vstonemagic/power/
cp ./system_sounds/oyasumi.wav /home/vstone/vstonemagic/power/
cp ./scripts/*.sh /home/vstone/vstonemagic/volume/

# make these overridden volume scripts executable
chmod +x /home/vstone/vstonemagic/volume/volume*.sh
chmod +x /home/vstone/vstonemagic/volume/speakerctl.sh

# replace all the volume<number>.wav files with a single volume sound
for f in /home/vstone/vstonemagic/volume/voice/volume*.wav; do
    cp ./system_sounds/im_now_this_loud.wav "$f"
done

# make the app automatically start on boot
## allow switching to demo jar when any command-line argument contains "demo"
# default jar
JARNAME=play
# if any arg contains the substring "demo", use demo.jar instead
for _a in "$@"; do
    if [[ "$_a" == *demo* ]]; then
        JARNAME=demo
        break
    fi
done

# make the app automatically start on boot
mkdir -p /home/vstone/vstonemagic/app/jar/${JARNAME}
if [[ "$JARNAME" == "demo" ]]; then
    cp ./demo.properties /home/vstone/vstonemagic/app/jar/app.properties
    else
    cp ./app.properties /home/vstone/vstonemagic/app/jar/
fi
cp ./sota.properties /home/vstone/vstonemagic/app/jar/${JARNAME}
cp /home/root/play/jars/${JARNAME}.jar /home/vstone/vstonemagic/app/jar/${JARNAME}/

# move the required libraries to the right place
cp /home/vstone/lib/libpocket* /usr/local/share/OpenCV/java

# shouldn't need to copy these ones, as they are present in /home/vstone/lib and linked with $LD_LIBRARY_PATH env variable (set in ~/.profile)
# cp /home/vstone/lib/commons* /usr/local/share/OpenCV/java
# cp /home/vstone/lib/core-2.2.jar /usr/local/share/OpenCV/java
# cp /home/vstone/lib/gdx* /usr/local/share/OpenCV/java
# cp /home/vstone/lib/javase-2.2.jar /usr/local/share/OpenCV/java
# cp /home/vstone/lib/jna-4.1.0.jar /usr/local/share/OpenCV/java
# cp /home/vstone/lib/sotalib.jar /usr/local/share/OpenCV/java
# cp /home/vstone/lib/SRClientHelper.jar /usr/local/share/OpenCV/java
# cp /home/vstone/lib/voice* /usr/local/share/OpenCV/java

# disable the sotaupdate service that's been causing issues
systemctl disable sotaupdate.service

# set the microphone gain
# ---amixer -c 2 cset numid=7 90% - OLD - would do it manually
# now we install a service to do it on every boot.
cp alsa-vol.service /lib/systemd/system
systemctl daemon-reload
systemctl enable alsa-vol.service
systemctl start alsa-vol.service

