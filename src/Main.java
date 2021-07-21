import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.concurrent.locks.LockSupport;

public class Main {
    static {
        System.loadLibrary("opencv_java452");
    }

    private static int targetWidth = 200;
    private static int targetHeight = 50;
    private static float targetAspectRatio = targetWidth / targetHeight;

    private static double prevNanos = 0;
    private static double currentNanos;

    private static VideoCapture capture = new VideoCapture();
    private static Mat frame = new Mat();

    private static BufferedImage image;

    private static char[] lumScale = {'.', ',', '-', '~', ':', ';', '=', '!', '*', '#', '$', '@'};
    private static StringBuilder output = new StringBuilder();

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Error: Expected video path as argument.");
            return;
        }

        capture.open(args[0]);
        if (!capture.isOpened()) {
            System.out.println("Error: Unable to open capture.");
            return;
        }

        double videoWidth = capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        double videoHeight = capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        double videoAspectRatio = videoWidth / videoHeight / 0.45d; // ASCII character width/height ratio compensated
        double nanosPerFrame = (1000000000 / capture.get(Videoio.CAP_PROP_FPS));

        if (videoAspectRatio > targetAspectRatio) {
            targetHeight = (int) (targetWidth / videoAspectRatio);
        } else {
            targetWidth = (int) (targetHeight * videoAspectRatio);
        }


        while(capture.read(frame)) {
            image = resizeImage(MatToBufferedImage(frame), targetWidth, targetHeight);

            for (int i = 0; i < targetHeight; i++) {
                for (int j = 0; j < targetWidth; j++) {
                    int pixel = image.getRGB(j, i);
                    Color color = new Color(pixel);

                    int lum = (int) (0.3 * color.getRed() + 0.59 * color.getGreen() + 0.11 * color.getBlue());
                    int lumIndex = lum / (255 / (lumScale.length - 1));
                    output.append(lumScale[lumIndex]);
                }
                output.append("\n");
            }

            // Wait till it's time to display next frame
            currentNanos = System.nanoTime();
            double nanosElapsed = currentNanos - prevNanos;

            if (nanosElapsed < nanosPerFrame) {
                double nanosTillNextFrame = nanosPerFrame - nanosElapsed;
                LockSupport.parkNanos((long) nanosTillNextFrame);
            }
            prevNanos = System.nanoTime();

            System.out.println(output);
            output.setLength(0);
        }

        capture.release();
    }

    // https://stackoverflow.com/questions/27086120
    private static BufferedImage MatToBufferedImage(Mat matrix) throws Exception {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", matrix, mob);
        byte[] ba = mob.toArray();

        return ImageIO.read(new ByteArrayInputStream(ba));
    }

    // https://www.baeldung.com/java-resize-image
    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
        return outputImage;
    }
}
