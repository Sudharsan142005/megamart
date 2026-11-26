import { Link, NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
export default function Navbar() {
  const { user, logout } = useAuth();
  const role = user?.roles?.[0]?.replace("ROLE_", "");
  const home =
    role === "ADMIN" ? "/admin" : role === "SELLER" ? "/seller" : "/";
  return (
    <header className="topbar">
      <Link className="brand" to={home}>
        <span className="brand-mark">M</span> Mega<span>Mart</span>
      </Link>
      <nav>
        <NavLink to="/">Shop</NavLink>
        {user && role === "CUSTOMER" && (
          <>
            <NavLink to="/cart">Cart</NavLink>
            <NavLink to="/orders">My orders</NavLink>
          </>
        )}
        {user && role === "ADMIN" && <NavLink to="/admin">Admin</NavLink>}
        {user && role === "SELLER" && <NavLink to="/seller">Seller</NavLink>}
      </nav>
      <div className="nav-actions">
        {user ? (
          <>
            <Link className="muted-link" to="/profile">
              {user.fullName || user.username}
            </Link>
            <button className="button button-quiet" onClick={logout}>
              Sign out
            </button>
          </>
        ) : (
          <>
            <Link to="/login">Sign in</Link>
            <Link className="button button-dark" to="/register">
              Join MegaMart
            </Link>
          </>
        )}
      </div>
    </header>
  );
}
