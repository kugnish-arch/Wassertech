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
     * Нижнее поле страницы в CSS пикселях.
     * Нижний край компонента не должен заходить за это поле.
     * То есть, если bottomCss компонента > (currentPageStart + pageHeightCss - BOTTOM_MARGIN_CSS),
     * то компонент должен переноситься на следующую страницу.
     * 
     * Вы можете изменить это значение для настройки нижнего поля.
     */
    private static final float BOTTOM_MARGIN_CSS = 15f; // По умолчанию 15px
    
    /**
     * Calculate page boundaries respecting component and section header boundaries.
     * Ensures that:
     * 1. No component is split across pages (break before component start)
     * 2. Section headers are not separated from their content (break before section header)
     * 3. If last page contains only signature, move last components from previous page to last page
     * 4. Components respect bottom margin (BOTTOM_MARGIN_CSS) - bottom edge should not enter the margin zone
     * 
     * @param contentHeightCss Total content height in CSS pixels
     * @param pageHeightCss Height of one page in CSS pixels
     * @param componentBoundaries List of component boundaries (top, bottom in CSS pixels)
     * @param sectionHeaderBoundaries List of section header boundaries (top, bottom in CSS pixels)
     * @param signatureBoundary Signature block boundary (top, bottom in CSS pixels), can be null
     * @return List of page boundaries (start positions in CSS pixels), including start (0) and end (contentHeightCss)
     */
    public static List<Float> calculatePageBoundaries(
            int contentHeightCss,
            float pageHeightCss,
            List<PdfBoundaryModels.ComponentBoundary> componentBoundaries,
            List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries,
            PdfBoundaryModels.SignatureBoundary signatureBoundary) {
        
        List<Float> boundaries = new ArrayList<>();
        boundaries.add(0f); // Start of first page
        
        float currentPageStart = 0f;
        int iteration = 0;
        final int MAX_ITERATIONS = 1000; // Safety limit
        
        // Эффективная высота страницы с учетом нижнего поля
        float effectivePageHeight = pageHeightCss - BOTTOM_MARGIN_CSS;
        
        while (currentPageStart < contentHeightCss && iteration < MAX_ITERATIONS) {
            iteration++;
            
            // Идеальный конец страницы с учетом нижнего поля
            float idealPageEnd = currentPageStart + effectivePageHeight;
            
            // If this would be the last page, just add the end
            if (idealPageEnd >= contentHeightCss - 0.1f) { // Small epsilon for floating point
                boundaries.add((float) contentHeightCss);
                break;
            }
            
            // Check if any section header would be split by this page boundary
            // Priority 1: Section headers should not be separated from their content
            // NEW LOGIC: If the first component of a section doesn't fit on current page,
            // the break should be BEFORE the section header (header moves with components)
            PdfBoundaryModels.SectionHeaderBoundary splitSectionHeader = null;
            float closestSectionHeaderTop = Float.MAX_VALUE;
            
            // Find the first component after each section header
            // If that component doesn't fit on current page, break before the header
            for (PdfBoundaryModels.SectionHeaderBoundary header : sectionHeaderBoundaries) {
                // Find the first component that comes after this header
                PdfBoundaryModels.ComponentBoundary firstComponentAfterHeader = null;
                for (PdfBoundaryModels.ComponentBoundary comp : componentBoundaries) {
                    if (comp.topCss > header.bottomCss) {
                        // This component comes after the header
                        if (firstComponentAfterHeader == null || comp.topCss < firstComponentAfterHeader.topCss) {
                            firstComponentAfterHeader = comp;
                        }
                    }
                }
                
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
                } else if (firstComponentAfterHeader != null) {
                    // Check if the first component after this header doesn't fit on current page
                    // If the component would be split or is too close to the page boundary,
                    // we should break BEFORE the header so header and components stay together
                    boolean componentWouldBreak = firstComponentAfterHeader.wouldBreakInside(idealPageEnd);
                    boolean componentTooClose = idealPageEnd > firstComponentAfterHeader.topCss - 50 && idealPageEnd < firstComponentAfterHeader.topCss + 50;
                    
                    // Also check if there's a large gap between header and first component
                    // If the gap would be split, break before header
                    boolean gapWouldBeSplit = idealPageEnd > header.bottomCss && idealPageEnd < firstComponentAfterHeader.topCss;
                    
                    // If first component doesn't fit, break before header
                    if ((componentWouldBreak || componentTooClose || gapWouldBeSplit) && 
                        header.topCss >= currentPageStart && header.topCss < closestSectionHeaderTop) {
                        splitSectionHeader = header;
                        closestSectionHeaderTop = header.topCss;
                        Log.d(TAG, "    -> Section header '" + header.toString() + "' first component doesn't fit: " + 
                              firstComponentAfterHeader.toString() + 
                              " (componentWouldBreak=" + componentWouldBreak + 
                              ", componentTooClose=" + componentTooClose + 
                              ", gapWouldBeSplit=" + gapWouldBeSplit + ")");
                        Log.d(TAG, "      -> Breaking BEFORE header to keep header with components");
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
            
            Log.d(TAG, "  Checking " + componentBoundaries.size() + " components (effective page height: " + effectivePageHeight + ", bottom margin: " + BOTTOM_MARGIN_CSS + "):");
            for (int idx = 0; idx < componentBoundaries.size(); idx++) {
                PdfBoundaryModels.ComponentBoundary comp = componentBoundaries.get(idx);
                // Check if idealPageEnd falls inside the component
                boolean wouldBreak = comp.wouldBreakInside(idealPageEnd);
                // Also check if idealPageEnd is very close to component top (within 5px) - we should break before it
                boolean isCloseToTop = idealPageEnd > comp.topCss - 5 && idealPageEnd <= comp.topCss + 5;
                // ВАЖНО: Проверяем, не заходит ли нижний край компонента в зону нижнего поля
                // Если bottomCss компонента > idealPageEnd, то компонент должен переноситься
                boolean bottomEntersMargin = comp.bottomCss > idealPageEnd;
                
                Log.d(TAG, "  Component[" + idx + "] " + comp.toString() + 
                      " - wouldBreak=" + wouldBreak + 
                      ", isCloseToTop=" + isCloseToTop +
                      ", bottomEntersMargin=" + bottomEntersMargin +
                      " (idealPageEnd=" + idealPageEnd + ", bottomCss=" + comp.bottomCss + " is " + 
                      (idealPageEnd < comp.topCss ? "before" : 
                       idealPageEnd > comp.bottomCss ? "after" : "inside") + " component)");
                
                if (wouldBreak || isCloseToTop || bottomEntersMargin) {
                    // This component would be split, break is too close to component start, or bottom enters margin zone
                    // We want to break BEFORE the component starts
                    if (comp.topCss >= currentPageStart && comp.topCss < closestComponentTop) {
                        splitComponent = comp;
                        closestComponentTop = comp.topCss;
                        Log.d(TAG, "    -> SELECTED as splitComponent (closest top: " + comp.topCss + 
                              ", wouldBreak=" + wouldBreak + ", isCloseToTop=" + isCloseToTop + 
                              ", bottomEntersMargin=" + bottomEntersMargin + ")");
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
        Log.d(TAG, "=== FINAL PAGE BOUNDARIES (before signature check) ===");
        for (int i = 0; i < boundaries.size(); i++) {
            Log.d(TAG, "  Boundary[" + i + "]: " + boundaries.get(i) + " CSS px");
        }
        Log.d(TAG, "");
        
        // Проверяем последнюю страницу: если там только подпись, переносим последние компоненты с предыдущей страницы
        if (boundaries.size() >= 2 && signatureBoundary != null) {
            float lastPageStart = boundaries.get(boundaries.size() - 2);
            float lastPageEnd = boundaries.get(boundaries.size() - 1);
            
            // Проверяем, есть ли компоненты на последней странице
            boolean hasComponentsOnLastPage = false;
            for (PdfBoundaryModels.ComponentBoundary comp : componentBoundaries) {
                // Компонент считается на последней странице, если он пересекается с ней
                if (comp.intersects(lastPageStart, lastPageEnd)) {
                    hasComponentsOnLastPage = true;
                    break;
                }
            }
            
            // Если на последней странице нет компонентов, но есть подпись
            if (!hasComponentsOnLastPage && signatureBoundary.intersects(lastPageStart, lastPageEnd)) {
                Log.d(TAG, "=== LAST PAGE HAS ONLY SIGNATURE, CHECKING FOR COMPONENT MIGRATION ===");
                Log.d(TAG, "Last page range: [" + lastPageStart + " - " + lastPageEnd + "] CSS px");
                Log.d(TAG, "Signature range: [" + signatureBoundary.topCss + " - " + signatureBoundary.bottomCss + "] CSS px");
                
                // Находим последние компоненты на предыдущей странице
                if (boundaries.size() >= 3) {
                    float prevPageStart = boundaries.get(boundaries.size() - 3);
                    float prevPageEnd = boundaries.get(boundaries.size() - 2);
                    
                    // Находим компоненты на предыдущей странице, которые можно перенести
                    // Ищем компоненты с конца предыдущей страницы, которые поместятся на последней странице
                    List<PdfBoundaryModels.ComponentBoundary> componentsToMove = new ArrayList<>();
                    
                    // Сортируем компоненты по их позиции (снизу вверх на предыдущей странице)
                    List<PdfBoundaryModels.ComponentBoundary> componentsOnPrevPage = new ArrayList<>();
                    for (PdfBoundaryModels.ComponentBoundary comp : componentBoundaries) {
                        if (comp.intersects(prevPageStart, prevPageEnd)) {
                            componentsOnPrevPage.add(comp);
                        }
                    }
                    // Сортируем по bottomCss (снизу вверх)
                    componentsOnPrevPage.sort((a, b) -> Float.compare(b.bottomCss, a.bottomCss));
                    
                    // Вычисляем доступное пространство на последней странице
                    float spaceOnLastPage = lastPageEnd - lastPageStart;
                    float spaceForSignature = signatureBoundary.bottomCss - signatureBoundary.topCss;
                    float spaceAvailable = spaceOnLastPage - spaceForSignature;
                    
                    // Пробуем добавить компоненты с конца предыдущей страницы, пока есть место
                    float totalSpaceUsed = 0f;
                    for (PdfBoundaryModels.ComponentBoundary comp : componentsOnPrevPage) {
                        float spaceNeeded = comp.bottomCss - comp.topCss;
                        if (totalSpaceUsed + spaceNeeded <= spaceAvailable) {
                            componentsToMove.add(comp);
                            totalSpaceUsed += spaceNeeded;
                            Log.d(TAG, "  Component to move: " + comp.toString() + 
                                  " (needs " + spaceNeeded + " px, total used " + totalSpaceUsed + " / " + spaceAvailable + " px)");
                        } else {
                            break; // Больше не помещается
                        }
                    }
                    
                    // Если есть компоненты для переноса, перемещаем границу последней страницы вверх
                    if (!componentsToMove.isEmpty()) {
                        // Находим самый ранний (верхний) компонент для переноса
                        float earliestComponentTop = Float.MAX_VALUE;
                        for (PdfBoundaryModels.ComponentBoundary comp : componentsToMove) {
                            if (comp.topCss < earliestComponentTop) {
                                earliestComponentTop = comp.topCss;
                            }
                        }
                        
                        // Переносим границу последней страницы вверх, чтобы включить компоненты
                        // Новая граница должна быть перед первым компонентом для переноса
                        float newLastPageStart = earliestComponentTop;
                        
                        // Проверяем, что новая граница не нарушает другие правила
                        if (newLastPageStart > prevPageStart && newLastPageStart < prevPageEnd) {
                            // Обновляем границы: последняя страница начинается раньше
                            boundaries.set(boundaries.size() - 2, newLastPageStart);
                            Log.d(TAG, "  ✓ Moved last page boundary from " + lastPageStart + 
                                  " to " + newLastPageStart + " CSS px");
                            Log.d(TAG, "  Components moved to last page: " + componentsToMove.size() + 
                                  " (total space: " + totalSpaceUsed + " px)");
                        } else {
                            Log.w(TAG, "  ✗ Cannot move boundary: newStart=" + newLastPageStart + 
                                  " not in range [" + prevPageStart + ", " + prevPageEnd + "]");
                        }
                    } else {
                        Log.d(TAG, "  No components to move (available space: " + spaceAvailable + " px)");
                    }
                }
            }
        }
        
        Log.d(TAG, "");
        Log.d(TAG, "=== FINAL PAGE BOUNDARIES (after signature check) ===");
        for (int i = 0; i < boundaries.size(); i++) {
            Log.d(TAG, "  Boundary[" + i + "]: " + boundaries.get(i) + " CSS px");
        }
        Log.d(TAG, "");
        
        return boundaries;
    }
}

