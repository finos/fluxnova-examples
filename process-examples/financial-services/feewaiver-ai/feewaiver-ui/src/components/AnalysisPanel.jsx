export default function AnalysisPanel({ analysis }) {
  return (
    <section className="panel">
      <h2>Analysis</h2>
      <div><strong>AI Summary:</strong> {analysis?.aiCaseSummary ?? ''}</div>
      <div><strong>Intent:</strong> {analysis?.aiIntent ?? ''}</div>
      <div><strong>Risk:</strong> {analysis?.aiRiskLevel ?? ''}</div>
      <div><strong>Confidence:</strong> {analysis?.aiConfidence ?? ''}</div>
      <div><strong>Recommended Action:</strong> {analysis?.aiRecommendedAction ?? ''}</div>
      <div><strong>Reasoning:</strong> {analysis?.aiReasoningSummary ?? ''}</div>
      <div><strong>DRL Escalate:</strong> {String(analysis?.drlEscalate ?? '')}</div>
      <div><strong>DRL Requires Review:</strong> {String(analysis?.drlRequiresReview ?? '')}</div>
      <div><strong>DRL Reason:</strong> {analysis?.drlReason ?? ''}</div>
      <div><strong>Tool Interaction Summary:</strong> {analysis?.toolInteractionSummary ?? ''}</div>
      <div><strong>Final Guardrail Override:</strong> {String(analysis?.finalGuardrailOverride ?? '')}</div>
      <div><strong>Final Guardrail Reason:</strong> {analysis?.finalGuardrailReason ?? ''}</div>
    </section>
  )
}