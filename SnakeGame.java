import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/*
 * SNAKE GAME - DSA MINI PROJECT (simple interactive GUI)
 * College concepts: 2D Array, Deque, HashSet, Insertion Sort, File Handling.
 * Compile: javac SnakeGame.java
 * Run:     java SnakeGame
 * Tests:   java SnakeGame --test-cases
 */
public class SnakeGame {
    static final int SIZE = 10;
    static final int CELL_SIZE = 42;
    static final int DELAY = 300;

    private JFrame frame;
    private BoardPanel boardPanel;
    private JLabel scoreLabel;
    private JLabel lengthLabel;
    private JLabel messageLabel;
    private JLabel bestLabel;
    private JButton startButton;
    private JButton pauseButton;
    private Timer timer;

    private Snake snake;
    private Food food;
    private GameBoard board;
    private ScoreManager scoreManager;
    private int score;
    private boolean running;
    private boolean paused;
    private boolean gameOver;
    private boolean scoreSaved;

    public SnakeGame() {
        scoreManager = new ScoreManager("scores.txt");
        resetGame();
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--test-cases")) {
            runTestCases();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    SnakeGame game = new SnakeGame();
                    game.createWindow();
                }
            });
        }
    }

    // Build a small window with the board, score and simple controls.
    private void createWindow() {
        frame = new JFrame("Snake Game - DSA Mini Project");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(760, 570);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(new Color(22, 29, 40));
        frame.setLayout(new BorderLayout(15, 15));

        JLabel title = new JLabel("S N A K E   G A M E", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 25));
        title.setForeground(new Color(110, 235, 150));
        title.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
        frame.add(title, BorderLayout.NORTH);

        boardPanel = new BoardPanel();
        frame.add(boardPanel, BorderLayout.CENTER);

        JPanel sidePanel = new JPanel();
        sidePanel.setPreferredSize(new Dimension(190, 10));
        sidePanel.setBackground(new Color(30, 40, 54));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(18, 12, 12, 12));
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));

        scoreLabel = makeInfoLabel("Score: 0");
        lengthLabel = makeInfoLabel("Length: 1");
        bestLabel = makeInfoLabel("Best: " + scoreManager.getBestScore());
        sidePanel.add(scoreLabel);
        sidePanel.add(Box.createVerticalStrut(12));
        sidePanel.add(lengthLabel);
        sidePanel.add(Box.createVerticalStrut(12));
        sidePanel.add(bestLabel);
        sidePanel.add(Box.createVerticalStrut(28));

        startButton = makeButton("START GAME");
        pauseButton = makeButton("PAUSE");
        JButton restartButton = makeButton("RESTART");
        JButton scoresButton = makeButton("TOP 5 SCORES");

        sidePanel.add(startButton);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(pauseButton);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(restartButton);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(scoresButton);
        sidePanel.add(Box.createVerticalStrut(25));

        JLabel controls = new JLabel("<html><b>CONTROLS</b><br><br>W / ↑ : Up<br>A / ← : Left<br>S / ↓ : Down<br>D / → : Right<br><br>Eat food to grow.<br>Avoid walls and yourself.</html>");
        controls.setForeground(Color.WHITE);
        controls.setFont(new Font("Arial", Font.PLAIN, 13));
        sidePanel.add(controls);
        sidePanel.add(Box.createVerticalGlue());
        messageLabel = makeInfoLabel("Press START to play");
        messageLabel.setForeground(new Color(110, 235, 150));
        sidePanel.add(messageLabel);
        frame.add(sidePanel, BorderLayout.EAST);

        startButton.addActionListener(e -> startGame());
        pauseButton.addActionListener(e -> togglePause());
        restartButton.addActionListener(e -> restartGame());
        scoresButton.addActionListener(e -> showScores());

        timer = new Timer(DELAY, e -> moveOneStep());
        addKeyboardControls();
        updateLabels();
        updateButtons();
        frame.setVisible(true);
        boardPanel.requestFocusInWindow();
    }

    private JLabel makeInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 15));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JButton makeButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBackground(new Color(75, 190, 120));
        button.setForeground(Color.BLACK);
        button.setMaximumSize(new Dimension(165, 38));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    // Keyboard commands change direction while the timer moves the snake continuously.
    private void addKeyboardControls() {
        boardPanel.setFocusable(true);
        boardPanel.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                int key = event.getKeyCode();
                if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) changeDirection(Direction.UP);
                else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) changeDirection(Direction.DOWN);
                else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) changeDirection(Direction.LEFT);
                else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) changeDirection(Direction.RIGHT);
                else if (key == KeyEvent.VK_SPACE) togglePause();
                boardPanel.requestFocusInWindow();
            }
        });
    }

    private void changeDirection(Direction newDirection) {
        if (running && !paused && !gameOver) {
            snake.changeDirection(newDirection);
        }
    }

    private void startGame() {
        if (gameOver) resetGame();
        if (!running) {
            running = true;
            paused = false;
            timer.start();
            messageLabel.setText("Game running");
            updateButtons();
            boardPanel.requestFocusInWindow();
            boardPanel.repaint();
        }
    }

    private void togglePause() {
        if (!running || gameOver) return;
        paused = !paused;
        if (paused) {
            timer.stop();
            messageLabel.setText("Game paused");
            pauseButton.setText("RESUME");
        } else {
            timer.start();
            messageLabel.setText("Game running");
            pauseButton.setText("PAUSE");
        }
        boardPanel.requestFocusInWindow();
        boardPanel.repaint();
    }

    private void restartGame() {
        if (timer != null) timer.stop();
        resetGame();
        messageLabel.setText("New game ready");
        updateLabels();
        updateButtons();
        boardPanel.repaint();
        boardPanel.requestFocusInWindow();
    }

    private void resetGame() {
        snake = new Snake(SIZE);
        food = new Food(SIZE);
        board = new GameBoard(SIZE);
        score = 0;
        running = false;
        paused = false;
        gameOver = false;
        scoreSaved = false;
    }

    // One timer event means one movement of the snake.
    private void moveOneStep() {
        if (!running || paused || gameOver) return;

        Cell nextHead = snake.getNextHead();
        boolean eating = nextHead.equals(food.getPosition());

        if (!board.isInside(nextHead)) {
            endGame("Wall collision!");
            return;
        }
        if (snake.willCollide(nextHead, eating)) {
            endGame("Snake hit its own body!");
            return;
        }

        snake.move(eating);
        if (eating) {
            score = score + 10;
            if (!food.placeNewFood(snake.getOccupiedCells())) {
                endGame("You filled the board. You win!");
                return;
            }
        }

        updateLabels();
        boardPanel.repaint();
    }

    private void endGame(String message) {
        timer.stop();
        running = false;
        gameOver = true;
        messageLabel.setText("Game Over");
        if (!scoreSaved) {
            scoreManager.saveScore(score);
            scoreSaved = true;
        }
        updateLabels();
        updateButtons();
        boardPanel.repaint();
        JOptionPane.showMessageDialog(frame,
                message + "\nFinal score: " + score + "\nSnake length: " + snake.getLength(),
                "Game Over", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateLabels() {
        if (scoreLabel == null) return;
        scoreLabel.setText("Score: " + score);
        lengthLabel.setText("Length: " + snake.getLength());
        bestLabel.setText("Best: " + scoreManager.getBestScore());
    }

    private void updateButtons() {
        if (startButton == null) return;
        startButton.setEnabled(!running || gameOver);
        pauseButton.setEnabled(running && !gameOver);
        pauseButton.setText(paused ? "RESUME" : "PAUSE");
    }

    private void showScores() {
        List<Integer> scores = scoreManager.getTopFiveScores();
        String text = "TOP FIVE SCORES\n\n";
        if (scores.size() == 0) text += "No scores saved yet.";
        for (int i = 0; i < scores.size(); i++) {
            text += (i + 1) + ".  " + scores.get(i) + " points\n";
        }
        JOptionPane.showMessageDialog(frame, text, "Leaderboard", JOptionPane.INFORMATION_MESSAGE);
        boardPanel.requestFocusInWindow();
    }

    // Draws the board using the 2D array from GameBoard.
    private class BoardPanel extends JPanel {
        public BoardPanel() {
            setPreferredSize(new Dimension(SIZE * CELL_SIZE + 30, SIZE * CELL_SIZE + 30));
            setBackground(new Color(22, 29, 40));
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            char[][] cells = board.createGrid(snake, food);
            int startX = 15;
            int startY = 15;

            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    int x = startX + col * CELL_SIZE;
                    int y = startY + row * CELL_SIZE;
                    g.setColor(new Color(38, 50, 65));
                    g.fillRoundRect(x, y, CELL_SIZE - 2, CELL_SIZE - 2, 8, 8);

                    if (cells[row][col] == 'H') {
                        g.setColor(new Color(100, 245, 130));
                        g.fillRoundRect(x + 3, y + 3, CELL_SIZE - 8, CELL_SIZE - 8, 10, 10);
                        g.setColor(Color.BLACK);
                        g.fillOval(x + 12, y + 11, 4, 4);
                        g.fillOval(x + 23, y + 11, 4, 4);
                    } else if (cells[row][col] == 'o') {
                        g.setColor(new Color(40, 185, 100));
                        g.fillRoundRect(x + 4, y + 4, CELL_SIZE - 10, CELL_SIZE - 10, 9, 9);
                    } else if (cells[row][col] == '*') {
                        g.setColor(new Color(255, 85, 95));
                        g.fillOval(x + 9, y + 9, CELL_SIZE - 21, CELL_SIZE - 21);
                    }
                }
            }

            if (!running || paused || gameOver) {
                g.setColor(new Color(0, 0, 0, 145));
                g.fillRoundRect(startX, startY, SIZE * CELL_SIZE - 2, SIZE * CELL_SIZE - 2, 12, 12);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 25));
                String text = gameOver ? "GAME OVER" : paused ? "PAUSED" : "PRESS START";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(text, startX + (SIZE * CELL_SIZE - fm.stringWidth(text)) / 2,
                        startY + (SIZE * CELL_SIZE) / 2);
            }
        }
    }

    // ---------------- DSA TEST CASES ----------------
    private static void runTestCases() {
        System.out.println("===== SNAKE GAME TEST CASES =====");

        Snake snake1 = new Snake(SIZE);
        Food food1 = new Food(SIZE);
        int score1 = 0;
        for (int i = 0; i < 2; i++) {
            Cell next = snake1.getNextHead();
            boolean eating = next.equals(food1.getPosition());
            snake1.move(eating);
            if (eating) score1 += 10;
        }
        boolean test1 = snake1.getHead().equals(new Cell(5, 7)) && score1 == 10 && snake1.getLength() == 2;
        printTest("TC1 Normal movement", test1);

        Snake snake2 = new Snake(SIZE, new Cell(0, 0), Direction.UP);
        boolean test2 = snake2.getNextHead().getRow() < 0;
        printTest("TC2 Wall collision", test2);

        Snake snake3 = new Snake(SIZE);
        Direction old = snake3.getDirection();
        boolean test3 = Direction.fromKey('X') == null && snake3.getDirection() == old;
        printTest("TC3 Invalid key", test3);

        ArrayList<Cell> body = new ArrayList<Cell>();
        body.add(new Cell(5, 3)); body.add(new Cell(5, 4)); body.add(new Cell(5, 5));
        body.add(new Cell(5, 6)); body.add(new Cell(5, 7));
        Snake snake4 = new Snake(SIZE, body, Direction.RIGHT);
        snake4.changeDirection(Direction.DOWN); snake4.move(false);
        snake4.changeDirection(Direction.LEFT); snake4.move(false);
        snake4.changeDirection(Direction.UP);
        Cell collisionCell = snake4.getNextHead();
        boolean collision = snake4.willCollide(collisionCell, false);
        ArrayList<Integer> scores = new ArrayList<Integer>();
        scores.add(30); scores.add(10); scores.add(40);
        ScoreManager.insertionSortDescending(scores);
        boolean test4 = collision && collisionCell.equals(new Cell(5, 6))
                && snake4.getLength() == 5 && scores.get(0) == 40
                && scores.get(1) == 30 && scores.get(2) == 10;
        printTest("TC4 Self collision and sorting", test4);

        if (test1 && test2 && test3 && test4) System.out.println("All four test cases passed.");
        else System.out.println("One or more tests failed.");
    }

    private static void printTest(String name, boolean passed) {
        System.out.println(name + ": " + (passed ? "PASS" : "FAIL"));
    }
}

enum Direction {
    UP, DOWN, LEFT, RIGHT;

    public boolean isOpposite(Direction other) {
        if (this == UP && other == DOWN) return true;
        if (this == DOWN && other == UP) return true;
        if (this == LEFT && other == RIGHT) return true;
        if (this == RIGHT && other == LEFT) return true;
        return false;
    }

    public static Direction fromKey(char key) {
        if (key == 'W') return UP;
        if (key == 'S') return DOWN;
        if (key == 'A') return LEFT;
        if (key == 'D') return RIGHT;
        return null;
    }
}

class Cell {
    private int row;
    private int col;

    public Cell(int row, int col) { this.row = row; this.col = col; }
    public int getRow() { return row; }
    public int getCol() { return col; }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Cell)) return false;
        Cell other = (Cell) obj;
        return row == other.row && col == other.col;
    }

    public int hashCode() { return row * 10 + col; }
}

class Snake {
    private int boardSize;
    private ArrayDeque<Cell> body;
    private HashSet<Integer> occupiedCells;
    private Direction direction;

    public Snake(int size) { this(size, new Cell(5, 5), Direction.RIGHT); }

    public Snake(int size, Cell start, Direction direction) {
        boardSize = size;
        this.direction = direction;
        body = new ArrayDeque<Cell>();
        occupiedCells = new HashSet<Integer>();
        body.addFirst(start);
        occupiedCells.add(getKey(start));
    }

    // The supplied body list is ordered from tail to head.
    public Snake(int size, List<Cell> startingBody, Direction direction) {
        boardSize = size;
        this.direction = direction;
        body = new ArrayDeque<Cell>();
        occupiedCells = new HashSet<Integer>();
        for (int i = 0; i < startingBody.size(); i++) {
            Cell cell = startingBody.get(i);
            body.addFirst(cell);
            occupiedCells.add(getKey(cell));
        }
    }

    private int getKey(Cell cell) { return cell.getRow() * boardSize + cell.getCol(); }
    public Cell getHead() { return body.peekFirst(); }
    public int getLength() { return body.size(); }
    public Direction getDirection() { return direction; }
    public HashSet<Integer> getOccupiedCells() { return occupiedCells; }
    public ArrayDeque<Cell> getBody() { return body; }

    public Cell getNextHead() {
        int row = getHead().getRow();
        int col = getHead().getCol();
        if (direction == Direction.UP) row--;
        else if (direction == Direction.DOWN) row++;
        else if (direction == Direction.LEFT) col--;
        else if (direction == Direction.RIGHT) col++;
        return new Cell(row, col);
    }

    public boolean changeDirection(Direction newDirection) {
        if (newDirection == null || direction.isOpposite(newDirection)) return false;
        direction = newDirection;
        return true;
    }

    public boolean willCollide(Cell nextHead, boolean eatingFood) {
        int key = getKey(nextHead);
        if (!occupiedCells.contains(key)) return false;
        Cell tail = body.peekLast();
        if (!eatingFood && nextHead.equals(tail)) return false;
        return true;
    }

    public void move(boolean eatingFood) {
        Cell newHead = getNextHead();
        body.addFirst(newHead);
        occupiedCells.add(getKey(newHead));
        if (!eatingFood) {
            Cell oldTail = body.removeLast();
            if (!oldTail.equals(newHead)) occupiedCells.remove(getKey(oldTail));
        }
    }
}

class Food {
    private int boardSize;
    private Cell position;
    private Random random;

    public Food(int size) {
        boardSize = size;
        position = new Cell(5, 7);
        random = new Random();
    }

    public Cell getPosition() { return position; }

    public boolean placeNewFood(HashSet<Integer> occupiedCells) {
        ArrayList<Integer> freeCells = new ArrayList<Integer>();
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                int key = row * boardSize + col;
                if (!occupiedCells.contains(key)) freeCells.add(key);
            }
        }
        if (freeCells.size() == 0) return false;
        int key = freeCells.get(random.nextInt(freeCells.size()));
        position = new Cell(key / boardSize, key % boardSize);
        return true;
    }
}

class GameBoard {
    private int size;
    private char[][] grid;

    public GameBoard(int size) { this.size = size; grid = new char[size][size]; }

    public boolean isInside(Cell cell) {
        return cell.getRow() >= 0 && cell.getRow() < size
                && cell.getCol() >= 0 && cell.getCol() < size;
    }

    public char[][] createGrid(Snake snake, Food food) {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) grid[row][col] = '.';
        }
        Cell foodCell = food.getPosition();
        if (foodCell != null) grid[foodCell.getRow()][foodCell.getCol()] = '*';
        boolean head = true;
        for (Cell cell : snake.getBody()) {
            grid[cell.getRow()][cell.getCol()] = head ? 'H' : 'o';
            head = false;
        }
        return grid;
    }
}

class ScoreManager {
    private String fileName;
    public ScoreManager(String fileName) { this.fileName = fileName; }

    public void saveScore(int score) {
        try {
            FileWriter writer = new FileWriter(fileName, true);
            writer.write(score + System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            System.out.println("Could not save score: " + e.getMessage());
        }
    }

    public List<Integer> getTopFiveScores() {
        ArrayList<Integer> scores = new ArrayList<Integer>();
        File file = new File(fileName);
        if (!file.exists()) return scores;
        try {
            Scanner input = new Scanner(file);
            while (input.hasNextLine()) {
                try { scores.add(Integer.parseInt(input.nextLine().trim())); }
                catch (NumberFormatException e) { /* Ignore invalid lines. */ }
            }
            input.close();
        } catch (IOException e) {
            System.out.println("Could not read scores: " + e.getMessage());
        }
        insertionSortDescending(scores);
        ArrayList<Integer> top = new ArrayList<Integer>();
        for (int i = 0; i < scores.size() && i < 5; i++) top.add(scores.get(i));
        return top;
    }

    public int getBestScore() {
        List<Integer> scores = getTopFiveScores();
        if (scores.size() == 0) return 0;
        return scores.get(0);
    }

    // Insertion Sort in descending order (largest score first).
    public static void insertionSortDescending(List<Integer> scores) {
        for (int i = 1; i < scores.size(); i++) {
            int current = scores.get(i);
            int j = i - 1;
            while (j >= 0 && scores.get(j) < current) {
                scores.set(j + 1, scores.get(j));
                j--;
            }
            scores.set(j + 1, current);
        }
    }
}
