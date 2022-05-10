package app;

import misc.Vector2d;

import java.util.Vector;

public class Line {
    double a;
    double b;
    double c;

    Line(double x1, double y1, double x2, double y2) {
        a = y2 - y1;
        b = x1 - x2;
        c = y1 * x2 - x1 * y2;
    }

    @Override
    public String toString(){
        return a + "," + b + "," + c;
    }
    Vector2d intersection(Line l) {
        if (b != 0) {
            if (b * l.a != a * l.b) {
                double x = (c * l.b - l.c * b) / (b * l.a - a * l.b);
                double y = -1 * (a * x + c) / b;
                Vector2d v = new Vector2d(x, y);
                return v;
            } else {
                return null;
            }
        } else {
            if (l.b == 0) {
                return null;
            } else {
                double x = -c / a;
                double y = (-l.c - l.a * x) / l.b;
                Vector2d v = new Vector2d(x, y);
                return v;
            }
        }
    }

    public boolean equals(Line l) {
        if (a == l.a && b == l.b && c == l.c) {
            return true;
        } else {
            return false;
        }
    }

}
