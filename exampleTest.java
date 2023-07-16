// import BucketNode;

public class exampleTest {
    public static void main(String[] args) {
        BucketArray newArr = new BucketArray();

        newArr.print_sorted();
        System.out.println("step 1");
        newArr.insert(1);

        newArr.print_sorted();
        System.out.println("step 2");
        newArr.insert(4);
        System.out.println("step 3");
        newArr.insert(5);
        
        
        System.out.println("step 4");
        newArr.print_sorted();
        
        System.out.println("step 5");
        newArr.insert(6);

        
        System.out.println("step 6");
        // newArr.delete(6);
        
        System.out.println("step 7");
        newArr.print_sorted();
        
        System.out.println("step 8");
        newArr.insert(2);
        
        System.out.println("step 9");
        newArr.print_sorted();
        System.out.println(newArr.member(6) == false);
        System.out.println(newArr.member(2) == true);
        
        
        System.out.println("step 10");
        newArr.insert(1);
        
        System.out.println("step 11");
        newArr.print_sorted();

        for (int i = 0; i < 20; i++) {
            System.out.printf("TEST STEP: %d\n", 100+i);
            newArr.insert(i+1);
        }

        // newArr.insert(1);

        newArr.print_sorted();


        newArr.delete(20);
        newArr.insert(5);

        newArr.print_sorted();

        newArr.delete(100);

        System.out.println(newArr.member(5));

        System.out.println("delete: 5");
        newArr.delete(5);

        newArr.print_sorted();

        newArr.delete(5);

        newArr.print_sorted();

        newArr.delete(5);
        newArr.delete(6);
        newArr.delete(6);
        
        newArr.print_sorted();
        newArr.cleanup();
        newArr.print_sorted();

        // newArr.insert(6);

        // newArr.print_sorted();
        /*
        newArr.insert(3);
        */
    }
}
