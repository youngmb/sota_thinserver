# Sota ThinServer

Thin server for the VStone Sota robot. Exposes a web API for controlling audio streaming, servo motor values, etc.

## usage

Compile, make jars, and upload resources to the robot using `ant`, configured in the `build.xml` file.

## configuration

### motor information
The `ServoMappingTools` object provides helper functions for servos and robot pose objects, and requires motor information stored in a `motorranges.dat` file.
If you don't already have one, can make this file by running `ServoRangeCalibrator.jar` as follows:

1. run `java -jar sotamotorcalibrator.jar`.
2. move all 8 servo motors to either end of their extremes so their ranges are known.
3. click Sota's power button to save the file.