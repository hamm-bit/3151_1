
// import java.util.ArrayList;
// import java.util.List;
// This is later for when we have nodes of array of items (buckets)

public class AVLNode<T> {
    private T item;
    private AVLNode<T>[] children;
    private int height;
    private int count;

    public AVLNode(T item, int height) {
        this.item = item;
        this.height = 0;
        this.count = 1;
    }

    public void setItem(T item) {
        this.item = item;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setChild(AVLNode<T> child, int idx) {
        children[idx] = child;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void addCount() {
        count++;
    }

    public boolean reduceCount() {
        count--;
        if (count == 0) return true;
        return false;
    }

    public T getItem() {
        return item;
    }

    public int getHeight() {
        return height;
    }

    public AVLNode<T> getChild(int idx) {
        return children[idx];
    }

    public int getCount() {
        return count;
    }
}
