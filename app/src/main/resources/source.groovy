// Test complex expressions and control flow

int a = 10;
int b = 5;
int c = 3;

// Test complex arithmetic
int result = a * b + c - 2;

// Test comparison and logical operators
boolean flag1 = a > b;
boolean flag2 = b <= c;
boolean combined = flag1 && flag2;

c = c + b + a;

// Test unary operators
int negated = -a;
boolean inverted = !flag1;

int test(int i) {
    i = i + 1;
    if (i > 10) return i; else return test(i);
}

class Calculator {
    double pi = 3.14159;

    double circleArea(double radius) {
        circleArea(1);
        return pi * radius * radius;
    }
}
