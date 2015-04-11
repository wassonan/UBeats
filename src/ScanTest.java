import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;


public class ScanTest {

	public static void main(String[] args) {
		scan();
	}
	
	public static void scan(){
		System.load(System.getProperty("user.dir") + "/libs/libopencv_java2411.dylib");
		VideoCapture camera = new VideoCapture(0);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		camera.open(0); 
		if(!camera.isOpened()){
			System.out.println("Camera Error");
		}
		Mat frame = new Mat();
		camera.read(frame);
		camera.read(frame);
		
		frame = Highgui.imread("images/base.jpg", 0);

		System.out.println(frame.size());
		frame = frame.submat(165, 665, 200, 850);
		//showResult(frame);
		
		Imgproc.threshold(frame, frame, 100, 255, Imgproc.THRESH_BINARY);

		MatOfByte bytemat = new MatOfByte();
		Highgui.imencode(".jpg", frame, bytemat);
		byte[] bytes = bytemat.toArray();
		InputStream in = new ByteArrayInputStream(bytes);
		BufferedImage img = null;
		try {
			img = ImageIO.read(in);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for(int i=0; i<img.getHeight(); i++){
			for(int j=0; j<img.getWidth(); j++){
				int pixel = img.getRGB(j,i);
				if(pixel == -1){
					img.setRGB(j, i, 0xFF);
				}else{
					j = img.getWidth();
				}
			}
		}
		for(int i=img.getHeight()-1; i>=0; i--){
			for(int j=img.getWidth()-1; j>=0; j--){
				int pixel = img.getRGB(j,i);
				if(pixel == -1){
					img.setRGB(j, i, 0xFF);
				}else{
					j =0;
				}
			}
		}
		for(int j=0; j<img.getWidth(); j++){
			for(int i=0; i<img.getHeight(); i++){
				int pixel = img.getRGB(j,i);
				if(pixel == -1){
					img.setRGB(j, i, 0xFF);
				}else{
					i = img.getHeight();
				}
			}
		}
		for(int j=img.getWidth()-1; j>=0; j--){
			for(int i=img.getHeight()-1; i>=0; i--){
				int pixel = img.getRGB(j,i);
				if(pixel == -1){
					img.setRGB(j, i, 0xFF);
				}else{
					i =0;
				}
			}
		}
		Stack<int[]> s = new Stack<int[]>();
		boolean[][] flags = new boolean[img.getWidth()][img.getHeight()];
		for(int i=0; i<img.getHeight(); i++){
			for(int j=0; j<img.getWidth(); j++){
				if(!flags[j][i]){
					flags[j][i] = true;
					if(img.getRGB(j,i) == -1){
						HashMap<String,Boolean> e = new HashMap<String,Boolean>();
						e.put(j+", "+i, true);
						s.push(new int[]{j+1,i});
						s.push(new int[]{j,i-1});
						s.push(new int[]{j-1,i});
						s.push(new int[]{j,i+1});
						while(!s.empty()){
							int[] a = s.pop();
							if(!flags[a[0]][a[1]]){
								flags[a[0]][a[1]] = true;
								if(img.getRGB(a[0],a[1]) == -1){
									e.put(a[0]+", "+a[1], true);
									s.push(new int[]{a[0]+1,a[1]});
									s.push(new int[]{a[0],a[1]-1});
									s.push(new int[]{a[0]-1,a[1]});
									s.push(new int[]{a[0],a[1]+1});
								}
							}
						}
						if(e.size()>1000){
							TestShapes.shapes.add(e);
						}
					}
				}
			}
		}

		try {
			File outputfile = new File("images/saved.png");
			ImageIO.write(img, "png", outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(TestShapes.shapes.size());
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
}
