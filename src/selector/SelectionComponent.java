package selector;

import static selector.SelectionModel.SelectionState.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.JComponent;

/**
 * A transparent (overlay) component enabling interactive selection (aka "tracing") of an underlying
 * image.  Layout must ensure that our upper-left corner coincides with that of the underlying image
 * view.
 */
public class SelectionComponent extends JComponent implements MouseListener, MouseMotionListener,
        PropertyChangeListener {

    /**
     * The current selection model that we are viewing and controlling.
     */
    private SelectionModel model;

    /* Interaction state */

    /**
     * The index of the selection segment whose starting point is currently being interacted with,
     * or -1 if no control point is currently being manipulated.
     */
    private int selectedIndex;

    /**
     * The last observed position of the mouse pointer over this component, constrained to lie
     * within the image area.  Must not alias a Point from a MouseEvent, as those objects may be
     * reused by future events.
     */
    private Point mouseLocation = new Point();

    /* View parameters */
    // TODO (embellishment): Customize these to your liking.  The API documentation for Color [1]
    //  should be helpful. (0 points - just for fun)
    //  [1] https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/Color.html

    /**
     * The radius of a control point, in pixels.  Used both for rendering and for tolerance when
     * selecting points with the mouse.
     */
    private int controlPointRadius = 4;

    /**
     * The color used to draw the current selection path.
     */
    private Color selectionPerimeterColor = Color.BLUE;

    /**
     * The color used to draw proposed segments that connect to the mouse pointer.
     */
    private Color liveWireColor = Color.YELLOW;

    /**
     * The color used to draw control points for a finished selection.
     */
    private Color controlPointColor = Color.CYAN;

    /**
     * Construct a new SelectionComponent that will participate in viewing and controlling the
     * selection modeled by `model`.  View will update upon receiving property change events from
     * `model`.
     */
    public SelectionComponent(SelectionModel model) {
        // Assign and listen to the provided model
        setModel(model);

        // Listen for mouse events that occur over us
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    /**
     * Have this component view and control `newModel` instead of whichever model it was using
     * previously.  This component will no longer react to events from the old model.  If a point
     * from the previous selection was being moved, that interaction is discarded.
     */
    public void setModel(SelectionModel newModel) {
        // Implementer's note: this method is safe to call during construction.

        // Stop receiving updates from our current model
        // (null check makes this save to call from the constructor)
        if (model != null) {
            model.removePropertyChangeListener(this);
        }

        // Assign and listen to the new model
        model = newModel;
        model.addPropertyChangeListener(this);

        // Update our preferred size to match the image used by the new model
        if (model.image() != null) {
            setPreferredSize(new Dimension(model.image().getWidth(), model.image().getHeight()));
        }

        // If we were in the process of moving a point, reset that interaction, since the selected
        // index may not be valid in the new model
        selectedIndex = -1;

        // Model state has changed; update our view.
        repaint();
    }

    /**
     * Return the selection model currently being viewed and controlled by this component.
     */
    public SelectionModel getModel() {
        return model;
    }

    /**
     * Record `p` as the most recent mouse pointer location and update the view.  If `p` is outside
     * of our model's image area, clamp `p`'s coordinates to the nearest edge of the image area.
     * This method does not modify or save a reference to `p` (meaning the client is free to mutate
     * it after this method returns, which Swing will do with Points used by MouseEvents).
     */
    private void updateMouseLocation(Point p) {
        // Clamp `p`'s coordinates to be within the image bounds and save them in our field
        mouseLocation.x = Math.clamp(p.x, 0, model.image().getWidth());
        mouseLocation.y = Math.clamp(p.y, 0, model.image().getHeight());

        // Update the view to reflect the new mouse location
        repaint();
    }

    /**
     * Return whether we are currently interacting with a control point of a closed selection.
     */
    private boolean isInteractingWithPoint() {
        return model.state() == SELECTED && selectedIndex != -1;
    }

    /**
     * Visualize our model's state, as well as our interaction state, by drawing our view using
     * `g`.
     */
    @Override
    public void paintComponent(Graphics g) {
        List<PolyLine> segments = model.selection();

        // Draw perimeter
        paintSelectionPerimeter(g, segments);


        // If dragging a point, draw guide lines
        if (isInteractingWithPoint() && mouseLocation != null) {
            paintMoveGuides(g, segments);
        }

        // Draw live wire
        if (model.state() == SELECTING && mouseLocation != null) {
            paintLiveWire(g);
        }

        // Draw handles
        if (model.state() == SELECTED) {
            paintControlPoints(g, segments);
        }
    }

    /**
     * Draw on `g` along the selection path represented by `segments` using our selection perimeter
     * color.
     */
    private void paintSelectionPerimeter(Graphics g, List<PolyLine> segments) {

        g.setColor(selectionPerimeterColor);
        for (PolyLine segment: segments) {
            Point point1 = segment.start();
            Point point2 = segment.end();

            int x1 = point1.x;
            int y1 = point1.y;
            int x2 = point2.x;
            int y2 = point2.y;
            g.drawLine(x1,y1,x2,y2);





        }

        // TODO 3B: Implement this method as specified.
        //  The Graphics API documentation [1] is essential to finding appropriate methods to draw
        //  the segments and control their color.
        //  [1] https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/Graphics.html
    }

    /**
     * Draw on `g` along our model's "live wire" path to our last-known mouse pointer location using
     * our live wire color.
     */
    private void paintLiveWire(Graphics g) {
        // TODO 3C: Implement this method as specified.  The same Graphics methods you used in
        //  `paintSelectionPerimeter()` are relevant here.

        g.setColor(liveWireColor);
        PolyLine line = model.liveWire(mouseLocation);


        Point point1 = line.start();
        Point point2 = line.end();


        int x1 = point1.x;
        int y1 = point1.y;
        int x2 = point2.x;
        int y2 = point2.y;

        g.drawLine(x1,y1,x2,y2);
    }

    /**
     * Draw filled circles on `g` centered at the control points between each of the selection path
     * segments in `segments` using our control point color.  The circles' radius should be our
     * control point radius.
     */
    private void paintControlPoints(Graphics g, List<PolyLine> segments) {
        // TODO 4A: Implement this method as specified.  Pay careful attention to the arguments
        //  expected by your chosen Graphics API call.

        g.setColor(controlPointColor);

        for (PolyLine line :segments){

            Point point = line.start();
            g.fillOval(point.x-controlPointRadius,point.y-controlPointRadius,
                    controlPointRadius*2,controlPointRadius*2);
            point = line.end();
            g.fillOval(point.x-controlPointRadius,point.y-controlPointRadius,
                    controlPointRadius*2,controlPointRadius*2);
        }


    }

    /**
     * Draw straight lines on `g` connecting our last-known mouse pointer location to the control
     * points before and after our selected point.  Requires `selectedIndex` is in
     * [0..segments.size()).
     */
    private void paintMoveGuides(Graphics g, List<PolyLine> segments) {
        // TODO 4G: Implement this method as specified.
        if (selectedIndex < 0 || selectedIndex >= segments.size()) {
            return;
        }

        PolyLine seg = segments.get(selectedIndex);
        Point pnt = seg.start();

        g.drawLine(mouseLocation.x, mouseLocation.y, pnt.x, pnt.y);

        if (selectedIndex > 0) {
            PolyLine prevSeg = segments.get(selectedIndex - 1);
            Point prevPnt = prevSeg.end();
            g.drawLine(pnt.x, pnt.y, prevPnt.x, prevPnt.y);
        }

        if (selectedIndex < segments.size() - 1) {
            PolyLine nextSeg = segments.get(selectedIndex + 1);
            Point nextPnt = nextSeg.start();
            g.drawLine(pnt.x, pnt.y, nextPnt.x, nextPnt.y);
        }
    }


    /* Event listeners */

    /**
     * When mouse button 1 is clicked and a selection has either not yet been started or is still in
     * progress, add the location of the point to the selection.  Note: `mousePressed()` and
     * `mouseReleased()` handle presses of button 1 when the selection is finished.
     * <p>
     * When mouse button 2 is clicked and a selection is in progress, finish the selection.
     * <p>
     * When mouse button 3 is clicked and a selection is either in progress or finished, undo the
     * last point added to the selection.
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        updateMouseLocation(e.getPoint());  // Always update the mouse location first

        // Handle actions based on mouse button and selection state
        if (e.getButton() == MouseEvent.BUTTON1) {

            if (model.state() == SELECTING || model.state() == NO_SELECTION) {
                model.addPoint(e.getPoint());
            }
        } else if (e.getButton() == MouseEvent.BUTTON2) {

            if (model.state() == SELECTING) {
                model.finishSelection();
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {

            if (model.state() == SELECTING || model.state() == SELECTED) {
                model.undo();
            }
        }
    }


    /**
     * When a selection is in progress, update our last-observed mouse location to the location of
     * this event and repaint ourselves to draw a "live wire" to the mouse pointer.
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (model.state() == SELECTING) {
            updateMouseLocation(e.getPoint());
        }
    }

    /**
     * When a selection is in progress, or when we are interacting with a control point, update our
     * last-observed mouse location to the location of this event and repaint ourselves to draw a
     * "live wire" to the mouse pointer.  (Note that mouseMoved events are not sent while dragging,
     * which is why this overlaps with the duties of that handler.)
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (model.state() == SELECTING || isInteractingWithPoint()) {
            updateMouseLocation(e.getPoint());
        }
    }

    /**
     * When mouse button 1 is pressed while our model's selection is complete, search for a control
     * point close to the mouse pointer and, if found, start interacting with that point.
     */
    @Override
    public void mousePressed(MouseEvent e) {

        // TODO 4F: Implement this method as specified.  Recall that the `selectedIndex` field is
        //  used to remember which control point a user is currently interacting with.


        if(e.getButton() == MouseEvent.BUTTON1){
            if(model.state() == SELECTED){
                int maxDistanceSq = controlPointRadius * controlPointRadius;
                Point clickPoint = e.getPoint();
                int closestIndex = model.closestPoint(clickPoint, maxDistanceSq);
                if (closestIndex != -1) { // Valid control point found within the threshold
                    selectedIndex = closestIndex; // Select this control point
                } else {
                    selectedIndex = -1; // No control point is close enough, reset the selected index
                }
            }
        }
    }

    /**
     * When mouse button 1 is released while we are interacting with a control point, move the
     * selected point to the current mouse location.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && isInteractingWithPoint()) {
            model.movePoint(selectedIndex, mouseLocation);
            // No need to call `repaint()` ourselves, since moving the point will trigger a property
            // change, which will then trigger a repaint when we observe it.

            // Stop interacting with the point
            selectedIndex = -1;
        }
    }

    /**
     * Repaint to update our view in response to any property changes from our model.  Additionally,
     * if the "image" property changed, update our preferred size to match the new image size.
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        // If model image changed, update preferred size
        if (e.getPropertyName().equals("image") && e.getNewValue() != null) {
            BufferedImage img = (BufferedImage) e.getNewValue();
            setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
        }

        // If the model's selection changed while we are interacting with a control point, cancel
        // that interaction (since our selected index may no longer be valid).
        if (e.getPropertyName().equals("selection")) {
            selectedIndex = -1;
        }

        // If any property of the model changed, repaint to update view
        repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Ignored
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Ignored
    }
}
