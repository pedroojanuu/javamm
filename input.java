class WhileIfFunctions {
    int otherFunction(int value) {
        return value;
    }
    int anotherFunction(int value) {
        return 20 - value;
    }
    int whileTest() {
        int value;
        value = 0;
        while (this.otherFunction(value) < this.anotherFunction(value)) {
            value = value + 1;
        }
        return value;
    }


    int ifTest() {
        int value;
        value = 5;
        if (this.otherFunction(value) < this.anotherFunction(value)) {
            value = 100;
        }
        else {
            value = 0 - 100;
        }
        return value;
    }
}
