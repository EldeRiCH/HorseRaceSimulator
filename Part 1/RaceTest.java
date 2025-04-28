import java.util.Scanner;

public class RaceTest {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter the length of the track: ");
        int distance = sc.nextInt();

        System.out.print("Enter number of lanes (2-6): ");
        int numberOfLanes = sc.nextInt();
        if (numberOfLanes < 2 || numberOfLanes > 6) {
            System.out.println("Invalid number of lanes. Using default of 4.");
            numberOfLanes = 4;
        }

        String[] horseNames       = {"Thunder", "Lightning", "Storm", "Blaze", "Comet", "Rocket"};
        double[] confidenceValues = {  0.7,       0.8,        0.6,     0.75,    0.85,     0.65 };

        Race race = new Race(distance, numberOfLanes);
        for (int i = 1; i <= numberOfLanes; i++) {
            String name = horseNames[(i - 1) % horseNames.length];
            char symbol = name.toUpperCase().charAt(0);
            double conf = confidenceValues[(i - 1) % confidenceValues.length];

            race.addHorse(new Horse(symbol, name, conf), i);
        }

        char again;
        do {
            race.startRace();
            System.out.print("Play again with the same horses? (y/n): ");
            again = sc.next().charAt(0);
            if (again == 'y' || again == 'Y') {
                race.resetRace();
                System.out.println("\n--- Restarting race! ---\n");
            }
        } while (again == 'y' || again == 'Y');

        sc.close();
    }
}
