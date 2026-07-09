package com.litsorbeklik.app.ui.navigation

object Routes {
    const val REGISTER = "register"
    const val LOGIN = "login"
    const val PROJECTS = "projects"
    const val SPEC = "spec/{projectId}"
    const val SETTINGS = "settings/{projectId}"

    fun spec(projectId: String) = "spec/$projectId"
    fun settings(projectId: String) = "settings/$projectId"
}
