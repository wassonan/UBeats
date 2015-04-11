import org.opencv.core.Core;
import org.opencv.core.Mat;
import com.googlecode.javacv.cpp.opencv_highgui.*;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class CVTest {

	public static void main(String[] args) {
		IplImage img = imread("base.jpg");
		Mat mtx(img); // convert IplImage* -> Mat
		absdiff(prev_frame, next_frame, d1);
	}

}
