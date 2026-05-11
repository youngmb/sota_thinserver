package httpserver.status;

import main.Properties;
import main.PropertyKey;

//@JsonIgnoreProperties(ignoreUnknown = true)
public class AudioStatus {
    // Use boxed types so incomplete json packet produces nulls.

    public Boolean enabled;
    public Integer volume;
    public Integer streamPort;
    public String streamIP;
    public String error;

    public Integer sampleRate;
    public Integer bufferSize;
    public Integer sampleSize;
}
