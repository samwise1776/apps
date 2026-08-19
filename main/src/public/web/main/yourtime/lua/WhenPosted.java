import java.io.File;
import java.io.IOException;

public class WhenPosted {

    private final File videoFile;

    public WhenPosted(File videoFile) {

        this.videoFile = videoFile;

        System.out.println(
                "Video received: "
                        + videoFile.getAbsolutePath()
        );

        try {

            // Start Lua and give it the selected video.
            ProcessBuilder process = new ProcessBuilder(
                    "lua",
                    "video.lua",
                    videoFile.getAbsolutePath()
            );

            process.inheritIO();

            process.start();

        } catch (IOException e) {

            System.err.println(
                    "Could not start Lua video window."
            );

            e.printStackTrace();
        }
    }

    public File getVideoFile() {
        return videoFile;
    }
}