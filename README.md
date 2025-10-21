# Gitstats

## Project 2 – API-Based Systems Integration

## Option 1) User Dashboard for GitHub Projects

Develop an application that connects to the GitHub API
with the goal of generating a personalized dashboard for each user involved in a GitHub project.  
The application should authenticate with the GitHub API and collect relevant data for each repository contributor,
presenting useful metrics and information in a clear and interactive way.

### Functional Requirements:

- Authentication via GitHub OAuth.
- Selection of a specific repository (should allow entering the URL or browsing the authenticated user’s repositories).
- Automatic identification of all project contributors.
- Generation of an individual dashboard for each contributor with:

        Number of commits made
        Lines of code added/removed
        Issues opened/closed
        Pull requests submitted and approved
        Recent activity

- Graphical visualizations (e.g., bar charts, line charts, or pie charts).
- Responsive and intuitive user interface.
