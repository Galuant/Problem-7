package app;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.humbleui.jwm.MouseButton;
import io.github.humbleui.skija.*;
import misc.CoordinateSystem2d;
import misc.CoordinateSystem2i;
import misc.Vector2d;
import misc.Vector2i;
import panels.PanelLog;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static app.Colors.*;

/**
 * Класс задачи
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Task {
    /**
     * Текст задачи
     */
    public static final String TASK_TEXT = """
            ПОСТАНОВКА ЗАДАЧИ:
            На плоскости задан треугольник и еще множество точек.
            Необходимо найти такие две точки множества,
            что прямая, проходящая через эти две точки,
            пересекает треугольник, и при этом отрезок этой прямой,
            оказавшейся внутри треугольника,
            оказывается наибольшей длины.""";


    /**
     * коэффициент колёсика мыши
     */
    private static final float WHEEL_SENSITIVE = 0.001f;

    /**
     * Вещественная система координат задачи
     */
    private final CoordinateSystem2d ownCS;
    /**
     * Список точек треугольника
     */
    private ArrayList<Point> triangle;
    /**
     * Список точек
     */
    private final ArrayList<Point> points;
    /**
     * Список точек Second_Set
     */
    private ArrayList<Point> points_out;
    /**
     * Список отрезков, образованных при пересечении треугольника и прямых
     */
    private ArrayList<Vector2d[]> points3;
    /**
     * Размер точки
     */
    private static final int POINT_SIZE = 3;
    /**
     * Последняя СК окна
     */
    private CoordinateSystem2i lastWindowCS;
    /**
     * Флаг, решена ли задача
     */
    private boolean solved;

    private ArrayList<Point> final_segment;

    /**
     * Порядок разделителя сетки, т.е. раз в сколько отсечек
     * будет нарисована увеличенная
     */
    private static final int DELIMITER_ORDER = 10;

    /**
     * Задача
     *
     * @param ownCS  СК задачи
     * @param points массив точек
     */
    @JsonCreator
    public Task(@JsonProperty("ownCS") CoordinateSystem2d ownCS, @JsonProperty("points") ArrayList<Point> points) {
        this.ownCS = ownCS;
        this.points = points;
        this.triangle = new ArrayList<>();
        this.points_out = new ArrayList<>();
        this.final_segment = new ArrayList<>();
        this.points3 = new ArrayList<Vector2d[]>();
    }

    /**
     * Рисование
     *
     * @param canvas   область рисования
     * @param windowCS СК окна
     */
    public void paint(Canvas canvas, CoordinateSystem2i windowCS) {
        // Сохраняем последнюю СК
        lastWindowCS = windowCS;
        // рисуем координатную сетку
        renderGrid(canvas, lastWindowCS);
        // рисуем задачу
        renderTask(canvas, windowCS);
    }

    /**
     * Рисование задачи
     *
     * @param canvas   область рисования
     * @param windowCS СК окна
     */
    private void renderTask(Canvas canvas, CoordinateSystem2i windowCS) {
        canvas.save();
        // создаём перо
        try (var paint = new Paint()) {
            for (Point p : points) {
                if (!solved) {
                    paint.setColor(p.getColor());
                } else {
                    if (final_segment.contains(p))
                        paint.setColor(CROSSED_COLOR);
                    else
                        paint.setColor(CROSSED_COLOR);
                }
                // y-координату разворачиваем, потому что у СК окна ось y направлена вниз,
                // а в классическом представлении - вверх
                Vector2i windowPos = windowCS.getCoords(p.pos.x, p.pos.y, ownCS);
                // рисуем точку
                canvas.drawRect(Rect.makeXYWH(windowPos.x - POINT_SIZE, windowPos.y - POINT_SIZE, POINT_SIZE * 2, POINT_SIZE * 2), paint);

            }
            for (Point p : triangle) {
                if (!solved) {
                    paint.setColor(p.getColor());
                } else {
                    if (final_segment.contains(p))
                        paint.setColor(CROSSED_COLOR);
                    else
                        paint.setColor(SUBTRACTED_COLOR);
                }
                // y-координату разворачиваем, потому что у СК окна ось y направлена вниз,
                // а в классическом представлении - вверх
                Vector2i windowPos = windowCS.getCoords(p.pos.x, p.pos.y, ownCS);
                // рисуем точку
                canvas.drawRect(Rect.makeXYWH(windowPos.x - POINT_SIZE, windowPos.y - POINT_SIZE, POINT_SIZE * 2, POINT_SIZE * 2), paint);
            }
            for (Point p1 : triangle) {
                for (Point p2 : triangle) {
                    if (p1.equals(p2)) {
                        continue;
                    } else {
                        paint.setColor(p1.getColor());
                        paint.setColor(p2.getColor());
                        Vector2i windowPos1 = windowCS.getCoords(p1.pos.x, p1.pos.y, ownCS);
                        Vector2i windowPos2 = windowCS.getCoords(p2.pos.x, p2.pos.y, ownCS);
                        canvas.drawLine(windowPos1.x, windowPos1.y, windowPos2.x, windowPos2.y, paint);
                    }
                }
            }
            for (Point p1 : final_segment) {
                for (Point p2 : final_segment) {
                    if (p1.equals(p2)) {
                        continue;
                    } else {
                        paint.setColor(SUBTRACTED_COLOR);
                        Vector2i windowPos1 = windowCS.getCoords(p1.pos.x, p1.pos.y, ownCS);
                        Vector2i windowPos2 = windowCS.getCoords(p2.pos.x, p2.pos.y, ownCS);
                        canvas.drawLine(windowPos1.x, windowPos1.y, windowPos2.x, windowPos2.y, paint);
                    }
                }
            }
            for (Point p : final_segment) {
                paint.setColor(SUBTRACTED_COLOR);
                Vector2i windowPos = windowCS.getCoords(p.pos.x, p.pos.y, ownCS);
                canvas.drawRect(Rect.makeXYWH(windowPos.x - POINT_SIZE, windowPos.y - POINT_SIZE, POINT_SIZE * 2, POINT_SIZE * 2), paint);
            }
        }
        canvas.restore();
    }

    /**
     * Добавить точку
     *
     * @param pos      положение
     * @param pointSet множество
     */
    public void addPoint(Vector2d pos, Point.PointSet pointSet) {
        solved = false;
        Point newPoint = new Point(pos, pointSet);
        if (pointSet.equals(Point.PointSet.FIRST_SET)) {
            if (triangle.size() < 3) {
                triangle.add(newPoint);
            }
        } else if (pointSet.equals(Point.PointSet.SECOND_SET)) {
            points_out.add(newPoint);
            points.add(newPoint);
        } else {
            points.add(newPoint);
        }
        PanelLog.info("точка " + newPoint + " добавлена в " + newPoint.getSetName());
    }


    /**
     * Клик мыши по пространству задачи
     *
     * @param pos         положение мыши
     * @param mouseButton кнопка мыши
     */
    public void click(Vector2i pos, MouseButton mouseButton) {
        if (lastWindowCS == null) return;
        // получаем положение на экране
        Vector2d taskPos = ownCS.getCoords(pos, lastWindowCS);
        // если левая кнопка мыши, добавляем в первое множество
        if (mouseButton.equals(MouseButton.PRIMARY)) {
            addPoint(taskPos, Point.PointSet.FIRST_SET);
            // если правая, то во второе
        } else if (mouseButton.equals(MouseButton.SECONDARY)) {
            addPoint(taskPos, Point.PointSet.SECOND_SET);
        }
    }


    /**
     * Добавить случайные точки
     *
     * @param cnt кол-во случайных точек
     */
    public void addRandomPoints(int cnt) {
        CoordinateSystem2i addGrid = new CoordinateSystem2i(30, 30);

        for (int i = 0; i < cnt; i++) {
            Vector2i gridPos = addGrid.getRandomCoords();
            Vector2d pos = ownCS.getCoords(gridPos, addGrid);
            // сработает примерно в половине случаев
            if (ThreadLocalRandom.current().nextBoolean())
                addPoint(pos, Point.PointSet.FIRST_SET);
            else
                addPoint(pos, Point.PointSet.SECOND_SET);
        }
    }


    /**
     * Рисование сетки
     *
     * @param canvas   область рисования
     * @param windowCS СК окна
     */
    public void renderGrid(Canvas canvas, CoordinateSystem2i windowCS) {
        // сохраняем область рисования
        canvas.save();
        // получаем ширину штриха(т.е. по факту толщину линии)
        float strokeWidth = 0.03f / (float) ownCS.getSimilarity(windowCS).y + 0.5f;
        // создаём перо соответствующей толщины
        try (var paint = new Paint().setMode(PaintMode.STROKE).setStrokeWidth(strokeWidth).setColor(TASK_GRID_COLOR)) {
            // перебираем все целочисленные отсчёты нашей СК по оси X
            for (int i = (int) (ownCS.getMin().x); i <= (int) (ownCS.getMax().x); i++) {
                // находим положение этих штрихов на экране
                Vector2i windowPos = windowCS.getCoords(i, 0, ownCS);
                // каждый 10 штрих увеличенного размера
                float strokeHeight = i % DELIMITER_ORDER == 0 ? 5 : 2;
                // рисуем вертикальный штрих
                canvas.drawLine(windowPos.x, windowPos.y, windowPos.x, windowPos.y + strokeHeight, paint);
                canvas.drawLine(windowPos.x, windowPos.y, windowPos.x, windowPos.y - strokeHeight, paint);
            }
            // перебираем все целочисленные отсчёты нашей СК по оси Y
            for (int i = (int) (ownCS.getMin().y); i <= (int) (ownCS.getMax().y); i++) {
                // находим положение этих штрихов на экране
                Vector2i windowPos = windowCS.getCoords(0, i, ownCS);
                // каждый 10 штрих увеличенного размера
                float strokeHeight = i % 10 == 0 ? 5 : 2;
                // рисуем горизонтальный штрих
                canvas.drawLine(windowPos.x, windowPos.y, windowPos.x + strokeHeight, windowPos.y, paint);
                canvas.drawLine(windowPos.x, windowPos.y, windowPos.x - strokeHeight, windowPos.y, paint);
            }
        }
        // восстанавливаем область рисования
        canvas.restore();
    }


    /**
     * Очистить задачу
     */
    public void clear() {
        points.clear();
        triangle.clear();
        points_out.clear();
        points3.clear();
        final_segment.clear();
        solved = false;
    }

    /**
     * Решить задачу
     */
    public void solve() {
        // очищаем списки

        final_segment.clear();
        points3.clear();

        Line l1 = new Line(triangle.get(0).pos.x, triangle.get(0).pos.y, triangle.get(1).pos.x, triangle.get(1).pos.y);
        Line l2 = new Line(triangle.get(1).pos.x, triangle.get(1).pos.y, triangle.get(2).pos.x, triangle.get(2).pos.y);
        Line l3 = new Line(triangle.get(2).pos.x, triangle.get(2).pos.y, triangle.get(0).pos.x, triangle.get(0).pos.y);
        Segment s1 = new Segment(triangle.get(0).pos, triangle.get(1).pos);
        Segment s2 = new Segment(triangle.get(1).pos, triangle.get(2).pos);
        Segment s3 = new Segment(triangle.get(2).pos, triangle.get(0).pos);
        boolean b1 = false;
        for (int i = 0; i < points_out.size(); i++) {
            for (int j = i + 1; j < points_out.size(); j++) {
                b1 = false;
                Line l = new Line(points_out.get(i).pos.x, points_out.get(i).pos.y, points_out.get(j).pos.x, points_out.get(j).pos.y);
                if (l1.equals(l)) {
                    Vector2d[] vArr = new Vector2d[2];
                    vArr[0] = triangle.get(0).pos;
                    vArr[1] = triangle.get(1).pos;
                    points3.add(vArr);
                } else if (l2.equals(l)) {
                    Vector2d[] vArr = new Vector2d[2];
                    vArr[0] = triangle.get(1).pos;
                    vArr[1] = triangle.get(2).pos;
                    points3.add(vArr);
                } else if (l3.equals(l)) {
                    Vector2d[] vArr = new Vector2d[2];
                    vArr[0] = triangle.get(2).pos;
                    vArr[1] = triangle.get(0).pos;
                    points3.add(vArr);
                } else {
                    if (s1.check(l.intersection(l1)) || s2.check(l.intersection(l2)) || s3.check(l.intersection(l3))) {
                        Vector2d[] vArr = new Vector2d[2];
                        if (l.intersection(l1) != null && s1.check(l.intersection(l1))) {
                            vArr[0] = l.intersection(l1);
                            b1 = true;
                        }
                        if (l.intersection(l2) != null && s2.check(l.intersection(l2))) {
                            if (b1) {
                                vArr[1] = l.intersection(l2);
                                b1 = false;
                            } else {
                                vArr[0] = l.intersection(l2);
                                b1 = true;
                            }
                        }
                        if (l.intersection(l3) != null && s3.check(l.intersection(l3)) && b1) {
                            vArr[1] = l.intersection(l3);
                        }
                        if (vArr[0] != null && vArr[1] != null) {
                            points3.add(vArr);
                        }
                    }
                }
            }
        }

        double max = 0;
        for (Vector2d[] v : points3) {
            if (Segment.length(v[0], v[1]) > max) {
                max = Segment.length(v[0], v[1]);
            }
        }
        int m = 0;
        for (Vector2d[] v : points3) {
            if (Segment.length(v[0], v[1]) == max) {
                break;
            }
            m += 1;
        }
        Point pp1 = new Point(points3.get(m)[0], Point.PointSet.FIRST_SET);
        Point pp2 = new Point(points3.get(m)[1], Point.PointSet.FIRST_SET);
        final_segment.add(pp1);
        final_segment.add(pp2);

        solved = true;
    }

    /**
     * Получить тип мира
     *
     * @return тип мира
     */
    public CoordinateSystem2d getOwnCS() {
        return ownCS;
    }



    /**
     * Отмена решения задачи
     */
    public void cancel() {
        solved = false;
    }

    /**
     * проверка, решена ли задача
     *
     * @return флаг
     */
    public boolean isSolved() {
        return solved;
    }

    /**
     * Масштабирование области просмотра задачи
     *
     * @param delta  прокрутка колеса
     * @param center центр масштабирования
     */
    public void scale(float delta, Vector2i center) {
        if (lastWindowCS == null) return;
        // получаем координаты центра масштабирования в СК задачи
        Vector2d realCenter = ownCS.getCoords(center, lastWindowCS);
        // выполняем масштабирование
        ownCS.scale(1 + delta * WHEEL_SENSITIVE, realCenter);
    }

    /**
     * Получить положение курсора мыши в СК задачи
     *
     * @param x        координата X курсора
     * @param y        координата Y курсора
     * @param windowCS СК окна
     * @return вещественный вектор положения в СК задачи
     */
    @JsonIgnore
    public Vector2d getRealPos(int x, int y, CoordinateSystem2i windowCS) {
        return ownCS.getCoords(x, y, windowCS);
    }


    /**
     * Рисование курсора мыши
     *
     * @param canvas   область рисования
     * @param windowCS СК окна
     * @param font     шрифт
     * @param pos      положение курсора мыши
     */
    public void paintMouse(Canvas canvas, CoordinateSystem2i windowCS, Font font, Vector2i pos) {
        // создаём перо
        try (var paint = new Paint().setColor(TASK_GRID_COLOR)) {
            // сохраняем область рисования
            canvas.save();
            // рисуем перекрестие
            canvas.drawRect(Rect.makeXYWH(0, pos.y - 1, windowCS.getSize().x, 2), paint);
            canvas.drawRect(Rect.makeXYWH(pos.x - 1, 0, 2, windowCS.getSize().y), paint);
            // смещаемся немного для красивого вывода текста
            canvas.translate(pos.x + 3, pos.y - 5);
            // положение курсора в пространстве задачи
            Vector2d realPos = getRealPos(pos.x, pos.y, lastWindowCS);
            // выводим координаты
            canvas.drawString(realPos.toString(), 0, 0, font, paint);
            // восстанавливаем область рисования
            canvas.restore();
        }
    }
}
