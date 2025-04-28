public class Horse {
    // Private fields for encapsulation
    private char symbol;
    private String name;
    private int distanceTravelled;
    private boolean fallen;
    private double confidence;

    /**
     * Constructor for Horse objects
     * @param horseSymbol The visual representation of the horse
     * @param horseName The name of the horse
     * @param horseConfidence The confidence rating (0-1)
     */
    public Horse(char horseSymbol, String horseName, double horseConfidence) {
        this.symbol = horseSymbol;
        this.name = horseName;
        this.distanceTravelled = 0;
        this.fallen = false;

        // Ensure confidence is within bounds
        if (horseConfidence < 0) {
            this.confidence = 0;
        } else if (horseConfidence > 1) {
            this.confidence = 1;
        } else {
            this.confidence = horseConfidence;
        }
    }

    // Accessor methods (getters)
    public double getConfidence() {
        return confidence;
    }

    public int getDistanceTravelled() {
        return distanceTravelled;
    }

    public String getName() {
        return name;
    }

    public char getSymbol() {
        return symbol;
    }

    public boolean hasFallen() {
        return fallen;
    }

    // Mutator methods (setters)
    public void setConfidence(double newConfidence) {
        if (newConfidence < 0) {
            this.confidence = 0;
        } else if (newConfidence > 1) {
            this.confidence = 1;
        } else {
            this.confidence = newConfidence;
        }
    }

    public void setSymbol(char newSymbol) {
        this.symbol = newSymbol;
    }


    // Behavior methods
    public void fall() {
        this.fallen = true;
        // Decrease confidence when horse falls
        this.confidence = Math.max(0, confidence - 0.1);
    }

    public void goBackToStart() {
        this.distanceTravelled = 0;
        this.fallen = false;
    }

    public void moveForward() {
        this.distanceTravelled++;
        // Slight confidence boost when moving forward
        this.confidence = Math.min(1, confidence + 0.01);
    }
}

