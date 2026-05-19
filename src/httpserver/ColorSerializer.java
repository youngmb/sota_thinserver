package httpserver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.awt.*;
import java.io.IOException;

// our own serializer for Java colors to make the hex instead of 3 fields.
public class ColorSerializer {

    static public class Serializer extends StdSerializer<Color> {

        public Serializer() { super(Color.class); }

        @Override
        public void serialize(Color color, JsonGenerator gen, SerializerProvider provider) throws IOException {
            String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            gen.writeString(hex);
        }
    }

    static public class Deserializer extends JsonDeserializer<Color> {

        @Override
        public Color deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            String hex = p.getText(); // hexstring

            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }

            if (hex.length() == 2) { // monochrome
                int c = Integer.parseInt(hex, 16);
                return new Color(c, c, c);

            } else if (hex.length() == 6) {  // RGB
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                return new Color(r, g, b);
            } else {
                throw new IllegalArgumentException("Invalid color hex: " + hex);
            }
        }
    }
}