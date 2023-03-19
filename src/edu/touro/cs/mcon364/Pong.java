package edu.touro.cs.mcon364;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Random;

public class Pong extends JFrame {
    public static void main(String[] args) {
        new Pong();
    }

    private static final int WIDTH = 465, HEIGHT = 640, WINDOW_BUFFER = 20, BALL_DIAMETER = 25,
            PADDLE_WIDTH = 20, PADDLE_HEIGHT = 100, EDGE_MARGIN = 10, GAME_WIDTH = WIDTH - (WINDOW_BUFFER * 2),
            GAME_HEIGHT = HEIGHT - (WINDOW_BUFFER * 2);
    private PriorityQueue<Standing> LEADERBOARD;
    private final Timer TICK_CLOCK;

    private Point ball, paddle;
    private int ballDx, ballDy, score;

    private Pong() {
        setTitle("Pong");
        setSize(WIDTH, HEIGHT);
        setResizable(false);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                TICK_CLOCK.stop();
                submitScore("Nul");
            }
        });

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        TICK_CLOCK = new Timer(25, new GameTick());
        loadStandings();

        JTextArea text = getStandings();
        text.setText(text.getText() + "\nAre you ready to play?");
        setVisible(true);

        int play = JOptionPane.showConfirmDialog(this, text);
        if (play == JOptionPane.YES_OPTION) {
            newGame();
            pack();
        } else {
            System.exit(0);
        }
    }

    private void saveScores() {
        Properties props = new Properties(3);

        for (int i = 0; i < 3; i++) {
            Standing s = LEADERBOARD.poll();

            if (s != null) {
                props.put(i + "n", s.NAME);
                props.put(i + "s", Integer.toString(s.SCORE));
            }
        }

        try {
            props.store(new FileOutputStream("C:/Users/askat/IdeaProjects/pong-one-player-aharonk/scores.properties"), "");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save scores.", "Saving Failed!",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadStandings() {
        Properties scores = new Properties(3);

        try {
            scores.load(new FileInputStream("C:/Users/askat/IdeaProjects/pong-one-player-aharonk/scores.properties"));
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load scores.", "Loading Failed!",
                    JOptionPane.ERROR_MESSAGE);
        }

        int[] scoreList = new int[3];
        String[] namesList = new String[3];
        for (Map.Entry<Object, Object> e : scores.entrySet()) {
            String key = (String) e.getKey();
            int place = Character.getNumericValue(key.charAt(0));
            if (key.charAt(1) == 'n') {
                namesList[place] = (String) e.getValue();
            } else {
                scoreList[place] = Integer.parseInt((String) e.getValue());
            }
        }

        LEADERBOARD = new PriorityQueue<>((o1, o2) -> o2.SCORE - o1.SCORE);
        for (int i = 0; i < 3; i++) {
            if (namesList[i] != null) {
                LEADERBOARD.add(new Standing(namesList[i], scoreList[i]));
            }
        }
    }

    private JTextArea getStandings() {
        JTextArea out = new JTextArea();
        out.setBorder(null);
        out.setOpaque(false);
        out.setFont(UIManager.getFont("Label.font"));

        StringBuilder scores = new StringBuilder("Hi Scores:\n");
        int i = 0;
        for (Standing s : LEADERBOARD) {
            scores.append(s).append("\n");
            if (i++ == 2) {
                break;
            }
        }

        out.setText(scores.toString());
        return out;
    }

    private void newGame() {
        score = 0;

        ball = new Point(WIDTH / 2 - BALL_DIAMETER / 2, HEIGHT / 2 - BALL_DIAMETER / 2);
        paddle = new Point(EDGE_MARGIN + WINDOW_BUFFER, HEIGHT / 2 - PADDLE_HEIGHT / 2);

        JPanel game = new GamePanel();
        add(game, BorderLayout.CENTER);
        game.setFocusable(true);
        game.requestFocusInWindow();
    }

    private void lose() {
        TICK_CLOCK.stop();

        submitScore(null);

        JTextArea text = getStandings();
        text.setText(text.getText() + "\nWould you like to play again?");

        int response = JOptionPane.showConfirmDialog(this, text,
                "Play again?", JOptionPane.YES_NO_OPTION);
        if (response == 0) {
            newGame();
        } else {
            saveScores();
            System.exit(0);
        }
    }

    private void submitScore(String name) {
        if (name == null) {
            if (LEADERBOARD.size() < 3) {
                name = JOptionPane.showInputDialog(this, "Enter your name for the leaderboard.");
            } else {
                for (Standing s : LEADERBOARD) {
                    if (score > s.SCORE) {
                        name = JOptionPane.showInputDialog(this, "Enter your name for the leaderboard.");
                        break;
                    }
                }
            }

            if (name == null || name.isEmpty()) {
                name = "Nul";
            }
        }

        LEADERBOARD.add(new Standing(name, score));
    }

    private static class Standing {
        public final String NAME;
        public final int SCORE;

        public Standing(String name, int score) {
            this.NAME = name.substring(0, 3);
            this.SCORE = score;
        }

        @Override
        public String toString() {
            return NAME + "\t" + SCORE;
        }
    }

    private class GamePanel extends JPanel {
        private static final int PADDLE_SPEED = 10;

        private GamePanel() {
            super(new BorderLayout());
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(Pong.WIDTH, Pong.HEIGHT));

            TICK_CLOCK.start();

            Random rand = new Random();
            ballDx = rand.nextInt() % 2 == 0 ? 4 : -4;
            ballDy = rand.nextInt() % 2 == 0 ? 4 : -4;

            addMouseWheelListener(e -> movePaddle(PADDLE_SPEED * e.getWheelRotation()));

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        movePaddle(-PADDLE_SPEED);
                    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        movePaddle(PADDLE_SPEED);
                    }
                }
            });
        }

        private void movePaddle(int dY) {
            int newY = paddle.y + dY;
            if (newY <= WINDOW_BUFFER) {
                paddle.y = WINDOW_BUFFER;
            } else if (paddle.y + PADDLE_HEIGHT > GAME_HEIGHT + WINDOW_BUFFER) {
                paddle.y = GAME_HEIGHT + WINDOW_BUFFER - PADDLE_HEIGHT;
            } else {
                paddle.translate(0, dY);
            }
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(0xd3d3d3));

            // score
            g.setFont(g.getFont().deriveFont(50f));
            g.drawString(Integer.toString(score), Pong.WIDTH - 85, 50 + WINDOW_BUFFER);

            // game area
            g.fillOval(ball.x, ball.y, BALL_DIAMETER, BALL_DIAMETER);
            g.fillRect(paddle.x, paddle.y, PADDLE_WIDTH, PADDLE_HEIGHT);
            g.drawRect(WINDOW_BUFFER, WINDOW_BUFFER, GAME_WIDTH, GAME_HEIGHT);

            g.setColor(new Color(0x41FF00));
            g.drawRect(WINDOW_BUFFER, WINDOW_BUFFER, GAME_WIDTH, GAME_HEIGHT);
        }
    }

    private class GameTick implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ball.translate(ballDx, ballDy);

            if (ball.x <= 0) {
                lose();
            } else if (ball.x + BALL_DIAMETER >= GAME_WIDTH + WINDOW_BUFFER) {
                ballDx = -ballDx;
            } else if (ball.x <= PADDLE_WIDTH + EDGE_MARGIN + WINDOW_BUFFER
                    && ball.y + BALL_DIAMETER >= paddle.y && ball.y <= paddle.y + PADDLE_HEIGHT && ballDx < 0) {
                score++;
                if (ball.y + BALL_DIAMETER <= paddle.y - 5 || ball.y >= paddle.y + PADDLE_HEIGHT - 5) {
                    ballDy = -ballDy;
                } else {
                    ballDx = -ballDx;
                }
            }

            if (ball.y + BALL_DIAMETER >= GAME_HEIGHT + WINDOW_BUFFER || ball.y <= WINDOW_BUFFER) {
                ballDy = -ballDy;
            }
            repaint();
        }
    }
}
