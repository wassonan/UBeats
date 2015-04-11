/* 
 * Wasson An
 * This class is the panel that lists the loaded sounds
 */

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.text.Position;


public class ListPanel extends JPanel{

	public static JList<String> list;
	
	//default constructor
	public ListPanel(int i){
		
		if(list == null)
			initialize();
		
		this.add(new JScrollPane(list));
		list.setSelectedIndex(i + 1);

	} //0 param constructor
	
	
	//sets up the list
	private void initialize(){
		
		System.out.println("Initializing List");
		
		String[] names = new String[TestShapes.sounds.size() + 1];
		names[0] = "None";
		
		for(int i = 0; i < TestShapes.sounds.size(); i++){
			
            String name = TestShapes.sounds.get(i);
			names[i + 1] = name; //.substring(name.lastIndexOf("/") + 1);
		} //for
		
		list = new JList(names);
		list.setLayoutOrientation(JList.VERTICAL);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	} //initialize
} //ListPanel