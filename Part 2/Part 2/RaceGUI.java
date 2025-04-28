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
import java.awt.Color;
import java.awt.Font;
import java.awt.Component;            // <— import this!

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.List;
import java.util.ArrayList;

public class RaceGUI extends JFrame {
    private JComboBox<Integer> laneCombo;
    private JTextField lengthField;
    private JPanel horseInputPanel;
    private JButton startBtn;
    private TrackPanel trackPanel;

    public RaceGUI() {
        super("Horse Race");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5,5));

        // --- Top: settings panel ---
        JPanel settings = new JPanel();
        settings.add(new JLabel("Lanes (2–6):"));
        laneCombo = new JComboBox<>(new Integer[]{2,3,4,5,6});
        settings.add(laneCombo);

        settings.add(new JLabel("Track length (px):"));
        lengthField = new JTextField("400", 5);
        settings.add(lengthField);

        startBtn = new JButton("Start Race");
        settings.add(startBtn);

        add(settings, BorderLayout.NORTH);

        // --- Center: dynamic horse inputs ---
        horseInputPanel = new JPanel(new GridBagLayout());
        add(new JScrollPane(horseInputPanel), BorderLayout.CENTER);

        // --- Bottom: track display ---
        trackPanel = new TrackPanel();
        add(trackPanel, BorderLayout.SOUTH);

        // rebuild inputs when lane count changes
        laneCombo.addActionListener(e -> rebuildHorseInputs());

        // start button action
        startBtn.addActionListener(e -> onStart());

        // initial build
        rebuildHorseInputs();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /** Rebuilds the grid of Horse Name + Confidence text fields. */
    private void rebuildHorseInputs() {
        horseInputPanel.removeAll();
        int lanes = (Integer) laneCombo.getSelectedItem();
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);
        c.gridx = 0; c.gridy = 0;
        for (int i = 1; i <= lanes; i++) {
            c.anchor = GridBagConstraints.LINE_END;
            horseInputPanel.add(new JLabel("Horse " + i + " name:"), c);
            c.gridx = 1; c.anchor = GridBagConstraints.LINE_START;
            horseInputPanel.add(new JTextField("Horse"+i,10), c);

            c.gridx = 2; c.anchor = GridBagConstraints.LINE_END;
            horseInputPanel.add(new JLabel("Confidence (0–1):"), c);
            c.gridx = 3; c.anchor = GridBagConstraints.LINE_START;
            horseInputPanel.add(new JTextField("0.7",5), c);

            c.gridy++;
            c.gridx = 0;
        }
        horseInputPanel.revalidate();
        horseInputPanel.repaint();
        pack();
    }

    /** Triggered when the user clicks “Start Race.” */
    private void onStart() {
        // parse track length
        int length;
        try {
            length = Integer.parseInt(lengthField.getText().trim());
            if (length <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a positive integer for track length.");
            return;
        }

        // gather horses from the input panel
        List<Horse> horses = new ArrayList<>();
        Component[] comps = horseInputPanel.getComponents();
        for (int i = 0; i < comps.length; i += 4) {
            JTextField nameF = (JTextField) comps[i+1];
            JTextField confF = (JTextField) comps[i+3];
            String name = nameF.getText().trim();
            double conf;
            try {
                conf = Double.parseDouble(confF.getText().trim());
                if (conf < 0 || conf > 1) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Confidence must be between 0 and 1.");
                return;
            }
            horses.add(new Horse(name.charAt(0), name, conf));
        }

        // disable inputs during race
        laneCombo.setEnabled(false);
        lengthField.setEnabled(false);
        for (Component comp : horseInputPanel.getComponents()) {
            comp.setEnabled(false);
        }
        startBtn.setEnabled(false);

        // start the race animation
        trackPanel.setupRace(horses, length, ()-> {
            // re-enable inputs when finished
            laneCombo.setEnabled(true);
            lengthField.setEnabled(true);
            for (Component comp : horseInputPanel.getComponents()) {
                comp.setEnabled(true);
            }
            startBtn.setEnabled(true);
        });

        pack();
    }

    // ------------------------------------------------------------------------
    // Inner class that draws and animates the track
    // ------------------------------------------------------------------------
    private static class TrackPanel extends JPanel {
        private List<Horse> horses;
        private int trackLength;
        private Timer timer;            // javax.swing.Timer
        private Runnable onFinish;

        public TrackPanel() {
            setPreferredSize(new Dimension(600, 300));
        }

        public void setupRace(List<Horse> horses, int length, Runnable onFinish) {
            this.horses = horses;
            this.trackLength = length;
            this.onFinish = onFinish;
            // reset positions
            horses.forEach(Horse::goBackToStart);

            if (timer != null) timer.stop();
            timer = new Timer(30, e -> step());
            timer.start();
        }

        private void step() {
            for (Horse h : horses) {
                if (Math.random() < h.getConfidence()) {
                    h.moveForward();
                }
            }
            repaint();

            boolean anyWin = horses.stream()
                    .anyMatch(h -> h.getDistanceTravelled() >= trackLength);
            if (anyWin) {
                timer.stop();
                Horse winner = horses.stream()
                        .filter(h->h.getDistanceTravelled()>=trackLength)
                        .findFirst().get();
                JOptionPane.showMessageDialog(this,
                        "🏆 And the winner is " + winner.getName() + "!");
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

            g.setColor(Color.LIGHT_GRAY);
            for (int i = 1; i <= lanes; i++) {
                int y = i * laneY;
                g.drawLine(10, y, 10 + trackLength, y);
            }

            for (int i = 0; i < lanes; i++) {
                Horse hr = horses.get(i);
                int x = 10 + hr.getDistanceTravelled();
                int y = (i+1) * laneY;
                g.setColor(new Color(50,50,200));
                g.fillOval(x, y-8, 16, 16);
                g.setColor(Color.BLACK);
                g.drawString(hr.getName(), x + 20, y + 4);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RaceGUI::new);
    }
}
