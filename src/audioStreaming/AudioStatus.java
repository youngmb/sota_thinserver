package audioStreaming;

//@JsonIgnoreProperties(ignoreUnknown = true)
public class AudioStatus {
    // Use boxed types so incomplete json packet produces nulls.

    public Boolean enabled;
    public Integer volume;
    public Integer streamPort;
    public String streamIP;

    public Integer bufferSize;

    public Integer sampleRate;
    public Integer sampleSize_bits;
    public Integer channels;
}
