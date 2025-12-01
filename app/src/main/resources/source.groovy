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

if (c > 5) {
    c = c + 1;
} else {
    c = c - 1;
}


while(flag1) {
    if (c > 5) {
        c = c + 1;
    } else {
        c = c - 1;
    }

    c = c + 1;

    flag1 = c > 20;
}

// Test unary operators
int negated = -a;
boolean inverted = !flag1;

int test(int i) {
    i = i + 1;

    return i;
}

class Calculator {
    double pi = 3.14159;

    double circleArea(double radius) {
        return pi * radius * radius;
    }
}
