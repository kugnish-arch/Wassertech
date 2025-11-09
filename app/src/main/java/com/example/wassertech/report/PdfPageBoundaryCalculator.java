package com.example.wassertech.report;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculates page boundaries for PDF generation, respecting component and section header boundaries.
 */
public class PdfPageBoundaryCalculator {
    private static final String TAG = "PdfPageBoundaryCalculator";
    
    /**
     * Calculate page boundaries respecting component and section header boundaries.
     * Ensures that:
     * 1. No component is split across pages (break before component start)
     * 2. Section headers are not separated from their content (break before section header)
     * 
     * @param contentHeightCss Total content height in CSS pixels
     * @param pageHeightCss Height of one page in CSS pixels
     * @param componentBoundaries List of component boundaries (top, bottom in CSS pixels)
     * @param sectionHeaderBoundaries List of section header boundaries (top, bottom in CSS pixels)
     * @return List of page boundaries (start positions in CSS pixels), including start (0) and end (contentHeightCss)
     */
    public static List<Float> calculatePageBoundaries(
            int contentHeightCss,
            float pageHeightCss,
            List<PdfBoundaryModels.ComponentBoundary> componentBoundaries,
            List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries) {
        
        List<Float> boundaries = new ArrayList<>();
        boundaries.add(0f); // Start of first page
        
        float currentPageStart = 0f;
        int iteration = 0;
        final int MAX_ITERATIONS = 1000; // Safety limit
        
        while (currentPageStart < contentHeightCss && iteration < MAX_ITERATIONS) {
            iteration++;
            
            float idealPageEnd = currentPageStart + pageHeightCss;
            
            // If this would be the last page, just add the end
            if (idealPageEnd >= contentHeightCss - 0.1f) { // Small epsilon for floating point
                boundaries.add((float) contentHeightCss);
                break;
            }
            
            // Check if any section header would be split by this page boundary
            // Priority 1: Section headers should not be separated from their content
            PdfBoundaryModels.SectionHeaderBoundary splitSectionHeader = null;
            float closestSectionHeaderTop = Float.MAX_VALUE;
            
            // Check if idealPageEnd falls inside any section header
            // Also check if idealPageEnd is very close to a section header (within 10px) - we should break before it
            for (PdfBoundaryModels.SectionHeaderBoundary header : sectionHeaderBoundaries) {
                // Check if idealPageEnd falls inside the header
                if (idealPageEnd > header.topCss && idealPageEnd < header.bottomCss) {
                    // Page boundary falls inside section header - move before header
                    if (header.topCss >= currentPageStart && header.topCss < closestSectionHeaderTop) {
                        splitSectionHeader = header;
                        closestSectionHeaderTop = header.topCss;
                        Log.d(TAG, "    -> Section header would be split: " + header.toString() + " (idealPageEnd=" + idealPageEnd + " is inside)");
                    }
                } else if (idealPageEnd > header.topCss - 10 && idealPageEnd <= header.topCss + 10) {
                    // Page boundary is very close to header top - break before it to be safe
                    if (header.topCss >= currentPageStart && header.topCss < closestSectionHeaderTop) {
                        splitSectionHeader = header;
                        closestSectionHeaderTop = header.topCss;
                        Log.d(TAG, "    -> Section header is very close to break point: " + header.toString() + " (idealPageEnd=" + idealPageEnd + " is within 10px)");
                    }
                }
            }
            
            // Check if any component would be split by this page boundary
            // Priority 2: Components should not be split
            PdfBoundaryModels.ComponentBoundary splitComponent = null;
            float closestComponentTop = Float.MAX_VALUE;
            
            Log.d(TAG, "=== Checking page boundary at " + idealPageEnd + " CSS px ===");
            Log.d(TAG, "  Current page start: " + currentPageStart + " CSS px");
            Log.d(TAG, "  Ideal page end: " + idealPageEnd + " CSS px");
            
            if (splitSectionHeader != null) {
                Log.d(TAG, "  Section header would be split: " + splitSectionHeader.toString());
            }
            
            Log.d(TAG, "  Checking " + componentBoundaries.size() + " components:");
            for (int idx = 0; idx < componentBoundaries.size(); idx++) {
                PdfBoundaryModels.ComponentBoundary comp = componentBoundaries.get(idx);
                // Check if idealPageEnd falls inside the component
                boolean wouldBreak = comp.wouldBreakInside(idealPageEnd);
                // Also check if idealPageEnd is very close to component top (within 5px) - we should break before it
                boolean isCloseToTop = idealPageEnd > comp.topCss - 5 && idealPageEnd <= comp.topCss + 5;
                
                Log.d(TAG, "  Component[" + idx + "] " + comp.toString() + 
                      " - wouldBreak=" + wouldBreak + 
                      ", isCloseToTop=" + isCloseToTop +
                      " (idealPageEnd=" + idealPageEnd + " is " + 
                      (idealPageEnd < comp.topCss ? "before" : 
                       idealPageEnd > comp.bottomCss ? "after" : "inside") + " component)");
                
                if (wouldBreak || isCloseToTop) {
                    // This component would be split or break is too close to component start
                    // We want to break BEFORE the component starts
                    if (comp.topCss >= currentPageStart && comp.topCss < closestComponentTop) {
                        splitComponent = comp;
                        closestComponentTop = comp.topCss;
                        Log.d(TAG, "    -> SELECTED as splitComponent (closest top: " + comp.topCss + ", wouldBreak=" + wouldBreak + ", isCloseToTop=" + isCloseToTop + ")");
                    }
                }
            }
            
            // Determine the safest break point
            // Priority: Section header > Component
            float safeBreakPoint = idealPageEnd;
            String breakReason = "ideal boundary";
            
            if (splitSectionHeader != null) {
                // Section header would be split - move boundary BEFORE section header
                float sectionBreakPoint = splitSectionHeader.getSafeBreakBefore();
                if (sectionBreakPoint > currentPageStart + 0.5f) {
                    safeBreakPoint = sectionBreakPoint;
                    breakReason = "before section header: " + splitSectionHeader.toString();
                    Log.d(TAG, "");
                    Log.d(TAG, "*** SECTION HEADER SPLIT DETECTED! ***");
                    Log.d(TAG, "  Section header: " + splitSectionHeader.toString());
                    Log.d(TAG, "  Moving boundary from " + idealPageEnd + " to " + safeBreakPoint + " CSS px");
                }
            } else if (splitComponent != null) {
                // Component would be split - move boundary BEFORE component start
                float componentBreakPoint = splitComponent.getSafeBreakBefore();
                if (componentBreakPoint > currentPageStart + 0.5f) {
                    safeBreakPoint = componentBreakPoint;
                    breakReason = "before component: " + splitComponent.toString();
                    Log.d(TAG, "");
                    Log.d(TAG, "*** COMPONENT SPLIT DETECTED! ***");
                    Log.d(TAG, "  Component: " + splitComponent.toString());
                    Log.d(TAG, "  Moving boundary from " + idealPageEnd + " to " + safeBreakPoint + " CSS px");
                } else {
                    Log.w(TAG, "  WARNING: Component too large, cannot avoid split!");
                    Log.w(TAG, "    Component: " + splitComponent.toString());
                    Log.w(TAG, "    Component starts at " + componentBreakPoint + ", page starts at " + currentPageStart);
                    Log.w(TAG, "    Using ideal boundary: " + idealPageEnd + " CSS px");
                    safeBreakPoint = idealPageEnd;
                    breakReason = "component too large, using ideal";
                }
            }
            
            // Apply the safe break point
            if (safeBreakPoint <= currentPageStart + 0.5f) {
                // Can't move back, use ideal boundary
                Log.w(TAG, "  Cannot move boundary back (safeBreakPoint=" + safeBreakPoint + 
                      " <= currentPageStart=" + currentPageStart + "), using ideal boundary");
                boundaries.add(idealPageEnd);
                currentPageStart = idealPageEnd;
            } else {
                boundaries.add(safeBreakPoint);
                currentPageStart = safeBreakPoint;
                Log.d(TAG, "  ✓ Page boundary set to " + safeBreakPoint + " CSS px (" + breakReason + ")");
            }
            Log.d(TAG, "");
        }
        
        if (iteration >= MAX_ITERATIONS) {
            Log.e(TAG, "WARNING: calculatePageBoundaries reached max iterations, content may be too large");
            // Force end boundary
            if (boundaries.get(boundaries.size() - 1) < contentHeightCss) {
                boundaries.add((float) contentHeightCss);
            }
        }
        
        Log.d(TAG, "");
        Log.d(TAG, "=== FINAL PAGE BOUNDARIES ===");
        for (int i = 0; i < boundaries.size(); i++) {
            Log.d(TAG, "  Boundary[" + i + "]: " + boundaries.get(i) + " CSS px");
        }
        Log.d(TAG, "");
        
        return boundaries;
    }
}

