import Evaluator.eval;
import io;

class BinarySearch {
    public static void main(String[] args) {
        int[] arr;
        int target;
        int result;
        BinarySearch bs;
        bs = new BinarySearch();

        arr = [1, 2, 3, 4, 5];
        target = 3;
        result = bs.binarySearch(arr, target);
        io.println(result);
    }

    int binarySearch(int[] arr, int target) {
        int left;
        int right;
        int mid;
        int midVal;
        int res;
        left = 0;
        right = arr.length - 1;
        res = 0 - 1;
        while (left < right + 1) {
            mid = left + (right - left) / 2;
            midVal = eval.evaluate(arr[mid]);
            if (!(midVal < target) && !(target < midVal)) { // ==
                left = right + 1;
                res = mid;
            } else {
                if (midVal < target) {  // achieves requirements
                    res = left;
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
        }
        return res;
    }
}
