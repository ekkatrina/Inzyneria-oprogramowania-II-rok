#include <iostream>
using namespace std;

template <typename T>
class A {
private:
    T t;

public:
    A(T value) : t(value) {}
    void pokaz_t() {
        cout << t << endl;
    }
};

int main() {
    A<int> a1(10);
    A<char> a2('A');
    A<double> a3(3.14);

    a1.pokaz_t();
    a2.pokaz_t();
    a3.pokaz_t();

    return 0;
}

