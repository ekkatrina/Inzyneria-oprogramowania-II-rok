#include <iostream>
using namespace std;

class A {
public:
    void pokaz() {
        cout << "Jestem z klasy A" << endl;
    }
};

class B : virtual public A {
    // dziedziczy po A wirtualnie
};

class C : virtual public A {
    // dziedziczy po A wirtualnie
};

class D : public B, public C {
    // dziedziczy po B i C
};

// Wirtualne dziedziczenie stosujemy po to, aby zapobiec wielokrotnemu dziedziczeniu tej samej klasy bazowej
// (tzw. problem diamentu), co pozwala uniknąć konfliktów i niejednoznaczności przy dostępie do jej elementów.

