function formatTimestamp(value) {
  if (!value) return ''

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return date.toLocaleString()
}

export default function MetadataPanel({ data }) {
  return (
    <section className="panel">
      <h2>Metadata</h2>
      <div><strong>Instance ID:</strong> {data.processInstanceId}</div>
      <div><strong>Definition ID:</strong> {data.processDefinitionId}</div>
      <div><strong>Definition Key:</strong> {data.processDefinitionKey}</div>
      <div><strong>Analysis ID:</strong> {data.analysisId ?? ''}</div>
      <div><strong>Start:</strong> {formatTimestamp(data.startTime)}</div>
      <div><strong>End:</strong> {formatTimestamp(data.endTime)}</div>
      <div><strong>Policy Route:</strong> {data.policyRoute ?? ''}</div>
      <div><strong>Resolution Status:</strong> {data.resolutionStatus ?? ''}</div>
      <div><strong>Review Status:</strong> {data.reviewStatus ?? ''}</div>
      <div><strong>Review Decision:</strong> {data.reviewDecision ?? ''}</div>
      <div><strong>Request Text:</strong> {data.requestText ?? ''}</div>
    </section>
  )
}