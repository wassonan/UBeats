import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import static com.googlecode.javacv.cpp.opencv_core.*;
public class CVTest {

	public static void main(String[] args) {
		BufferedImage img1 = null, img2 = null;
		try {
		    img1 = ImageIO.read(new File("base.jpg"));
		    img2 = ImageIO.read(new File("motion.jpg"));
		} catch (IOException e){}
		IplImage prev_frame = IplImage.createFrom(img1);
		IplImage next_frame = IplImage.createFrom(img1);
		IplImage result = null;
		cvAbsDiff(prev_frame, next_frame, result);
		//cvShowImage("test", result);
	}

}
