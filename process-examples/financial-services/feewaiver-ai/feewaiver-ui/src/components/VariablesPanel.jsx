function classifyVariables(variables) {
  const business = {}
  const decision = {}
  const technical = {}

  const businessKeys = new Set([
    'requestText',
    'policyRoute',
    'reviewDecision',
    'reviewStatus',
    'resolutionStatus',
    'entityId',
    'recordId'
  ])

  const decisionPrefixes = [
    'ai',
    'drl',
    'finalGuardrail',
    'tool'
  ]

  const technicalKeys = new Set([
    'processInstanceId',
    'executionId',
    'processDefinitionId',
    'businessKey'
  ])

  Object.entries(variables ?? {}).forEach(([key, value]) => {
    if (businessKeys.has(key)) {
      business[key] = value
      return
    }

    if (decisionPrefixes.some((prefix) => key.startsWith(prefix))) {
      decision[key] = value
      return
    }

    if (
      technicalKeys.has(key) ||
      key.toLowerCase().includes('id') ||
      key.toLowerCase().includes('timestamp') ||
      key.toLowerCase().includes('date')
    ) {
      technical[key] = value
      return
    }

    business[key] = value
  })

  return { business, decision, technical }
}

function VariableSection({ title, values }) {
  const entries = Object.entries(values)

  return (
    <div className="variable-section">
      <h3>{title}</h3>
      {entries.length === 0 ? (
        <div className="muted-text">None</div>
      ) : (
        <div className="variable-grid">
          {entries.map(([key, value]) => (
            <div key={key} className="variable-row">
              <div className="variable-key">{key}</div>
              <div className="variable-value">
                {typeof value === 'object' && value !== null
                  ? JSON.stringify(value, null, 2)
                  : String(value)}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default function VariablesPanel({ variables }) {
  const { business, decision, technical } = classifyVariables(variables)

  return (
    <section className="panel">
      <h2>Variables</h2>
      <VariableSection title="Business / Request Data" values={business} />
      <VariableSection title="Decision / AI Data" values={decision} />
      <VariableSection title="Technical / System Data" values={technical} />
    </section>
  )
}