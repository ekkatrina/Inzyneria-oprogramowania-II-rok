class A{
    public:
     void funkcjaA(){

     }
};

class B : public A{
    public:
    void funkcjaB(){

    }
};

void main(){
    B *b = new B();
    b->funkcjaA();

    delete b;

    A *a = new A();

    delete a;
}
