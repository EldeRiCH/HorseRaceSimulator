// RaceGUI.java
import javax.swing.*;
import javax.imageio.ImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class RaceGUI extends JFrame {
    private JComboBox<Integer> lengthCombo;
    private JComboBox<Integer> laneCombo;
    private JButton startBtn;
    private TrackPanel trackPanel;

    public RaceGUI() {
        super("Horse Race");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5,5));

        // --- Top controls ---
        JPanel controls = new JPanel();
        lengthCombo = new JComboBox<>(new Integer[]{20,40,60,80,100});
        laneCombo   = new JComboBox<>(new Integer[]{2,3,4,5,6});
        startBtn    = new JButton("Start Race");
        controls.add(new JLabel("Length:"));
        controls.add(lengthCombo);
        controls.add(new JLabel("Lanes:"));
        controls.add(laneCombo);
        controls.add(startBtn);
        add(controls, BorderLayout.NORTH);

        // --- Center drawing panel ---
        trackPanel = new TrackPanel();
        add(trackPanel, BorderLayout.CENTER);

        // Start button action
        startBtn.addActionListener(e -> {
            int length = (Integer) lengthCombo.getSelectedItem();
            int lanes  = (Integer) laneCombo.getSelectedItem();
            trackPanel.startRace(length, lanes);
            startBtn.setEnabled(false);
            lengthCombo.setEnabled(false);
            laneCombo.setEnabled(false);
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Inner non-static so we can re-enable controls on the outer frame
    private class TrackPanel extends JPanel {
        private BufferedImage trackImg;
        private BufferedImage horseIcon;
        private List<Horse> horses;
        private int raceLength;
        private Timer timer;

        // sample confidences & names (you can adjust or randomize)
        private static final double[] CONFIDENCES = {0.7,0.8,0.6,0.75,0.85,0.65};
        private static final String[] NAMES       = {
                "Thunder","Lightning","Storm","Blaze","Comet","Rocket"
        };

        public TrackPanel() {
            // load images
            try {
                trackImg   = ImageIO.read(new File("track.jpg"));
                horseIcon  = ImageIO.read(new File("blackHorse.png"));
            } catch(IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to load images:\n" + ex.getMessage(),
                        "Image Load Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            }
            // tile background twice horizontally
            int w = trackImg.getWidth()*2;
            int h = trackImg.getHeight();
            setPreferredSize(new Dimension(w, h));
        }

        /** Called by the frame to set up and start the race. */
        public void startRace(int length, int lanes) {
            this.raceLength = length;
            horses = new ArrayList<>();
            for (int i = 0; i < lanes; i++) {
                horses.add(new Horse(
                        (char)('A'+i),
                        NAMES[i % NAMES.length],
                        CONFIDENCES[i % CONFIDENCES.length]
                ));
            }
            horses.forEach(Horse::goBackToStart);

            // Swing timer drives the animation every 100 ms
            timer = new Timer(100, evt -> step());
            timer.start();
        }

        /** One tick: move, repaint, check end. */
        private void step() {
            // 1) move/fall logic
            for (Horse h : horses) {
                if (!h.hasFallen()) {
                    if (Math.random() < h.getConfidence()) {
                        h.moveForward();
                    }
                    if (Math.random() < 0.1*h.getConfidence()*h.getConfidence()) {
                        h.fall();
                        h.setConfidence(Math.max(0, h.getConfidence()-0.1));
                    }
                }
            }
            // 2) redraw
            repaint();
            // 3) check end conditions
            boolean anyWin = horses.stream()
                    .anyMatch(h->h.getDistanceTravelled()>=raceLength);
            boolean allFall = horses.stream()
                    .allMatch(Horse::hasFallen);
            if (anyWin||allFall) {
                timer.stop();
                String msg;
                if (anyWin) {
                    Horse winner = horses.stream()
                            .filter(h->h.getDistanceTravelled()>=raceLength)
                            .findFirst().get();
                    msg = "🏆 And the winner is "+winner.getName()+"!";
                } else {
                    msg = "💥 All horses fell—no winner!";
                }
                JOptionPane.showMessageDialog(this, msg);
                // re-enable controls
                startBtn.setEnabled(true);
                lengthCombo.setEnabled(true);
                laneCombo.setEnabled(true);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (horses==null) return;

            int panelW = getWidth(), panelH = getHeight();
            int lanes  = horses.size();
            int laneY  = panelH/(lanes+1);

            // draw the track twice
            g.drawImage(trackImg, 0, 0, this);
            g.drawImage(trackImg, trackImg.getWidth(), 0, this);

            // draw each horse icon
            for (int i=0; i<lanes; i++) {
                Horse h = horses.get(i);
                double frac = (double)h.getDistanceTravelled()/raceLength;
                int x = (int)(frac * (trackImg.getWidth()*2));
                int y = (i+1)*laneY;

                // center icon
                int ix = x - horseIcon.getWidth()/2;
                int iy = y - horseIcon.getHeight()/2;
                g.drawImage(horseIcon, ix, iy, this);

                // draw name above it
                g.setFont(new Font("SansSerif",Font.BOLD,14));
                g.setColor(Color.BLACK);
                String nm = h.getName();
                int sw = g.getFontMetrics().stringWidth(nm);
                g.drawString(nm, x-sw/2, iy-5);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RaceGUI::new);
    }
}
