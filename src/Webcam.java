
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

public class Webcam {

	public static void main (String args[]){
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
		camera.open(0); 
		if(!camera.isOpened()){
			System.out.println("Camera Error");
		}
		else{
			System.out.println("Camera OK?");
		}
		Mat frame = new Mat();
		Mat prev_frame = new Mat();
		Mat curr_frame = new Mat();
		Mat next_frame = new Mat();
		Mat d1 = new Mat();
		Mat d2 = new Mat();

		//camera.grab();
		//System.out.println("Frame Grabbed");
		//camera.retrieve(frame);
		//System.out.println("Frame Decoded");
		camera.read(frame);
		//prev_frame = Highgui.imread("images/base.jpg", 0);
		camera.read(prev_frame);
		try {
			Thread.sleep(3000);
			System.out.println("FINGAHNNOW");
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("FINGAHNNOW");
		/*camera.read(curr_frame);
		camera.read(next_frame);
		showResult(next_frame);*/
		for(int i = 0; i <5; i++){
			//camera.read(prev_frame);
			//camera.read(curr_frame);
			camera.read(next_frame);
			System.out.println("base:" + prev_frame.size());
			System.out.println("next:" + next_frame.size());

			Core.subtract(prev_frame, next_frame, d1);
			Imgproc.threshold(d1, d1, 70, 255, Imgproc.THRESH_BINARY);
			//Core.subtract(curr_frame, next_frame, d2);
			//Core.bitwise_and(d1, d2, frame);
			showResult(d1);
		}

		
		System.out.println("Frame Obtained");

		/* No difference
	    camera.release();
		 */

		System.out.println("Captured Frame Width " + frame.width());

		Highgui.imwrite("images/camera.jpg", frame);
		System.out.println("OK");
	}
	
	public static void showResult(Mat img) {
	    Imgproc.resize(img, img, new Size(640, 480));
	    MatOfByte matOfByte = new MatOfByte();
	    Highgui.imencode(".jpg", img, matOfByte);
	    byte[] byteArray = matOfByte.toArray();
	    BufferedImage bufImage = null;
	    try {
	        InputStream in = new ByteArrayInputStream(byteArray);
	        bufImage = ImageIO.read(in);
	        JFrame frame = new JFrame();
	        frame.getContentPane().add(new JLabel(new ImageIcon(bufImage)));
	        frame.pack();
	        frame.setVisible(true);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
}

