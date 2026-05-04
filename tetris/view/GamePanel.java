// Game panel 
public class GamePanel extends JPanel {
    public static final int TILE_SIZE = 30; // size of one tile
    public static final int COLS = 10;
    public static final int ROWS = 20;

    public GamePanel() {
        setPreferredSize(new Dimension(COLS * TILE_SIZE, ROWS * TILE_SIZE));
        setBackground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBoard(g);
    }

    private void drawBoard(Graphics g) {
        g.setColor(Color.GRAY);
        for(int i=0; i<=COLS i++){
            g.drawLine(i * TILE_SIZE, 0, i * TILE_SIZE, getHeight());
        }
        for(int i=0; i<=ROWS; i++){
            g.drawLine(0, i * TILE_SIZE, getWidth(), i * TILE_SIZE);
        }
    }
}