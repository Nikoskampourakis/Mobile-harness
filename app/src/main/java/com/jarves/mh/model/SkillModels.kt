package com.jarves.mh.model

import java.util.UUID

/**
 * Categories for developer skills usable by coding agents.
 */
enum class SkillCategory(val displayName: String, val iconKey: String) {
    DEVELOPMENT("Development", "code"),
    SYSTEM("System & Tools", "terminal"),
    AUTOMATION("Automation", "smart_toy"),
    DATA_ANALYSIS("Data & SQL", "database"),
    RESEARCH("Research & Web", "travel_explore"),
    CUSTOM("Custom Skills", "extension"),
}

/**
 * Definition of a single tool or action exposed by a skill.
 */
data class SkillToolDef(
    val name: String,
    val description: String,
    val parameterExample: String = "",
)

/**
 * Represents an extensible developer skill that can be enabled, configured, and
 * injected into coding agents (Claude Code, DeepSeek Harness, Codex CLI, Antigravity).
 */
data class AgentSkill(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: SkillCategory,
    val description: String,
    val iconKey: String = "extension",
    val version: String = "1.0.0",
    val author: String = "Mobile Harness",
    val isEnabled: Boolean = true,
    val systemPromptInstructions: String,
    val requiredEnvVars: List<String> = emptyList(),
    val envConfig: Map<String, String> = emptyMap(),
    val tools: List<SkillToolDef> = emptyList(),
    val isCustom: Boolean = false,
) {
    companion object {
        fun defaultSkills(): List<AgentSkill> = listOf(
            AgentSkill(
                id = "skill_web_search",
                name = "Web Search & Doc Scraper",
                category = SkillCategory.RESEARCH,
                description = "Live web queries, technical documentation retrieval, and online library search.",
                iconKey = "travel_explore",
                version = "1.2.0",
                author = "Mobile Harness Core",
                isEnabled = true,
                systemPromptInstructions = """
                    You have access to live web search and documentation scraping.
                    When researching libraries, APIs, or fixing bugs:
                    - Formulate precise, focused search terms.
                    - Extract authoritative facts, API signatures, and version requirements.
                    - Always cite documentation URLs and avoid outdated deprecated APIs.
                """.trimIndent(),
                requiredEnvVars = listOf("SERPER_API_KEY"),
                tools = listOf(
                    SkillToolDef("web_search", "Search the web for up-to-date documentation and code solutions", "query: \"Compose navigation 3 migration\""),
                    SkillToolDef("web_scrape", "Fetch and parse webpage content as clean markdown", "url: \"https://developer.android.com/...\""),
                ),
            ),
            AgentSkill(
                id = "skill_git_workflow",
                name = "Git & GitHub Automation",
                category = SkillCategory.DEVELOPMENT,
                description = "Branching, PR creation, git diff analysis, issue management via gh CLI.",
                iconKey = "code",
                version = "1.4.0",
                author = "Mobile Harness Core",
                isEnabled = true,
                systemPromptInstructions = """
                    You have full access to Git and GitHub CLI (`gh`).
                    - Use `git status -s` and `git diff` before and after modifications.
                    - Write concise, conventional commit messages (e.g., feat: ..., fix: ...).
                    - Never force push without confirmation.
                """.trimIndent(),
                requiredEnvVars = listOf("GITHUB_TOKEN"),
                tools = listOf(
                    SkillToolDef("git_status", "Check working tree status and pending modified files"),
                    SkillToolDef("git_diff", "Inspect patch hunks for modified files"),
                    SkillToolDef("gh_pr_create", "Create or open a pull request on GitHub", "title: \"...\", body: \"...\""),
                ),
            ),
            AgentSkill(
                id = "skill_proot_sandbox",
                name = "Linux PRoot & Terminal Sandbox",
                category = SkillCategory.SYSTEM,
                description = "Isolated Linux execution environment with bash, apt, python3, and nodejs.",
                iconKey = "terminal",
                version = "2.0.0",
                author = "Mobile Harness Core",
                isEnabled = true,
                systemPromptInstructions = """
                    You are operating in an Ubuntu ARM64 PRoot environment.
                    - Run commands using non-interactive flags (e.g. `apt-get -y`, `npm -y`).
                    - Check exit codes and handle transient errors gracefully.
                    - Keep background processes monitored and kill defunct processes.
                """.trimIndent(),
                tools = listOf(
                    SkillToolDef("run_shell", "Execute a bash command inside the PRoot Linux guest", "command: \"ls -la\", cwd: \"/workspace\""),
                    SkillToolDef("check_process", "Inspect running background processes and memory", "pid: 1248"),
                ),
            ),
            AgentSkill(
                id = "skill_android_sdk",
                name = "Android SDK & Logcat Diagnostics",
                category = SkillCategory.DEVELOPMENT,
                description = "Gradle compilation, manifest verification, Compose layouts, and runtime Logcat filtering.",
                iconKey = "phone_android",
                version = "1.3.0",
                author = "Mobile Harness Core",
                isEnabled = true,
                systemPromptInstructions = """
                    You have deep Android SDK and Jetpack Compose engineering capabilities.
                    - Enforce Material 3 standards, 48dp touch targets, and proper WindowInsets.
                    - Parse Logcat crash stacks, ANRs, and runtime exceptions.
                    - Maintain Gradle Version Catalogs (libs.versions.toml) with dot-notation.
                """.trimIndent(),
                tools = listOf(
                    SkillToolDef("gradle_compile", "Compile and verify Android build without running clean"),
                    SkillToolDef("logcat_filter", "Inspect Android logcat buffer for tags or errors", "tag: \"AndroidRuntime\""),
                ),
            ),
            AgentSkill(
                id = "skill_database_sql",
                name = "Database & SQL Studio",
                category = SkillCategory.DATA_ANALYSIS,
                description = "SQLite database inspector, Room entity migration generator, and SQL optimization.",
                iconKey = "database",
                version = "1.1.0",
                author = "Mobile Harness Core",
                isEnabled = true,
                systemPromptInstructions = """
                    You are an expert in local databases, SQLite, Room, and Firestore schema design.
                    - Design clean normalized schemas with proper indexing.
                    - Provide automated schema migration scripts for version upgrades.
                    - Write transaction-safe queries with parameterized inputs.
                """.trimIndent(),
                tools = listOf(
                    SkillToolDef("sql_exec", "Run an exploratory SQL query on local SQLite database", "query: \"SELECT count(*) FROM users\""),
                    SkillToolDef("schema_inspect", "Inspect table definitions and index structures", "table: \"projects\""),
                ),
            ),
            AgentSkill(
                id = "skill_code_quality",
                name = "Code Quality & Linter Engine",
                category = SkillCategory.AUTOMATION,
                description = "Static code analysis, memory leak detection, accessibility auditing, and refactoring.",
                iconKey = "verified",
                version = "1.5.0",
                author = "Mobile Harness Core",
                isEnabled = true,
                systemPromptInstructions = """
                    You enforce world-class software engineering quality:
                    - Avoid unnecessary recompositions and expensive memory allocations in loops.
                    - Verify all interactive UI components have unique testTag identifiers.
                    - Keep source files modular and under 500 lines wherever practical.
                """.trimIndent(),
                tools = listOf(
                    SkillToolDef("lint_check", "Run code linting and verify syntax safety"),
                    SkillToolDef("verify_accessibility", "Audit Compose UI components for 48dp touch targets and contentDescription"),
                ),
            ),
            AgentSkill(
                id = "skill_reasoning_optimizer",
                name = "Deep Reasoning & Task Planner",
                category = SkillCategory.AUTOMATION,
                description = "Multi-step plan decomposition, milestone tracking, and reflection before execution.",
                iconKey = "psychology",
                version = "1.0.0",
                author = "Mobile Harness Core",
                isEnabled = true,
                systemPromptInstructions = """
                    You structure complex tasks into clear, executable milestones:
                    - Break down requests into concise phases.
                    - Verify assumptions before modifying existing code.
                    - Provide a scannable summary upon task completion.
                """.trimIndent(),
                tools = listOf(
                    SkillToolDef("plan_steps", "Decompose a complex task into ordered milestones"),
                ),
            ),
        )
    }
}
