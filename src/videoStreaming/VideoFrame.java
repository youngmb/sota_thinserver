package videoStreaming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VideoFrame {
        public int size;
        public byte[] data;
        public VideoFrame(int size, byte[] data) { this.size = size; this.data = data;}

        @JsonFormat(shape = JsonFormat.Shape.OBJECT)
        public enum ImageSize {
                QVGA(320, 240),
                VGA(640, 480),
                SVGA(800, 600),
                XGA(1024, 768),
                HD_720(1280, 720),
                SXGA(1280, 1024),
                UXGA(1600, 1200),
                HD_1080(1920, 1080),
                QXGA(2048, 1536),
                MP_5(2592, 1944);

                public final int width;
                public final int height;

                ImageSize(int width, int height) { this.width = width;  this.height = height;}

                // for the json serializer to expose the Enum name as well
                public String getName() { return name();  }

                @JsonCreator
                public static ImageSize fromJson( @JsonProperty("name") String name) {
                        return ImageSize.valueOf(name);
                }
        }

        public enum ImageFormat {  // to fit the underlying library
                YUV2(0),
                MJPG(1),
                BGR_3BYTE(2),
                BYTE_GRAY(3);

                public final int libraryKey;

                ImageFormat(int libraryKey) {
                        this.libraryKey = libraryKey;
                }

                // for the json serializer to expose the Enum name as well
                public String getName() { return name();  }

                @JsonCreator
                public static ImageFormat fromJson( @JsonProperty("name") String name) {
                        return ImageFormat.valueOf(name);
                }
        }
}
