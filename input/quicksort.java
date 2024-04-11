import ArrayParser;
import copy;
import io;

class QuickSort {
    public static void main(String[] input) {
        ArrayParser ap;
        int[] arr;
        int[] sorted;
        int i;
        QuickSort qs;

        ap = new ArrayParser();
        bs = new BubbleSort();

        arr = ap.parse(input);
        sorted = qs.quickSort(arr);

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

    int[] quickSort(int... arr) {
        int[] arrayCopy;
        int n;

        arrayCopy = copy.copy(arr);
        n = arrayCopy.length;
        this.quickSortRecursive(arrayCopy, 0, n - 1);
        return arrayCopy;
    }

    int choosePivotIndex(int low, int high) {
        return (low + high) / 2;
    }

    int quickSortRecursive(int[] arr, int low, int high) {
        int pivot;
        int pivot_index;
        int i;
        int j;
        int temp;

        if (low < high) {
            pivot_index = this.choosePivotIndex(low, high);
            pivot = arr[pivot_index];
            i = low - 1;
            j = low;
            while (j < high) {  // did not read this, ai generated
                if (arr[j] < pivot) {
                    i = i + 1;
                    temp = arr[i];
                    arr[i] = arr[j];
                    arr[j] = temp;
                }
                else {}
                j = j + 1;
            }
            temp = arr[i + 1];
            arr[i + 1] = arr[high];
            arr[high] = temp;

            this.quickSortRecursive(arr, low, i);
            this.quickSortRecursive(arr, i + 2, high);
        }
        else {}
        return 0;
    }
}
