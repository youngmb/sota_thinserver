package sota.tools;

import jp.vstone.RobotLib.CRobotPose;
import jp.vstone.RobotLib.CSotaMotion;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

import java.awt.*;
import java.io.*;
import java.util.Map;

public class LEDHelp {

    final public static int LED_ENTRY_COUNT = 10; // RGB lights each count as 3

    public enum LEDs { // start index of light. note that RGB has 3 entries
        POWER("power", 0, 3), // RGB
        ///  I wonder what 3-7 are mapped to
        EYE_L("leftEye", 11, 3), // RGB
        EYE_R("rightEye", 8, 3),
        MOUTH("mouth", 14, 1);

        public final byte index;
        public final int count;
        public final String label;
        LEDs(String label, int index, int count) {
            this.index = (byte)index; this.count = count; this.label = label;
        }

        static public LEDs fromLabel(String label) {
            for (LEDs led: LEDs.values()) {
                if (label.equals(led.label))
                    return led;
            }
            return null;
        }
    }

    public static Color getLEDColor(Map<Byte, Short> LEDData, LEDs led) {
        if (led.count == 1) { // monochrome
            Short c = LEDData.get(led.index);
            return new Color(c, c, c);

        } else if (led.count == 3) {
            Short r = LEDData.get(led.index);
            Short g = LEDData.get((byte)(led.index+1));
            Short b = LEDData.get((byte)(led.index+1));
            return new Color(r, g, b);
        }

        // should never happen
        return null;
    }

    public static void setLED(Map<Byte, Short> LEDMap, String LED, Color color){ setLED(LEDMap, LEDs.fromLabel(LED), color); }

    public static void setLED(Map<Byte, Short> LEDMap, LEDs LED, Color color){
        if (LEDMap == null || LED == null || color == null) {
            System.err.println("ERROR: setting LED with null");
            return;
        }

        boolean flipGreen = LED == LEDs.POWER; // special case for power button

        float[] rgb = color.getRGBColorComponents(null);
        for (int i = 0; i < LED.count; i++) {
            short c = (short) (rgb[i] * 255);
            if (flipGreen && i==1) // GREEN
                c = (short)(255-c);
            LEDMap.put((byte) (LED.index + i), c);
        }
    }
}