public class Main {
    public static void main(String[] args) throws InterruptedException {
        Elevator e1 = new Elevator();

        while (true) {
            e1.pressButton(new Elevator.Person().setRandom());
            Thread.sleep(15000);
        }
    }
}