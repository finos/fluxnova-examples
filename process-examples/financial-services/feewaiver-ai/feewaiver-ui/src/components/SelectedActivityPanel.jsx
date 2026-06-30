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

export default function SelectedActivityPanel({ activity }) {
  if (!activity) {
    return (
      <section className="panel">
        <h2>Selected Activity</h2>
        <div className="muted-text">Select an activity from the timeline.</div>
      </section>
    )
  }

  return (
    <section className="panel">
      <h2>Selected Activity</h2>

      <div><strong>Name:</strong> {activity.activityName || ''}</div>
      <div><strong>Activity ID:</strong> {activity.activityId || ''}</div>
      <div><strong>Type:</strong> {getActivityTypeLabel(activity.activityType)}</div>
      <div><strong>Sequence:</strong> {activity.sequence ?? ''}</div>
      <div><strong>Start:</strong> {formatTimestamp(activity.startTime)}</div>
      <div><strong>End:</strong> {formatTimestamp(activity.endTime)}</div>
      <div><strong>Duration:</strong> {formatDuration(activity.durationInMillis)}</div>
    </section>
  )
}