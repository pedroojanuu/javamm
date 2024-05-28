import io;

class WhileHell {
    int function() {
        int value;
        value = 1;
        while (value < 2) {
            while (value < 2) {
                while (value < 3) {
                    if (value < 6) {
                        value = value + 2;
                    }
                    else {
                        value = value + 1;
                    }
                }
            }
        }
        return 0;
    }

    int anotherFunction(int a) {
        int value;
        value = 0;
        while (value < a) value = value + a;
        value = 0;
        while (value < a)
            if (value < 5) {
                io.println(value + 5);
            }
            else {
                io.println(value);
            }

        while (value < a)
            if (value < 5) io.println(value + 5);
            else io.println(value);

        return value;
    }

    int if_hell(int a) {
        int v;
        v = a;
        if (v < 5)
            if (v < 5 + 2)
                if (true) io.println(5);
                else io.println(6);
            else io.println(7);
        else io.println(8);
        return v;
    }
}
