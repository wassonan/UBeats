import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/* Wasson An
 * This is the main driver the the program
 */

public class Driver {

	//The main function that initiated our program
	public static void main(String[] args){
		
		System.load(System.getProperty("user.dir") + "/libs/libopencv_java2411.dylib");
		System.out.println("Starting Program");
		MainFrame mf = new MainFrame();
		
		File base = new File("images/base.jpg");
		try {
			Files.deleteIfExists(base.toPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Thread t = new Thread(new Watcher());
		t.start();
	} //main
} //Driver
