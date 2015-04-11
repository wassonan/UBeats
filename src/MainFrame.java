/*
 * Wasson An
 * This is the main frame that contains our program
 */

import javax.swing.JFrame;

public class MainFrame extends JFrame{
	
	//default constructor
	public MainFrame(){
		
		initialize();
		this.setVisible(true);
	} //0 param constructor
	
	//initialized the frame
	private void initialize(){
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("UBeats");
		this.setSize(500, 800);

	} //initialize

} //MainFrame
