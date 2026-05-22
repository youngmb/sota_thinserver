package videoStreaming;

public class VideoFrame {
        public int size;
        public byte[] data;
        public VideoFrame(int size, byte[] data) { this.size = size; this.data = data;}

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
        }
}
