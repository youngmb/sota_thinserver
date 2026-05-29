package main;

import java.io.*;

public class Properties {

    final public static String FALLBACK_DEFAULT_PROPERTIES_FILE = "/home/root/thinserver/sotathinclient.properties";
    private static final String PROPERTIES_FILE = "sotathinclient.properties";    ///NOTE FRAGILE DEPENDENCY
    private static java.util.Properties sotaProperties = loadProperties();

    private static String findFile() {
        File f = new File(PROPERTIES_FILE);
        if (f.exists() && !f.isDirectory())
            return PROPERTIES_FILE;

        f = new File(FALLBACK_DEFAULT_PROPERTIES_FILE);
        if (f.exists() && !f.isDirectory())
            return FALLBACK_DEFAULT_PROPERTIES_FILE;

        System.err.println("Error: cannot find motor ranges mapping file, checked both: \n\t"+PROPERTIES_FILE+"\n\t"+FALLBACK_DEFAULT_PROPERTIES_FILE);
        return null;
    }

    private static java.util.Properties loadProperties() {
        java.util.Properties prop = null;

        String filename = findFile();

        try (InputStream input = new FileInputStream(filename)) {
            prop = new java.util.Properties();
            prop.load(input);

        } catch (FileNotFoundException e) {
            System.out.println("Sota .properties file not found.. Expecting "+filename);

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

    public static void setProperty(PropertyKey pk, String value) {
        sotaProperties.setProperty(pk.key(), value);
    }
}
