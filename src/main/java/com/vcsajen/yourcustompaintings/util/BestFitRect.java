package com.vcsajen.yourcustompaintings.util;

/**
 * Created by VcSaJen on 14.08.2017 22:00.
 */

import com.flowpowered.math.vector.Vector2i;

import javax.annotation.Nullable;

/**
 * Created by VcSaJen on 13.08.2017 15:23.
 */
public class BestFitRect {
    public static class Rectangle {
        private final Vector2i ulCorner;
        private final Vector2i brCorner;

        public Vector2i getUlCorner() { return ulCorner; }
        public Vector2i getBrCorner() { return brCorner; }
        public Vector2i getSize() { return brCorner.sub(ulCorner).add(1,1); }

        public Vector2i getOffset() {
            return ulCorner.sub(getSize().sub(1,1));
        }

        public Rectangle(Vector2i ulCorner, Vector2i brCorner) {
            this.ulCorner = new Vector2i(Math.min(ulCorner.getX(), brCorner.getX()),
                    Math.min(ulCorner.getY(), brCorner.getY()));
            this.brCorner = new Vector2i(Math.max(ulCorner.getX(), brCorner.getX()),
                    Math.max(ulCorner.getY(), brCorner.getY()));
        }

        @Override
        public String toString() {
            return "{" + ulCorner + "; " + brCorner + '}';
        }
    }

    /*private static void printIt(boolean[][] matrix, int x1, int y1, int x2, int y2) {
        StringBuilder sb = new StringBuilder();
        int rows = matrix.length;
        int cols = matrix[0].length;
        for (int j=0;j<cols;j++) {
            for (int i = 0; i < rows; i++) {
                sb.append((i>=x1 && i<=x2 && j>=y1 && j<=y2)?'*':(matrix[i][j]?'-':'#'));
            }
            sb.append("\r\n");
        }
        sb.append("\r\n");
        System.out.println(sb.toString());
    }*/

    private static int getEl(int[][] a, int x, int y) {
        return (x<0||y<0||a.length<1||x>=a.length||y>=a[0].length)?0:a[x][y];
    }

    private static int getEl(boolean[][] a, int x, int y) {
        return (x<0||y<0||a.length<1||x>=a.length||y>=a[0].length)?0:(a[x][y]?1:0);
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @Nullable
    public static Rectangle getBestFitRect(boolean[][] matrix) {
        if (matrix.length<1)
            throw new IllegalArgumentException("Matrix mustn't be empty!");
        Vector2i targetSize = new Vector2i((matrix.length+1)/2, (matrix[0].length+1)/2);
        Vector2i targetPoint = targetSize.sub(1,1);

        int cols = matrix.length;   //xlen
        int rows = matrix[0].length;//ylen

        Vector2i bestUpperLeft =  new Vector2i(-1,-1);
        Vector2i bestLowerRight = new Vector2i(-1,-1);
        int bestDistSqr = Integer.MAX_VALUE-2;

        int[][] summedAreaTable = new int[cols][rows];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                summedAreaTable[x][y] = getEl(matrix,x,y) - getEl(summedAreaTable,x-1,y-1) + getEl(summedAreaTable,x,y-1) + getEl(summedAreaTable,x-1,y);
            }
        }

        for (int y = 0; y < targetSize.getY(); y++) {
            for (int x = 0; x < targetSize.getX(); x++) {
                int x0 = x;
                int y0 = y;
                int x1 = x+targetSize.getX()-1;
                int y1 = y+targetSize.getY()-1;

                int sum = getEl(summedAreaTable,x1,y1) + getEl(summedAreaTable,x0-1,y0-1) - getEl(summedAreaTable,x1,y0-1) - getEl(summedAreaTable,x0-1,y1);
                if (sum==targetSize.getX()*targetSize.getY()) {
                    int curDistSqr = targetPoint.distanceSquared(x0,y0);
                    if (curDistSqr<bestDistSqr) {
                        bestDistSqr = curDistSqr;
                        bestUpperLeft = new Vector2i(x0,y0);
                        bestLowerRight = new Vector2i(x1,y1);
                    }
                }
                if (bestDistSqr==0) break;
            }
            if (bestDistSqr==0) break;
        }
        if (bestDistSqr>=(Integer.MAX_VALUE-2)) return null;
        return new Rectangle(bestUpperLeft, bestLowerRight);
    }
}

