import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;


public class QuickCap {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.load(System.getProperty("user.dir") + "/libs/libopencv_java2411.dylib");

		System.out.println("Hello, OpenCV");
		// Load the native library.

		VideoCapture camera = new VideoCapture(0);
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		camera.open(1); 
		if(!camera.isOpened()){
			System.out.println("Camera Error");
		}
		else{
			System.out.println("Camera OK?");
		}
		Mat frame = new Mat();

		camera.read(frame);
		camera.read(frame);
		camera.read(frame);
		Highgui.imwrite("images/base.jpg", frame);
		
	}

}
