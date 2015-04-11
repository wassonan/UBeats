import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JOptionPane;
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
		this.setPreferredSize (new Dimension(500, 650)); 
		this.addMouseListener(new ShapeClicker(this));
	} //0 param constructor

	
	//function to paint panel
	public void paint(Graphics g){
		
		//set background
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, 499, 649);
		
		for(int i = 0; i < TestShapes.shapes.size(); i++){
			
			if(TestShapes.buttonSounds.get(i) != null)
				g.setColor(Color.GREEN);
			
			else
				g.setColor(Color.RED);
			Iterator<String> iter = TestShapes.shapes.get(i).keySet().iterator();
			
			//iterates all the points in the shape and colors them
			while(iter.hasNext()){
				
				String coord = iter.next();
				int x = Integer.parseInt(coord.substring(0, coord.indexOf(",")));
				int y = Integer.parseInt(coord.substring(coord.indexOf(" ") + 1, 
						coord.length()));
				
				g.drawLine(y, x, y, x);
			} //while
		} //for
		
	} //paint
	
	private class ShapeClicker implements MouseListener{

		private BeatMap beat;

		//default constructor
		public ShapeClicker(BeatMap bm){
			
			beat = bm;
		} //0 param constructor
		
		
		//when the mouse is clicked
		public void mouseClicked(MouseEvent me) {
			
			int x = me.getX();
			int y = me.getY();
			String coord = y + ", " + x;
			
			for(int i = 0; i < TestShapes.shapes.size(); i++){
				
				if(TestShapes.shapes.get(i).containsKey(coord)){

					if(TestShapes.buttonSounds.get(i) != null)
						new AePlayWave(TestShapes.buttonSounds.get(i)).start();
					
					if(JOptionPane.showConfirmDialog(null, new ListPanel(
							TestShapes.sounds.indexOf(TestShapes.buttonSounds.get(i))),
							"Test", JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE) == 0){
						
						int index = ListPanel.list.getSelectedIndex();
						
						if(index == 0)
							TestShapes.buttonSounds.set(i, null);
						
						else{
							
							String name = TestShapes.sounds.get(index - 1);
							TestShapes.buttonSounds.set(i, name);
						} //else
					} //if
				} //if
			} //for
			
			beat.repaint();
		} //mouseClicked


		//when the mouse enters
		public void mouseEntered(MouseEvent me) {

		} //mouseEntered

		
		//when mouse exites area
		public void mouseExited(MouseEvent me) {

		} //mouseExited


		//when mouse is pressed
		public void mousePressed(MouseEvent me) {

		} //mousePressed

		
		//when mouse is released
		public void mouseReleased(MouseEvent me) {

		} //mouseReleased
	} //ShapeClicker
} //BeatMap
