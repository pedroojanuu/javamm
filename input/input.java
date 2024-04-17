class Simple {

    int a;

    public Simple newS() {
        a = this.x();
        return this;
    }

    int x() {
        return 1;
    }
}
