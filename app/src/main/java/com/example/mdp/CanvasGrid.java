package com.example.mdp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CanvasGrid extends View {
    private static final int noOfCols = 20;
    private static final int noOfRows = 20;
    private static final int cellWidth = 35;
    private static final int cellHeight = 35;

    private final Paint gridLinePaint = new Paint();
    private final Paint blackPaint = new Paint();
    private final Paint whitePaint = new Paint();
    private final Paint canvasBackground = new Paint();
    private final Paint textPaint = new Paint();

    public CanvasGrid(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        // Initialize paints with direct RGB values instead of hex strings
        blackPaint.setColor(Color.BLACK);
        whitePaint.setColor(Color.WHITE);

        // Brick/question block color for background (orange-yellow)
        canvasBackground.setColor(Color.rgb(250, 190, 75)); // #FABE4B

        // Darker brick outline color for grid lines
        gridLinePaint.setColor(Color.rgb(200, 110, 0));    // #C86E00
        gridLinePaint.setStrokeWidth(2);

        // Brown color for text/numbers
        textPaint.setColor(Color.rgb(139, 69, 19));        // #8B4513
        textPaint.setTextSize(14);
        textPaint.setFakeBoldText(true);
    }

    @Override
    public void onDraw(Canvas canvas) {
        // Clear canvas first
        canvas.drawColor(Color.rgb(92, 148, 252)); // Sky blue border #5C94FC

        // Draw background cells (brick pattern)
        for (int i = 0; i < noOfCols; i++) {
            for (int j = 0; j < noOfRows; j++) {
                canvas.drawRect(i * cellWidth, j * cellHeight,
                        (i + 1) * cellWidth, (j + 1) * cellHeight,
                        canvasBackground);
            }
        }

        // Vertical lines
        for (int i = 1; i < noOfCols; i++) {
            canvas.drawLine(i * cellWidth, 0, i * cellWidth, noOfRows * cellHeight, gridLinePaint);
        }

        // Horizontal lines
        for (int i = 1; i < noOfRows; i++) {
            canvas.drawLine(0, i * cellHeight, noOfCols * cellWidth, i * cellHeight, gridLinePaint);
        }

        // Vertical grid axis (numbers)
        for (int i = noOfRows - 1; i >= 0; i--) {
            canvas.drawText(String.valueOf(i), 0, cellHeight * (noOfRows - i - 1) + 15, textPaint);
        }

        // Horizontal grid axis (numbers)
        for (int i = 1; i < noOfCols; i++) {
            canvas.drawText(String.valueOf(i), cellWidth * i + 5, cellHeight * 10 + 15, textPaint);
        }

        // Call invalidate to ensure repainting if needed
        invalidate();
    }
}