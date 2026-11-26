import { NavLink } from "react-router-dom";
export default function Sidebar({ items }) {
  return (
    <aside className="sidebar">
      <div className="side-label">WORKSPACE</div>
      {items.map(([label, to]) => (
        <NavLink key={to} to={to} end={to === "/admin" || to === "/seller"}>
          {label}
        </NavLink>
      ))}
    </aside>
  );
}
