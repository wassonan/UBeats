import java.awt.*;

/**
 * An ImageButton is a custom GUI widget which is a button with an
 *   image on it.  (The AWT does not have such a widget, presumably
 *   because it's not very portable, but I find that this works fine.)
 */
class ImageButton extends Canvas {

  /**
   * The image for the "up" state of the button.
   */
  Image upImage = null;

  /**
   * The image for the "down" state of the button.
   */
  Image downImage = null;

  /**
   * The current image for the button (this is always one of
   * upImage or downImage).
   */
  Image image = null;

  /**
   * Are we in a state where a mouse button was pressed down
   * in the button and hasn't been released yet?
   */
  boolean mouseDown = false;

  /**
   * Is the mouse cursor over the button?
   */
  boolean mouseIn = false;

  /**
   * The state of the button (true corresponds to down).
   */
  boolean state = false;

  /**
   * Create a new button with the given images, group, and initial
   * state.
   */
  public ImageButton(Image upImage, Image downImage,
		     boolean state) {
    
    this.upImage = upImage;
    this.downImage = downImage;
    this.image = upImage;
    setState(state);
  }

  /**
   * Create a new button with the given images and group,
   * and initially in its "false" (up) state.
   */
  public ImageButton(Image upImage, Image downImage) {
    this(upImage, downImage, false);
  }

  public Dimension preferredSize() {
    int width = upImage.getWidth(this);
    int height = upImage.getHeight(this);
    return (new Dimension(width,height));
  }

  public Dimension minimumSize() {
    return preferredSize();
  }

  public void paint(Graphics g) {
    g.drawImage(image, 0, 0, this);
  }

  /**
   * Return the state (true corresponds to down) of the button.
   */
  public boolean getState() {
    return state;
  }

  /**
   * Set the state of the button.  True corresponds to down.
   */
  public void setState(boolean state) {
    setStateInternal(state);
  }

  public void setStateInternal(boolean state) {
    this.state = state;
    if (state) {
      image = downImage;
    } else {
      image = upImage;
    }
    repaint();
  }

  public boolean mouseDown(Event evt, int x, int y) {
    image = downImage;
    mouseDown = true;
    repaint();
    return true;
  }

  public boolean mouseUp(Event evt, int x, int y) {
	  
	  image = upImage;
	/*
    if (mouseDown && mouseIn) {
      postEvent(new Event(this,
			  evt.when,
			  Event.ACTION_EVENT,
			  evt.x,
			  evt.y,
			  evt.key,
			  evt.modifiers,
			  evt.arg));
      setState(!state);
    }
    if (state) {
      image = downImage;
    } else {
      image = upImage;
    }
    */
	ScanTest.scan();
    repaint();
    mouseDown = false;
    return true;
  }

  public boolean mouseEnter(Event evt, int x, int y) {
    mouseIn = true;
    if (mouseDown) {
      image = downImage;
    }
    repaint();
    return true;
  }

  public boolean mouseExit(Event evt, int x, int y) {
    mouseIn = false;
    if (mouseDown) {
      image = upImage;
    }
    repaint();
    return true;
  }

}
