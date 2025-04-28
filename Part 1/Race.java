import java.util.concurrent.TimeUnit;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * A simple horse race simulation with 2–6 lanes.
 * Each horse has a confidence level and may fall.
 * You can replay the race with fallen horses keeping their reduced confidence.
 */
public class Race {
    private int raceLength;
    private List<Horse> lanes;
    private int numberOfLanes;

    /**
     * Create a new race.
     * @param distance  length of track in steps
     * @param lanes     number of lanes (2–6)
     */
    public Race(int distance, int lanes) {
        if (lanes < 2 || lanes > 6) {
            throw new IllegalArgumentException("Need between 2 and 6 lanes");
        }
        this.raceLength    = distance;
        this.numberOfLanes = lanes;
        this.lanes         = new ArrayList<>();
        for (int i = 0; i < lanes; i++) {
            this.lanes.add(null);
        }
    }

    /**
     * Put a horse in a lane (1-based index).
     */
    public void addHorse(Horse theHorse, int laneNumber) {
        if (laneNumber < 1 || laneNumber > numberOfLanes) {
            System.out.println(
                    "Can't add horse to lane " + laneNumber +
                            ", only " + numberOfLanes + " lanes available."
            );
            return;
        }
        lanes.set(laneNumber - 1, theHorse);
    }

    /**
     * Run the race until someone wins or everyone falls, then announce result.
     */
    public void startRace() {
        // reset all horses to the starting line
        for (Horse horse : lanes) {
            if (horse != null) {
                horse.goBackToStart();
            }
        }

        boolean finished = false;
        while (!finished) {
            // try moving each horse
            for (Horse horse : lanes) {
                if (horse != null) {
                    moveHorse(horse);
                }
            }

            // show the current positions
            printRace();

            // did someone cross the finish?
            for (Horse horse : lanes) {
                if (horse != null && raceWonBy(horse)) {
                    finished = true;
                    break;
                }
            }
            // or did everyone take a tumble?
            if (!finished && allHorsesFallen()) {
                finished = true;
            }

            // slow it down so we can watch
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // announce the result
        System.out.println();
        for (Horse horse : lanes) {
            if (horse != null && raceWonBy(horse)) {
                System.out.println("🏆 And the winner is " + horse.getName() + "! 🏆");
                return;
            }
        }
        System.out.println("💥 All horses fell! 💥");
    }

    /**
     * Decide if a horse moves or falls this turn.
     * Confidence affects both chances.
     */
    private void moveHorse(Horse theHorse) {
        if (!theHorse.hasFallen()) {
            // forward with probability = confidence
            if (Math.random() < theHorse.getConfidence()) {
                theHorse.moveForward();
            }
            // small chance to fall, then knock off 0.1 confidence
            if (Math.random() < 0.1 * theHorse.getConfidence() * theHorse.getConfidence()) {
                theHorse.fall();
                double newC = Math.max(0.0, theHorse.getConfidence() - 0.1);
                theHorse.setConfidence(newC);
            }
        }
    }

    /**
     * Has a horse reached or passed the finish line?
     */
    private boolean raceWonBy(Horse theHorse) {
        return theHorse.getDistanceTravelled() >= raceLength;
    }

    /**
     * Check if every horse has fallen.
     */
    private boolean allHorsesFallen() {
        for (Horse horse : lanes) {
            if (horse != null && !horse.hasFallen()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Clear screen using ANSI codes.
     */
    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Draw track, horses, and confidence values.
     */
    private void printRace() {
        clearScreen();
        // top border
        multiplePrint('=', raceLength + 2);
        System.out.println();

        for (Horse horse : lanes) {
            if (horse != null) {
                printLane(horse);
            }
        }

        // bottom border
        multiplePrint('=', raceLength + 2);
        System.out.println();
    }

    /**
     * Draw one lane. Fallen horses show X, others their symbol.
     */
    private void printLane(Horse theHorse) {
        int rawPos = theHorse.getDistanceTravelled();
        int idx    = Math.min(rawPos, raceLength - 1);

        // make a blank track of fixed length
        char[] track = new char[raceLength];
        Arrays.fill(track, ' ');
        // mark position
        track[idx] = theHorse.hasFallen() ? 'X' : theHorse.getSymbol();

        // frame and print
        System.out.print("|");
        System.out.print(track);
        System.out.print("|");
        // show confidence on the side
        System.out.printf(" %.2f%n", theHorse.getConfidence());
    }

    /**
     * Print a char several times in a row.
     */
    private void multiplePrint(char ch, int times) {
        for (int i = 0; i < times; i++) {
            System.out.print(ch);
        }
    }

    /**
     * Reset all horses to the start without changing confidence.
     */
    public void resetRace() {
        for (Horse horse : lanes) {
            if (horse != null) {
                horse.goBackToStart();
            }
        }
    }
}