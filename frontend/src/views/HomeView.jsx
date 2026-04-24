import { useEffect, useState } from "react";
import { createUser, listUsers } from "../api/user";

export default function HomeView() {
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState({
    username: "",
    email: ""
  });

  async function loadUsers() {
    const resp = await listUsers();
    setUsers(resp.data ?? []);
  }

  async function onSubmit(event) {
    event.preventDefault();
    await createUser(form);
    setForm({
      username: "",
      email: ""
    });
    await loadUsers();
  }

  useEffect(() => {
    void loadUsers();
  }, []);

  return (
    <main className="page">
      <section className="card">
        <h1>Java Web MVC Skeleton</h1>
        <p>Frontend: React. Backend: Spring Boot.</p>

        <form className="form" onSubmit={onSubmit}>
          <input
            value={form.username}
            onChange={(event) =>
              setForm((prev) => ({ ...prev, username: event.target.value }))
            }
            placeholder="username"
            required
          />
          <input
            value={form.email}
            onChange={(event) =>
              setForm((prev) => ({ ...prev, email: event.target.value }))
            }
            type="email"
            placeholder="email"
            required
          />
          <button type="submit">Create User</button>
        </form>

        <button className="refresh" onClick={loadUsers}>Refresh Users</button>

        <ul className="list">
          {users.map((user) => (
            <li key={user.id}>
              #{user.id} {user.username} ({user.email})
            </li>
          ))}
        </ul>
      </section>
    </main>
  );
}
