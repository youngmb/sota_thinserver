package httpserver.status;

import main.Properties;
import main.PropertyKey;

public class SpeakerStatus {
    public Boolean enabled;
    public Integer volume = 50;
    public Integer streamListenPort;
    public String error;

    private final Integer sampleRate = Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_RATE);
    private final Integer bufferSize = Properties.getPropAsInt(PropertyKey.KEY_SPK_BUFFER_SIZE);
    private final Integer micSampleSize = Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_SIZE);
}