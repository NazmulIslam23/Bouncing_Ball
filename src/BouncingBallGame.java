import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;

public class BouncingBallGame extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private Timer obstacleTimer;
    private int ballX, ballY, ballSize = 20;
    private int ballDX, ballDY;
    private int paddleX, paddleY = 350, paddleWidth = 100, paddleHeight = 10;
    private int score, level;
    private boolean gameRunning = false;
    private boolean gamePaused = false;
    private Clip bounceSound;
    private Rectangle bonus;
    private Rectangle spike;
    private ArrayList<Rectangle> obstacles = new ArrayList<>();
    private Random random = new Random();

    public BouncingBallGame() {
        setPreferredSize(new Dimension(400, 400));
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(10, this);
        obstacleTimer = new Timer(10000, e -> repositionObstacles());
        loadAssets();
        resetGame();
    }

    private void loadAssets() {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(getClass().getResource("/bounce.wav"));
            bounceSound = AudioSystem.getClip();
            bounceSound.open(audioIn);
        } catch (Exception e) {
            System.out.println("Error loading sound: " + e.getMessage());
        }
    }

    private void spawnBonus() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 20 || height <= 20) {
            bonus = new Rectangle(0, 0, 20, 20);
            return;
        }
        Rectangle newBonus;
        do {
            int x = random.nextInt(width - 20);
            int y = random.nextInt((height - 100) / 2) + 50;
            newBonus = new Rectangle(x, y, 20, 20);
        } while (intersectsWithObstacles(newBonus) || (spike != null && newBonus.intersects(spike)));
        bonus = newBonus;
    }

    private void spawnSpike() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 20 || height <= 20) {
            spike = new Rectangle(0, 0, 20, 20);
            return;
        }
        Rectangle newSpike;
        Rectangle ballStartZone = new Rectangle(100 - 30, 100 - 30, ballSize + 60, ballSize + 60);
        do {
            int x = random.nextInt(width - 20);
            int y = random.nextInt((height - 100) / 2) + 50;
            newSpike = new Rectangle(x, y, 20, 20);
        } while (newSpike.intersects(ballStartZone)
                || (bonus != null && newSpike.intersects(bonus))
                || intersectsWithObstacles(newSpike));
        spike = newSpike;
    }

    private void spawnObstacles() {
        obstacles.clear();
        int width = getWidth();
        if (width <= 60) return;
        for (int i = 0; i < 3; i++) {
            Rectangle newObs;
            do {
                int x = random.nextInt(width - 60);
                int y = random.nextInt(80) + 10;
                newObs = new Rectangle(x, y, 60, 10);
            } while (intersectsWithObstacles(newObs)
                    || (spike != null && newObs.intersects(spike))
                    || (bonus != null && newObs.intersects(bonus)));
            obstacles.add(newObs);
        }
    }

    private boolean intersectsWithObstacles(Rectangle rect) {
        for (Rectangle obs : obstacles) {
            if (obs.intersects(rect)) return true;
        }
        return false;
    }

    private void repositionObstacles() {
        spawnObstacles();
        spawnBonus();
        repaint();
    }

    private void resetGame() {
        ballX = 100;
        ballY = 100;
        ballDX = 2;
        ballDY = 2;
        paddleX = 150;
        score = 0;
        level = 1;
        gamePaused = false;
        bonus = null;
        spike = null;
        obstacles.clear();
        spawnObstacles();
        spawnBonus();
        spawnSpike();
        timer.start();
        obstacleTimer.start();
        gameRunning = false;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(50, 150, 50));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.RED);
        g.fillOval(ballX, ballY, ballSize, ballSize);

        g.setColor(Color.BLUE);
        g.fillRect(paddleX, paddleY, paddleWidth, paddleHeight);

        if (bonus != null) {
            Color[] bonusColors = {
                    Color.BLUE, Color.MAGENTA, Color.ORANGE, Color.CYAN, Color.PINK
            };
            int index = (score / 10) % bonusColors.length;
            g.setColor(bonusColors[index]);
            g.fillRect(bonus.x, bonus.y, bonus.width, bonus.height);
        }

        if (spike != null) {
            g.setColor(Color.BLACK);
            g.fillRect(spike.x, spike.y, spike.width, spike.height);

            int[] xPoints = { spike.x + spike.width / 2, spike.x + 5, spike.x + spike.width - 5 };
            int[] yPoints = { spike.y + 5, spike.y + spike.height - 5, spike.y + spike.height - 5 };
            g.setColor(Color.RED);
            g.fillPolygon(xPoints, yPoints, 3);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics fm = g.getFontMetrics();
            String dangerMark = "!";
            int textWidth = fm.stringWidth(dangerMark);
            g.drawString(dangerMark, spike.x + spike.width / 2 - textWidth / 2, spike.y + spike.height - 12);
        }

        g.setColor(Color.BLACK);
        for (Rectangle obs : obstacles) {
            g.fillRect(obs.x, obs.y, obs.width, obs.height);
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Score: " + score, 10, 20);
        g.drawString("Level: " + level, 300, 20);

        if (!gameRunning) {
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.setColor(Color.RED);
            g.drawString("Press Enter to Start", 60, 200);
        }

        if (gamePaused) {
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.setColor(Color.ORANGE);
            g.drawString("Paused", 150, 200);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning || gamePaused) return;

        ballX += ballDX;
        ballY += ballDY;

        if (ballX <= 0 || ballX + ballSize >= getWidth()) {
            ballDX *= -1;
            playSound();
        }
        if (ballY <= 0) {
            ballDY *= -1;
            playSound();
        }

        if (ballY + ballSize >= paddleY && ballX + ballSize >= paddleX && ballX <= paddleX + paddleWidth) {
            ballDY *= -1;
            score++;
            playSound();
            if (score % 5 == 0) {
                level++;
                ballDX += (ballDX > 0) ? 1 : -1;
                ballDY += (ballDY > 0) ? 1 : -1;
            }
        }

        Rectangle ballRect = new Rectangle(ballX, ballY, ballSize, ballSize);
        for (Rectangle obs : obstacles) {
            if (ballRect.intersects(obs)) {
                ballDY *= -1;
                playSound();
                break;
            }
        }

        if (bonus != null && ballRect.intersects(bonus)) {
            score += 5;
            spawnBonus();
        }

        if (spike != null && ballRect.intersects(spike)) {
            gameRunning = false;
            timer.stop();
            obstacleTimer.stop();
        }

        if (ballY + ballSize >= getHeight()) {
            gameRunning = false;
            timer.stop();
            obstacleTimer.stop();
        }

        repaint();
    }

    private void playSound() {
        if (bounceSound != null) {
            bounceSound.setFramePosition(0);
            bounceSound.start();
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_ENTER) {
            if (!gameRunning) {
                resetGame();
                gameRunning = true;
            }
        } else if ((key == KeyEvent.VK_P || key == KeyEvent.VK_SPACE) && gameRunning) {
            gamePaused = !gamePaused;
        }

        if (!gameRunning || gamePaused) return;

        if (key == KeyEvent.VK_LEFT && paddleX > 0) {
            paddleX -= 20;
        } else if (key == KeyEvent.VK_RIGHT && paddleX + paddleWidth < getWidth()) {
            paddleX += 20;
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Bouncing Ball");
        BouncingBallGame gamePanel = new BouncingBallGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(gamePanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
