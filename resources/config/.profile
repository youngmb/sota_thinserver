# ~/.profile: executed by the command interpreter for login shells.
# This file is not read by bash(1), if ~/.bash_profile or ~/.bash_login
# exists.
# see /usr/share/doc/bash/examples/startup-files for examples.
# the files are located in the bash-doc package.

# the default umask is set in /etc/profile; for setting the umask
# for ssh logins, install and configure the libpam-umask package.
#umask 022

# if running bash
if [ -n "$BASH_VERSION" ]; then
    # include .bashrc if it exists
    if [ -f "$HOME/.bashrc" ]; then
	. "$HOME/.bashrc"
    fi
fi

# set PATH so it includes user's private bin if it exists
if [ -d "$HOME/bin" ] ; then
    PATH="$HOME/bin:$PATH"
fi

#export LANG="ja_JP.utf8"
# set language to default to get English wherever possible
export LANG=C

#add Java Binaries to the path
export PATH="/home/vstone/java/jdk1.8.0_40/bin/:$PATH"

#let Java know where the libs are
export CLASSPATH="/home/vstone/lib;.;$CLASSPATH"

#set the link path so that Java knows where to find binaries
export LD_LIBRARY_PATH=/usr/local/share/OpenCV/java/:/home/vstone/lib

# Sota play microphone volume
amixer -c 2 cset numid=7 90%
