/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.ui.main;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List; // modnar: change to cleaner icon set
import java.util.ArrayList; // modnar: change to cleaner icon set

import rotp.model.colony.Colony;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.SystemViewer;
import rotp.util.ImageManager;

import javax.swing.*;
import static rotp.model.colony.Colony.ECOLOGY;
import static rotp.model.colony.Colony.SHIP;
import static rotp.model.colony.Colony.DEFENSE;
import static rotp.model.colony.Colony.INDUSTRY;

public class EmpireColonySpendingPane extends BasePanel {
    private static final long serialVersionUID = 1L;
    static final Color sliderHighlightColor = new Color(255,255,255);
    static final Color sliderBoxEnabled = new Color(34,140,142);
    static final Color sliderBoxDisabled = new Color(102,137,137);
    static final Color sliderErrEnabled = new Color(140,34,34);
    static final Color sliderErrDisabled = new Color(137,102,102);
    static final Color sliderBackEnabled = Color.black;
    static final Color sliderBackDisabled = new Color(65,65,65);
    static final Color sliderTextEnabled = Color.black;
    static final Color sliderTextDisabled = new Color(65,65,65);

    Color borderHi, borderLo, textC, backC;

    EmpireSliderPane shipSlider, defSlider, indSlider, ecoSlider, researchSlider;

    private final SystemViewer parent;
    private GalaxyMapPanel mapListener;
    public EmpireColonySpendingPane(SystemViewer p, Color c0, Color text, Color hi, Color lo) {
        parent = p;
        textC = text;
        backC = c0;
        borderHi = hi;
        borderLo = lo;
        init();
    }
    public void mapListener(GalaxyMapPanel map)  { mapListener = map; }
    @Override
    public String textureName()            { return parent.subPanelTextureName(); }
    private void init() {
        shipSlider     = new EmpireSliderPane(this, Colony.SHIP);
        defSlider      = new EmpireSliderPane(this, Colony.DEFENSE);
        indSlider      = new EmpireSliderPane(this, Colony.INDUSTRY);
        ecoSlider      = new EmpireSliderPane(this, Colony.ECOLOGY);
        researchSlider = new EmpireSliderPane(this, Colony.RESEARCH);

        GridLayout layout = new GridLayout(6,0);
        layout.setVgap(s1);
        setOpaque(true);

        setBackground(backC);
        setLayout(layout);
        add(new EmpireSliderPane(this, -1));  // this is the "Colony Spending" label
        add(shipSlider);
        add(defSlider);
        add(indSlider);
        add(ecoSlider);
        add(researchSlider);
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        int mods = e.getModifiersEx();
        switch (k) {
            case KeyEvent.VK_1:
                switch (mods) {
                    case 0: shipSlider.increment(true); break;
                    case 64: shipSlider.decrement(true); break;
                    case 128: shipSlider.toggleLock();    break;
                    default:  break;
                }
                return;
            case KeyEvent.VK_2:
                switch (mods) {
                    case 0: defSlider.increment(true); break;
                    case 64: defSlider.decrement(true); break;
                    case 128: defSlider.toggleLock();    break;
                    default: break;
                }
                return;
            case KeyEvent.VK_3:
                switch (mods) {
                    case 0: indSlider.increment(true); break;
                    case 64: indSlider.decrement(true); break;
                    case 128: indSlider.toggleLock();    break;
                    default: break;
                }
                return;
            case KeyEvent.VK_4:
                switch (mods) {
                    case 0: ecoSlider.increment(true); break;
                    case 64: ecoSlider.decrement(true); break;
                    case 128: ecoSlider.toggleLock();    break;
                    default: break;
                }
                return;
            case KeyEvent.VK_5:
                switch (mods) {
                    case 0: researchSlider.increment(true); break;
                    case 64: researchSlider.decrement(true); break;
                    case 128: researchSlider.toggleLock();    break;
                    default:  break;
                }
                return;
            case KeyEvent.VK_Q:
            {
                toggleGovernor();
                break;
            }
            case KeyEvent.VK_W:
            {
                toggleAutoShips();
                break;
            }
        }
    }
    class EmpireSliderPane extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        EmpireColonySpendingPane mgmtPane;
        private final Polygon leftArrow = new Polygon();
        private final Polygon rightArrow = new Polygon();
        private final Rectangle labelBox = new Rectangle();
        private final Rectangle sliderBox = new Rectangle();
        private final Rectangle resultBox = new Rectangle();
        private Shape hoverBox;
        // polygon coordinates for left & right increment buttons
        private final int leftButtonX[] = new int[3];
        private final int leftButtonY[] = new int[3];
        private final int rightButtonX[] = new int[3];
        private final int rightButtonY[] = new int[3];
        private final int category;
        EmpireSliderPane(EmpireColonySpendingPane ui, int cat) {
            mgmtPane = ui;
            category = cat;
            init();
        }
        private void init() {
            setOpaque(false);
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);

            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;
            Colony colony = sys.colony();
            if  (colony == null)
                return;

            int w = getWidth();

            if (category < 0) {
                Color color;
                if (colony.isGovernor()) {
                    color = Color.green;
                } else {
                    color = MainUI.shadeBorderC();
                }

                g.setFont(narrowFont(20));
                String titleText = text("MAIN_COLONY_ALLOCATE_SPENDING");
                int titleY = getHeight() - s6;
                drawShadowedString(g, titleText, 2, s5, titleY, color, textC);

                // crappy ASCII art. Should be something else.
                // TODO: for future use
//                if (1 == 0) {
//                    if (colony.isAutoShips()) {
//                        color = Color.green;
//                    } else {
//                        color = MainUI.shadeBorderC();
//                    }
//                    String shipAutomateText = "]=>";
//                    drawShadowedString(g, shipAutomateText, 2, w - s95, titleY, color, textC);
//                }
                String governorOptionsText = text("GOVERNOR_OPTIONS");
                drawShadowedString(g, governorOptionsText, 2, w-s60, titleY, MainUI.shadeBorderC(), textC);
                return;
            }
            String text = text(Colony.categoryName(category));

            // label
            Color textC;
            if (hoverBox == labelBox)
                textC = SystemPanel.yellowText;
            else if (colony.canAdjust(category))
                textC = sliderTextEnabled;
            else
                textC = sliderTextDisabled;
            String labelText = text(text);
            g.setColor(textC);
            g.setFont(narrowFont(18));
            drawString(g,labelText, s10, getHeight()-s10);
            labelBox.setBounds(s5, 0, leftMargin()-s15, getHeight());

            int boxL = boxLeftX();
            int boxW = boxRightX() - boxL;
            int boxTopY = boxTopY();
            int boxBottomY = boxBottomY();
            int boxH = boxBottomY - boxTopY;

            // slider
            float pct = colony.pct(category);
            leftButtonX[0] = leftMargin(); leftButtonX[1] = leftMargin()+buttonWidth(); leftButtonX[2] = leftMargin()+buttonWidth();
            leftButtonY[0] = buttonMidY(); leftButtonY[1] = buttonTopY(); leftButtonY[2] = buttonBottomY();

            rightButtonX[0] = w-rightMargin(); rightButtonX[1] = w-rightMargin()-buttonWidth(); rightButtonX[2] = w-rightMargin()-buttonWidth();
            rightButtonY[0] = buttonMidY(); rightButtonY[1] = buttonTopY(); rightButtonY[2] = buttonBottomY();

            Color c1  = colony.canAdjust(category) ? sliderBoxEnabled : sliderBoxDisabled;
            Color c1a = colony.canAdjust(category) ? sliderErrEnabled: sliderErrDisabled;
            Color c2  = colony.canAdjust(category) ? sliderBackEnabled : sliderBackDisabled;

            Color c3 = hoverBox == leftArrow ? SystemPanel.yellowText : c2;
            g.setColor(c3);
            g.fillPolygon(leftButtonX, leftButtonY, 3);

            c3 = hoverBox == rightArrow ? SystemPanel.yellowText : c2;
            g.setColor(c3);
            g.fillPolygon(rightButtonX, rightButtonY, 3);

            leftArrow.reset();
            rightArrow.reset();
            for (int i=0;i<leftButtonX.length;i++) {
                leftArrow.addPoint(leftButtonX[i], leftButtonY[i]);
                rightArrow.addPoint(rightButtonX[i], rightButtonY[i]);
            }

            sliderBox.x = boxL;
            sliderBox.y = boxTopY;
            sliderBox.width = boxW;
            sliderBox.height = boxH;

            g.setColor(c2);
            g.fillRect(boxL+boxBorderW(), boxTopY, boxW-(2*boxBorderW()), boxH);

            if (colony.warning(category))  
                g.setColor(c1a);
            else
                g.setColor(c1);
            
            Rectangle fillRect;
            
            
            if (pct == 1)           
                fillRect = new Rectangle(boxL+boxBorderW(), boxTopY+s2, boxW-(2*boxBorderW()), boxH-s3);
            else
                fillRect = new Rectangle(boxL+boxBorderW(), boxTopY+s2, (int) (pct*(boxW-(2*boxBorderW()))), boxH-s3);
                
            g.fill(fillRect);

            if (category == Colony.ECOLOGY)  {
                int popGrowth = colony.ecology().upcomingPopGrowth();
                g.setFont(narrowFont(14));
                String popStr = text("MAIN_COLONY_SPENDING_ECO_GROWTH",strFormat("%+3d", popGrowth));
                int sw1 = g.getFontMetrics().stringWidth(popStr);
                int x1 = (boxW-sw1)/2;
                
                if (popGrowth < 0)
                    g.setColor(SystemPanel.darkOrangeText);
                else
                    g.setColor(Color.gray);
                 drawString(g,popStr, boxL+x1, boxTopY+boxH-s4);
                
                if (popGrowth < 0)
                    g.setColor(SystemPanel.orangeText);
                else
                    g.setColor(Color.lightGray);
                Shape prevClip = g.getClip();
                g.setClip(fillRect);
                drawString(g,popStr, boxL+x1, boxTopY+boxH-s4);
                g.setClip(prevClip);
            }

            if (hoverBox == sliderBox) {
                g.setColor(SystemPanel.yellowText);
                Stroke prev = g.getStroke();
                g.setStroke(stroke2);
                g.drawRect(boxL+s3, boxTopY+s1, boxW-s6, boxH-s2);
                g.setStroke(prev);
            }

            // result
            textC = SystemPanel.blackText;
            if (hoverBox == resultBox)
                textC = SystemPanel.yellowText;
            String resultText = text(colony.category(category).upcomingResult());

            g.setColor(textC);
            scaledFont(g, resultText, rightMargin()-s10, 18, 14);
            g.setFont(narrowFont(18));
            int sw = g.getFontMetrics().stringWidth(resultText);
            drawString(g,resultText, getWidth()-sw-s10, getHeight()-s10);
            resultBox.setBounds(getWidth()-rightMargin(), 0, rightMargin(), getHeight());
        }
        private int leftMargin()        { return s58; }
        private int rightMargin()       { return s70; }
        private int buttonTopY()        { return s6; }
        private int buttonWidth()       { return s10; }
        private int buttonBottomY()     { return getHeight()-s7; }
        private int buttonMidY()        { return (buttonTopY()+buttonBottomY())/2; }
        private int boxLeftX()          { return leftMargin()+s10; }
        private int boxRightX()         { return getWidth()-rightMargin()-s10; }
        private int boxTopY()           { return s6; }
        private int boxBottomY()        { return getHeight()-s6; }
        private int boxBorderW()        { return s3; }

        public void decrement(boolean click) {
            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;
            Colony colony = sys.colony();
            if (colony == null)
                return;
            
            float prevTech = mapListener == null ? 0 : colony.totalPlanetaryResearch();
            if (colony.increment(category, -1)) {
                if (mapListener == null)
                    RotPUI.instance().techUI().resetPlanetaryResearch();
                else {
                    float techAdj = colony.totalPlanetaryResearch() - prevTech;
                    RotPUI.instance().techUI().adjustPlanetaryResearch(techAdj);
                    mapListener.repaintTechStatus();
                }
                if (click)
                    softClick();
                parent.repaint();
            }
            else if (click)
                misClick();
        }
        public void maxSlider(boolean click, int button) {
            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;
            Colony colony = sys.colony();
            if (colony == null)
                return;
            if(!colony.locked(category)) {
                colony.clearUnlockedSpending();
                int allocation = colony.allocationRemaining();
                if(button == 1)
                {
                    switch(category)
                    {
                        case SHIP:
                            if(colony.shipyard().buildLimit() > 0)
                                allocation = min(allocation, colony.shipyard().maxAllocationNeeded());
                            break;
                        case DEFENSE:
                            allocation = min(allocation, colony.defense().maxAllocationNeeded());
                            break;
                        case INDUSTRY:
                            allocation = min(allocation, colony.industry().maxAllocationNeeded());
                            break;
                        case ECOLOGY:
                            allocation = min(allocation, colony.ecology().maxAllocationNeeded());
                            break;
                        default:
                            break;
                    }
                    if(allocation == 0)
                        allocation = colony.allocationRemaining();
                }
                colony.setAllocation(this.category, allocation);
                if(category != ECOLOGY)
                    colony.checkEcoAtClean();
                else
                    colony.redistributeReducedEcoSpending();
            }
            if (click)
                softClick();
            parent.repaint();
        }
        public void rightClickResultBox(boolean click) {
            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;
            Colony colony = sys.colony();
            if (colony == null)
                return;
            if(!colony.locked(category)) {
                float prevTech = mapListener == null ? 0 : colony.totalPlanetaryResearch();
                switch(category) {
                    case SHIP:		break;
                    case DEFENSE:	break;
                    case INDUSTRY:	break;
                    case ECOLOGY:
                    	colony.checkEcoAtTerraform();
                    	colony.keepEcoLockedToClean = false;
                        break;
                    default:		break;
                }
                if (mapListener == null)
                    RotPUI.instance().techUI().resetPlanetaryResearch();
                else {
                    float techAdj = colony.totalPlanetaryResearch() - prevTech;
                    RotPUI.instance().techUI().adjustPlanetaryResearch(techAdj);
                    mapListener.repaintTechStatus();
                }
            }
            if (click)
                softClick();
            parent.repaint();
        }
        public void middleClickResultBox(boolean click) {
            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;
            Colony colony = sys.colony();
            if (colony == null)
                return;
            if(!colony.locked(category)) {
                float prevTech = mapListener == null ? 0 : colony.totalPlanetaryResearch();
                switch(category) {
                    case SHIP:		break;
                    case DEFENSE:	break;
                    case INDUSTRY:	break;
                    case ECOLOGY:
                    	colony.keepEcoLockedToClean = true;
                    	colony.checkEcoAtClean();
                        break;
                    default:		break;
                }
                if (mapListener == null)
                    RotPUI.instance().techUI().resetPlanetaryResearch();
                else {
                    float techAdj = colony.totalPlanetaryResearch() - prevTech;
                    RotPUI.instance().techUI().adjustPlanetaryResearch(techAdj);
                    mapListener.repaintTechStatus();
                }
            }
            if (click)
                softClick();
            parent.repaint();
        }
        public void increment(boolean click) {
            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;
            Colony colony = sys.colony();
            if (colony == null)
                return;
            
            float prevTech = mapListener == null ? 0 : colony.totalPlanetaryResearch();
            if (colony.increment(category, 1)) {
                if (mapListener == null)
                    RotPUI.instance().techUI().resetPlanetaryResearch();
                else {
                    float techAdj = colony.totalPlanetaryResearch() - prevTech;
                    RotPUI.instance().techUI().adjustPlanetaryResearch(techAdj);
                    mapListener.repaintTechStatus();
                }
                if (click)
                    softClick();
                parent.repaint();
            }
            else if (click)
                misClick();
        }
        public void toggleLock() {
            softClick();
            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;
            Colony colony = sys.colony();
            if (colony == null)
                return;
            colony.toggleLock(category);
            repaint();
        }
        @Override
        public void mouseClicked(MouseEvent arg0) {}
        @Override
        public void mouseEntered(MouseEvent arg0) {}
        @Override
        public void mouseExited(MouseEvent arg0) {
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
        @Override
        public void mousePressed(MouseEvent ev) { }
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            int x = e.getX();
            int y = e.getY();
            if (labelBox.contains(x,y))
                toggleLock();
            else if (leftArrow.contains(x,y))
                decrement(true);
            else if (rightArrow.contains(x,y))
                increment(true);
            else if (resultBox.contains(x,y))
            	if (SwingUtilities.isRightMouseButton(e))
            		rightClickResultBox(true);
            	else if (SwingUtilities.isMiddleMouseButton(e))
            		middleClickResultBox(true);
            	else
            		maxSlider(true, e.getButton());
            else {
                if (this.category < 0) {
// TODO: for future use
//                    if (x < EmpireColonySpendingPane.this.getWidth() - s95) {
//                        toggleGovernor();
//                    } else if (x < EmpireColonySpendingPane.this.getWidth() - s60) {
//                        toggleAutoShips();
                    if (x < EmpireColonySpendingPane.this.getWidth() - s60) {
                        toggleGovernor();
                    } else {
                        governorOptions();
                    }
                }
                float pct = pctBoxSelected(x,y);
                if (pct >= 0) {
                    Colony colony = parent.systemViewToDisplay().colony();
                    if (!colony.canAdjust(category))
                        misClick();
                    else {
                        softClick();
                        // clicks near the edge of the box are typically trying
                        // to zero or max them out. Assume that.
                        if (pct < .05)
                            pct = 0;
                        else if (pct > .95)
                            pct = 1;
                        float prevTech = mapListener == null ? 0 : colony.totalPlanetaryResearch();
                        colony.forcePct(category, pct);
                        if (category == Colony.ECOLOGY)
                            colony.keepEcoLockedToClean = false;
                        if (mapListener == null)
                            RotPUI.instance().techUI().resetPlanetaryResearch();
                        else {
                            float techAdj = colony.totalPlanetaryResearch() - prevTech;
                            RotPUI.instance().techUI().adjustPlanetaryResearch(techAdj);
                            mapListener.repaintTechStatus();
                        }
                        parent.repaint();
                    }
                }
            }
        }
        @Override
        public void mouseDragged(MouseEvent arg0) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            Shape newHover = null;
            if (labelBox.contains(x,y))
                newHover = labelBox;
            else if (sliderBox.contains(x,y))
                newHover = sliderBox;
            else if (leftArrow.contains(x,y))
                newHover = leftArrow;
            else if (rightArrow.contains(x,y))
                newHover = rightArrow;
            else if (resultBox.contains(x,y))
                newHover = resultBox;

            if (newHover != hoverBox) {
                hoverBox = newHover;
                repaint();
            }
        }
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            int rot = e.getWheelRotation();
            if (hoverBox == sliderBox) {
                if (rot > 0)
                    decrement(false);
                else if (rot  < 0)
                    increment(false);
            }
        }
        public float pctBoxSelected(int x, int y) {
            int bw = boxBorderW();
            int minX = sliderBox.x+bw;
            int maxX = sliderBox.x+sliderBox.width-bw;

            if ((x < minX)
            || (x > maxX)
            || (y < (boxTopY()-bw))
            || (y > (boxBottomY()+bw)))
                return -1;

            float num = x - minX;
            float den = maxX-minX;
            return num/den;
        }
    }

    JFrame governorOptionsFrame = null;
    private void governorOptions() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
			public void run() {
                if (governorOptionsFrame == null) {
                    governorOptionsFrame = new JFrame("GovernorOptions");
                    // make this window have an icon, same as main window
                    // modnar: change to cleaner icon set
                    List<Image> iconImages = new ArrayList<Image>();
                    iconImages.add(ImageManager.current().image("ROTP_MOD_ICON3"));
                    iconImages.add(ImageManager.current().image("ROTP_MOD_ICON2"));
                    iconImages.add(ImageManager.current().image("ROTP_MOD_ICON1"));
                    governorOptionsFrame.setIconImages(iconImages);
                    governorOptionsFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

                    //Create and set up the content pane.
                    GovernorOptionsPanel newContentPane = new GovernorOptionsPanel(governorOptionsFrame);
                    newContentPane.setOpaque(true); //content panes must be opaque
                    governorOptionsFrame.setContentPane(newContentPane);
                    newContentPane.applyStyle(); // Keep after set to the frame
                }
                //Display the window.
                governorOptionsFrame.pack();
                governorOptionsFrame.setVisible(true);
            }
        });
    }

    private void toggleGovernor() {
        if (parent.systemViewToDisplay() != null && parent.systemViewToDisplay().colony() != null) {
            Colony colony = parent.systemViewToDisplay().colony();
            colony.setGovernor(!colony.isGovernor());
            if (colony.isGovernor()) {
                colony.govern();
            }
            parent.repaint();
        }
    }
    private void toggleAutoShips() {
        if (parent.systemViewToDisplay() != null && parent.systemViewToDisplay().colony() != null) {
            Colony colony = parent.systemViewToDisplay().colony();
            colony.setAutoShips(!colony.isAutoShips());
            parent.repaint();
        }
    }
}