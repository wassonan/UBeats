/*
 * Wasson An
 * This is the main frame that contains our program
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
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
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
		JPanel panel = new JPanel();

		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		
		//this panel contains the scanned shapes
		BeatMap bmPanel = new BeatMap();
		
		panel.add(bmPanel);
		panel.add(Box.createVerticalGlue());
		
		//creates a button for scanning
		JPanel scanPanel = new JPanel();
		ImageButton scan = null;
		try {
			scan = new ImageButton(new ImageIcon(ImageIO.read(new File("images/up.jpg"))).getImage(),
					new ImageIcon(ImageIO.read(new File("images/down.jpg"))).getImage(), bmPanel);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		scanPanel.setLayout(new BorderLayout());
		scanPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		scanPanel.setMaximumSize(new Dimension(400, 100));
		scanPanel.setMinimumSize(new Dimension(400, 100));

//		scanPanel.setBorder(BorderFactory.createCompoundBorder(
//				BorderFactory.createLineBorder(Color.red), scanPanel.getBorder()));

		scanPanel.add(scan, BorderLayout.CENTER);
		panel.add(scanPanel);
		panel.add(Box.createVerticalGlue());
		this.add(panel);
	} //initialize

} //MainFrame
