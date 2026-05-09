package main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Properties {

    private static final String PROPERTIES_FILE = "sota.properties";
    private static java.util.Properties sotaProperties = loadProperties();

    private static java.util.Properties loadProperties() {
        java.util.Properties prop = null;

        try (InputStream input = new FileInputStream(PROPERTIES_FILE)) {
            prop = new java.util.Properties();
            prop.load(input);

        } catch (FileNotFoundException e) {
            System.out.println("Sota .properties file not found.. Expecting "+PROPERTIES_FILE);

        } catch (IOException e) {
            System.out.println("IO Exception.");
            e.printStackTrace();
        }
        return prop;
    }

    public static java.util.Properties getProperties() {
        if (sotaProperties == null)
            sotaProperties = loadProperties();
        return sotaProperties;
    }
    public static int getPropAsInt(PropertyKey pk) {
        if (sotaProperties != null && sotaProperties.containsKey(pk.key())) {
            String value = sotaProperties.getProperty(pk.key());
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Err could not parse property '"+pk.key()+"' value '"+value+"' into int. " +
                        "Reverting to Default of '"+pk.defaultValue()+"'.");
            }
        }
        return Integer.parseInt(pk.defaultValue());
    }

    public static String getProperty(PropertyKey pk) {
        if (sotaProperties.containsKey(pk.key()))
          return sotaProperties.getProperty(pk.key());
        else
          return pk.defaultValue();
    }
}
