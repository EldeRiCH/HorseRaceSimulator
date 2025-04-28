// RaceGUI.java

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.JTextField;


import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.awt.Component;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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

        // Top panel: choose lanes and track length
        JPanel top = new JPanel();
        top.add(new JLabel("Lanes:"));
        laneCombo = new JComboBox<>(new Integer[]{2,3,4,5,6});
        top.add(laneCombo);

        top.add(new JLabel("Track (px):"));
        lengthCombo = new JComboBox<>(new Integer[]{200,400,600,800,1000});
        top.add(lengthCombo);

        startBtn = new JButton("Start Race");
        top.add(startBtn);
        add(top, BorderLayout.NORTH);

        // Middle: horse name + confidence inputs
        horseInputPanel = new JPanel(new GridBagLayout());
        add(new JScrollPane(horseInputPanel), BorderLayout.CENTER);

        // Bottom: where we draw the race
        trackPanel = new TrackPanel();
        add(trackPanel, BorderLayout.SOUTH);

        // Rebuild input fields when number of lanes changes
        laneCombo.addActionListener(e -> rebuildHorseInputs());

        // When Start is clicked, gather data and launch animation
        startBtn.addActionListener(e -> onStart());

        rebuildHorseInputs();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Rebuilds the rows of [Name][Confidence] based on lane count
    private void rebuildHorseInputs() {
        horseInputPanel.removeAll();
        int lanes = (Integer) laneCombo.getSelectedItem();
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

    // Called when Start Race is pressed
    private void onStart() {
        int length = (Integer) lengthCombo.getSelectedItem();
        int lanes  = (Integer) laneCombo.getSelectedItem();

        // build Horse objects from the input fields
        List<Horse> horses = new ArrayList<>();
        Component[] comps = horseInputPanel.getComponents();
        for (int i = 0; i < comps.length; i += 4) {
            String name = ((JTextField)comps[i+1]).getText().trim();
            double conf;
            try {
                conf = Double.parseDouble(((JTextField)comps[i+3]).getText().trim());
                if (conf < 0 || conf > 1) throw new NumberFormatException();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Confidence must be 0–1.");
                return;
            }
            horses.add(new Horse(name.charAt(0), name, conf));
        }

        // disable inputs during the race
        laneCombo.setEnabled(false);
        lengthCombo.setEnabled(false);
        for (Component comp : horseInputPanel.getComponents())
            comp.setEnabled(false);
        startBtn.setEnabled(false);

        // start animation, re-enable on complete
        trackPanel.setupRace(horses, length, ()-> {
            laneCombo.setEnabled(true);
            lengthCombo.setEnabled(true);
            for (Component comp : horseInputPanel.getComponents())
                comp.setEnabled(true);
            startBtn.setEnabled(true);
        });

        pack();
    }

    // ------------------------------------------------------------------------
    // Inner panel where the race is drawn
    // ------------------------------------------------------------------------
    private static class TrackPanel extends JPanel {
        private List<Horse> horses;
        private int trackLen;
        private Timer timer;            // javax.swing.Timer
        private Runnable onFinish;

        public TrackPanel() {
            setPreferredSize(new Dimension(800, 400));
        }

        // Configure and launch the timer
        public void setupRace(List<Horse> horses, int length, Runnable onFinish) {
            this.horses = horses;
            this.trackLen = length;
            this.onFinish = onFinish;
            horses.forEach(Horse::goBackToStart);

            if (timer != null) timer.stop();
            timer = new Timer(30, e -> step());
            timer.start();
        }

        // Called on each Timer tick
        private void step() {
            for (Horse h : horses) {
                if (!h.hasFallen()) {
                    // try to move forward
                    if (Math.random() < h.getConfidence()) {
                        h.moveForward();
                        // boost confidence slightly
                        h.setConfidence(Math.min(1, h.getConfidence() + 0.01));
                    }
                    // small chance to fall
                    if (Math.random() < 0.1 * h.getConfidence() * h.getConfidence()) {
                        h.fall();
                        // drop confidence
                        h.setConfidence(Math.max(0, h.getConfidence() - 0.1));
                    }
                }
            }
            repaint();

            // check for winner or everyone fallen
            boolean anyWin = horses.stream()
                    .anyMatch(h -> h.getDistanceTravelled() >= trackLen);
            boolean allFall = horses.stream()
                    .allMatch(Horse::hasFallen);

            if (anyWin || allFall) {
                timer.stop();
                String msg;
                if (anyWin) {
                    Horse win = horses.stream()
                            .filter(h -> h.getDistanceTravelled() >= trackLen)
                            .findFirst().get();
                    msg = "🏆 " + win.getName() + " wins!";
                } else {
                    msg = "💥 All fell—no winner!";
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

            // draw each horse as a circle + name
            for (int i = 0; i < lanes; i++) {
                Horse hr = horses.get(i);
                int x = 10 + hr.getDistanceTravelled();
                int y = (i+1) * laneY;
                g.setColor(new Color(50,50,200));
                g.fillOval(x, y-8, 16,16);
                g.setColor(Color.BLACK);
                g.setFont(new Font("SansSerif",Font.PLAIN,12));
                g.drawString(hr.getName(), x+20, y+4);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RaceGUI::new);
    }
}
