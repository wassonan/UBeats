import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;



public class Watcher implements Runnable{

	ArrayList<Boolean> playable;

	public void run() {
		System.load(System.getProperty("user.dir") + "/libs/libopencv_java2411.dylib");
		Mat cam = new Mat();
		Mat base = new Mat();
		Mat frame = new Mat();
		VideoCapture camera = new VideoCapture(0);
		

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		camera.open(1); 
		if(!camera.isOpened()){
			System.out.println("Camera Error");
		}
		camera.read(frame);
		camera.read(base);
		base = base.submat(165, 665, 200, 850);
		//while
		while(true){
			camera.read(cam);
			//System.out.println(frame.rows());
			cam = cam.submat(165, 665, 200, 850);
			Core.subtract(base, cam, frame);
			Imgproc.threshold(frame, frame, 70, 255, Imgproc.THRESH_BINARY);
			//showResult(frame);
			
			analyze(frame);
		} //while
	} //run
	public static void analyze(Mat frame){

		BufferedImage test = new BufferedImage(1280, 960, BufferedImage.TYPE_BYTE_BINARY);
		Graphics2D    graphics = test.createGraphics();

				graphics.setColor ( Color.white );
				graphics.fillRect ( 0, 0, test.getWidth(), test.getHeight() );
		graphics.setColor(Color.GREEN);
		for(int i = 0; i < TestShapes.shapes.size(); i++){

			HashMap<String, Boolean> shape = TestShapes.shapes.get(i);					
			int activate = 0;
			Iterator<String> iter = shape.keySet().iterator();

			while(activate<50 && iter.hasNext()){

				String coord = iter.next();
				int x = Integer.parseInt(coord.substring(0, coord.indexOf(",")));
				int y = Integer.parseInt(coord.substring(coord.indexOf(" ") + 1));

				//System.out.println(x + ", " + y);

				double[] rgb = frame.get(y, x);
				if(rgb[0] > 240 && rgb[1] < 30 && rgb[2] < 30){
					activate++;
				}

				graphics.setColor(new Color((int)rgb[2], (int)rgb[1], (int)rgb[0]));
				graphics.drawLine(x, y, x, y);
			} //while
			System.out.println(activate);
			if(activate>=50)
				new AePlayWave(TestShapes.buttonSounds.get(i)).start();
		}//for		

		File output = new File("images/kk.jpg");
		try {
			if(!output.exists() && TestShapes.shapes.size() > 0)
				ImageIO.write(test, "jpg", output);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void showResult(Mat img) {
		//Imgproc.resize(img, img, new Size(640, 480));
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
} //Watcher
