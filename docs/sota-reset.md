# Sota Factory Reset

This document outlines instructions to factory reset the Sota robot in case it is having issues.

The document is inspired by [this post on VStone's website](https://sota.vstone.co.jp/sota/resource/faq/faq.php?faq_cd=22), but be warned it contains some wrong turns so follow these instructions here (e.g., it gives the wrong link for the zip and also suggests you run their awful update service after).

## Requirements
- Sota needs to be connected to a network with internet access, and have an accessible IP.
- **do not, under any circumstances, run the Sota Update found in the buttons menu** (step 12 of the linked instructions).

## Procedure

SSH or serial into your Sota and run the following commands:

`mkfs.vfat /dev/mmcblk0p9` \
`mount -t vfat /dev/mmcblk0p9 /mnt` \
`cd /mnt` \
`wget www.vstone.co.jp/sota/update/edison_repair/edison-image-ww25-15.tar.bz2` \
`tar jxvf edison-image-ww25-15.tar.bz2` \
`mv edison-image-ww25-15/* ./` \
`reboot ota`

With the last command, Sota will reboot.

You now need to be connected to Sota via serial (USB) as it will not remember the internet connection.

Login as root, with an empty password this time. Connect to a network:

`configure_edison --wifi`

Once network connection established (test with `curl google.com`), proceed:

`rm -r /home/vstone/*` \
`mkfs.vfat /dev/mmcblk0p9` \
`mount -t vfat /dev/mmcblk0p9 /mnt` \
`cd /mnt` \
`wget www.vstone.co.jp/sota/update/edison_repair/edisoninit.zip` \
`unzip edisoninit.zip` \
`cd edisoninit` \
`chmod +x init.sh` \
`./init.sh` \
`export PATH=$PATH":/home/vstone/java/jdk1.8.0_40/bin"`

Finally, run `poweroff`

You should be done!