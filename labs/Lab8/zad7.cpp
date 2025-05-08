class A {
public:
    void fA() {}
};

class B {
    A* pa;
public:
    B(A* a) : pa(a) {}
    void fB() {}
};

class Punkt {
    // ...
};

class Kolo {
    double dPromienKola;
    Punkt SrodekKola;
public:
    void WstawSrodek() {
        // ...
    }
};
