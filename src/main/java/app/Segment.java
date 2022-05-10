package app;

import misc.Vector2d;

public class Segment {
    Vector2d v1;
    Vector2d v2;

    Segment(Vector2d v1, Vector2d v2) {
        this.v1 = v1;
        this.v2 = v2;
    }
    /**
     * Проверяет вхождение точки в отрезок
     *
     * возвращает true или false
     */
    public boolean check(Vector2d v) {
        if (v == null){
            return true;
        }
        return v.x >= Math.min(v1.x, v2.x) && v.x <= Math.max(v1.x, v2.x) && v.y >= Math.min(v1.y, v2.y) && v.y <= Math.max(v1.y, v2.y);
    }

    public static double length(Vector2d v1, Vector2d v2){
        return Math.sqrt(Math.pow(v1.x - v2.x, 2) + Math.pow(v1.y - v2.y, 2));
    }
}
