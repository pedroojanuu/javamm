import io;

class WhileIfComplex {
    int[] myField;
    int myIntField;
    public static void main(String[] args) {
        int i;
        i = 0;
        while (i * 20 - 10 < 10 + 5 * 3) {
            if (i < 5) {
                i = i + 1;
            }
            else {
                i = i + 2;
            }
        }
    }
    int arrayInIfTest(int... a) {
        int res;
        if (a[a[a[a[a[a[a[a.length - 1]]]]]]] < [1, 2, 3, 4, [1, 2, 3][0], 5][0]) {
            res = 1;
        }
        else {
            res = 0;
        }
        return res;
    }
    int arrayInWhileTest(int... a) {
        int res;
        res = 0;
        while (a[a[a[a[a[a[a[a.length - 1]]]]]]] * a[a[a[a[a[0]]]]] < [1, 2, 3, 4, [1, 2, 3][0], 5][0]) {
            res = res + 1;
            a[0] = a[0] + 1;
        }
        return res;
    }

    int testingWhileWithField(int a) {
        int res;
        myField = [1, 2, 3, 4, 5, 6, 7, [1, 2, 3][1]];
        myIntField = a;

        res = 0;
        while (myField[myField[myField[myField.length - 1]]] * myField[myField[myField[0]]] < [1, 2, 3, 4, [1, 2, 3][0], 5][0]) {
            res = res + 1;
            myField[0] = myField[0] + 1;
        }

        while (myIntField * myIntField * myField[0] < myIntField - [1, 2, 3, 4][2]) {
            res = res + 1;
            myIntField = myIntField - myField[0];
        }

        return res;
    }

    int testingIfWithField(int a) {
        int res;
        myField = [1, 2, 3, 4, 5, 6, 7, [1, 2, 3][1]];
        myIntField = a;
        res = 0;
        if (myField[myField[myField[myField.length - 1]]] * myField[myField[myField[0]]] < [1, 2, 3, 4, [1, 2, 3][0], 5][0]) {
            res = 1;
        }
        else {
            res = 0;
        }

        if (myIntField * myIntField * myField[0] < myIntField - [1, 2, 3, 4][2]) {
            res = res + 1;
        }
        else {
            res = res + 2;
        }

        return res;
    }


}