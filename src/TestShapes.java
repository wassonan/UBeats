import java.applet.AudioClip;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class TestShapes {

	public static ArrayList<String> sounds; //contains all sounds in sound folder
	public static ArrayList<HashMap<String, Boolean>> shapes; //the shapes
	public static ArrayList<String> buttonSounds; //the sounds linked to shapes
	public static Clip clip;

	static {

		try {
			clip = AudioSystem.getClip();
		} catch (LineUnavailableException e1) {
			e1.printStackTrace();
		} //try catch

		File folder = new File("sounds");
		File[] files = folder.listFiles();
		sounds = new ArrayList<String>();
		buttonSounds = new ArrayList<String>();
		
		for(int i = 0; i < 10; i++)
			buttonSounds.add(null);

		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {

				sounds.add("sounds/" + files[i].getName());
//				new AePlayWave("sounds/" + files[i].getName()).start();
//				clip.open(sounds.get(i));

//				System.out.println(files[i].getName());
				//clip.start();
			} //if
		} //for

		buttonSounds.set(0, sounds.get(0));
		buttonSounds.set(1, sounds.get(1));

		shapes = new ArrayList<HashMap<String, Boolean>>();
		shapes.add(new HashMap<String, Boolean>());
		for(int i = 0; i < 100; i++)
			for(int j = 0; j < 100; j++)
				shapes.get(0).put((i + 200) + ", " + (j + 200), true);

		shapes.add(new HashMap<String, Boolean>());
		for(int i = 0; i < 100; i++)
			for(int j = 0; j < 100; j++)
				shapes.get(1).put((i + 400) + ", " + (j + 300), true);
		
		shapes = new ArrayList<HashMap<String, Boolean>>();
	}

}
