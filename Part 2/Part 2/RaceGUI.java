// RaceGUI.java

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.Component;
import java.awt.geom.AffineTransform;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;
import java.util.ArrayList;

public class RaceGUI extends JFrame {
    private JComboBox<Integer> laneCombo;
    private JComboBox<Integer> lengthCombo;
    private JPanel horseInputPanel;
    private JButton startBtn;
    private TrackPanel trackPanel;

    public RaceGUI() {
        super("Horse Race");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5,5));

        // Top controls: lane count and track length dropdowns
        JPanel top = new JPanel();
        top.add(new JLabel("Lanes:"));
        laneCombo = new JComboBox<>(new Integer[]{2,3,4,5,6});
        top.add(laneCombo);

        top.add(new JLabel("Track (px):"));
        lengthCombo = new JComboBox<>(new Integer[]{200,300,400,500,600,700});
        top.add(lengthCombo);

        startBtn = new JButton("Start Race");
        top.add(startBtn);
        add(top, BorderLayout.NORTH);

        // Middle: inputs for each horse's name and confidence
        horseInputPanel = new JPanel(new GridBagLayout());
        add(new JScrollPane(horseInputPanel), BorderLayout.CENTER);

        // Bottom: where the race is drawn
        trackPanel = new TrackPanel();
        add(trackPanel, BorderLayout.SOUTH);

        laneCombo.addActionListener(e -> rebuildHorseInputs());
        startBtn.addActionListener(e -> onStart());

        rebuildHorseInputs();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Build rows of [Name][Confidence] fields based on selected lanes
    private void rebuildHorseInputs() {
        horseInputPanel.removeAll();
        int lanes = (Integer)laneCombo.getSelectedItem();
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);
        c.gridy = 0;

        for (int i = 1; i <= lanes; i++) {
            c.gridx = 0; c.anchor = GridBagConstraints.LINE_END;
            horseInputPanel.add(new JLabel("Horse " + i + " name:"), c);
            c.gridx = 1; c.anchor = GridBagConstraints.LINE_START;
            horseInputPanel.add(new JTextField("Horse"+i,10), c);

            c.gridx = 2; c.anchor = GridBagConstraints.LINE_END;
            horseInputPanel.add(new JLabel("Conf (0–1):"), c);
            c.gridx = 3; c.anchor = GridBagConstraints.LINE_START;
            horseInputPanel.add(new JTextField("0.7",5), c);

            c.gridy++;
        }

        horseInputPanel.revalidate();
        horseInputPanel.repaint();
        pack();
    }

    // Collect inputs, disable controls, and kick off the race animation
    private void onStart() {
        int trackLen = (Integer)lengthCombo.getSelectedItem();
        List<Horse> horses = new ArrayList<>();
        Component[] comps = horseInputPanel.getComponents();

        for (int i = 0; i < comps.length; i += 4) {
            String name = ((JTextField)comps[i+1]).getText().trim();
            double conf;
            try {
                conf = Double.parseDouble(((JTextField)comps[i+3]).getText().trim());
                if (conf < 0 || conf > 1) throw new NumberFormatException();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Confidence must be between 0 and 1.");
                return;
            }
            horses.add(new Horse(name.charAt(0), name, conf));
        }

        laneCombo.setEnabled(false);
        lengthCombo.setEnabled(false);
        for (Component comp : horseInputPanel.getComponents())
            comp.setEnabled(false);
        startBtn.setEnabled(false);

        trackPanel.setupRace(horses, trackLen, () -> {
            laneCombo.setEnabled(true);
            lengthCombo.setEnabled(true);
            for (Component comp : horseInputPanel.getComponents())
                comp.setEnabled(true);
            startBtn.setEnabled(true);
        });

        pack();
    }

    // ------------------------------------------------------------------
    // Panel responsible for drawing and animating the race
    // ------------------------------------------------------------------
    private static class TrackPanel extends JPanel {
        private List<Horse> horses;
        private int trackLen;
        private Timer timer;           // javax.swing.Timer
        private Runnable onFinish;

        public TrackPanel() {
            setPreferredSize(new Dimension(800, 300));
        }

        public void setupRace(List<Horse> horses, int length, Runnable onFinish) {
            this.horses = horses;
            this.trackLen = length;
            this.onFinish = onFinish;
            horses.forEach(Horse::goBackToStart);

            if (timer != null) timer.stop();
            timer = new Timer(30, e -> step());
            timer.start();
        }

        // one tick of the animation
        private void step() {
            for (Horse h : horses) {
                if (!h.hasFallen()) {
                    // move forward based on confidence
                    if (Math.random() < h.getConfidence()) {
                        h.moveForward();
                        h.setConfidence(Math.min(1, h.getConfidence()+0.01));
                    }
                    // very rare fall
                    double fallProb = 0.01 * h.getConfidence()*h.getConfidence();
                    if (Math.random() < fallProb) {
                        h.fall();
                        h.setConfidence(Math.max(0, h.getConfidence()-0.1));
                    }
                }
            }
            repaint();

            boolean anyWin = horses.stream()
                    .anyMatch(h -> h.getDistanceTravelled() >= trackLen);
            boolean allFall = horses.stream()
                    .allMatch(Horse::hasFallen);

            if (anyWin || allFall) {
                timer.stop();
                String msg;
                if (anyWin) {
                    Horse win = horses.stream()
                            .filter(h->h.getDistanceTravelled()>=trackLen)
                            .findFirst().get();
                    msg = "🏆 " + win.getName() + " wins!";
                } else {
                    msg = "❌ All horses fell!";
                }
                JOptionPane.showMessageDialog(this, msg);
                onFinish.run();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (horses == null) return;

            int w = getWidth(), h = getHeight();
            int lanes = horses.size();
            int laneY = h / (lanes + 1);

            // draw lanes
            g.setColor(Color.LIGHT_GRAY);
            for (int i = 1; i <= lanes; i++) {
                int y = i * laneY;
                g.drawLine(10, y, 10 + trackLen, y);
            }

            Graphics2D g2 = (Graphics2D) g;
            Font emojiFont = new Font("Segoe UI Emoji", Font.PLAIN, 40);

            for (int i = 0; i < lanes; i++) {
                Horse hr = horses.get(i);
                int x = 10 + hr.getDistanceTravelled();
                int y = (i+1) * laneY;

                if (hr.hasFallen()) {
                    // draw ❌ at the fall position
                    g2.setFont(new Font("SansSerif", Font.BOLD, 40));
                    g2.setColor(Color.RED);
                    g2.drawString("❌", x-20, y+20);
                } else {
                    // flip 🐎 horizontally
                    String horse = "🐎";
                    g2.setFont(emojiFont);
                    int fw = g2.getFontMetrics().stringWidth(horse);
                    AffineTransform old = g2.getTransform();
                    g2.translate(x + fw, y - 20);
                    g2.scale(-1, 1);
                    g2.drawString(horse, 0, 0);
                    g2.setTransform(old);
                }

                // horse name
                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g2.setColor(Color.BLACK);
                g2.drawString(hr.getName(), x + 30, y);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RaceGUI::new);
    }
}
