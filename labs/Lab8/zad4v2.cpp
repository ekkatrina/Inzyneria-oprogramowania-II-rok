#include <iostream>
#include <type_traits>

template <typename T, typename = typename enable_if<
    is_same<T, int>::value || is_same<T, char>::value || is_same<T, double>::value
>::type>
class A {
    T t;
public:
    A(T value) : t(value) {}
    void pokaz_t() {
       std::cout << t << endl;
    }
};
