import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
 
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
		BufferedImage img = result.getBufferedImage();
		File outputfile = new File("image.jpg");
		try {
			ImageIO.write(img, "jpg", outputfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
