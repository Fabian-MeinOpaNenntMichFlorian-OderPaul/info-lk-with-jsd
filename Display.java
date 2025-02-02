import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Display extends Thread {
    private JFrame frame;
    private Input input;
    private Graphics graphics, g;
    private BufferedImage image;

    private int width, height;
    private int[] oldMousePos={-1,-1};

    private double[] pos={0.0,0.5};

    private double zoom=1.0;
    private int[][] adjazenzmatrix;
    private int[][] laplacematrix;

    private int[] listByLeastConncection;

    private Knot[] knots;
    private Knoten[] knoten=null;

    private boolean darkmode=true;
    private Color c_white=Color.WHITE;

    private Color c_black=Color.BLACK;

    private Color c_orange=Color.ORANGE;

    private Color c_red=Color.RED;

    private float knot_radius=25;
    private Knot currentDraggedKnot=null;

    private boolean printWeight=true;

    private boolean fillKnots=true;

    public Display(int w, int h, boolean autostart){
        initilize(w, h, null, autostart);
    }
    public Display(int w, int h){
        initilize(w, h, null,true);
    }
    public Display(){
        initilize(800, 600, null,true);
    }

    public Display(int w, int h, int[][] adjazenzmatrix, boolean autostart){
        initilize(w, h, adjazenzmatrix, autostart);
    }
    public Display(int w, int h, int[][] adjazenzmatrix){
        initilize(w, h, adjazenzmatrix,true);
    }
    public Display(int[][] adjazenzmatrix){
        initilize(800, 600, adjazenzmatrix,true);
    }
    public void initilize(int w, int h, int[][] adjazenzmatrix, boolean autostart){
        width=w;
        height=h;
        frame= new JFrame();
        frame.setVisible(true);
        frame.setSize(new Dimension(width+16, height+30));

        frame.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        Image icon = Toolkit.getDefaultToolkit().getImage("./src/DisplayingGraphLogo.png");
        frame.setIconImage(icon);

        frame.setTitle("Graph - Visualisation");
        frame.getContentPane().setBackground(Color.BLACK);

        input=new Input();
        frame.addKeyListener(input);
        frame.addMouseListener(input);
        frame.addMouseMotionListener(input);
        frame.addMouseWheelListener(input);
        frame.addFocusListener(input);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        g= frame.getGraphics();
        g.setColor(darkmode?c_white:c_black);
        g.setFont(new Font("Arial", Font.PLAIN, 32));

        image=new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        graphics= image.getGraphics();
        graphics.setColor(darkmode?c_white:c_black);
        graphics.setFont(new Font("Arial", Font.PLAIN, 32));

        this.adjazenzmatrix=adjazenzmatrix;

        if (autostart)
            this.start();
        credtis();
    }

    public Input getInput(){
        return input;
    }

    public void setPrintWeight(boolean printWeight) {
        this.printWeight = printWeight;
    }

    public boolean getPrintWeigth(){
        return printWeight;
    }

    public void setFillKnots(boolean f){fillKnots=f;}

    public boolean getFillKnots(){return fillKnots;}

    private int[][] getLaplaceMatrix(int[][] a){
        if(laplacematrix!=null)
            return laplacematrix;
        laplacematrix=new int[a.length][a[0].length];
        for(int i=0; i<a.length; i++) {
            int numOfConnections=0;
            for (int j=0; j < a.length; j++) {
                if (j != i && a[i][j] != 0)
                    numOfConnections++;
                laplacematrix[i][j]=a[i][j];
            }
            laplacematrix[i][i]=numOfConnections;
            System.out.println(String.format("%d : %d", i, laplacematrix[i][i]));
        }
        return laplacematrix;
    }

    private void prepareKnots(int[][] a){
        laplacematrix=getLaplaceMatrix(a);
        if(knots==null)
            knots = new Knot[a.length];
        if(a.length!= knots.length)
            knots = new Knot[a.length];
        for(int i=0; i<a.length; i++){
            if(knots[i]==null)
                prepareKnot(a, Integer.toString(i), i);
        }
    }

    private void prepareKnot(int[][] a, String name, int i){
        float[] pos = getKnotPos(a, i);
        if(knoten==null)
            knots[i]=new Knot(pos, name);
        else if(knoten[i]==null)
            knots[i]=new Knot(pos, name);
        else
            knots[i]=new Knot(pos, knoten[i].getBezeichnung());
    }

    private float[] getKnotPos(int[][] a, int i){
        float[] pos=new float[2];
        int length=a.length;
        if(length%2==0){
            if(i==0)
                return new float[]{width / 2, height / 2};
            i-=1;
            length-=1;
        }
        if(i%2!=0)
            i=length-(i+1)/2;
        else
            i=i/2;
        if(true) {
            pos[0] = (float) (Math.sin(2 * Math.PI / length * i)) * width / 3 + width / 2;
            pos[1] = (float) (Math.cos(2 * Math.PI / length * i)) * height / 3 + height / 2;
        }
        return pos;
        //return new float[]{(float) (Math.random() * width), (float) (Math.random() * height)};
    }

    private void draw(){
        update();
        if (adjazenzmatrix!=null) {
            prepareKnots(adjazenzmatrix);
            drawGraph(adjazenzmatrix);
        }
        swapBuffers();
    }

    private void drawGraph(int[][] a){
        for(int i=0; i < knots.length; i++)
            for (int j = 0; j < adjazenzmatrix.length; j++)
                if (adjazenzmatrix[i][j] != 0)
                    drawLineBetweenKnots(i, j);
        for(int i=0; i < knots.length; i++){
            drawKnot(i);
        }
    }

    private void drawKnot(int i){
        graphics.setColor(darkmode?c_white:c_black);
        graphics.fillOval((int)(knots[i].getX()-knot_radius+pos[0]),(int)(knots[i].getY()-knot_radius+pos[1]),(int)(knot_radius*2),(int)(knot_radius*2));

        graphics.setColor((fillKnots?darkmode:!darkmode)?c_white:c_black);
        graphics.fillOval((int)(knots[i].getX()-(knot_radius-1)+pos[0]),(int)(knots[i].getY()-(knot_radius-1)+pos[1]),(int)((knot_radius-1)*2),(int)((knot_radius-1)*2));

        graphics.setColor((fillKnots?!darkmode:darkmode)?c_white:c_black);
        graphics.setFont(new Font("Arial", Font.PLAIN, 12));
        FontMetrics fontMetrics = graphics.getFontMetrics();
        int nameWidth = fontMetrics.stringWidth(knots[i].getName());
        int nameHeight = fontMetrics.getHeight();
        graphics.drawString(
                knots[i].getName(),
                (int)(knots[i].getX() - nameWidth / 2 + pos[0]),
                (int)(knots[i].getY() + nameHeight / 4 + pos[1])
        );
    }

    private void drawLineBetweenKnots(int i, int j) {
        if(adjazenzmatrix[i][j]==0&&adjazenzmatrix[j][i]==0)
            return;
        if(adjazenzmatrix[i][j] >= 0 && adjazenzmatrix[j][i] >= 0)
            graphics.setColor(darkmode ? c_white : c_black);
        else if(adjazenzmatrix[i][j] < 0 || adjazenzmatrix[j][i] < 0)
            graphics.setColor(darkmode ? c_red : c_orange);
        graphics.drawLine((int) (knots[i].getX()+pos[0]), (int) (knots[i].getY()+pos[1]), (int) (knots[j].getX()+pos[0]), (int) (knots[j].getY()+pos[1]));

        if(!printWeight)
            return;

        int midX = (int) ((knots[i].getX() + knots[j].getX()) / 2 + pos[0]);
        int midY = (int) ((knots[i].getY() + knots[j].getY()) / 2 + pos[1]);

        graphics.setColor(darkmode ? c_white : c_black);
        graphics.setFont(new Font("Arial", Font.PLAIN, 12));
        FontMetrics fontMetrics = graphics.getFontMetrics();

        if(i<=j)
            if(Math.abs(adjazenzmatrix[i][j])==Math.abs(adjazenzmatrix[j][i])) {
                String weight = Integer.toString(Math.abs(adjazenzmatrix[i][j]));
                int weightWidth = fontMetrics.stringWidth(weight);
                int weightHeight = fontMetrics.getHeight();
                graphics.drawString(weight, midX, midY - weightHeight/2);
            }
            else{
                String weight = (adjazenzmatrix[i][j]==0?"":"-> "+Math.abs(adjazenzmatrix[i][j]))+((adjazenzmatrix[i][j]==0||adjazenzmatrix[j][i]==0)?"":" | ")+(adjazenzmatrix[j][i]==0?"":"<- "+Math.abs(adjazenzmatrix[j][i]));
                int weightWidth = fontMetrics.stringWidth(weight);
                int weightHeight = fontMetrics.getHeight();
                graphics.drawString(weight, midX, midY - weightHeight / 2);
            }
    }
    private void update(){
        updateSize();
        updatePos();
        image=new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        graphics= image.getGraphics();
    }

    private void updateSize(){
        height=frame.getHeight();
        width=frame.getWidth();
    }
    private void swapBuffers(){
        g.drawImage(image, 0, 0, frame);
    }

    private void updatePos(){
        if(input.inFocus){
            if(input.getMouse(1)) {
                mouseMoved();
            }
            else {
                frame.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                currentDraggedKnot=null;
            }
            zoom+= input.getMouseWheel()/-50.0;
            if(zoom<=0){
                zoom=0;
            }
        }
        oldMousePos[0] = input.getMouseX();
        oldMousePos[1] = input.getMouseY();
    }

    private void mouseMoved(){
        int i=0;
        for(Knot knot : knots){
            float[] knotPos = knot.getPos();
            float distance = (float) Math.sqrt(Math.pow(oldMousePos[0] - (knotPos[0] + pos[0]), 2) + Math.pow(oldMousePos[1] - (knotPos[1] + pos[1]), 2));

            if ((distance < knot_radius && currentDraggedKnot == null) || currentDraggedKnot == knot) {
                currentDraggedKnot=knot;
                knot.setX((float) (input.getMouseX() - pos[0]));
                knot.setY((float) (input.getMouseY() - pos[1]));
                frame.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return;
            }
            i++;
        }
        currentDraggedKnot = null;
        pos[0] += (input.getMouseX() - oldMousePos[0]);
        pos[1] += (input.getMouseY() - oldMousePos[1]);
        frame.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }


    public void run(){
        while (true) {
            draw();
            if(input.getKey(KeyEvent.VK_ESCAPE))
                executeOrder66();
        }

    }

    private void executeOrder66() {System.exit(0);}

    public void setAdjazenzmatrix(int[][] a){adjazenzmatrix=a;}
    public int[][] getAdjazenzmatrix(){return adjazenzmatrix;}

    public class Knot{
        private float[] pos = new float[2];
        private String name="";
        public Knot(float[] pos, String name){
            this.pos=pos;
            this.name=name;
        }

        public void setPos(float[] pos) {
            this.pos = pos;
        }

        public void setX(float x) {
            this.pos[0] = x;
        }
        public void setY(float y) {
            this.pos[1] = y;
        }

        public void setName(String name) {
            this.name = name;
        }

        public float[] getPos() {
            return pos;
        }

        public float getX(){
            return pos[0];
        }
        public float getY(){
            return pos[1];
        }

        public String getName() {
            return name;
        }
    }


    public void credtis() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Credits");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Create an instance of the CreditsPanel class
            CreditsPanel creditsPanel = new CreditsPanel(frame);
            frame.add(creditsPanel);

            // Set frame properties
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Start the scrolling animation
            creditsPanel.startAnimation();
        });
    }

    class CreditsPanel extends JPanel {

        private String monologue = "GRAPH\n" +
                "WARS\n" +
                "\n" +
                "A long time ago in a galaxy far, far away...\n" +
                "An Info LK from Zimmermann fought an endless battle\n" +
                "against the \"Zentralabitur\" and its brutal regulations\n" +
                "and operators.\n" +
                "\n" +
                "But a group of students did not stop resisting the intruders.\n" +
                "\n" +
                "\n";

        private int scrollSpeed = 2;
        private Timer timer;
        private int yOffset;

        private JFrame frame;

        public CreditsPanel(JFrame f) {
            setBackground(Color.BLACK);
            setForeground(Color.YELLOW);
            frame=f;
            setFont(new Font("Arial", Font.PLAIN, 20));
            this.grabFocus();
        }

        public void startAnimation() {
            timer = new Timer(20, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    yOffset -= scrollSpeed;
                    repaint();
                }
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(getForeground());

            String[] monologue_array=monologue.split("\n");

            for (int i=0; i<monologue_array.length; i++) {
                String m=monologue_array[i];

                // Set the font for the monologue text
                FontMetrics fontMetrics = g2d.getFontMetrics();
                int textWidth = fontMetrics.stringWidth(m);
                int textHeight = fontMetrics.getHeight();

                // Draw the monologue text at the calculated position
                int x = (getWidth() - textWidth) / 2;
                int y = (int)(getHeight() + yOffset + textHeight*i);
                g2d.drawString(m, x, y);

                // Draw the text again to handle the case when it goes off the screen
                if (getHeight() + yOffset + textHeight * monologue_array.length < 0) {
                    frame.setVisible(false);
                    frame.dispose();
                }
            }
        }
    }

}