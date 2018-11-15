package com.deitel.cannongame;

import android.graphics.Point;

public class Line {
    public Point getStart() {
        return start;
    }

    public void setStart(Point start) {
        this.start = start;
    }

    public Point start; // starting point
    public Point end; // ending point

    // default constructor initializes points
    public Line() {
        start = new Point(0,0); // start point
        end = new Point(0,0); // end point
    } // end method Line

    public Line(Point start, Point end) {
        setStart(start);
        this.end = end;
    }

}
