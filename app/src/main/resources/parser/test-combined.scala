int factorial(int n) {
    if (n <= 1) {
        return 1;
    } else {
        return n * factorial(n - 1);
    }
}

class Calculator {
    double pi = 3.14159;

    double circleArea(double radius) {
        return pi * radius * radius;
    }
}

int main() {
    int num = 5;
    int fact = factorial(num);
    println("Factorial of " + num + " is " + fact);

    double area = circleArea(2.5);
    println("Circle area: " + area);

    return 0;
}