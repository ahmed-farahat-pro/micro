import java.util.Scanner;

public class main {
    public static void main(String[] args) {

        int cycle=1;
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the number of clock cycles for add/sub operation: ");
        int addSubCycles = scanner.nextInt();

        System.out.print("Enter the number of clock cycles for divide operation: ");
        int divideCycles = scanner.nextInt();

        System.out.print("Enter the number of clock cycles for multiply operation: ");
        int multiplyCycles = scanner.nextInt();

        System.out.print("Enter the number of  operations: ");
        int operationNum = scanner.nextInt();

        System.out.print("Enter the size of  add buffer : ");
        int addBuffer = scanner.nextInt();

        System.out.print("Enter the size of  mul buffer : ");
        int mulBuffer = scanner.nextInt();

        System.out.print("Enter the size  of  load buffer : ");
        int loadBuffer = scanner.nextInt();

        System.out.print("Enter the size  of  store  buffer : ");
        int storeBuffer = scanner.nextInt();

        scanner.nextLine();
        String added = "";
        String operations[] = new String[operationNum];
        int count = 0;
        while (count < operationNum) {
            System.out.println("Enter operation number: " + (count + 1));
            added = scanner.nextLine();
            operations[count] = added;
            count++;
        }
        scanner.close();
        for (int i = 0; i < operations.length; i++) {
            System.out.println(operations[i]);
        }
    }
}
