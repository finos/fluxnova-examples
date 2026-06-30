function formatTimestamp(value) {
  if (!value) return ''

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return date.toLocaleString()
}

function formatDuration(durationInMillis) {
  if (durationInMillis == null) return ''

  if (durationInMillis < 1000) {
    return `${durationInMillis} ms`
  }

  const totalSeconds = Math.floor(durationInMillis / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  if (hours > 0) {
    return `${hours}h ${minutes}m ${seconds}s`
  }

  if (minutes > 0) {
    return `${minutes}m ${seconds}s`
  }

  return `${seconds}s`
}

function getActivityTypeLabel(activityType) {
  switch (activityType) {
    case 'startEvent':
      return 'Start Event'
    case 'endEvent':
      return 'End Event'
    case 'userTask':
      return 'User Task'
    case 'serviceTask':
      return 'Service Task'
    case 'businessRuleTask':
      return 'Business Rule'
    case 'exclusiveGateway':
      return 'Exclusive Gateway'
    case 'parallelGateway':
      return 'Parallel Gateway'
    case 'callActivity':
      return 'Call Activity'
    default:
      return activityType || 'Activity'
  }
}

export default function TimelinePanel({
  activityHistory,
  selectedActivityId,
  selectedSequence,
  onSelectActivity
}) {
  return (
    <section className="panel">
      <h2>Activity History</h2>

      {(activityHistory ?? []).map((a) => {
        const selected =
          a.activityId === selectedActivityId && a.sequence === selectedSequence

        return (
          <button
            key={`${a.sequence}-${a.activityId}-${a.startTime ?? ''}`}
            type="button"
            className={`timeline-item timeline-button ${selected ? 'timeline-item-selected' : ''}`}
            onClick={() => onSelectActivity(a)}
          >
            <div className="timeline-header">
              <div className="timeline-title">
                <strong>{a.sequence}. {a.activityName || a.activityId}</strong>
              </div>
              <span className="activity-type-badge">
                {getActivityTypeLabel(a.activityType)}
              </span>
            </div>

            <div className="timeline-meta">
              <div><strong>Started:</strong> {formatTimestamp(a.startTime)}</div>
              <div><strong>Ended:</strong> {formatTimestamp(a.endTime)}</div>
              <div><strong>Duration:</strong> {formatDuration(a.durationInMillis)}</div>
            </div>
          </button>
        )
      })}
    </section>
  )
}