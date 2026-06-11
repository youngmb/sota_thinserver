# Sota ThinServer

A thin server package for the VStone Sota robot that enables mostly remote Sota use and programming once installed.
* installs a Sota app that runs at boot and launches an HTTP server
* installs a suite of test programs in the root home folder
* replaces the Japanese core audio (menus, etc.) with English (TODO: make this optional and configurble)

The API is straightforward but a thinclient in Python that connects to this server can be found at 
[the Sota thinclient git repository](https://github.com/youngmb/sota_thinclient).

## requirements
* It is recommended to start with a completely fresh Sota. You can reset Sota using the instructions found in `docs/sota-reset.md`. We have noticed some oddities with Sota versioning so if things do not work, try first to reset the sota  and be sure **do not run the Sota update**.

## installation

The build and installation is done using Gradle. Gradle scripts and targets have been included. Make sure to have
a JDK installed and configured that can target Java8, e.g., JDK 21

Edit the `gradle/build.gradle` file to set `scpServer` to the Sota IP. Configure the sota username and root
(the defaults are probably fine)

Run the `installAll` Gradle target, which compiles the server to target Java 8, installs the jars to the home folder,
acquires (from Maven) and copies the required libs to the Sota, sets our app to start on Sota boot, and does various
system setups including replacing the Japanese default voices with English.

(TODO: distribute precompiled class files since targeting Java 8 will become increasingly difficult)

If `installAll` fails due to peer disconnect, try again as the Sota wifi can sometimes be flaky. If it fails for
other reasons reach out so we can consider what happened.

Once finished, reboot the Sota (e.g., using the `sotaReboot` Gradle target). Once the server is loaded you will hear
a coin sound (as from Super mario!), indicating that the server is ready.

## configuration

### Sota server configuration
The server has minimal configuration exposed via a Java .properties file. You can modify the 
`/home/root/thinserver/sotathinclient.properties` file and reboot the server for the changes to take effect.
Note that re-installing this package will overwrite that file so consider editing this package file 
`install/resources/sotathinclient.properties` instead.
* the HTTP port defaults to 8080 but can be changed here
* various audio settings can be tuned but probably best to be done through the API instead (TODO: remove items now
  available in the API)

### Sota motor calibration
The thinserver's kinematics code relies on accurate calibration of the Sota's motor ranges. Although the packaged 
default settings should be fine, some Sota robots have more variation in how their motors are installed.

The default calibration is stored in a Java serialization file, `/home/root/sota/motorranges-default.dat`. You can
calibrate yourself to a specific Sota, which will produce a `motorranges.dat` file to override the defaults.
You can delete your file at any time to revert to the defaults, no need to delete the defaults file.

Launch the motor calibrator found in the `/home/root/thinserver` directory.

`java -jar ./ServoRangeCalibrator.jar`

It will show a readout for each joint on the Sota, including the minimum, maximum, and mid point, and how this converts
to radians. To conduct a calibration:
- one by one, move each motor to its extreme positions slowly. There are 8 motors.
- press the sota power button to close the tool and save the new `motorranges.dat` file.

## testing
Once the thinserver is installed you can test in two ways

### test programs
The following programs are installed in `/home/root/thinserver` and can be run to test local functionalty
- `ServoAngleReader` - reads servo values and prints them in radians. Tests servo reading.
- `ServoFKTest` - reads servo values and prints their cartesian coordinates wrt the Sota frame. Note that Sota looks 
down the negative Y axis and it's a right handed system.
- `ServoRangeTest` - tests servo motion. Centers the robot's motors, then tests each servo one by one.
- `ServoIKTest` - tests the IK enging by making the robot do a prescribed circular hand motion while moving the
head. Note that the head twitches as part of the design and the body also rotates slightly.

### HTTP server test
Once the server is running you can test it on the localhost using a REST interface. We have included a suite of
(poorly curated) test cases in `tests/HTTPTests.txt` which you can run on the command line. A simple test is to
get the current details of the Sota motors:
- `curl http://localhost:8080/pose/system | python -m json.tool`
- `curl http://localhost:8080/pose/ | python -m json.tool`


## usage
The server exposes HTTP end points that you can use to configure, read, and command the motors and LEDs. You can also
use this to enable audio streaming (in and out) and set the parameters and video streaming.

This is meant to be used with [the Sota thinclient](https://github.com/youngmb/sota_thinclient) but the interface is
simple and can be integrated into other projects.