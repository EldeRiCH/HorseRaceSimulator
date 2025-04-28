// RaceGUI.java

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
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

        // Top: lanes + track length selection
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

        // Middle: horse name + confidence sliders
        horseInputPanel = new JPanel(new GridBagLayout());
        add(new JScrollPane(horseInputPanel), BorderLayout.CENTER);

        // Bottom: drawing + stats panel
        trackPanel = new TrackPanel();
        add(trackPanel, BorderLayout.SOUTH);

        laneCombo.addActionListener(e -> rebuildHorseInputs());
        startBtn.addActionListener(e -> onStart());

        rebuildHorseInputs();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Build input rows for each horse
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
            horseInputPanel.add(new JLabel("Confidence:"), c);

            JSlider slider = new JSlider(0,100,70);
            slider.setMajorTickSpacing(20);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            slider.setPreferredSize(new Dimension(150, slider.getPreferredSize().height));

            c.gridx = 3; c.anchor = GridBagConstraints.LINE_START;
            horseInputPanel.add(slider, c);

            c.gridy++;
        }

        horseInputPanel.revalidate();
        horseInputPanel.repaint();
        pack();
    }

    // Gather inputs, show odds and betting, then start the race
    private void onStart() {
        int trackLen = (Integer)lengthCombo.getSelectedItem();
        List<Horse> horses = new ArrayList<>();
        Component[] comps = horseInputPanel.getComponents();
        for (int i = 0; i < comps.length; i += 4) {
            String name = ((JTextField)comps[i+1]).getText().trim();
            double conf = ((JSlider)comps[i+3]).getValue()/100.0;
            horses.add(new Horse(name.charAt(0), name, conf));
        }

        // prepare betting options with odds = 1/confidence
        String[] betOptions = new String[horses.size()];
        double[] oddsArr     = new double[horses.size()];
        for (int i = 0; i < horses.size(); i++) {
            double conf = horses.get(i).getConfidence();
            double odds = 1.0 / conf;
            oddsArr[i] = odds;
            betOptions[i] = String.format(
                    "%s (odds %.2f×)", horses.get(i).getName(), odds);
        }

        JComboBox<String> betCombo = new JComboBox<>(betOptions);
        JTextField betField = new JTextField("100", 7);
        JPanel betPanel = new JPanel();
        betPanel.add(new JLabel("Bet on:"));
        betPanel.add(betCombo);
        betPanel.add(new JLabel("Amount:"));
        betPanel.add(betField);

        int choice = JOptionPane.showConfirmDialog(
                this, betPanel, "Place your bet", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) return;
        int betIdx;
        double betAmt;
        try {
            betIdx = betCombo.getSelectedIndex();
            betAmt = Double.parseDouble(betField.getText().trim());
            if (betAmt <= 0) throw new NumberFormatException();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid bet.");
            return;
        }
        double betOdds = oddsArr[betIdx];

        // disable controls during race
        laneCombo.setEnabled(false);
        lengthCombo.setEnabled(false);
        for (Component comp : horseInputPanel.getComponents())
            comp.setEnabled(false);
        startBtn.setEnabled(false);

        // start race with betting info
        trackPanel.setupRace(
                horses, trackLen, betIdx, betAmt, betOdds,
                () -> {
                    // re-enable when done
                    laneCombo.setEnabled(true);
                    lengthCombo.setEnabled(true);
                    for (Component comp : horseInputPanel.getComponents())
                        comp.setEnabled(true);
                    startBtn.setEnabled(true);
                }
        );
        pack();
    }

    // -------------------------------------------------------------------
    // Panel that draws, animates, and displays end-of-race stats
    // -------------------------------------------------------------------
    private static class TrackPanel extends JPanel {
        private List<Horse> horses;
        private int trackLen;
        private Timer timer;
        private int betIdx;
        private double betAmt, betOdds;
        private Runnable onFinish;
        private long startTime;

        public TrackPanel() {
            setPreferredSize(new Dimension(800,300));
        }

        public void setupRace(
                List<Horse> horses,
                int length,
                int betIdx,
                double betAmt,
                double betOdds,
                Runnable onFinish
        ) {
            this.horses = horses;
            this.trackLen = length;
            this.betIdx = betIdx;
            this.betAmt = betAmt;
            this.betOdds = betOdds;
            this.onFinish = onFinish;
            horses.forEach(Horse::goBackToStart);

            if (timer != null) timer.stop();
            startTime = System.currentTimeMillis();
            timer = new Timer(30, e -> step());
            timer.start();
        }

        private void step() {
            for (Horse h : horses) {
                if (!h.hasFallen()) {
                    if (Math.random() < h.getConfidence()) {
                        h.moveForward();
                        if (h.getDistanceTravelled() % 10 == 0) {
                            h.setConfidence(Math.min(
                                    1.0,
                                    h.getConfidence() + 0.000002
                            ));
                        }
                    }
                    double fallProb = 0.001 * h.getConfidence() * h.getConfidence();
                    if (Math.random() < fallProb) {
                        h.fall();
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
                long endTime = System.currentTimeMillis();
                long durationMs = endTime - startTime;
                Horse winner = null;
                if (anyWin) {
                    winner = horses.stream()
                            .filter(h->h.getDistanceTravelled()>=trackLen)
                            .findFirst().get();
                }

                // announce winner
                if (winner != null) {
                    JOptionPane.showMessageDialog(
                            this, "🏆 " + winner.getName() + " wins!"
                    );
                } else {
                    JOptionPane.showMessageDialog(
                            this, "❌ All horses fell!"
                    );
                }

                // betting result
                if (winner != null && horses.indexOf(winner) == betIdx) {
                    double payout = betAmt * betOdds;
                    JOptionPane.showMessageDialog(
                            this,
                            String.format("You won! Payout: %.2f", payout)
                    );
                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            String.format("You lost your bet of %.2f", betAmt)
                    );
                }

                // show stats
                StringBuilder stats = new StringBuilder();
                stats.append(String.format(
                        "Race duration: %.2f seconds%n",
                        durationMs/1000.0
                ));
                if (winner != null) {
                    stats.append(String.format(
                            "Winner finish time: %.2f seconds%n%n",
                            durationMs/1000.0
                    ));
                }
                stats.append("Final distances:\n");
                for (Horse h : horses) {
                    stats.append(String.format(
                            "%s: %d px%n",
                            h.getName(),
                            h.getDistanceTravelled()
                    ));
                }
                JOptionPane.showMessageDialog(this, stats.toString());

                onFinish.run();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (horses == null) return;

            int hgt = getHeight(), lanes = horses.size();
            int laneY = hgt / (lanes + 1);

            Graphics2D g2 = (Graphics2D)g;
            Font emojiFont = new Font("Segoe UI Emoji",Font.PLAIN,40);
            Font infoFont  = new Font("SansSerif",Font.PLAIN,12);

            // draw lanes
            g2.setColor(Color.LIGHT_GRAY);
            for (int i = 1; i <= lanes; i++) {
                int y = i * laneY;
                g2.drawLine(10, y, 10 + trackLen, y);
            }

            // draw each horse
            for (int i = 0; i < lanes; i++) {
                Horse hr = horses.get(i);
                int x = 10 + hr.getDistanceTravelled();
                int y = (i+1)*laneY;

                if (hr.hasFallen()) {
                    g2.setFont(new Font("SansSerif",Font.BOLD,40));
                    g2.setColor(Color.RED);
                    g2.drawString("❌", x-20, y+20);
                } else {
                    String horse = "🐎";
                    g2.setFont(emojiFont);
                    int fw = g2.getFontMetrics().stringWidth(horse);
                    AffineTransform old = g2.getTransform();
                    g2.translate(x+fw, y-20);
                    g2.scale(-1,1);
                    g2.setColor(Color.BLACK);
                    g2.drawString(horse,0,0);
                    g2.setTransform(old);
                }

                // name + real-time confidence
                g2.setFont(infoFont);
                g2.setColor(Color.BLACK);
                g2.drawString(hr.getName(), x+30, y+4);
                g2.drawString(
                        String.format("Conf: %.2f", hr.getConfidence()),
                        x+30, y+18
                );
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RaceGUI::new);
    }
}
