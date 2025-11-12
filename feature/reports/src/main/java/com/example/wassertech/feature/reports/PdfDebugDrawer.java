package ru.wassertech.feature.reports;

import android.graphics.Canvas;
import android.util.Log;

import java.util.List;

/**
 * Draws debug lines on PDF bitmap for visualization of component boundaries and page breaks.
 */
public class PdfDebugDrawer {
    private static final String TAG = "PdfDebugDrawer";
    
    /**
     * Draw debug lines on bitmap:
     * - Component boundaries (green) - offset 2px UP
     * - Section header boundaries (blue) - offset 4px UP
     * - Page break lines (purple) - no offset
     * 
     * @param canvas Canvas to draw on
     * @param bitmapWidth Bitmap width in device pixels
     * @param bitmapHeight Bitmap height in device pixels
     * @param density Device pixel density
     * @param componentBoundaries List of component boundaries
     * @param sectionHeaderBoundaries List of section header boundaries
     * @param pageBoundariesCss List of page boundaries in CSS pixels
     */
    public static void drawDebugLines(Canvas canvas, int bitmapWidth, int bitmapHeight, float density,
                                      List<PdfBoundaryModels.ComponentBoundary> componentBoundaries,
                                      List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries,
                                      List<Float> pageBoundariesCss) {
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setStrokeWidth(4f); // Make lines thicker for better visibility
        paint.setStyle(android.graphics.Paint.Style.STROKE);
        
        // Draw section header boundaries in blue (offset 4px UP to avoid overlap)
        paint.setColor(0xFF0000FF); // Blue
        Log.d(TAG, "Drawing " + sectionHeaderBoundaries.size() + " section header boundaries (blue, offset -4px)");
        for (int i = 0; i < sectionHeaderBoundaries.size(); i++) {
            PdfBoundaryModels.SectionHeaderBoundary header = sectionHeaderBoundaries.get(i);
            // Offset UP by 4 device pixels
            int topDevicePx = (int) (header.topCss * density) - 4;
            int bottomDevicePx = (int) (header.bottomCss * density) - 4;
            // Top boundary
            if (topDevicePx >= 0 && topDevicePx < bitmapHeight) {
                canvas.drawLine(0, topDevicePx, bitmapWidth, topDevicePx, paint);
                Log.d(TAG, "  SectionHeader[" + i + "] top at " + header.topCss + " CSS px (" + topDevicePx + " device px, offset -4px)");
            }
            // Bottom boundary
            if (bottomDevicePx >= 0 && bottomDevicePx < bitmapHeight) {
                canvas.drawLine(0, bottomDevicePx, bitmapWidth, bottomDevicePx, paint);
                Log.d(TAG, "  SectionHeader[" + i + "] bottom at " + header.bottomCss + " CSS px (" + bottomDevicePx + " device px, offset -4px)");
            }
        }
        
        // Draw component boundaries in green (offset 2px UP to avoid overlap)
        paint.setColor(0xFF00FF00); // Green
        Log.d(TAG, "Drawing " + componentBoundaries.size() + " component boundaries (green, offset -2px)");
        for (int i = 0; i < componentBoundaries.size(); i++) {
            PdfBoundaryModels.ComponentBoundary comp = componentBoundaries.get(i);
            // Offset UP by 2 device pixels
            int topDevicePx = (int) (comp.topCss * density) - 2;
            int bottomDevicePx = (int) (comp.bottomCss * density) - 2;
            // Top boundary
            if (topDevicePx >= 0 && topDevicePx < bitmapHeight) {
                canvas.drawLine(0, topDevicePx, bitmapWidth, topDevicePx, paint);
                Log.d(TAG, "  Component[" + i + "] top at " + comp.topCss + " CSS px (" + topDevicePx + " device px, offset -2px)");
            }
            // Bottom boundary
            if (bottomDevicePx >= 0 && bottomDevicePx < bitmapHeight) {
                canvas.drawLine(0, bottomDevicePx, bitmapWidth, bottomDevicePx, paint);
                Log.d(TAG, "  Component[" + i + "] bottom at " + comp.bottomCss + " CSS px (" + bottomDevicePx + " device px, offset -2px)");
            }
        }
        
        // Draw page break lines in purple (no offset)
        paint.setColor(0xFFFF00FF); // Purple/Magenta
        paint.setStrokeWidth(5f); // Make purple lines even thicker to stand out
        Log.d(TAG, "Drawing " + (pageBoundariesCss.size() - 1) + " page break lines (purple, no offset)");
        for (int i = 1; i < pageBoundariesCss.size(); i++) {
            float boundaryCss = pageBoundariesCss.get(i);
            int boundaryDevicePx = (int) (boundaryCss * density);
            if (boundaryDevicePx >= 0 && boundaryDevicePx < bitmapHeight) {
                canvas.drawLine(0, boundaryDevicePx, bitmapWidth, boundaryDevicePx, paint);
                Log.d(TAG, "  Page break[" + i + "] at " + boundaryCss + " CSS px (" + boundaryDevicePx + " device px)");
            }
        }
        
        Log.d(TAG, "Debug lines drawn on bitmap - Blue (section headers, -4px), Green (components, -2px), Purple (page breaks, 0px)");
    }
}


