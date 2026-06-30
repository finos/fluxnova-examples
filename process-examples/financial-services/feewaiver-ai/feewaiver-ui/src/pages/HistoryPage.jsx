import { useEffect, useState } from 'react'
import Header from '../components/Header'
import MetadataPanel from '../components/MetadataPanel'
import AnalysisPanel from '../components/AnalysisPanel'
import VariablesPanel from '../components/VariablesPanel'
import TimelinePanel from '../components/TimelinePanel'
import SelectedActivityPanel from '../components/SelectedActivityPanel'
import BpmnViewer from '../components/BpmnViewer'

function getInitialInstanceId() {
  const params = new URLSearchParams(window.location.search)
  return params.get('processInstanceId') || ''
}

export default function HistoryPage() {
  const [processInstanceId, setProcessInstanceId] = useState(getInitialInstanceId())
  const [data, setData] = useState(null)
  const [bpmnXml, setBpmnXml] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const [selectedActivity, setSelectedActivity] = useState(null)

  const loadInstance = async (explicitId) => {
    const trimmedId = (explicitId ?? processInstanceId).trim()
    if (!trimmedId) return

    setLoading(true)
    setError(null)
    setSelectedActivity(null)

    try {
      const historyRes = await fetch(
        `/ui/history/instances/${encodeURIComponent(trimmedId)}`
      )

      if (!historyRes.ok) {
        throw new Error(`History request failed: ${historyRes.status}`)
      }

      const historyJson = await historyRes.json()
      setData(historyJson)

      const processDefinitionId = historyJson.processDefinitionId
      if (!processDefinitionId) {
        throw new Error('No processDefinitionId returned from history endpoint')
      }

      const bpmnRes = await fetch(
        `/ui/history/process-definitions/${encodeURIComponent(processDefinitionId)}/bpmn`
      )

      if (!bpmnRes.ok) {
        throw new Error(`BPMN request failed: ${bpmnRes.status}`)
      }

      const bpmnJson = await bpmnRes.json()
      setBpmnXml(bpmnJson.bpmnXml)

      const params = new URLSearchParams(window.location.search)
      params.set('processInstanceId', trimmedId)
      window.history.replaceState({}, '', `${window.location.pathname}?${params.toString()}`)

      const firstActivity = historyJson.activityHistory?.[0] || null
      setSelectedActivity(firstActivity)
    } catch (e) {
      setError(e.message || 'Failed to load instance')
      setData(null)
      setBpmnXml(null)
      setSelectedActivity(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const initialId = getInitialInstanceId()
    if (initialId) {
      loadInstance(initialId)
    }
  }, [])

  return (
    <div className="page-shell">
      <Header />

      <main className="page-content">
        <section className="toolbar-panel">
          <div className="toolbar-label">Load Process Instance History</div>
          <div className="toolbar">
            <input
              value={processInstanceId}
              onChange={(e) => setProcessInstanceId(e.target.value)}
              placeholder="Enter process instance ID"
              className="instance-input"
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  loadInstance()
                }
              }}
            />
            <button onClick={() => loadInstance()} disabled={loading}>
              {loading ? 'Loading...' : 'Load'}
            </button>
          </div>
        </section>

        {error && <div className="error-banner">{error}</div>}

        {!data && !error && (
          <section className="empty-state">
            Enter a process instance ID to load history, variables, analysis details,
            and BPMN diagram state.
          </section>
        )}

        {data && (
          <div className="layout">
            <div className="left-panel">
                <MetadataPanel data={data} />
                <AnalysisPanel analysis={data.analysis} />
                <SelectedActivityPanel activity={selectedActivity} />
                <VariablesPanel variables={data.variables} />
            </div>

            <div className="right-panel">
                <section className="panel diagram-panel">
                <h2>Process Diagram</h2>
                {bpmnXml ? (
                    <BpmnViewer
                    xml={bpmnXml}
                    diagramState={data.diagramState}
                    selectedActivityId={selectedActivity?.activityId}
                    />
                ) : (
                    <div>No BPMN XML loaded.</div>
                )}
                </section>

                <TimelinePanel
                activityHistory={data.activityHistory}
                selectedActivityId={selectedActivity?.activityId}
                selectedSequence={selectedActivity?.sequence}
                onSelectActivity={setSelectedActivity}
                />
            </div>
          </div>
        )}
      </main>
    </div>
  )
}