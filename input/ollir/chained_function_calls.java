import ImportedClass;

class A {
    A self() {
        return this;
    }

    int foo() {
        return 0;
    }

    ImportedClass returnImport() {
        return new ImportedClass();
    }

    int testing() {
        return this.self().self().self().self().self().self().self().self().self().self().self().self().foo();
    }

    int testing_import() {
        return this.self().self().self().self().self().self().self().self().self().self().self().returnImport().importFunction();
    }

    public static void main(String[] args) {}
}
