package com.example.wassertech.report;

/**
 * Models for storing component and section header boundaries in PDF generation.
 */
public class PdfBoundaryModels {
    
    /**
     * Stores component boundaries (top, bottom, and internal structure boundaries).
     */
    public static class ComponentBoundary {
        public final int topCss;
        public final int bottomCss;
        public final int headerTopCss;
        public final int headerBottomCss;
        public final int fieldsTopCss;
        public final int fieldsBottomCss;
        
        public ComponentBoundary(int topCss, int bottomCss, int headerTopCss, int headerBottomCss, 
                                 int fieldsTopCss, int fieldsBottomCss) {
            this.topCss = topCss;
            this.bottomCss = bottomCss;
            this.headerTopCss = headerTopCss;
            this.headerBottomCss = headerBottomCss;
            this.fieldsTopCss = fieldsTopCss;
            this.fieldsBottomCss = fieldsBottomCss;
        }
        
        public boolean contains(float yCss) {
            return yCss >= topCss && yCss < bottomCss;
        }
        
        public boolean intersects(float startCss, float endCss) {
            return !(endCss <= topCss || startCss >= bottomCss);
        }
        
        /**
         * Check if a point would break inside the component (not at safe boundaries).
         * We want to prevent breaking inside component fields, only allow at component boundaries.
         */
        public boolean wouldBreakInside(float yCss) {
            // If point is outside component, it's safe
            if (yCss < topCss || yCss >= bottomCss) {
                return false;
            }
            
            // NEVER break inside a component - only at exact boundaries
            // Safe to break exactly at component start (with very small tolerance)
            if (Math.abs(yCss - topCss) < 0.5f) {
                return false;
            }
            
            // Safe to break exactly at component end (with very small tolerance)
            if (Math.abs(yCss - bottomCss) < 0.5f) {
                return false;
            }
            
            // For ANY other point inside component, it's bad
            // We want the entire component on one page
            return true;
        }
        
        /**
         * Get the safe break point BEFORE this component (component top).
         */
        public float getSafeBreakBefore() {
            return topCss;
        }
        
        @Override
        public String toString() {
            return String.format("Component[top=%d, bottom=%d, header=%d-%d, fields=%d-%d]", 
                topCss, bottomCss, headerTopCss, headerBottomCss, fieldsTopCss, fieldsBottomCss);
        }
    }
    
    /**
     * Stores section header boundaries (top and bottom positions).
     */
    public static class SectionHeaderBoundary {
        public final int topCss;
        public final int bottomCss;
        
        public SectionHeaderBoundary(int topCss, int bottomCss) {
            this.topCss = topCss;
            this.bottomCss = bottomCss;
        }
        
        public boolean contains(float yCss) {
            return yCss >= topCss && yCss < bottomCss;
        }
        
        /**
         * Get the safe break point BEFORE this section header.
         */
        public float getSafeBreakBefore() {
            return topCss;
        }
        
        @Override
        public String toString() {
            return String.format("SectionHeader[top=%d, bottom=%d]", topCss, bottomCss);
        }
    }
}

