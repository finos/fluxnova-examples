import summit58Logo from '../assets/summit58-logo.png'

export default function Header() {
  return (
    <header className="app-header">
      <div className="brand-block">
        <img
          src={summit58Logo}
          alt="Summit58"
          className="brand-logo"
        />
        <div>
          <h1 className="app-title">Workflow History Viewer</h1>
          <div className="app-subtitle">
            History-driven process instance inspection
          </div>
        </div>
      </div>
    </header>
  )
}