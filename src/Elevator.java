import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Elevator {
    private final short MAX_WEIGHT = 300; // kg
    private static final byte FLOORS_NUMBER = 9;
    private final short FLOOR_INTERVAL = 1000;

    public static class Person {
        private final int MAX_PERSON_WEIGHT = 140;
        private final int MIN_PERSON_WEIGHT = 40;
        public int weight;
        public int fromFloor;
        public int toFloor;

        public Person setRandom() {
            Random r = new Random();
            weight = MIN_PERSON_WEIGHT + Math.abs(r.nextInt() % (MAX_PERSON_WEIGHT - MIN_PERSON_WEIGHT + 1)); // 40..140
            fromFloor = toFloor = 1 + Math.abs(r.nextInt() % FLOORS_NUMBER); // 1..9
            while (toFloor == fromFloor) {
                toFloor = 1 + Math.abs(r.nextInt() % FLOORS_NUMBER); // 1..9
            }
            return this;
        }

        @Override
        public String toString() {
            return "w=" + weight + " from=" + fromFloor + " to=" + toFloor;
        }
    }

    private int currentFloor;
    private int currentWeight;

    private boolean[] floorNeedStop;
    ArrayList<Queue<Person>> floorPersonIn;
    ArrayList<Queue<Person>> floorPersonOut;
    private ExecutorService executor;


    public Elevator() {
        executor = Executors.newSingleThreadExecutor();
        floorNeedStop = new boolean[FLOORS_NUMBER];

        floorPersonIn = new ArrayList<>(FLOORS_NUMBER);
        for (int i = 0; i < FLOORS_NUMBER; ++i) {
            floorPersonIn.add(new LinkedList<>());
        }

        floorPersonOut = new ArrayList<>(FLOORS_NUMBER);
        for (int i = 0; i < FLOORS_NUMBER; ++i) {
            floorPersonOut.add(new LinkedList<>());
        }
    }

    public void pressButton(Person person) {
        sendMessage("Person with weight " +
                person.weight +
                " called the elevator from floor " +
                person.fromFloor +
                " to " +
                person.toFloor);
        if (person.fromFloor == person.toFloor) {
            return;
        }

        if (currentFloor == person.fromFloor - 1 && currentWeight + person.weight <= MAX_WEIGHT) {
            currentWeight += person.weight;
            floorPersonOut.get(person.toFloor - 1).add(person);
            floorNeedStop[person.toFloor - 1] = true;
        } else {
            floorPersonIn.get(person.fromFloor - 1).add(person);
            floorNeedStop[person.fromFloor - 1] = true;
        }

        executor.submit(() -> {
            try {
                moveUp();
                moveDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void moveUp() throws InterruptedException {
        for (int i = currentFloor; i < floorNeedStop.length; ++i) {
            if (floorNeedStop[i]) {
                moveTo(i);
                return;
            }
        }
    }

    private void moveDown() throws InterruptedException {
        for (int i = currentFloor; i > 0; --i) {
            if (floorNeedStop[i]) {
                moveTo(i);
                return;
            }
        }
    }

    private void moveTo(final int targetFloor) throws InterruptedException {
        int step = 1;
        if (targetFloor < currentFloor) {
            step = -1;
        }

        while (currentFloor != targetFloor) {
            Thread.sleep(FLOOR_INTERVAL);
            currentFloor += step;
            sendMessage("Current floor: " + (currentFloor + 1));

            if (floorNeedStop[currentFloor]) {
                sendMessage("Doors opened");
                while (!floorPersonOut.get(currentFloor).isEmpty()) {
                    sendMessage("Person out: " + floorPersonOut.get(currentFloor).peek().toString());
                    currentWeight -= floorPersonOut.get(currentFloor).poll().weight;
                }

                Person p = null;
                while (!floorPersonIn.get(currentFloor).isEmpty() && currentWeight + (p = floorPersonIn.get(currentFloor).peek()).weight <= MAX_WEIGHT) {
                    sendMessage("Person in: " + p.toString());

                    currentWeight += p.weight;
                    floorPersonOut.get(p.toFloor - 1).add(p);

                    floorPersonIn.get(currentFloor).poll();
                }

                if (floorPersonIn.get(currentFloor).isEmpty()) {
                    floorNeedStop[currentFloor] = false;
                }

                sendMessage("Doors closed");
            }
        }
    }

    private void sendMessage(String message) {
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME) + " " + message);
    }
}