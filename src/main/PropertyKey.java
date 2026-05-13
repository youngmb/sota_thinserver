package main;

// SETUP the property keys all here. This keeps them all in one place and avoids
// having stray strings everywhere that need maintenance

public enum PropertyKey {
   // Network
   KEY_NET_SEQ_MOD("net_seq_mod", "1000000"), // packet sequence numbers always positive, when to wrap
   KEY_NET_UDP_WAITBUFFER_SIZE("net_UDP_waitbuffer_size", "5"),
   KEY_NET_HTTP_PORT("net_http_port", "8080"),

    // Mic
    KEY_MIC_SAMPLE_RATE("microphone_sample_rate", "16000"),
    KEY_MIC_BUFFER_SIZE("microphone_buffer_size", "512"),
    KEY_MIC_SAMPLE_SIZE("microphone_sample_size", "16"),
    KEY_MIC_CHANNELS("microphone_channels", "1"),

    // Speaker
    KEY_SPK_SAMPLE_RATE("speaker_sample_rate", "16000"),
    KEY_SPK_SAMPLE_SIZE("speaker_sample_size", "16"), // bits
    KEY_SPK_BUFFER_SIZE("speaker_buffer_size", "512"),
    KEY_SPK_CHANNELS("speaker_channels", "1"),
    ;

    private final String key;
    private final String defaultValue;

    PropertyKey(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String key() {
        return key;
    }

    public String defaultValue() {
        return defaultValue;
    }
}


