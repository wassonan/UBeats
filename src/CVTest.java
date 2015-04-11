import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;;


public class CVTest {

	public static void main(String[] args) {
		System.load(System.getProperty("user.dir") + "/libs/libopencv_java2411.dylib");
		Mat prev_frame = Highgui.imread("images/base.jpg", 0);
		Mat next_frame = Highgui.imread("images/motion.jpg", 0);
		Mat frame = Highgui.imread("images/test.jpg", 0);
		double[] a = frame.get(315, 465);
		System.out.println(frame.channels());//a[0] +" "+ a[1] +" "+ a[2]);
		Mat d1 = new Mat();
		Core.subtract(prev_frame, next_frame, d1);
		Imgproc.threshold(d1, d1, 70, 255, Imgproc.THRESH_BINARY);
		Highgui.imwrite("images/image.jpg", d1);
		
		
		/*BufferedImage img1 = null, img2 = null;
		try {
		    img1 = ImageIO.read(new File("base.jpg"));
		    img2 = ImageIO.read(new File("motion.jpg"));
		} catch (IOException e){}
		IplImage prev_frame = IplImage.createFrom(img1);
		IplImage next_frame = IplImage.createFrom(img1);
		IplImage result = null;
		cvAbsDiff(prev_frame, next_frame, result);
		BufferedImage img = result.getBufferedImage();
		File outputfile = new File("image.jpg");
		try {
			ImageIO.write(img, "jpg", outputfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

}
