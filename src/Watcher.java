import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.opencv.core.Mat;

// Thread t = new Thread(new Watcher());
// t.start();

public class Watcher implements Runnable{

	ArrayList<Boolean> playable;

	public void run() {
		
		//while
		while(true){
			
			//Check if base exists
			File base = new File("images/base.jpg");
			if(base.exists()){
				
				//DO PICTURE SHIT HERE AND GET NEGATIVE
				Mat negative = null;
				
				for(int i = 0; i < TestShapes.shapes.size(); i++){
					
					HashMap<String, Boolean> shape = TestShapes.shapes.get(i);					
					Boolean activate = false;
					Iterator<String> iter = shape.keySet().iterator();

					while(!activate && iter.hasNext()){
						
						String coord = iter.next();
						int x = Integer.parseInt(coord.substring(0, coord.indexOf(",")));
						int y = Integer.parseInt(coord.substring(coord.indexOf(" ") + 1));
						//CHECK NEGATIVE TO SEE IF BLUE AT X, Y
					} //while
					
					if(activate)
						new AePlayWave(TestShapes.buttonSounds.get(i)).start();
				} //for
				
			} //if
		} //while
	} //run
} //Watcher
