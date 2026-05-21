/// /////// DECOMPILED FROM SOTA PACKAGED FILES


package sota.tools;

import com.sun.jna.Library;

interface LibCameraV4L2 extends Library {
    int open_device(String var1);

    int init_device(int var1, int var2, int var3, int var4);

    int start_capturing(int var1, int var2);

    int get_capturing(int var1, int var2, int var3, int var4, byte[] var5);

    int getfile_capturing(int var1, int var2, int var3, int var4, String var5);

    void stop_capturing(int var1);

    void uninit_device(int var1);

    void close_device(int var1);
}