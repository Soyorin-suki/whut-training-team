import { useState } from "react";
import { adminCreateUser, listUsers, login, logout, registerUser } from "../api/user";

export default function HomeView() {
  const [users, setUsers] = useState([]);
  const [registerForm, setRegisterForm] = useState({
    username: "",
    email: "",
    password: ""
  });
  const [loginForm, setLoginForm] = useState({
    username: "",
    password: ""
  });
  const [adminCreateForm, setAdminCreateForm] = useState({
    username: "",
    email: "",
    password: "",
    role: "USER"
  });
  const [auth, setAuth] = useState(null);
  const [errorMessage, setErrorMessage] = useState("");

  async function loadUsers() {
    if (!auth) {
      return;
    }
    const resp = await listUsers(auth);
    if (resp.code !== 200) {
      setErrorMessage(resp.message);
      return;
    }
    setUsers(resp.data ?? []);
  }

  async function onRegister(event) {
    event.preventDefault();
    const resp = await registerUser(registerForm);
    if (resp.code !== 200) {
      setErrorMessage(resp.message);
      return;
    }
    setErrorMessage("");
    setRegisterForm({
      username: "",
      email: "",
      password: ""
    });
    await loadUsers();
  }

  async function onAdminCreate(event) {
    event.preventDefault();
    const resp = await adminCreateUser(adminCreateForm, auth);
    if (resp.code !== 200) {
      setErrorMessage(resp.message);
      return;
    }
    setErrorMessage("");
    setAdminCreateForm({
      username: "",
      email: "",
      password: "",
      role: "USER"
    });
    await loadUsers();
  }

  async function onLogin(event) {
    event.preventDefault();
    const resp = await login(loginForm);
    if (resp.code !== 200) {
      setErrorMessage(resp.message);
      return;
    }

    setErrorMessage("");
    setAuth(resp.data);
    const listResp = await listUsers(resp.data);
    if (listResp.code === 200) {
      setUsers(listResp.data ?? []);
    }
    setLoginForm({
      username: "",
      password: ""
    });
  }

  async function onLogout() {
    if (!auth) {
      return;
    }

    const resp = await logout(auth);
    if (resp.code !== 200) {
      setErrorMessage(resp.message);
      return;
    }
    setErrorMessage("");
    setAuth(null);
    setUsers([]);
  }

  return (
    <main className="page">
      <section className="card">
        <h1>Java Web MVC Skeleton</h1>
        <p>Frontend: React. Backend: Spring Boot.</p>

        {errorMessage && <p>{errorMessage}</p>}

        <h3>Register</h3>
        <form className="form" onSubmit={onRegister}>
          <input
            value={registerForm.username}
            onChange={(event) =>
              setRegisterForm((prev) => ({ ...prev, username: event.target.value }))
            }
            placeholder="username"
            required
          />
          <input
            value={registerForm.email}
            onChange={(event) =>
              setRegisterForm((prev) => ({ ...prev, email: event.target.value }))
            }
            type="email"
            placeholder="email"
            required
          />
          <input
            value={registerForm.password}
            onChange={(event) =>
              setRegisterForm((prev) => ({ ...prev, password: event.target.value }))
            }
            type="password"
            placeholder="password"
            required
          />
          <button type="submit">Register</button>
        </form>

        <h3>Login</h3>
        <form className="form" onSubmit={onLogin}>
          <input
            value={loginForm.username}
            onChange={(event) =>
              setLoginForm((prev) => ({ ...prev, username: event.target.value }))
            }
            placeholder="username"
            required
          />
          <input
            value={loginForm.password}
            onChange={(event) =>
              setLoginForm((prev) => ({ ...prev, password: event.target.value }))
            }
            type="password"
            placeholder="password"
            required
          />
          <button type="submit">Login</button>
        </form>

        <p>
          Login State: {auth ? `logged in as ${auth.username} (${auth.role})` : "not logged in"}
        </p>
        {auth?.accessToken && <p>Access Token: {auth.accessToken}</p>}
        {auth?.refreshToken && <p>Refresh Token: {auth.refreshToken}</p>}
        <button className="refresh" onClick={onLogout} disabled={!auth}>
          Logout
        </button>

        {auth?.role === "ADMIN" && (
          <>
            <h3>Admin Create User</h3>
            <form className="form" onSubmit={onAdminCreate}>
              <input
                value={adminCreateForm.username}
                onChange={(event) =>
                  setAdminCreateForm((prev) => ({ ...prev, username: event.target.value }))
                }
                placeholder="username"
                required
              />
              <input
                value={adminCreateForm.email}
                onChange={(event) =>
                  setAdminCreateForm((prev) => ({ ...prev, email: event.target.value }))
                }
                type="email"
                placeholder="email"
                required
              />
              <input
                value={adminCreateForm.password}
                onChange={(event) =>
                  setAdminCreateForm((prev) => ({ ...prev, password: event.target.value }))
                }
                type="password"
                placeholder="password"
                required
              />
              <select
                value={adminCreateForm.role}
                onChange={(event) =>
                  setAdminCreateForm((prev) => ({ ...prev, role: event.target.value }))
                }
              >
                <option value="USER">USER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
              <button type="submit">Create</button>
            </form>
          </>
        )}

        <button className="refresh" onClick={loadUsers}>Refresh Users</button>

        <ul className="list">
          {users.map((user) => (
            <li key={user.id}>
              #{user.id} {user.username} ({user.email}) [{user.role}]
            </li>
          ))}
        </ul>
      </section>
    </main>
  );
}
