import ArrayParser;
import copy;
import io;

class BubbleSort {
    public static void main(String[] input) {
        ArrayParser ap;
        int[] arr;
        int[] sorted;
        int i;
        BubbleSort bs;

        ap = new ArrayParser();
        bs = new BubbleSort();

        arr = ap.parse(input);
        sorted = bs.bubbleSort(arr);

        i = 0;
        while (i < arr.length) {
            io.println(arr[i]);
            i = i + 1;
        }
        io.println();

        i = 0;
        while (i < sorted.length) {
            io.println(sorted[i]);
            i = i + 1;
        }

    }

    int[] bubbleSort(int... arr) {
        int[] arrayCopy;
        int i;
        int k;
        int temp;
        int n;

        arrayCopy = copy.copy(arr);
        n = arrayCopy.length;
        i = 0;
        while (i < n) {
            k = 0;
            while (k < n - 1) {
                if (arrayCopy[k + 1] < arrayCopy[k]) {
                    temp = arrayCopy[k];
                    arrayCopy[k] = arrayCopy[k + 1];
                    arrayCopy[k + 1] = temp;
                }
                else {}
                k = k + 1;
            }
            i = i + 1;
        }
        return arrayCopy;
    }
}
