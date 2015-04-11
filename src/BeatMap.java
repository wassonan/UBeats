import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;

/*
 * Wasson An
 * This is the panel that holds the beatmap
 */

public class BeatMap extends JPanel{
	
	//default constructor
	public BeatMap(){
		
		this.setBackground(Color.BLUE);

		//height is to be proportional with width inline with paper
		this.setPreferredSize (new Dimension(500, 647)); 
	} //0 param constructor

} //BeatMap
