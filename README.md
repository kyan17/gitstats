# GitStats – GitHub Dashboard

This project was developed as part of **Project 2 – API-Based Systems Integration (Option 1: User Dashboard for GitHub Projects)** at **ISCTE**.

The application connects to the **GitHub API** to provide an interactive dashboard where authenticated users can explore their **public repositories**, visualize project metrics, and analyze contributor activity.

---

## 🧠 SCRUM Framework

Our team follows the **SCRUM methodology** for project management.  
Work is organized into **sprints**, and all tasks are managed through Trello.

**Trello Board:** https://trello.com/b/dCj0BVcA/user-dashboard-for-github-projects

---

## 👥 Team Roles

| Member           | SCRUM Role     | Responsibilities                                                            |
|------------------|----------------|----------------------------------------------------------------------------|
| **Alan Skorka**  | Product Owner  | Defines product vision, prioritizes backlog items, ensures value delivery. |
| **Kyan Jamshid** | Scrum Master   | Facilitates SCRUM processes, resolves blockers, ensures coordination.       |
| **Ilan Piczenik**| Developer      | Implements functionality, maintains code quality, supports testing and documentation. |

---

## 🚀 Project Summary

The application integrates with **GitHub OAuth** and allows the user to:

### 🔐 Authentication
- Log in securely using a GitHub OAuth flow.

### 📂 Repository Management
- Automatically retrieve the list of **public repositories** from the authenticated user.
- Select a repository to explore its metrics and insights.

### 📊 Repository Dashboard (General)
For each selected repository, the user can view:
- Repository metadata (name, description, creation date, visibility)
- Contributors count
- Total commits
- Issues count
- Pull requests count
- Languages used

### 👤 Contributor Metrics (Individual)
The dashboard automatically identifies contributors and displays:
- Number of commits per contributor
- Commit share distribution
- Repository activity breakdown

### 📈 Visualizations
The project includes two main charts:

#### **1. Bar Chart – Commits per Contributor**
Shows the total number of commits made by each contributor.

#### **2. Pie Chart – Commit Share**
Displays the percentage of total commits contributed by each collaborator.

Both charts update dynamically based on the selected repository.

### 💻 User Interface
- Fully responsive
- Clean visual layout
- Simple and intuitive navigation

All core functionality is fully implemented.

---

## 📅 Sprint Progress

### **Sprint 1 – Completed**
**Goal:** Authentication + repository listing

**Delivered:**
- GitHub OAuth authentication  
- Retrieval of the user’s public repositories  
- Repository selection view  

---

### **Sprint 2 – Completed**
**Goal:** Metrics + dashboards + visualizations

**Delivered:**
- Contributor identification  
- Commit metrics per contributor  
- Bar and pie chart visualizations  
- Complete responsive dashboard  
- Final UI and integration improvements  

---

## ✅ Definition of Ready (DoR)

A user story is considered **Ready** when:
- It is clearly described with acceptance criteria  
- It is estimated and prioritized  
- Dependencies are identified  
- The team fully understands the task  

---

## 🏁 Definition of Done (DoD)

A user story is considered **Done** when:
- It meets acceptance criteria  
- It is fully functional on the main branch  
- Documentation is updated  
- Reviewed and validated by the team  

---

## 🧾 Project Management Artifacts

- **Product Backlog**  
- **Sprint Backlog**  
- **Increment**  
- **Sprint Planning / Review / Weeklys**

---

## 📖 Final Sprint Summary

- **Sprint 1:** Authentication and repository listing  
- **Sprint 2:** Contributor metrics, bar/pie visualizations, final dashboard  

The project is now complete and fully functional.

---

**Version:** **v1.0 – Final Delivery**
