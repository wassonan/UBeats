/* Wasson An
 * This is the main driver the the program
 */

public class Driver {

	//The main function that initiated our program
	public static void main(String[] args){
		
		System.load(System.getProperty("user.dir") + "/libs/libopencv_java2411.dylib");
		System.out.println("Starting Program");
		MainFrame mf = new MainFrame();
	} //main
} //Driver
