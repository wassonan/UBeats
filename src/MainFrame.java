/*
 * Wasson An
 * This is the main frame that contains our program
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

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
		
		JPanel panel = new JPanel();

		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		
		//this panel contains the scanned shapes
		BeatMap bmPanel = new BeatMap();
		
		panel.add(bmPanel);
		panel.add(Box.createVerticalGlue());
		
		//creates a button for scanning
		JPanel scanPanel = new JPanel();
		JButton scan = null;
		try {
			scan = new JButton(new ImageIcon(ImageIO.read(new File("images/button.jpg"))));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		scan.setBorder(BorderFactory.createEmptyBorder());
		scan.setContentAreaFilled(false);
		
		scanPanel.setLayout(new BorderLayout());
		scanPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		scanPanel.setMaximumSize(new Dimension(400, 100));
		scanPanel.setMinimumSize(new Dimension(400, 100));
		scanPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.red), scanPanel.getBorder()));
		scanPanel.add(scan, BorderLayout.CENTER);

//		JPanel paddingNorth = new JPanel();
//		JPanel paddingEast = new JPanel();
//		JPanel paddingSouth = new JPanel();
//		JPanel paddingWest = new JPanel();
//		scanPanel.add(scan, BorderLayout.NORTH);
//		scanPanel.add(paddingNorth, BorderLayout.NORTH);
//		scanPanel.add(paddingEast, BorderLayout.EAST);
//		scanPanel.add(paddingSouth, BorderLayout.SOUTH);
//		scanPanel.add(paddingWest, BorderLayout.WEST);
//		this.add(scanPanel, BorderLayout.CENTER);

		panel.add(scanPanel);

		panel.add(Box.createVerticalGlue());
		this.add(panel);
	} //initialize

} //MainFrame
