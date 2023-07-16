// import BucketNode;

public class exampleTest {
    public static void main(String[] args) {
        BucketArray newArr = new BucketArray();

        newArr.insert(1);
        newArr.insert(4);
        newArr.insert(5);
        newArr.print_sorted();
        newArr.insert(6);
        newArr.delete(6);
        newArr.print_sorted();
        newArr.insert(2);
        assert(newArr.member(6) == false);
        assert(newArr.member(2) == true);

    }
}
