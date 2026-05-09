package httpserver.status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import main.Properties;
import main.PropertyKey;

//@JsonIgnoreProperties(ignoreUnknown = true)
public class MicStatus {
    // Use boxed types so incomplete json packet produces nulls.

    public Boolean enabled;
    public Integer volume;
    public Integer streamSendPort;
    public String streamIP;
    public String error;

    private final Integer sampleRate = Properties.getPropAsInt(PropertyKey.KEY_MIC_SAMPLE_RATE);
    private final Integer bufferSize = Properties.getPropAsInt(PropertyKey.KEY_MIC_BUFFER_SIZE);
    private final Integer micSampleSize = Properties.getPropAsInt(PropertyKey.KEY_MIC_SAMPLE_SIZE);
}
