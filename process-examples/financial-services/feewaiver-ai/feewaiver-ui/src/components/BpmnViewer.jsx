import { useEffect, useRef } from 'react'
import BpmnJS from 'bpmn-js'

export default function BpmnViewer({ xml, diagramState, selectedActivityId }) {
  const containerRef = useRef(null)
  const viewerRef = useRef(null)
  const renderVersionRef = useRef(0)
  const previousSelectedRef = useRef(null)

  useEffect(() => {
    if (!containerRef.current) return

    viewerRef.current = new BpmnJS({
      container: containerRef.current
    })

    return () => {
      try {
        viewerRef.current?.destroy()
      } catch (e) {
        console.error('Failed to destroy BPMN viewer cleanly', e)
      } finally {
        viewerRef.current = null
      }
    }
  }, [])

  useEffect(() => {
    const viewer = viewerRef.current
    if (!viewer || !xml) return

    let cancelled = false
    const renderVersion = ++renderVersionRef.current

    const renderDiagram = async () => {
      try {
        await viewer.importXML(xml)

        if (cancelled || !viewerRef.current || renderVersion !== renderVersionRef.current) {
          return
        }

        const canvas = viewer.get('canvas')
        const overlays = viewer.get('overlays')
        const elementRegistry = viewer.get('elementRegistry')

        overlays.clear()
        canvas.zoom('fit-viewport')
        canvas.scroll({ dx: 80, dy: 10 })

        const currentZoom = canvas.zoom()
        if (typeof currentZoom === 'number') {
            canvas.zoom(currentZoom * 0.98)
        }

        ;(diagramState?.completedActivityIds || []).forEach((activityId) => {
          if (elementRegistry.get(activityId)) {
            canvas.addMarker(activityId, 'completed-activity')
          } else {
            console.warn('Skipping completed marker; BPMN element not found:', activityId)
          }
        })

        ;(diagramState?.activeActivityIds || []).forEach((activityId) => {
          if (elementRegistry.get(activityId)) {
            canvas.addMarker(activityId, 'active-activity')
          } else {
            console.warn('Skipping active marker; BPMN element not found:', activityId)
          }
        })

        ;(diagramState?.activeActivityIds || []).forEach((activityId) => {
          if (elementRegistry.get(activityId)) {
            overlays.add(activityId, {
              position: { top: 2, right: 2 },
              html: '<div class="active-overlay-badge">Active</div>'
            })
          } else {
            console.warn('Skipping active overlay; BPMN element not found:', activityId)
          }
        })

        ;(diagramState?.overlays || []).forEach((overlay) => {
          if (elementRegistry.get(overlay.elementId)) {
            overlays.add(overlay.elementId, {
              position: { bottom: 0, right: 0 },
              html: `<div class="overlay-badge">${escapeHtml(overlay.label)}</div>`
            })
          } else {
            console.warn('Skipping overlay; BPMN element not found:', overlay.elementId)
          }
        })

        if (selectedActivityId && elementRegistry.get(selectedActivityId)) {
          canvas.addMarker(selectedActivityId, 'selected-activity')
          previousSelectedRef.current = selectedActivityId
          
        } else {
          previousSelectedRef.current = null
        }

        addCompletedOverlays(
            overlays,
            elementRegistry,
            diagramState?.completedActivityIds || []
        )
      } catch (e) {
        if (!cancelled) {
          console.error('Failed to render BPMN diagram', e)
        }
      }
    }

    renderDiagram()

    return () => {
      cancelled = true
    }
  }, [xml, diagramState, selectedActivityId])

  useEffect(() => {
    const viewer = viewerRef.current
    if (!viewer || !xml) return

    const canvas = viewer.get('canvas')
    const elementRegistry = viewer.get('elementRegistry')

    if (previousSelectedRef.current && elementRegistry.get(previousSelectedRef.current)) {
      canvas.removeMarker(previousSelectedRef.current, 'selected-activity')
    }

    if (selectedActivityId && elementRegistry.get(selectedActivityId)) {
      canvas.addMarker(selectedActivityId, 'selected-activity')
      previousSelectedRef.current = selectedActivityId
      //zoomToElement(viewer, selectedActivityId)
    } else {
      previousSelectedRef.current = null
    }
  }, [selectedActivityId, xml])

  return <div ref={containerRef} className="diagram-container" />
}

function zoomToElement(viewer, elementId) {
  const elementRegistry = viewer.get('elementRegistry')
  const canvas = viewer.get('canvas')
  const element = elementRegistry.get(elementId)

  if (!element) return

  canvas.scrollToElement(element)
  canvas.zoom(1.0, {
    x: element.x + element.width / 2,
    y: element.y + element.height / 2
  })
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function addCompletedOverlays(overlays, elementRegistry, completedActivityIds) {
  ;(completedActivityIds || []).forEach((activityId) => {
    if (!elementRegistry.get(activityId)) return

    overlays.add(activityId, {
      position: { top: 2, left: 2 },
      html: '<div class="completed-mini-badge">✓</div>'
    })
  })
}